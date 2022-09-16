package org.city.common.core.service;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.RuntimeErrorException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.Response;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.dto.remote.RemoteDto;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.dto.remote.RemoteMethodDto;
import org.city.common.api.dto.remote.RemoteMethodDto.ReturnResult;
import org.city.common.api.in.Task;
import org.city.common.api.in.parse.MethodNameParse;
import org.city.common.api.in.parse.RestReponseParse;
import org.city.common.api.in.remote.RemoteInvokeApi;
import org.city.common.api.util.AttributeUtil;
import org.city.common.api.util.HeaderUtil;
import org.city.common.api.util.MyUtil;
import org.city.common.api.util.SpringUtil;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;

/**
 * @作者 ChengShi
 * @日期 2022-07-27 15:18:20
 * @版本 1.0
 * @描述 远程执行服务实现
 */
@Service
public class RemoteInvokeService implements RemoteInvokeApi,RestReponseParse,MethodNameParse {
	/* 远程执行地址 */
	private final String remotePath = "/remote/path/invoke";
	@Autowired
	private RemoteConfigDto remoteConfigDto;
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private Task task;
	/* 本地缓存 */
	private final Map<String, ObjectInfo> LOCAL_CACHE = new ConcurrentHashMap<>();
	
	@Override
	public Object invoke(String beanName, RemoteMethodDto data, RemoteIpPortDto remoteIpPortDto, Object... args) throws Throwable {
		String tranId = RemoteAdapter.REMOTE_TRANSACTIONAL.get();
		HttpHeaders header = setContentTypeJson();
		
		if (tranId == null) {return postRp(header, beanName, data, remoteIpPortDto, args);}
		else { //使用子线程执行事务远程调用
			return task.putTaskSys(tranId, remoteConfigDto.getReadTimeout(), () -> {
				header.set(CommonConstant.REMOTE_TRANSACTIONAL_HEADER_NAME, tranId); //添加分布式事务ID头
				return postRp(header, beanName, data, remoteIpPortDto, args);
			}, MasterParam -> $localInvoke((Param) MasterParam));
		}
	}
	/* 远程执行 */
	private Object postRp(HttpHeaders header, String beanName, RemoteMethodDto data, RemoteIpPortDto remoteIpPortDto, Object... args) {
		HttpEntity<RemoteDto> httpEntity = new HttpEntity<>(getRemoteDto(beanName, data, args), header);
		return parsePostRp("http://" + remoteIpPortDto.toString() + remotePath, httpEntity, restTemplate);
	}
	/* 获取远程调用参数 */
	private RemoteDto getRemoteDto(String beanName, RemoteMethodDto remoteMethodDto, Object...args) {
		try {
			String md5 = MyUtil.md5(remoteMethodDto.getClassName() + remoteMethodDto.getName() + remoteConfigDto.getVerify());
			return RemoteDto.of(md5, beanName, remoteMethodDto, args);
		} catch (Exception e) {throw new RuntimeException("远程调用加密失败！", e);}
	}
	/* 设置json类型 */
	private HttpHeaders setContentTypeJson() {
		HttpHeaders header = HeaderUtil.getHeader();
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
				header.set(AttributeUtil.ATTR_HEADER_PREFIX + entry.getKey(), JSONObject.toJSONString(entry.getValue()));
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
	 * @throws Throwable 
	 */
	public Response $invoke(RemoteDto remoteDto, HttpServletRequest request, HttpServletResponse response) throws Throwable {
		Param param = new Param(remoteDto, request, response);
		String tranId = HeaderUtil.getHeaderVal(CommonConstant.REMOTE_TRANSACTIONAL_HEADER_NAME);
		return task.runMaster(tranId, remoteConfigDto.getReadTimeout(), param, () -> $localInvoke(param));
	}
	/* 本地执行 */
	private Response $localInvoke(Param param) {
		if (!remoteConfigDto.isInvoke()) {throw new RuntimeErrorException(new Error("服务器未开启远程调用功能，执行失败！"));}
		RemoteMethodDto remoteMethodDto = param.remoteDto.getRemoteMethodDto();
		/* 加密验证 */
		String key = MyUtil.md5(remoteMethodDto.getClassName()+ remoteMethodDto.getName() + remoteConfigDto.getVerify());
		if (!key.equals(param.remoteDto.getVerify())) {throw new VerifyError("服务器Key认证失败，无法远程调用！");}
		
		/* 执行前 */
		headerToAttr(param.request);
		/* 本地执行 */
		Response result = localInvoke(param.remoteDto.getBeanName(), remoteMethodDto.getName(), remoteMethodDto, param.remoteDto.getArgs());
		/* 执行后 */
		attrToHeader(param.request, param.response);
		return result;
	}
	/* 本地执行 */
	private Response localInvoke(String beanName, String methodName, RemoteMethodDto remoteMethodDto, Object[] args) {
		ObjectInfo objInfo = LOCAL_CACHE.get(beanName);
		if (objInfo == null) {
			Object bean = SpringUtil.getBean(beanName);
			Assert.notNull(bean, String.format("Spring根据[%s]名称获取到空的实例对象！", beanName));
			
			/* 生成方法信息 */
			Class<?> targetClass = ClassUtils.getUserClass(AopProxyUtils.ultimateTargetClass(bean));
			Map<String, Entry<Method, Type>> methods = new HashMap<>();
			for (Method method : targetClass.getMethods()) { //只处理公开方法 - 包括继承的公开方法
				methods.computeIfAbsent(parse(method), k -> new Entry<Method, Type>() {
					private final Type returnType = getReturnType(targetClass, method);
					@Override
					public Method getKey() {return method;}
					@Override
					public Type getValue() {return returnType;}
					@Override
					public Type setValue(Type value) {return null;}
				});
			}
			
			/*生成对象信息*/
			objInfo = new ObjectInfo(bean, methods);
			LOCAL_CACHE.put(beanName, objInfo);
		}
		
		/* 转换参数类型 */
		Entry<Method, Type> entry = objInfo.methods.get(methodName);
		if (args != null) {
			Type[] types = entry.getKey().getGenericParameterTypes();
			for (int i = 0, j = args.length; i < j; i++) {
				args[i] = parse(args[i], types[i]);
			}
		}
		
		/* 执行方法 */
		Object returnValue = ReflectionUtils.invokeMethod(entry.getKey(), objInfo.bean, args);
		return Response.ok(new ReturnResult().setReturnValue(returnValue).$setReturnType(entry.getValue()));
	}
	
	/* 头转属性 */
	private void headerToAttr(HttpServletRequest request) {
		try {
			Enumeration<String> headerNames = request.getHeaderNames();
			while(headerNames.hasMoreElements()) {
				String name = headerNames.nextElement();
				if (name.indexOf(AttributeUtil.ATTR_HEADER_PREFIX) == 0) {
					request.setAttribute(name, JSON.parse(request.getHeader(name)));
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
					response.addHeader(name, JSONObject.toJSONString(request.getAttribute(name)));
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
	@AllArgsConstructor
	private class ObjectInfo {
		/* 实例对象 */
		private final Object bean;
		/* 执行方法 */
		private final Map<String, Entry<Method, Type>> methods;
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2024年3月1日
	 * @版本 1.0
	 * @描述 参数
	 */
	@AllArgsConstructor
	private class Param {
		/* 远程对象 */
		private final RemoteDto remoteDto;
		/* 请求对象 */
		private final HttpServletRequest request;
		/* 响应对象 */
		private final HttpServletResponse response;
	}
}
