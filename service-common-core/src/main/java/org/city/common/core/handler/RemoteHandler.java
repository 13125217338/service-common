package org.city.common.core.handler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.ConnectException;

import javax.validation.groups.Default;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.remote.RemoteMethodDto;
import org.city.common.api.exception.RemoteSpeedLimitException;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.parse.MethodNameParse;
import org.city.common.api.in.remote.RemoteInvokeApi;
import org.city.common.api.in.remote.RemoteSave.RemoteInfo;
import org.city.common.api.in.util.Validations;
import org.city.common.api.util.SpeedLimitUtil;
import org.springframework.validation.annotation.Validated;

/**
 * @作者 ChengShi
 * @日期 2022-09-28 08:57:09
 * @版本 1.0
 * @描述 远程调用处理
 */
public class RemoteHandler implements InvocationHandler,MethodNameParse,JSONParser,Validations {
	private final RemoteInfo remoteInfo;
	private final RemoteInvokeApi remoteInvokeApi;
	private final int failTimeout;
	public RemoteHandler(RemoteInfo remoteInfo, RemoteInvokeApi remoteInvokeApi, int failTimeout) {
		this.remoteInfo = remoteInfo; this.remoteInvokeApi = remoteInvokeApi; this.failTimeout = failTimeout;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (remoteInfo.getTurnOnTime() > System.currentTimeMillis()) {throw new ConnectException("远程服务不可用！");}
		return SpeedLimitUtil.limitExec(remoteInfo.getSpeedLimit(), method, () -> new RemoteSpeedLimitException("远程服务限流！"), () -> invoke(method, args == null ? new Object[0] : args));
	}
	/* 执行远程调用 */
	private Object invoke(Method method, Object[] args) throws Throwable {
		verifys(method.getParameters(), args); //验证参数
		/* 获取当前远程方法 */
		RemoteMethodDto remoteMethodDto = remoteInfo.getRemoteClassDto().getMethods().get(parse(method));
		if (remoteMethodDto == null) {return parse(null, method.getGenericReturnType());} //执行本地调用
		return exec(method, args, remoteInfo.getRemoteClassDto().getBeanName(), remoteMethodDto); //执行远程调用
	}
	/* 正常执行 */
	private Object exec(Method method, Object[] args, String beanName, RemoteMethodDto remoteMethodDto) throws Throwable {
		RemoteAdapter remoteAdapter = remoteInfo.getRemoteAdapter(); //当前远程适配器
		long recordTime = System.currentTimeMillis(); //记录当前时间
		try {return remoteInvokeApi.invoke(beanName, remoteMethodDto, remoteInfo.getRemoteIpPortDto(), args);} //远程调用
		/* 连接异常不可用标记 */
		catch (Throwable e) {if (CommonConstant.isConnectTimeout(e) || CommonConstant.isConnectTimeout(e.getCause())) {remoteInfo.setTurnOnTime(System.currentTimeMillis() + failTimeout);} throw e;}
		/* 回调执行时间与远程信息 */
		finally {if (remoteAdapter != null) {remoteAdapter.invokedInfo((int) (System.currentTimeMillis() - recordTime), remoteInfo);}}
	}
	/* 验证参数 */
	private void verifys(Parameter[] parameters, Object[] args) {
		for (int i = 0, j = parameters.length; i < j; i++) {
			Validated validated = parameters[i].getAnnotation(Validated.class);
			if (validated != null) {
				verify(args[i], validated.value().length == 0 ? new Class[] {Default.class} : validated.value());
			}
		}
	}
}
