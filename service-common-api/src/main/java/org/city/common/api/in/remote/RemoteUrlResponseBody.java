package org.city.common.api.in.remote;

import java.lang.reflect.Method;

import org.springframework.http.HttpHeaders;

/**
 * @作者 ChengShi
 * @日期 2023-04-06 16:18:01
 * @版本 1.0
 * @描述 远程调用响应
 */
public interface RemoteUrlResponseBody {
	/**
	 * @描述 处理响应
	 * @param method 原方法
	 * @param responseHeaders 响应头信息
	 * @param body 响应数据
	 * @return 响应结果
	 */
	public Object handler(Method method, HttpHeaders responseHeaders, String body);
}
