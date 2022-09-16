package org.city.common.core.handler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.in.remote.RemoteSave.RemoteInfo;
import org.city.common.api.util.PlugUtil;
import org.city.common.api.util.SpringUtil;

/**
 * @作者 ChengShi
 * @日期 2022-10-07 11:17:50
 * @版本 1.0
 * @描述 远程代理处理
 */
public class RemoteProxyHandler implements InvocationHandler {
	private final Class<?> orginClass;
	private final Class<? extends RemoteAdapter> remoteAdapterCls;
	private RemoteAdapter remoteAdapter;
	public RemoteProxyHandler(Class<?> orginClass, Class<? extends RemoteAdapter> remoteAdapterCls) {
		this.orginClass = orginClass; this.remoteAdapterCls = remoteAdapterCls;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return invoke(method, args == null ? new Object[0] : args);
	}
	/* 执行方法 */
	private Object invoke(Method method, Object[] args) throws Throwable {
		RemoteInfo remoteInfo = PlugUtil.getRemote(null, getAdapter(), orginClass);
		/* 执行原方法调用 */
		try {return method.invoke(remoteInfo.getBean(), args);}
		/* 如果当前远程调用被熔断 - 则重新调用其他远程 */
		catch (Throwable e) {if (remoteInfo.getTurnOnTime() > System.currentTimeMillis()) {return invoke(method, args);} else {throw e;}}
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
}
