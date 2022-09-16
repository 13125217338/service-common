package org.city.common.api.in.remote;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.http.HttpHeaders;

/**
 * @作者 ChengShi
 * @日期 2023-04-06 16:12:08
 * @版本 1.0
 * @描述 远程调用头
 */
public interface RemoteUrlHeaders {
	/**
	 * @描述 获取头信息
	 * @param method 原方法
	 * @param uriVariables 地址参数（获取所有@PathVariable注解入参值）
	 * @return 头信息
	 */
	public HttpHeaders getHeaders(Method method, Map<String, Object> uriVariables);
}
