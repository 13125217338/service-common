package org.city.common.core.service;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig.Configurator;
import javax.websocket.ContainerProvider;
import javax.websocket.OnMessage;
import javax.websocket.Session;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.dto.remote.RemoteDto;
import org.city.common.api.dto.remote.RemoteDto.Result;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.dto.remote.RemoteMethodDto;
import org.city.common.api.exception.ResponseException;
import org.city.common.api.in.Task;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.remote.RemoteInvokeApi;
import org.city.common.api.util.HeaderUtil;
import org.city.common.api.util.SpringUtil;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * @作者 ChengShi
 * @日期 2022-07-27 15:18:20
 * @版本 1.0
 * @描述 远程执行服务实现
 */
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@ClientEndpoint(configurator = RemoteInvokeService.class)
public class RemoteInvokeService extends Configurator implements RemoteInvokeApi,JSONParser {
	private final static Map<String, Session> REMOTE_CACHE = new ConcurrentHashMap<>();
	private final static RemoteIpPortDto LOCAL_IP_PORT = SpringUtil.getBean(RemoteIpPortDto.class);
	private final static RemoteConfigDto REMOTE_CONFIG_DTO = SpringUtil.getBean(RemoteConfigDto.class);
	private final static CommonInfoService COMMON_INFO_SERVICE = SpringUtil.getBean(CommonInfoService.class);
	private final static ThreadLocal<String> REMOTE_IP_PORT = new ThreadLocal<>();
	final static Task TASK = new TaskService(REMOTE_CONFIG_DTO);
	
	@Override
	public void beforeRequest(Map<String, List<String>> headers) {
		String remoteAuthValue = COMMON_INFO_SERVICE.getRemoteAuthValue(REMOTE_IP_PORT.get());
		headers.put(CommonConstant.AUTHORIZATION.toLowerCase(), Arrays.asList(remoteAuthValue));
		headers.put(LocalInvokeService.APP_ADDRESS, Arrays.asList(LOCAL_IP_PORT.toString()));
	}
	
	@OnMessage(maxMessageSize = 1024 * 1024)
	public void onMessage(byte[] body, Session session) {
		try {
			Result result = JSONObject.parseObject(body, Result.class);
			Object requestId = session.getUserProperties().get(result.getRequestId());
			session.getUserProperties().put(result.getRequestId(), result);
			synchronized (requestId) {requestId.notifyAll();}
		} catch (Throwable e) {}
	}
	
	@Override
	public Object invoke(String beanName, RemoteMethodDto remoteMethod, RemoteIpPortDto remoteIpPort, Object... args) throws Throwable {
		Session session = REMOTE_CACHE.computeIfAbsent(remoteIpPort.toString(), (k) -> getSession(k));
		if (!session.isOpen()) {REMOTE_CACHE.remove(remoteIpPort.toString()); return invoke(beanName, remoteMethod, remoteIpPort, args);}
		final String requestId = System.currentTimeMillis() + "-" + Thread.currentThread().getId();
		final String tranId = RemoteAdapter.REMOTE_TRANSACTIONAL.get();
		final Map<String, String> headers = HeaderUtil.get();
		
		try {
			/* 添加请求信息 - 远程执行 */
			session.getUserProperties().put(requestId, requestId);
			byte[] invokeData = JSON.toJSONBytes(RemoteDto.of(requestId, tranId, headers, beanName, remoteMethod, args));
			synchronized (session) {session.getBasicRemote().sendBinary(ByteBuffer.wrap(invokeData));}
			
			/* 根据事务ID决定 - 同步或者异步等待执行 */
			if (tranId == null) {return syncExec(session, requestId, remoteIpPort, remoteMethod);}
			else {return asyncExec(tranId, session, requestId, remoteIpPort, remoteMethod);}
		} finally {session.getUserProperties().remove(requestId);}
	}
	
	/* 同步等待执行 */
	private Object syncExec(Session session, String requestId, RemoteIpPortDto remoteIpPort, RemoteMethodDto remoteMethod) throws Throwable {
		/* 同步等待 */
		Object syncId = session.getUserProperties().get(requestId);
		synchronized (syncId) {syncId.wait(REMOTE_CONFIG_DTO.getReadTimeout());}
		/* 解析响应结果 */
		Object result = session.getUserProperties().get(requestId);
		return parseResponse(Result.class.isInstance(result) ? (Result) result : null, remoteIpPort, remoteMethod);
	}
	/* 异步等待执行 */
	private Object asyncExec(String tranId, Session session, String requestId, RemoteIpPortDto remoteIpPort, RemoteMethodDto remoteMethod) throws Throwable {
		return TASK.putTaskSys(tranId, REMOTE_CONFIG_DTO.getReadTimeout(), () -> { //异步子线程执行 - 解析响应结果
			try {return syncExec(session, requestId, remoteIpPort, remoteMethod);}
			catch (Throwable e) {throw new RuntimeException(e.getMessage(), e);}
		}, (mp) -> { //当前主线程执行 - 回调执行原逻辑
			if (mp instanceof Supplier) {return ((Supplier<?>) mp).get();}
			else {throw new RuntimeException("主线程参数非[Supplier]对象！");}
		});
	}
	/* 解析返回值 */
	private Object parseResponse(Result result, RemoteIpPortDto remoteIpPort, RemoteMethodDto remoteMethod) {
		/* 验证结果 */
		Assert.notNull(result, String.format("远程服务[%s]调用方法[%s]超时！", remoteIpPort, remoteMethod.getName()));
		Assert.notNull(result.getResponse(), String.format("远程服务[%s]调用方法[%s]无返回值！", remoteIpPort, remoteMethod.getName()));
		Assert.notNull(result.getReturnType(), String.format("远程服务[%s]调用方法[%s]无返回值类型！", remoteIpPort, remoteMethod.getName()));
		
		/* 成功 */
		if (result.getResponse().getCode() == HttpStatus.OK.value()) {
			return parse(result.getResponse().getData(), result.$getReturnType());
		}
		/* 抛出异常 */
		throw new ResponseException(result.getResponse(), remoteIpPort);
	}
	/* 获取会话 */
	private Session getSession(String remoteIpPort) {
		try {
			REMOTE_IP_PORT.set(remoteIpPort);
			URI path = URI.create(String.format("ws://%s%s", remoteIpPort, LocalInvokeService.WS_URI));
			return ContainerProvider.getWebSocketContainer().connectToServer(this, path);
		} catch (Throwable e) {throw new RuntimeException("获取远程服务会话异常！", e);}
	}
}
