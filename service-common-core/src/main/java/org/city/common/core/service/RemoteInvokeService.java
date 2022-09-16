package org.city.common.core.service;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.dto.remote.RemoteDto;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.dto.remote.RemoteMethodDto;
import org.city.common.api.in.parse.MethodNameParse;
import org.city.common.api.in.parse.RestReponseParse;
import org.city.common.api.open.RemoteInvokeApi;
import org.city.common.api.util.AttributeUtil;
import org.city.common.api.util.HeaderUtil;
import org.city.common.api.util.MyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;

import lombok.Data;

/**
 * @作者 ChengShi
 * @日期 2022-07-27 15:18:20
 * @版本 1.0
 * @描述 远程执行服务实现
 */
@Service
public class RemoteInvokeService implements RemoteInvokeApi,RestReponseParse,MethodNameParse{
	@Autowired
	private RemoteConfigDto remoteConfigDto;
	@Value("${remote.path:/remote/path}/invoke")
	private String remotePath;
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private ApplicationContext applicationContext;
	/* 本地缓存 */
	private final Map<String, ObjectInfo> LOCAL_CACHE = new ConcurrentHashMap<>();
	
	@Override
	public Object invoke(RemoteMethodDto data, HttpHeaders headers, RemoteIpPortDto remoteIpPortDto, Object... args) {
		/* 设置json类型 */
		headers = setContentTypeJson(headers == null ? new HttpHeaders() : headers);
		/* 远程调用解析 */
		return parsePostRp("http://" + remoteIpPortDto.toString() + remotePath, 
				new HttpEntity<>(getRemoteDto(data, args), headers), data.$getReturnType(), restTemplate);
	}
	/* 获取远程调用参数 */
	private RemoteDto getRemoteDto(RemoteMethodDto remoteMethodDto, Object...args) {
		try {return RemoteDto.of(MyUtil.md5(remoteMethodDto.getBeanName() + remoteMethodDto.getName() + remoteConfigDto.getVerify()),
				remoteMethodDto, args);}
		catch (Exception e) {throw new RuntimeException("远程调用加密失败！", e);}
	}
	/* 设置json类型 */
	private HttpHeaders setContentTypeJson(HttpHeaders headers) {
		HttpHeaders header = HeaderUtil.getHeader();
		header.putAll(headers);
		/* 去除所有内容头 */
		header.remove(HttpHeaders.CONTENT_TYPE);
		header.remove(HttpHeaders.CONTENT_LENGTH);
		header.remove(HttpHeaders.CONTENT_DISPOSITION);
		header.remove(HttpHeaders.CONTENT_ENCODING);
		header.remove(HttpHeaders.CONTENT_LANGUAGE);
		header.remove(HttpHeaders.CONTENT_LOCATION);
		header.remove(HttpHeaders.CONTENT_RANGE);
		/* 添加json类型 */
		header.setContentType(MediaType.APPLICATION_JSON);
		/* 添加属性值传递 */
		Map<String, Object> attrs = AttributeUtil.getAttrs();
		if (attrs != null) {
			for (Entry<String, Object> entry : attrs.entrySet()) {
				header.set(AttributeUtil.ATTR_HEADER_PREFIX + entry.getKey(),
						JSONObject.toJSONString(entry.getValue(), SerializerFeature.WriteClassName));
			}
		}
		return header;
	}
	
	/**
	 * @描述 被调用的服务器
	 * @param remoteDto 远程参数
	 * @param request 请求对象
	 * @param response 响应对象
	 * @return 调用结果
	 */
	public Object $invoke(RemoteDto remoteDto, HttpServletRequest request, HttpServletResponse response) {
		if (!remoteConfigDto.isInvoke()) {throw new RuntimeErrorException(new Error("服务器未开启远程调用功能，执行失败！"));}
		RemoteMethodDto remoteMethodDto = remoteDto.getRemoteMethodDto();
		/* 加密验证 */
		String key = MyUtil.md5(remoteMethodDto.getBeanName() + remoteMethodDto.getName() + remoteConfigDto.getVerify());
		if (!key.equals(remoteDto.getVerify())) {throw new VerifyError("服务器Key认证失败，无法远程调用！");}
		
		/* 转换参数类型 */
		if (remoteDto.getArgs() != null) {
			Object[] args = remoteDto.getArgs();
			Type[] types = remoteMethodDto.$getParameterTypes();
			for (int i = 0, j = args.length; i < j; i++) {
				args[i] = parse(args[i], types[i]);
			}
		}
		
		/* 执行前 */
		headerToAttr(request);
		/* 本地执行 */
		Object result = localInvoke(remoteMethodDto.getBeanName(), remoteMethodDto.getName(), remoteMethodDto, remoteDto.getArgs());
		/* 执行后 */
		attrToHeader(request, response);
		return result;
	}
	/* 本地执行 */
	private Object localInvoke(String beanName, String methodName, RemoteMethodDto remoteMethodDto, Object[] args) {
		try {
			ObjectInfo objInfo = LOCAL_CACHE.get(beanName);
			if (objInfo == null) {
				Object bean = applicationContext.getBean(beanName);
				Assert.notNull(bean, String.format("Spring根据[%s]名称获取到空的实例对象！", beanName));
				/*生成对象信息*/
				objInfo = new ObjectInfo(bean, Arrays.asList(bean.getClass().getMethods()).stream()
						.collect(Collectors.toMap(k -> parse(k), v -> v, (k1, k2) -> k1)));
				LOCAL_CACHE.put(beanName, objInfo);
			}
			/* 执行方法 */
			return ReflectionUtils.invokeMethod(objInfo.getMethods().get(methodName), objInfo.getBean(), args);
		} catch (Exception e) {throw new RuntimeException("本地执行出现异常！" + e.getMessage(), e);}
	}
	
	/* 头转属性 */
	private void headerToAttr(HttpServletRequest request) {
		try {
			Enumeration<String> headerNames = request.getHeaderNames();
			while(headerNames.hasMoreElements()) {
				String name = headerNames.nextElement();
				if (name.indexOf(AttributeUtil.ATTR_HEADER_PREFIX) == 0) {
					request.setAttribute(name, JSON.parse(request.getHeader(name), Feature.SupportAutoType));
				}
			}
		} catch (Exception e) {/* 不处理 */}
	}
	/* 属性转头 */
	private void attrToHeader(HttpServletRequest request, HttpServletResponse response) {
		/* 执行后 */
		try {
			Enumeration<String> attributeNames = request.getAttributeNames();
			while(attributeNames.hasMoreElements()) {
				String name = attributeNames.nextElement();
				if (name.indexOf(AttributeUtil.ATTR_HEADER_PREFIX) == 0) {
					response.addHeader(name, JSONObject.toJSONString(request.getAttribute(name), SerializerFeature.WriteClassName));
				}
			}
		} catch (Exception e) {/* 不处理 */}
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2022-09-27 16:54:33
	 * @版本 1.0
	 * @parentClass RemoteInvokeService
	 * @描述 对象信息
	 */
	@Data
	private class ObjectInfo {
		/* 实例对象 */
		private final Object bean;
		/* 执行方法 */
		private final Map<String, Method> methods;
	}
}
