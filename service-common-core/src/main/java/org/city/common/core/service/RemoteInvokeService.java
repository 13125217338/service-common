package org.city.common.core.service;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.remote.RemoteInvokeApi;
import org.city.common.api.util.HeaderUtil;
import org.city.common.api.util.MyUtil;
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
	private final static RemoteConfigDto REMOTE_CONFIG_DTO = SpringUtil.getBean(RemoteConfigDto.class);
	private final static RemoteIpPortDto LOCAL_IP_PORT = SpringUtil.getBean(RemoteIpPortDto.class);
	
	@Override
	public void beforeRequest(Map<String, List<String>> headers) {
		headers.put(CommonConstant.AUTHORIZATION.toLowerCase(), Arrays.asList(MyUtil.sha256(REMOTE_CONFIG_DTO.getVerify())));
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
			/* 远程执行 */
			session.getUserProperties().put(requestId, requestId);
			byte[] invokeData = JSON.toJSONBytes(RemoteDto.of(requestId, tranId, headers, beanName, remoteMethod, args));
			synchronized (session) {session.getBasicRemote().sendBinary(ByteBuffer.wrap(invokeData));}
			synchronized (requestId) {requestId.wait(REMOTE_CONFIG_DTO.getReadTimeout());}
			
			/* 解析响应结果 */
			Object result = session.getUserProperties().get(requestId);
			return parseResponse(Result.class.isInstance(result) ? (Result) result : null, remoteIpPort, remoteMethod);
		} finally {session.getUserProperties().remove(requestId);}
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
			/* 连接并返回 */
			URI path = URI.create(String.format("ws://%s%s", remoteIpPort, LocalInvokeService.WS_URI));
			return ContainerProvider.getWebSocketContainer().connectToServer(this, path);
		} catch (Exception e) {throw new RuntimeException(e);}
	}
}
