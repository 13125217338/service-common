package org.city.common.core.handler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.Response;
import org.city.common.api.exception.ResponseException;
import org.city.common.api.in.parse.RestReponseParse;
import org.city.common.api.util.HeaderUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @作者 ChengShi
 * @日期 2022-06-24 13:47:41
 * @版本 1.0
 * @描述 全局扩展点拦截
 */
public class GlobalExtensionHandler implements InvocationHandler,RestReponseParse{
	private final RemoteParam remoteParam;
	private final RestTemplate restTemplate;
	
	public GlobalExtensionHandler(RemoteParam remoteParam, RestTemplate restTemplate) {
		this.remoteParam = remoteParam; this.restTemplate = restTemplate;
	}

	@Override
	public Object invoke(Object paramObject, Method paramMethod, Object[] paramArrayOfObject) throws Throwable {
		/* 类地址路径 */
		String prefix = remoteParam.getPrifixPath();
		if (prefix == null) {throw new IllegalArgumentException(String.format("扩展类未有前缀路径参数，无法远程调用，对应方法[%s]！", paramMethod.getName()));}
		/* 类方法地址路径 */
		String path = remoteParam.getMethodPath().get(paramMethod.getName());
		if (path == null) {throw new IllegalArgumentException(String.format("扩展类方法未有前缀路径参数，无法远程调用，对应方法[%s]！", paramMethod.getName()));}
		/* 类方法参数名称 */
		String[] paramNames = remoteParam.getMethodParamNames().get(paramMethod.getName());
		if (paramNames.length != paramArrayOfObject.length) {
			throw new IllegalArgumentException(String.format("扩展类方法参数名称个数[%d]与实际参数个数[%d]不一致！", paramNames.length, paramArrayOfObject.length));
		}
		/*封装参数*/
		JSONObject data = new JSONObject();
		for (int i = 0, j = paramArrayOfObject.length; i < j; i++) {
			data.put(paramNames[i], paramArrayOfObject[i]);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.add(CommonConstant.REMOTE_IP_PORT_HEADER, remoteParam.getIpPort());
		/*取出当前请求所有头信息并设置*/
		Map<String, List<String>> headerVal = HeaderUtil.getHeader();
		for (Entry<String, List<String>> entry : headerVal.entrySet()) {
			headers.addAll(entry.getKey(), entry.getValue());
		}
		
		/*远程调用*/
		ResponseEntity<Object> responseEntity = restTemplate.postForEntity(prefix + path, new HttpEntity<JSONObject>(data, headers), Object.class);
		/* 如果调用不成功直接抛出异常 */
		if (HttpStatus.OK != responseEntity.getStatusCode()) {
			throw ResponseException.of(Response.error(responseEntity.getStatusCodeValue(),
					responseEntity.getBody(), responseEntity.getStatusCode().getReasonPhrase()));
		}
		
		/* 正常内容解析 */
		return parseRp(responseEntity.getBody(), paramMethod.getGenericReturnType());
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2022年7月23日
	 * @版本 1.0
	 * @描述 远程参数
	 */
	@Data
	@AllArgsConstructor
	public static class RemoteParam {
		/* 当前服务地址和端口 */
		private final String ipPort;
		/* 前缀地址 */
		private final String prifixPath;
		/* 方法地址 */
		private final Map<String, String> methodPath;
		/* 方法参数名称 */
		private final Map<String, String[]> methodParamNames;
	}
}
