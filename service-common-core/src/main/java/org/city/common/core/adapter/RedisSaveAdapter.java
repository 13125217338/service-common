package org.city.common.core.adapter;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.city.common.api.adapter.RemoteSaveAdapter;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.remote.RemoteClassDto;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.open.RemoteInvokeApi;
import org.city.common.api.open.Task;
import org.city.common.core.handler.RemoteHandler;
import org.city.common.core.handler.RemoteTransactionalHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2022-09-27 14:29:46
 * @版本 1.0
 * @描述 Redis远程调用缓存适配
 */
@Slf4j
@Component(CommonConstant.REDIS_REMOTE_ADAPTER_NAME)
public class RedisSaveAdapter implements RemoteSaveAdapter{
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private RemoteInvokeApi remoteInvokeApi;
	@Autowired
	private RemoteTransactionalHandler remoteTransactionalHandler;
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private Task task;
	@Autowired
	private RemoteIpPortDto localIpPort;
	@Value("${global.remote.timeout:30000}")
	private int expireTime; //不能小于20秒
	private final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>("local a=redis.call('TIME'); return (a[1]*1000000+a[2])/1000", Long.class);
	/* 本地所有远程信息 */
	private final Map<String, List<RemoteInfo>> LOCAL_REMOTE = new HashMap<>();
	/* 本地缓存远程信息 */
	private final Map<Object, RemoteCache> LOCAL_CACHE = new ConcurrentHashMap<>();
	
	@Override
	public void init() {
		expireTime = expireTime < 20000 ? 30000 : expireTime;
		timeUpdate();
		
		/* 初始beanName为bean */
		for (List<RemoteInfo> remoteInfos : LOCAL_REMOTE.values()) {
			for (RemoteInfo remoteInfo : remoteInfos) {
				if (remoteInfo.getBean() instanceof String) {
					remoteInfo.setBean(applicationContext.getBean((String) remoteInfo.getBean()));
				}
			}
		}
	}
	/* 定时更新 */
	private void timeUpdate() {
		/* 定时更新时间 */
		task.schedula("REDIS_" + TIME_UPDATE_ID, new org.city.common.api.in.Runnable() {
			@Override
			public void run() throws Throwable {
				try {
					long curTime = getCurTime(); //Redis存储则取Redis时间
					for (Entry<String, List<RemoteInfo>> entry : LOCAL_REMOTE.entrySet()) {
						
						/* 取远程类信息 */
						List<RemoteClassDto> datas = entry.getValue().stream()
								.map(k -> k.getRemoteClassDto().setRecordTime(curTime)).collect(Collectors.toList());
						/* Redis重新存储 */
						redisTemplate.opsForHash().put(REMOTE_PREFIX_KEY + entry.getKey(), localIpPort.toString(), datas);
					}
				} catch (Exception e) {
					String errorMsg = "更新远程过期时间失败》》》 " + e.getMessage();
					System.err.println(errorMsg); log.error(errorMsg, e);
				}
			}
		}, null, expireTime >> 1, false);
	}
	
	@Override
	public long getCurTime() {return redisTemplate.execute(SCRIPT, new ArrayList<>());}
	
	@Override
	@SuppressWarnings("unchecked")
	public void save(String key, RemoteClassDto remoteClassDto, Object curBean) {
		remoteClassDto.setRecordTime(getCurTime());
		/* 本地存储 */
		List<RemoteInfo> remoteDefault = LOCAL_REMOTE.computeIfAbsent(key, k -> new ArrayList<>());
		remoteDefault.add(new RemoteInfo(remoteClassDto, localIpPort).setBean(curBean));
		
		/* 取出原来存储在追加 */
		List<RemoteClassDto> remotes = (List<RemoteClassDto>) redisTemplate.opsForHash().get(REMOTE_PREFIX_KEY + key, localIpPort.toString());
		if (remotes == null) {remotes = new ArrayList<>();}
		remotes.add(remoteClassDto);
		/* Redis存储 */
		redisTemplate.opsForHash().put(REMOTE_PREFIX_KEY + key, localIpPort.toString(), remotes);
	}
 	
	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Map<String, List<RemoteClassDto>>> getScopeRemote(String scanKey) {
		Map<String, Map<String, List<RemoteClassDto>>> remotes = new HashMap<>();
		/* 范围找key */
		Set<String> keys = redisTemplate.keys(REMOTE_PREFIX_KEY + "*" + scanKey + "*");
		for (String key : keys) {
			Map<String, List<RemoteClassDto>> datas = new HashMap<>();
			/* 取出key对应所有值 */
			for (Entry<Object, Object> entry : redisTemplate.opsForHash().entries(key).entrySet()) {
				datas.put((String) entry.getKey(), (List<RemoteClassDto>) entry.getValue());
			}
			remotes.put(key, datas);
		}
		return remotes;
	}
	
	@Override
	public Map<String, RemoteInfo> getScopeLocal(String scanKey) {
		Map<String, RemoteInfo> remotes = new HashMap<>();
		for (Entry<String, List<RemoteInfo>> entry : LOCAL_REMOTE.entrySet()) {
			/* 范围查询对应远程对象信息 */
			if (entry.getKey().contains(scanKey)) {
				for (RemoteInfo remote : entry.getValue()) {
					remotes.put(remote.getRemoteClassDto().getName(), remote);
				}
			}
		}
		return remotes;
	}
	
	@Override
	public List<RemoteInfo> get(Class<?> interfaceCls) {
		RemoteCache remoteCache = LOCAL_CACHE.get(interfaceCls);
		/* 如果没有信息或者超时则重新设置 */
		if (remoteCache == null || (System.currentTimeMillis() - remoteCache.getRecordTime()) > expireTime) {
			remoteCache = new RemoteCache();
			setRedisRemote(interfaceCls.getName(), remoteCache.getRemoteInfos());
			LOCAL_CACHE.put(interfaceCls, remoteCache);
		} else {
			/* 删除已经禁用的远程调用 */
			ListIterator<RemoteInfo> rmInfo = remoteCache.getRemoteInfos().listIterator();
			while(rmInfo.hasNext()) {if (rmInfo.next().isDisable()) {rmInfo.remove();}}
		}
		return remoteCache.getRemoteInfos();
	}
	
	@Override
	public List<RemoteInfo> get(String methodName) {
		Assert.hasText(methodName, "方法名称不能为空！");
		RemoteCache remoteCache = LOCAL_CACHE.get(methodName);
		/* 如果没有信息或者超时则重新设置 - 方法名需带前缀分割符 */
		if (remoteCache == null || (System.currentTimeMillis() - remoteCache.getRecordTime()) > expireTime) {
			remoteCache = new RemoteCache();
			setRedisRemote(REMOTE_KEY_SPLITE + methodName, remoteCache.getRemoteInfos());
			LOCAL_CACHE.put(methodName, remoteCache);
		} else {
			/* 删除已经禁用的远程调用 */
			ListIterator<RemoteInfo> rmInfo = remoteCache.getRemoteInfos().listIterator();
			while(rmInfo.hasNext()) {if (rmInfo.next().isDisable()) {rmInfo.remove();}}
		}
		return remoteCache.getRemoteInfos();
	}
	
	/* 设置远程信息 */
	private void setRedisRemote(String scopeKey, List<RemoteInfo> remotes) {
		long curTime = getCurTime(); //Redis存取则取Redis时间
		Map<String, List<String>> removes = new HashMap<>();
		
		/* 迭代判断 */
		for (Entry<String, Map<String, List<RemoteClassDto>>> scopeRemote : getScopeRemote(scopeKey).entrySet()) {
			for (Entry<String, List<RemoteClassDto>> entry : scopeRemote.getValue().entrySet()) {
				/* 非自己的迭代添加 */
				if (!localIpPort.toString().equals(entry.getKey())) {
					RemoteClassDto rtc = entry.getValue().iterator().next();
					/* 如果超时添加到超时列表待删除 - 一个超时代表下面都超时*/
					if((curTime - rtc.getRecordTime()) > expireTime) {
						removes.computeIfAbsent(scopeRemote.getKey(), k -> new ArrayList<>()).add(entry.getKey());
					} else {
						
						/* 所有实现类的接口 */
						Class<?> interfaceCls;
						try {interfaceCls = Class.forName(rtc.getInterfaceName());} catch (ClassNotFoundException e) {throw new RuntimeException(e);}
						for (RemoteClassDto remoteClassDto : entry.getValue()) {
							RemoteInfo remoteInfo = new RemoteInfo(remoteClassDto, new RemoteIpPortDto(entry.getKey()));
							
							/* 设置生成的代理对象 */
							remoteInfo.setBean(Proxy.newProxyInstance(RedisSaveAdapter.class.getClassLoader(),
									new Class[] {interfaceCls},
									new RemoteHandler(remoteInfo, remoteInvokeApi, remoteTransactionalHandler)));
							remotes.add(remoteInfo);
						}
					}
				}
			}
		}
		/* 自己则范围全部添加 */
		remotes.addAll(getScopeLocal(scopeKey).values());
		
		/* 批量删除超时数据 */
		if (removes.size() > 0) {
			for (Entry<String, List<String>> remove : removes.entrySet()) {
				redisTemplate.opsForHash().delete(remove.getKey(), remove.getValue().toArray());
			}
		}
	}
}
