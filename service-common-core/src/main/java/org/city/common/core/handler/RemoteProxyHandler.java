package org.city.common.core.handler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.validation.groups.Default;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.dto.remote.RemoteMethodDto;
import org.city.common.api.dto.remote.RemoteParameterDto;
import org.city.common.api.in.remote.RemoteSave.RemoteInfo;
import org.city.common.api.in.util.Validations;
import org.city.common.api.util.PlugUtil;
import org.city.common.api.util.SpringUtil;
import org.springframework.validation.annotation.Validated;

/**
 * @作者 ChengShi
 * @日期 2022-10-07 11:17:50
 * @版本 1.0
 * @描述 远程代理处理
 */
public class RemoteProxyHandler implements InvocationHandler,Validations {
	private final Class<?> orginClass;
	private final Class<? extends RemoteAdapter> remoteAdapterCls;
	private RemoteAdapter remoteAdapter;
	public RemoteProxyHandler(Class<?> orginClass, Class<? extends RemoteAdapter> remoteAdapterCls) {
		this.orginClass = orginClass; this.remoteAdapterCls = remoteAdapterCls;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		args = args == null ? new Object[0] : args;
		return invoke(method, args, true);
	}
	/* 执行方法 */
	private Object invoke(Method method, Object[] args, boolean isVerify) throws Throwable {
		RemoteInfo remoteInfo = PlugUtil.getRemote(null, getAdapter(), orginClass);
		/* 验证参数 */
		if (isVerify) {verifys(remoteInfo.getRemoteClassDto().getMethods().get(method.getName()), method.toString(), args);}
		/* 执行原方法调用 */
		try {return method.invoke(remoteInfo.getBean(), args);}
		/* 如果当前远程调用被熔断 - 则重新调用其他远程 */
		catch (Throwable e) {if (remoteInfo.getTurnOnTime() > System.currentTimeMillis()) {return invoke(method, args, false);} else {throw e;}}
	}
	/* 获取远程适配实例 - 懒加载 */
	private RemoteAdapter getAdapter() {
		if (remoteAdapter == null) {
			synchronized (remoteAdapterCls) {
				if (remoteAdapter == null) {
					remoteAdapter = SpringUtil.getBean(remoteAdapterCls);
				}
			}
		}
		return remoteAdapter;
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
}
