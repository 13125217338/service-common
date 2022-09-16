package org.city.common.core.service;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.websocket.HandshakeResponse;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.ExceptionDto;
import org.city.common.api.dto.GlobalExceptionDto;
import org.city.common.api.dto.Response;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.dto.remote.RemoteDto;
import org.city.common.api.dto.remote.RemoteDto.Result;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.in.Runnable;
import org.city.common.api.in.Task;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.parse.MethodNameParse;
import org.city.common.api.in.redis.RedisMake;
import org.city.common.api.in.util.ThrowableMessage;
import org.city.common.api.util.HeaderUtil;
import org.city.common.api.util.SpringUtil;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2022-07-27 15:18:20
 * @版本 1.0
 * @描述 本地执行服务实现
 */
@Slf4j
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@ServerEndpoint(value = LocalInvokeService.WS_URI, configurator = LocalInvokeService.class)
public class LocalInvokeService extends Configurator implements JSONParser,MethodNameParse,ThrowableMessage {
	public final static String WS_URI = "/remote/path/invoke", APP_ADDRESS = "app-address";
	private final static RemoteIpPortDto LOCAL_IP_PORT = SpringUtil.getBean(RemoteIpPortDto.class);
	private final static LocalInvokeService LOCAL_INVOKE_SERVICE = SpringUtil.getBean(LocalInvokeService.class);
	private final static Map<String, ObjectInfo> LOCAL_CACHE = new ConcurrentHashMap<>();
	private final static RemoteConfigDto REMOTE_CONFIG_DTO = SpringUtil.getBean(RemoteConfigDto.class);
	private final static RedisMake REDIS_MAKE = SpringUtil.getBean(RedisMake.class);
	private final static Task TASK = new TaskService(REMOTE_CONFIG_DTO);
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
		return (T) LOCAL_INVOKE_SERVICE; //使用单例运行
	}
	
	@Override
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
		Map<String, Object> userProperties = sec.getUserProperties();
		for (Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
			userProperties.put(entry.getKey(), entry.getValue().iterator().next());
		}
	}
	
	@OnOpen
	public void onOpen(Session session) {
		Assert.isTrue(REMOTE_CONFIG_DTO.isInvoke(), "当前服务未开启远程调用功能，执行失败！");
		Object auth = session.getUserProperties().get(CommonConstant.AUTHORIZATION.toLowerCase());
		Assert.isTrue(CommonConstant.REMOTE_AUTH_VALUE.equals(auth), "当前服务认证失败，无法远程调用！");
	}
	
	@OnError
	public void onError(Throwable throwable, Session session) {
		String appAddress = String.valueOf(session.getUserProperties().get(APP_ADDRESS));
		log.error(String.format("远程服务[%s]发生异常》》》 %s", appAddress, throwable.getMessage()));
	}
	
	@OnMessage(maxMessageSize = 1024 * 1024)
	public void onMessage(byte[] body, Session session) {
		TASK.putTask(new Runnable() {
			private final String appAddress = String.valueOf(session.getUserProperties().get(APP_ADDRESS));
			private final RemoteDto remote = JSONObject.parseObject(body, RemoteDto.class);
			
			@Override
			public void run() throws Throwable {
				try {
					/* 设置远程头信息与事务ID */
					HeaderUtil.set(remote.getHeaders()); RemoteAdapter.setTranId(REDIS_MAKE, LOCAL_IP_PORT, REMOTE_CONFIG_DTO, remote.getTranId());
					Supplier<Result> supplier = new Supplier<Result>() {
						@Override
						public Result get() {
							return localInvoke(remote.getBeanName(), remote.getRemoteMethod().getName(), remote.getArgs());
						}
					};
					
					/* 使用主线程执行对应方法 */
					Result result = RemoteInvokeService.TASK.runMaster(remote.getTranId(), REMOTE_CONFIG_DTO.getReadTimeout(), supplier, supplier);
					sendResult(session, JSON.toJSONBytes(result.setRequestId(remote.getRequestId())));
				} catch (Throwable e) {
					final String trackId = UUID.randomUUID().toString();
					log.error(String.format("客户端[%s]调用本地方法[%s]异常》》》 [%s]", appAddress, remote.getRemoteMethod().getName(), trackId), e);
					
					/* 响应异常信息 */
					ExceptionDto exception = getException(getRealExcept(e));
					GlobalExceptionDto globalException = new GlobalExceptionDto(SpringUtil.getAppName(), exception.getAppMsg(), trackId);
					Result result = new Result().setResponse(new Response<>(exception.getErrorMsg(), globalException)).$setReturnType(GlobalExceptionDto.class);
					sendResult(session, JSON.toJSONBytes(result.setRequestId(remote.getRequestId())));
				} finally {RemoteAdapter.removeTranId(REDIS_MAKE); HeaderUtil.remove();}
			}
			
			/* 发送响应结果 */
			private void sendResult(Session session, byte[] result) {
				try {synchronized (session) {session.getBasicRemote().sendBinary(ByteBuffer.wrap(result));}}
				catch (Throwable e) {log.error(String.format("响应客户端[%s]结果失败！", appAddress), e);}
			}
		});
	}
	
	/* 本地执行 */
	private Result localInvoke(String beanName, String methodName, Object[] args) {
		ObjectInfo objInfo = LOCAL_CACHE.computeIfAbsent(beanName, (k) -> {
			Object bean = SpringUtil.getBean(beanName);
			Assert.notNull(bean, String.format("Spring根据[%s]名称获取到空的实例对象！", beanName));
			
			/* 生成方法信息 */
			Class<?> targetClass = ClassUtils.getUserClass(AopProxyUtils.ultimateTargetClass(bean));
			Map<String, Entry<Method, Type>> methods = new HashMap<>();
			for (Method method : targetClass.getMethods()) { //只处理公开方法 - 包括继承的公开方法
				methods.computeIfAbsent(parse(method), mk -> new Entry<Method, Type>() {
					private final Type returnType = getReturnType(targetClass, method);
					@Override
					public Method getKey() {return method;}
					@Override
					public Type getValue() {return returnType;}
					@Override
					public Type setValue(Type value) {return null;}
				});
			}
			
			/* 生成对象信息 */
			return new ObjectInfo(bean, methods);
		});
		
		/* 转换参数类型 */
		Entry<Method, Type> entry = objInfo.methods.get(methodName);
		if (args != null) {
			Type[] types = entry.getKey().getGenericParameterTypes();
			for (int i = 0, j = args.length; i < j; i++) {
				args[i] = parse(args[i], types[i]);
			}
		}
		
		/* 执行方法 */
		Object returnValue = ReflectionUtils.invokeMethod(entry.getKey(), objInfo.bean, args);
		return new Result().setResponse(new Response<>(returnValue)).$setReturnType(entry.getValue());
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2022-09-27 16:54:33
	 * @版本 1.0
	 * @parentClass RemoteInvokeService
	 * @描述 对象信息
	 */
	@AllArgsConstructor
	private class ObjectInfo {
		/* 实例对象 */
		private final Object bean;
		/* 执行方法 */
		private final Map<String, Entry<Method, Type>> methods;
	}
}
