package org.city.common.api.exception;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.annotation.plug.Remote;
import org.springframework.util.StringUtils;

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
	private final String methodName;
	
	public ServiceNotFoundException(Object param, RemoteAdapter remoteAdapter, Class<?> interfaceCls) {
		super(String.format("接口[%s]对应服务未找到！", getInterfaceName(interfaceCls)));
		this.param = param; this.remoteAdapter = remoteAdapter; this.interfaceCls = interfaceCls; this.methodName = null;
	}
	
	public ServiceNotFoundException(Object param, RemoteAdapter remoteAdapter, String methodName) {
		super(String.format("方法[%s]对应服务未找到！", methodName));
		this.param = param; this.remoteAdapter = remoteAdapter; this.interfaceCls = null; this.methodName = methodName;
	}
	
	/* 获取接口名称 */
	private static String getInterfaceName(Class<?> interfaceCls) {
		Remote remote = interfaceCls.getDeclaredAnnotation(Remote.class);
		return remote == null || !StringUtils.hasText(remote.remark()) ? interfaceCls.getSimpleName() : remote.remark();
	}
}
