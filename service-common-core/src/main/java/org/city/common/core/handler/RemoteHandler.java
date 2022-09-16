package org.city.common.core.handler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.ConnectException;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.adapter.RemoteSaveAdapter.RemoteInfo;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.remote.RemoteMethodDto;
import org.city.common.api.dto.remote.RemoteTransactionalDto;
import org.city.common.api.in.Runnable;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.parse.MethodNameParse;
import org.city.common.api.open.RemoteInvokeApi;
import org.springframework.web.client.ResourceAccessException;

/**
 * @作者 ChengShi
 * @日期 2022-09-28 08:57:09
 * @版本 1.0
 * @描述 远程调用处理
 */
public class RemoteHandler implements InvocationHandler,MethodNameParse,JSONParser{
	private final RemoteInfo remoteInfo;
	private final RemoteInvokeApi remoteInvokeApi;
	private final RemoteTransactionalHandler handler;
	public RemoteHandler(RemoteInfo remoteInfo, RemoteInvokeApi remoteInvokeApi, RemoteTransactionalHandler handler) {
		this.remoteInfo = remoteInfo; this.remoteInvokeApi = remoteInvokeApi; this.handler = handler;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		/* 获取当前远程方法 */
		RemoteMethodDto remoteMethodDto = remoteInfo.getRemoteClassDto().getMethods().get(parse(method));
		if (remoteMethodDto == null) {return parse(null, method.getGenericReturnType());}
		
		String tId = RemoteAdapter.REMOTE_TRANSACTIONAL.get();
		/* 如果没有头直接执行 - 否则分布式事务异步执行 */
		if (tId == null) {return nomal(proxy, method, args, remoteMethodDto);}
		else {
			/* 添加远程事务 */
			remoteInfo.getHeaders().set(CommonConstant.REMOTE_TRANSACTIONAL_HEADER_NAME, tId);
			RemoteTransactionalDto dto = handler.getRemoteTransactionalDto(tId);
			return handler.handler(dto, new Runnable() {
				@Override
				public void run() throws Throwable {
					try {dto.setReturnVal(nomal(proxy, method, args, remoteMethodDto));}
					catch (Throwable e) {dto.setReturnVal(e);} finally {synchronized (dto) {dto.setEnd(true); dto.notifyAll();}}
				}
			});
		}
	}
	/* 正常执行 */
	private Object nomal(Object proxy, Method method, Object[] args, RemoteMethodDto remoteMethodDto) {
		try {return remoteInvokeApi.invoke(remoteMethodDto, remoteInfo.getHeaders(), remoteInfo.getRemoteIpPortDto(), args);}
		/* 连接异常不可用标记 */
		catch (ResourceAccessException e) {if (e.getCause() instanceof ConnectException) {remoteInfo.setDisable(true);} throw e;}
	}
}
