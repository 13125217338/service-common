package org.city.common.api.exception;

import java.lang.reflect.Method;

import lombok.Getter;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 19:00:32
 * @版本 1.0
 * @描述 远程方法调用限速异常
 */
@Getter
public class RemoteMethodSpeedLimitException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private final String address;
	private final Method method;
	
	public RemoteMethodSpeedLimitException(String address, Method method) {
		super(String.format("接口方法[%s.%s]对应服务[%s]限流！", method.getDeclaringClass().getSimpleName(), method.getName(), address));
		this.address = address; this.method = method;
	}
}
