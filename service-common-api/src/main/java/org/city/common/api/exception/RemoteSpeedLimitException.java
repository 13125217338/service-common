package org.city.common.api.exception;

import java.lang.reflect.Method;

import lombok.Getter;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 19:00:32
 * @版本 1.0
 * @描述 远程调用限速异常
 */
@Getter
public class RemoteSpeedLimitException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private final String address;
	private final Method method;
	
	public RemoteSpeedLimitException(String address, Method method) {
		super(String.format("远程服务[%s.%s]限流！", method.getDeclaringClass().getSimpleName(), method.getName()));
		this.address = address; this.method = method;
	}
}
