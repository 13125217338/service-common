package org.city.common.api.in.remote;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.http.HttpHeaders;

/**
 * @作者 ChengShi
 * @日期 2023-04-06 16:15:08
 * @版本 1.0
 * @描述 远程调用请求体
 */
public interface RemoteUrlRequestBody {
	/**
	 * @描述 获取请求体
	 * @param method 原方法
	 * @param requestHeaders 请求头信息
	 * @param args 原方法入参
	 * @param uriVariables 地址参数（获取所有@PathVariable注解入参值）
	 * @return 请求体
	 */
	public Object getBody(Method method, HttpHeaders requestHeaders, Object[] args, Map<String, Object> uriVariables);
}
