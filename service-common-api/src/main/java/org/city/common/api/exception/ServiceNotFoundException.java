package org.city.common.api.exception;

import org.city.common.api.adapter.RemoteAdapter;

import lombok.Getter;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 17:00:32
 * @版本 1.0
 * @描述 服务未找到异常
 */
@Getter
public class ServiceNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private final Object param;
	private final RemoteAdapter remoteAdapter;
	private final Class<?> interfaceCls;
	
	public ServiceNotFoundException(Object param, RemoteAdapter remoteAdapter, Class<?> interfaceCls) {
		super(String.format("接口[%s]对应服务未找到！", interfaceCls.getSimpleName()));
		this.param = param; this.remoteAdapter = remoteAdapter; this.interfaceCls = interfaceCls;
	}
}
