package org.city.common.core.make;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.city.common.api.in.MakeInvoke;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.redis.RedisMake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2023年6月28日
 * @版本 1.0
 * @描述 Redis缓存操作
 */
@Component
public class RedisCacheMake implements MakeInvoke,JSONParser {
	@Autowired
    private RedisMake redisMake;
	@Value("${redis.localCache.size:100}")
	private int cacheSize; //本地缓存数量
	private final ReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock(true); //公平读写锁
	private final Map<String, Entry<Object, Long>> LOCAL_CACHE = new LinkedHashMap<>(150); //本地缓存池
	
	@Override
	public void invoke(Process process, int value, String[] values) throws Throwable {
		if (values.length == 0) {throw new RuntimeException("Redis缓存操作对应[values]值不能为空！");}
		int waitTime = value < 10 ? Short.MAX_VALUE : value;
		boolean isFlush = values.length > 1 ? Boolean.TRUE.toString().equalsIgnoreCase(values[1]) : false;
		
		/* 获取缓存数据 */
		Object cache = isFlush ? null : getLocal(values[0], process.getMethod().getGenericReturnType());
		if (cache == null) {
			NotReturn(process); //执行原方法并设置缓存
			setLocal(values[0], process.getReturn(), waitTime);
		} else {
			process.setReturn(parse(cache, process.getMethod().getGenericReturnType()));
			process.end(); //结束链路执行 - 后续有其他Make也不会执行
		}
	}
	
	/* 获取本地缓存 */
	private Object getLocal(String key, Type returnType) {
		if (cacheSize > 0) { //本地缓存数量大于0才有效
			try {
				verifyExpire(key); //验证本地缓存过期
				READ_WRITE_LOCK.readLock().lock();
				Entry<Object, Long> cache = LOCAL_CACHE.get(key); //先查询本地缓存 - 没有才查询Redis缓存
				return cache == null ? redisMake.getValue(key, returnType) : cache.getKey();
			} finally {
				READ_WRITE_LOCK.readLock().unlock();
			}
		} else {
			return redisMake.getValue(key, returnType);
		}
	}
	/* 设置本地缓存 */
	private void setLocal(String key, Object data, int waitTime) {
		if (cacheSize > 0) { //本地缓存数量大于0才有效
			try {
				READ_WRITE_LOCK.writeLock().lock();
				if (LOCAL_CACHE.size() > cacheSize) { //如果本地缓存超过设置值 - 删除头数据，追加尾数据
					Iterator<Entry<String, Entry<Object, Long>>> iterator = LOCAL_CACHE.entrySet().iterator();
					if (iterator.hasNext()) {iterator.next(); iterator.remove();} //删除头数据
				}
				LOCAL_CACHE.put(key, getEntry(waitTime, data)); //追加尾数据
				redisMake.setValue(key, data, waitTime, TimeUnit.MILLISECONDS); //Redis缓存
			} finally {
				READ_WRITE_LOCK.writeLock().unlock();
			}
		} else {
			redisMake.setValue(key, data, waitTime, TimeUnit.MILLISECONDS); //Redis缓存
		}
	}
	/* 验证缓存过期 - 过期移除 */
	private void verifyExpire(String key) {
		Entry<Object, Long> cache = LOCAL_CACHE.get(key);
		if (cache != null && System.currentTimeMillis() > cache.getValue()) { //过期则删除本地缓存
			try {
				READ_WRITE_LOCK.writeLock().lock();
				LOCAL_CACHE.remove(key);
			} finally {
				READ_WRITE_LOCK.writeLock().unlock();
			}
		}
	}
	/* 获取记录时间对应本地缓存 */
	private Entry<Object, Long> getEntry(int waitTime, Object data) {
		return new Entry<Object, Long>() {
			private final Long record = System.currentTimeMillis() + waitTime;
			@Override
			public Long setValue(Long value) {return null;}
			@Override
			public Long getValue() {return record;}
			@Override
			public Object getKey() {return data;}
		};
	}
}
