package org.city.common.api.in.remote;

import java.lang.reflect.Method;

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
	 * @return 头信息
	 */
	public HttpHeaders getHeaders(Method method);
}
