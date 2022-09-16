package org.city.common.core.adapter;

import java.lang.annotation.Annotation;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.city.common.api.adapter.ExtensionIOAdapter;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.GlobalExtensionDto;
import org.city.common.api.dto.MethodCacheDto;
import org.city.common.api.in.Task;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.util.MyUtil;
import org.city.common.core.handler.GlobalExtensionHandler;
import org.city.common.core.handler.GlobalExtensionHandler.RemoteParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2022-06-24 12:58:44
 * @版本 1.0
 * @描述 通过Redis适配存取扩展接口
 */
@Slf4j
@Component(CommonConstant.REDIS_EXT_ADAPTER_NAME)
public class RedisExtensionAdapter implements ExtensionIOAdapter,JSONParser{
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private ApplicationContext applicationContext;
	/*一级缓存*/
	private final Map<String, Cache> LOCAL_CACHE = new HashMap<>();
	/* 二级缓存 */
	private final Map<String, Cache> REMOTE_CACHE = new HashMap<>();
	/* 多个接口缓存 */
	private final Map<Class<?>, Caches> ALL_CACHE = new HashMap<>();
	@Autowired
	private Task task;
	@Value("${global.extension.timeout:600000}")
	private int expireTime; //不能小于半分钟
	@Value("${server.port}")
	private int port;
	/* 端口地址 */
	private String ipPort;
	@PostConstruct
	private void init() throws UnknownHostException {ipPort = MyUtil.getIpPort(port); expireTime = expireTime < Short.MAX_VALUE ? 600000 : expireTime;}
	/* Redis服务器时间脚本 */
	private final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>("local a=redis.call('TIME'); return (a[1]*1000000+a[2])/1000", Long.class);
	
	@Override
	public long getCurTime() {return redisTemplate.execute(SCRIPT, new ArrayList<>());}
	
	@Override
	public void removeBean(String zone) throws Exception {
		/* 移除当前区域前缀存储 */
		try(Cursor<Entry<Object, Object>> scan = redisTemplate.opsForHash().scan(CommonConstant.REDIS_EXT_KEY_NAME,
				ScanOptions.scanOptions().match(zone + "*").build())) {
			while(scan.hasNext()) {redisTemplate.opsForHash().delete(CommonConstant.REDIS_EXT_KEY_NAME, scan.next().getKey());}
		}
		
		/* 定时更新时间 */
		task.schedula(TIME_UPDATE_ID, new org.city.common.api.in.Runnable() {
			@Override
			public void run() throws Exception{
				try {
					long curTime = getCurTime();
					for (Entry<String, Cache> entry : LOCAL_CACHE.entrySet()) {
						entry.getValue().getGlobalExtensionDto().setRecordTime(curTime);
						redisTemplate.opsForHash().put(CommonConstant.REDIS_EXT_KEY_NAME, entry.getValue().getID(), entry.getValue().getGlobalExtensionDto());
					}
				} catch (Exception e) {
					System.err.println("更新全局扩展点过期时间失败》》》 " + e.getMessage());
				}
			}
		}, null, expireTime >> 1, false);
	}
	
	@Override
	public void saveBean(String zone, String beanName, Map<String, GlobalExtensionDto> param) throws Exception {
		long curTime = getCurTime();
		for (Entry<String, GlobalExtensionDto> entry : param.entrySet()) {
			/* 记录时间（用作超时） */
			entry.getValue().setRecordTime(curTime);
			/* 判断是否存在 */
			String onlyId = entry.getValue().getOnlyId();
			String curId = entry.getKey(), existId = getId(onlyId);
			
			/* 已存在打印警告 - 存在采用覆盖 */
			if (existId != null) {
				log.warn(String.format("当前扩展点ID[%s]已存在，请检查存在的二者[%s][%s]代码问题！", onlyId, curId, existId));
			}
			
			/*缓存本地当前的对象*/
			LOCAL_CACHE.put(onlyId, new Cache(entry.getKey(), entry.getValue(), beanName, 0L));
		}
		/* 缓存Redis */
		redisTemplate.opsForHash().putAll(CommonConstant.REDIS_EXT_KEY_NAME, param);
	}
	
	@Override
	public String getId(String onlyId) {
		Cursor<Entry<Object, Object>> scan = redisTemplate.opsForHash().scan(CommonConstant.REDIS_EXT_KEY_NAME,
				ScanOptions.scanOptions().match("*" + onlyId).build());
		return scan.hasNext() ? String.valueOf(scan.next().getKey()) : null;
	}
	
	@Override
	public GlobalExtensionDto getGlobalExtensionDto(String onlyId) {
		Cursor<Entry<Object, Object>> scan = redisTemplate.opsForHash().scan(CommonConstant.REDIS_EXT_KEY_NAME,
				ScanOptions.scanOptions().match("*" + onlyId).build());
		return scan.hasNext() ? (GlobalExtensionDto) scan.next().getValue() : null;
	}
	
	@Override
	public List<GlobalExtensionDto> getGlobalExtensionDtos(String proxyInterfaceName) {
		List<GlobalExtensionDto> geds = new ArrayList<>();
		Cursor<Entry<Object, Object>> scan = redisTemplate.opsForHash().scan(CommonConstant.REDIS_EXT_KEY_NAME,
				ScanOptions.scanOptions().match("*" + proxyInterfaceName + MH + "*").count(Short.MAX_VALUE).build());
		while(scan.hasNext()) {geds.add((GlobalExtensionDto) scan.next().getValue());}
		return geds;
	}
	
	/*获取缓存*/
	private Cache getCache(String onlyId, Class<?> proxyInterface) {
		/*缓存获取*/
		Cache cache = LOCAL_CACHE.get(onlyId);
		if (cache == null) {
			/* 二级缓存 */
			cache = REMOTE_CACHE.get(onlyId);
			long curTime = getCurTime();
			
			/* 二级超时 */
			if (cache == null || (curTime - cache.getRecordTime()) > expireTime) {
				/*Redis获取*/
				GlobalExtensionDto ged = getGlobalExtensionDto(onlyId);
				if (ged == null) {cache = new Cache(null, null, null, curTime);}
				else {
					
					/* 记录超时 */
					if ((curTime - ged.getRecordTime()) > expireTime) {
						cache = new Cache(null, null, null, curTime);
						/* 删除超时的key */
						redisTemplate.opsForHash().delete(CommonConstant.REDIS_EXT_KEY_NAME, getId(onlyId));
					} else {
						
						/*通过代理生成远程调用*/
						Object remote = java.lang.reflect.Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{proxyInterface}, 
								new GlobalExtensionHandler(new RemoteParam(ipPort, ged.getPrifixPath(), ged.getMethodPath(), ged.getMethodParamNames()), restTemplate));
						cache = new Cache(null, ged, remote, ged.getRecordTime());
					}
				}
				REMOTE_CACHE.put(onlyId, cache);
			}
		}
		return cache;
	}
	/* 获取多个缓存 */
	private List<Cache> getCaches(Class<?> proxyInterface) {
		List<Cache> values = new ArrayList<>();
		/*缓存获取*/
		Caches caches = ALL_CACHE.get(proxyInterface);
		/* 多个缓存和全局扩展点无关，所以使用当前服务器时间判断是可以的 */
		if (caches == null || (System.currentTimeMillis() - caches.getRecordTime()) > expireTime) {
			Set<String> onlyIds = new HashSet<>();
			
			/* 获取匹配接口所有扩展点 */
			List<GlobalExtensionDto> globalExtensionDtos = getGlobalExtensionDtos(proxyInterface.getName());
			for (GlobalExtensionDto ged : globalExtensionDtos) {
				onlyIds.add(ged.getOnlyId());
				values.add(getCache(ged.getOnlyId(), proxyInterface));
			}
			ALL_CACHE.put(proxyInterface, new Caches(onlyIds, System.currentTimeMillis()));
		} else {
			/* 直接调用获取单个缓存方法 */
			for (String onlyId : caches.onlyIds) {values.add(getCache(onlyId, proxyInterface));}
		}
		return values;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T getBean(String id, Class<T> proxyInterface) {
		Cache cache = getCache(getOnlyId(id, proxyInterface), proxyInterface);
		return cache.bean instanceof String ? (T) applicationContext.getBean((String) cache.bean) : (T) cache.bean;
	}
	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> getAllBean(Class<T> proxyInterface) {
		List<T> proxys = new ArrayList<>();
		for (Cache cache : getCaches(proxyInterface)) {proxys.add(cache.bean instanceof String ? (T) applicationContext.getBean((String) cache.bean) : (T) cache.bean);}
		return proxys;
	}
	
	@Override
	public <A extends Annotation> A getValue(String id, Class<?> proxyInterface, Class<A> annotationClass) {
		GlobalExtensionDto globalExtensionDto = getCache(getOnlyId(id, proxyInterface), proxyInterface).globalExtensionDto;
		return globalExtensionDto == null ? null : globalExtensionDto.getAnnotation(annotationClass);
	}
	@Override
	public <A extends Annotation> List<A> getAllValue(Class<?> proxyInterface, Class<A> annotationClass) {
		List<A> annotationVals = new ArrayList<>();
		for (Cache cache : getCaches(proxyInterface)) {
			annotationVals.add(cache.globalExtensionDto == null ? null : cache.globalExtensionDto.getAnnotation(annotationClass));
		}
		return annotationVals;
	}
	
	@Override
	public void saveInitMethod(String id, MethodCacheDto methodCacheDto) {
		redisTemplate.opsForHash().put(CommonConstant.REDIS_INIT_METHOD_CACHE_NAME, id, methodCacheDto);
	}
	@Override
	public MethodCacheDto getInitMethod(String id) {
		return (MethodCacheDto) redisTemplate.opsForHash().get(CommonConstant.REDIS_INIT_METHOD_CACHE_NAME, id);
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2022-06-28 13:16:34
	 * @版本 1.0
	 * @parentClass RedisExtensionAdapter
	 * @描述 缓存的扩展数据
	 */
	@Data
	@AllArgsConstructor
	private static class Cache{
		private final String ID;
		private final GlobalExtensionDto globalExtensionDto;
		
		private final Object bean;
		private final long recordTime;
	}
	/**
	 * @作者 ChengShi
	 * @日期 2022-06-28 13:16:34
	 * @版本 1.0
	 * @parentClass RedisExtensionAdapter
	 * @描述 多个缓存的扩展数据
	 */
	@Data
	@AllArgsConstructor
	private static class Caches{
		private final Set<String> onlyIds;
		private final long recordTime;
	}
}
