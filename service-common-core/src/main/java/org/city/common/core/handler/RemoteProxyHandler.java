package org.city.common.core.handler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.constant.group.Default;
import org.city.common.api.dto.remote.RemoteMethodDto;
import org.city.common.api.dto.remote.RemoteParameterDto;
import org.city.common.api.in.remote.RemoteSave.RemoteInfo;
import org.city.common.api.in.util.Validations;
import org.city.common.api.util.PlugUtil;
import org.city.common.api.util.SpringUtil;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

/**
 * @作者 ChengShi
 * @日期 2022-10-07 11:17:50
 * @版本 1.0
 * @描述 远程代理处理
 */
public class RemoteProxyHandler implements InvocationHandler,Validations {
	private final RemoteProxyInfo remoteProxyInfo;
	private RemoteAdapter remoteAdapter;
	public RemoteProxyHandler(RemoteProxyInfo remoteProxyInfo) {this.remoteProxyInfo = remoteProxyInfo;}
	/* 获取远程适配实例 */
	private RemoteAdapter getAdapter() {
		if (remoteAdapter == null) {this.remoteAdapter = SpringUtil.getBean(remoteProxyInfo.remoteAdapter);}
		return this.remoteAdapter;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {return invoke(method, args, true);}
	/* 执行方法 */
	private Object invoke(Method method, Object[] args, boolean isVerify) throws Throwable {
		RemoteInfo remoteInfo = PlugUtil.getRemote(null, getAdapter(), remoteProxyInfo.interfaceCls);
		/* 验证参数 */
		if (isVerify) {verifys(remoteInfo.getRemoteClassDto().getMethods().get(method.getName()), method.toString(), args);}
		/* 执行原方法调用 */
		try {return method.invoke(remoteInfo.getBean(), args);}
		/* 如果当前远程调用被熔断 - 则重新调用其他远程 */
		catch (Throwable e) {if (remoteInfo.getTurnOnTime() > System.currentTimeMillis()) {return invoke(method, args, false);} else {throw e;}}
	}
	/* 验证参数 */
	private void verifys(RemoteMethodDto remoteMethodDto, String methodName, Object...args) {
		if (remoteMethodDto == null) {return;}
		int index = 0;
		for (RemoteParameterDto parameter : remoteMethodDto.getParameter().values()) {
			Validated validated = parameter.getAnnotation(Validated.class);
			if (validated != null) {
				verify(args[index++], validated.value().length == 0 ? new Class[] {Default.class} : validated.value());
			}
		}
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2022-10-07 11:20:31
	 * @版本 1.0
	 * @parentClass RemoteProxyHandler
	 * @描述 远程代理信息类
	 */
	@Data
	public static class RemoteProxyInfo {
		/* 接口类 */
		private final Class<?> interfaceCls;
		/* 远程适配器 */
		private final Class<? extends RemoteAdapter> remoteAdapter;
	}
}
