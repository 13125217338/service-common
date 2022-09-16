package org.city.common.api.remote.response;

import java.lang.reflect.Method;

import org.city.common.api.dto.Response;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.remote.RemoteUrlResponseBody;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2023-04-07 18:14:21
 * @版本 1.0
 * @描述 公共响应体
 */
@Slf4j
@Component
public class CommonResponseBody implements RemoteUrlResponseBody,JSONParser {
	
	@Override
	public Object handler(Method method, HttpHeaders responseHeaders, String body) {
		Response<?> response = parse(body, Response.class); //统一响应消息
		switch (response.getCode()) { //根据Code响应数据
			case 200: return parse(response.getData(), method.getGenericReturnType()); //正常返回
			default: log.error("远程请求方法[{}]响应异常，消息》》》 {}", method.getName(), response.getMsg());
					 throw new RuntimeException(response.getMsg()); //原错误信息
		}
	}
}
