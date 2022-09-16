package org.city.common.core.service;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.city.common.api.annotation.plug.Remote;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.remote.RemoteClassDto;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.in.Runnable;
import org.city.common.api.in.Task;
import org.city.common.api.in.function.FunctionRequestVoid;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.redis.RedisMake;
import org.city.common.api.in.remote.RemoteInvokeApi;
import org.city.common.api.in.remote.RemoteSave;
import org.city.common.api.open.CommonInfoApi;
import org.city.common.api.util.PlugUtil;
import org.city.common.api.util.SpringUtil;
import org.city.common.core.handler.RemoteHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2022-09-27 14:29:46
 * @版本 1.0
 * @描述 Redis远程调用保存服务
 */
@Slf4j
@Component
public class RedisRemoteSaveService implements RemoteSave,JSONParser,ApplicationRunner {
	@Autowired
	private RedisMake redisMake;
	@Autowired
	private RemoteInvokeApi remoteInvokeApi;
	@Autowired
	private RemoteIpPortDto localIpPort;
	@Autowired
	private RemoteConfigDto remoteConfigDto;
	@Autowired
	private Task task;
	/* 本地缓存远程信息 */
	private final Map<Object, List<RemoteInfo>> LOCAL_CACHE = new ConcurrentHashMap<>();
	/* 远程集合类型 */
	private final Type REMOTE_LIST = new ParameterizedType() {
		@Override
		public Type getRawType() {return List.class;}
		@Override
		public Type getOwnerType() {return null;}
		@Override
		public Type[] getActualTypeArguments() {return new Type[] {RemoteClassDto.class};}
	};
	
	@Override
	public void init() {
		for (String key : redisMake.getRedisTemplate().keys(CommonConstant.REDIS_REMOTE_PREFIX_KEY + "*")) {
			redisMake.delHKey(key, Arrays.asList(localIpPort.toString())); //移除自己的接口数据
		}
		redisMake.setValue(localIpPort.toString() + CommonConstant.REDIS_REMOTE_PREFIX_KEY, true, 10, TimeUnit.MINUTES);
		redisMake.setHValue(CommonConstant.REDIS_REMOTE_AUTH_HKEY, localIpPort.toString(), CommonConstant.REMOTE_AUTH_VALUE);
	}
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		flush(); //自己刷新缓存操作
		manyRequest(cia -> ((CommonInfoApi) cia.getBean()).online(localIpPort)); //通知所有服务
	}
	
	@Override
	public void save(String key, RemoteClassDto remoteClassDto) {
		/* 取出原来存储在追加 */
		List<RemoteClassDto> remotes = redisMake.getHValue(CommonConstant.REDIS_REMOTE_PREFIX_KEY + key, localIpPort.toString(), REMOTE_LIST);
		if (remotes == null) {remotes = new ArrayList<>();}
		remotes.add(remoteClassDto);
		/* Redis存储 */
		redisMake.setHValue(CommonConstant.REDIS_REMOTE_PREFIX_KEY + key, localIpPort.toString(), remotes);
	}
	
	@Override
	public void flush() {
		manyRequest(cia -> { //查询所有服务
			RemoteIpPortDto remoteIpPort = ((CommonInfoApi) cia.getBean()).getIpPort(); //远程服务IP端口
			if (!cia.getRemoteIpPortDto().equals(remoteIpPort)) {throw new ConnectException("远程服务非真实服务！");} //服务IP端口不一致则为脏数据
		});
		LOCAL_CACHE.clear(); //清空本地缓存重新拉取
	}
	
	@Override
	public Map<String, Map<String, List<RemoteClassDto>>> getScopeRemote(String scanKey) {
		Map<String, Map<String, List<RemoteClassDto>>> remotes = new HashMap<>();
		/* 范围找key */
		Set<String> keys = redisMake.getRedisTemplate().keys(CommonConstant.REDIS_REMOTE_PREFIX_KEY + "*" + scanKey + "*");
		for (String key : keys) {
			Map<String, List<RemoteClassDto>> datas = new HashMap<>();
			/* 取出key对应所有值 */
			for (Entry<String, Object> entry : redisMake.entry(key).entrySet()) {
				datas.put(entry.getKey(), parse(entry.getValue(), REMOTE_LIST));
			}
			remotes.put(key, datas);
		}
		return remotes;
	}
	
	@Override
	public List<RemoteInfo> get(Class<?> interfaceCls) {
		List<RemoteInfo> remoteInfos = LOCAL_CACHE.get(interfaceCls);
		/* 如果没有信息则重新设置 */
		if (remoteInfos == null) {
			remoteInfos = new ArrayList<>();
			setRedisRemote(interfaceCls.getName(), remoteInfos);
			LOCAL_CACHE.put(interfaceCls, remoteInfos);
		} else {
			/* 保留未熔断的远程调用 */
			long curTime = System.currentTimeMillis();
			remoteInfos = remoteInfos.stream().filter(v -> v.getTurnOnTime() < curTime).collect(Collectors.toList());
		}
		return remoteInfos;
	}
	
	@Override
	public List<RemoteInfo> get(String methodName) {
		Assert.hasText(methodName, "方法名称不能为空！");
		List<RemoteInfo> remoteInfos = LOCAL_CACHE.get(methodName);
		/* 如果没有信息则重新设置 - 方法名需带前缀分割符 */
		if (remoteInfos == null) {
			remoteInfos = new ArrayList<>();
			setRedisRemote("-" + methodName, remoteInfos);
			LOCAL_CACHE.put(methodName, remoteInfos);
		} else {
			/* 保留未熔断的远程调用 */
			long curTime = System.currentTimeMillis();
			remoteInfos = remoteInfos.stream().filter(v -> v.getTurnOnTime() < curTime).collect(Collectors.toList());
		}
		return remoteInfos;
	}
	
	/* 设置远程信息 */
	private void setRedisRemote(String scopeKey, List<RemoteInfo> remotes) {
		/* 迭代判断 */
		for (Entry<String, Map<String, List<RemoteClassDto>>> scopeRemote : getScopeRemote(scopeKey).entrySet()) {
			for (Entry<String, List<RemoteClassDto>> entry : scopeRemote.getValue().entrySet()) {
				RemoteClassDto rtc = entry.getValue().iterator().next();
				/* 所有实现类的接口 */
				Class<?> interfaceCls;
				try {interfaceCls = Class.forName(rtc.getInterfaceName());} catch (ClassNotFoundException e) {throw new RuntimeException(e);}
				Remote remote = interfaceCls.getDeclaredAnnotation(Remote.class);
				
				/* 添加本地代理对象 */
				for (RemoteClassDto remoteClassDto : entry.getValue()) {
					boolean isLocal = localIpPort.toString().equals(entry.getKey()); //是否是本地服务
					RemoteInfo remoteInfo = new RemoteInfo(remote.speedLimit(), remoteClassDto, isLocal ? localIpPort : new RemoteIpPortDto(entry.getKey()));
					
					/* 如果是本地服务 - 直接设置Bean对象 - 否则设置生成的代理对象 */
					Object bean = isLocal ? SpringUtil.getBean(remoteClassDto.getBeanName()) : getJdkProxy(interfaceCls, remoteInfo);
					remotes.add(remoteInfo.setBean(bean));
				}
			}
		}
	}
	/* 获取JDK动态代理对象 */
	private Object getJdkProxy(Class<?> interfaceCls, RemoteInfo remoteInfo) {
		RemoteHandler remoteHandler = new RemoteHandler(remoteInfo, remoteInvokeApi, remoteConfigDto.getFailTimeout()); //自定义远程处理对象
		return java.lang.reflect.Proxy.newProxyInstance(interfaceCls.getClassLoader(), new Class[] {interfaceCls}, remoteHandler);
	}
	/* 公共接口多请求 */
	private void manyRequest(FunctionRequestVoid<RemoteInfo> consumer) {
		List<RemoteInfo> ciApiInfos = PlugUtil.getRemotes(CommonInfoApi.class);
		CountDownLatch countDownLatch = new CountDownLatch(ciApiInfos.size());
		
		/* 操作所有服务 */
		for (RemoteInfo ciApiInfo : ciApiInfos) {
			task.putTask(new Runnable() {
				@Override
				public void run() throws Throwable {
					try {consumer.apply(ciApiInfo);}
					catch (Throwable e) {
						if (CommonConstant.isConnectTimeout(e) || CommonConstant.isConnectTimeout(e.getCause())) { //连接异常删除脏数据
							if (!redisMake.hasKey(ciApiInfo.getRemoteIpPortDto().toString() + CommonConstant.REDIS_REMOTE_PREFIX_KEY)) {
								for (String key : redisMake.getRedisTemplate().keys(CommonConstant.REDIS_REMOTE_PREFIX_KEY + "*")) {
									redisMake.delHKey(key, Arrays.asList(ciApiInfo.getRemoteIpPortDto().toString())); //移除脏数据
								}
							}
						} else { //非连接异常退出当前服务
							try {log.error("远程服务异常，3秒后退出当前服务！", e); Thread.sleep(3000);}
							finally {System.exit(-1);} //退出当前服务
						}
					} finally {countDownLatch.countDown();}
				}
			});
		}
		
		/* 必须保证请求完成 */
		try {countDownLatch.await();} catch (InterruptedException e) {throw new RuntimeException(e);}
	}
}
