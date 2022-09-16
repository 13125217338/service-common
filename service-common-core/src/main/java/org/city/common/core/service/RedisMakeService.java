package org.city.common.core.service;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.city.common.api.dto.DataList;
import org.city.common.api.dto.RedisDto;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.dto.sql.BaseDto;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.redis.RedisMake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @作者 ChengShi
 * @日期 2023年5月9日
 * @版本 1.0
 * @描述 Redis操作服务
 */
@Service
public class RedisMakeService implements RedisMake,JSONParser {
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private RemoteIpPortDto localIpPort;
	
	@Override
	public DataList<String> keys(RedisDto redisDto) {
		setPage(redisDto); //设置合适分页
		
		/* 手动扫描分页 */
		ScanOptions options = ScanOptions.scanOptions().match(redisDto.getPattern()).build();
		RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
		Cursor<byte[]> scan = connection.scan(options);
		long curIndex = 0, offset = (redisDto.getPageNum() - 1) * redisDto.getPageSize();
		
		/* 获取原key序列对象反序列化 */
		StringRedisSerializer keySerializer = (StringRedisSerializer) redisTemplate.getKeySerializer();
		List<String> keys = new ArrayList<>();
		while(scan.hasNext()) {
			byte[] next = scan.next(); //获取数据
			if (curIndex >= redisDto.getPageSize()) {break;} //已获取所有分页数据
			else if (curIndex > --offset) {keys.add(keySerializer.deserialize(next)); curIndex++;} //反序列化key并自增数量
		}
		
		/* 分页后的key */
		return new DataList<>(keys, connection.dbSize());
	}
	@Override
	public boolean hasKey(String key) {
		return redisTemplate.hasKey(key);
	}
	@Override
	public boolean hasHKey(String key, String hKey) {
		return redisTemplate.opsForHash().hasKey(key, hKey);
	}
	@Override
	public void delKey(String key) {
		redisTemplate.delete(key);
	}
	
	@Override
	public Object get(String key) {
		switch (redisTemplate.type(key)) {
			case STRING: return redisTemplate.opsForValue().get(key);
			case HASH: return redisTemplate.opsForHash().entries(key);
			case SET: return redisTemplate.opsForSet().members(key);
			case LIST: return redisTemplate.opsForList().range(key, 0, redisTemplate.opsForList().size(key));
			case ZSET: return redisTemplate.opsForZSet().range(key, 0, redisTemplate.opsForZSet().size(key));
			default: return null;
		}
	}
	
	@Override
	public <T> T getValue(String key, Type type) {
		return parse(redisTemplate.opsForValue().get(key), type);
	}
	@Override
	public void setValue(String key, Object value) {
		redisTemplate.opsForValue().set(key, value);
	}
	@Override
	public void setValue(String key, Object value, long timeout, TimeUnit timeUnit) {
		redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
	}
	
	@Override
	public <T> T getHValue(String key, String hKey, Type type) {
		return parse(redisTemplate.opsForHash().get(key, hKey), type);
	}
	@Override
	public Map<String, Object> entry(String key) {
		Set<Entry<Object, Object>> entrySet = redisTemplate.opsForHash().entries(key).entrySet();
		return entrySet.stream().collect(Collectors.toMap(k -> String.valueOf(k.getKey()), v -> v.getValue()));
	}
	@Override
	public DataList<Entry<Object, Object>> entry(RedisDto redisDto) {
		setPage(redisDto); //设置合适分页
		
		/* 手动扫描分页 */
		ScanOptions options = ScanOptions.scanOptions().match(redisDto.getPattern()).build();
		Cursor<Entry<Object, Object>> scan = redisTemplate.opsForHash().scan(redisDto.getKey(), options);
		long curIndex = 0, offset = (redisDto.getPageNum() - 1) * redisDto.getPageSize();
		
		/* 迭代获取分页数据 */
		List<Entry<Object, Object>> datas = new ArrayList<>();
		while(scan.hasNext()) {
			Entry<Object, Object> next = scan.next(); //获取数据
			if (curIndex >= redisDto.getPageSize()) {break;} //已获取所有分页数据
			else if (curIndex > --offset) {datas.add(next); curIndex++;} //添加数据并自增数量
		}
		
		/* 分页后的数据 */
		return new DataList<>(datas, redisTemplate.opsForHash().size(redisDto.getKey()));
	}
	@Override
	public void setHValue(String key, String hKey, Object value) {
		redisTemplate.opsForHash().put(key, hKey, value);
	}
	@Override
	public void setHValue(String key, String hKey, Object value, long timeout, TimeUnit timeUnit) {
		setHValue(key, hKey, value);
		redisTemplate.expire(key, timeout, timeUnit);
	}
	@Override
	public void delHKey(String key, List<String> hKeys) {
		if (CollectionUtils.isEmpty(hKeys)) {return;}
		redisTemplate.opsForHash().delete(key, hKeys.toArray());
	}
	
	@Override
	public DataList<String> getList(RedisDto redisDto) {
		setPage(redisDto); //设置合适分页
		
		/* 计算起始与终止位置 */
		long start = (redisDto.getPageNum() - 1) * redisDto.getPageSize();
		Long size = redisTemplate.opsForList().size(redisDto.getKey()) - 1; //总数量结束下标
		long end = redisDto.getPageNum() * redisDto.getPageSize() - 1;
		end = end > size ? size.intValue() : end; //取最小结束下标
		
		/* 分页后的值 */
		List<String> datas = redisTemplate.opsForList().range(redisDto.getKey(), start, end).stream().map(v -> String.valueOf(v)).collect(Collectors.toList());
		return new DataList<>(datas, size + 1);
	}
	@Override
	public String leftPop(String key) {
		return String.valueOf(redisTemplate.opsForList().leftPop(key));
	}
	@Override
	public String rightPop(String key) {
		return String.valueOf(redisTemplate.opsForList().rightPop(key));
	}
	@Override
	public void addList(String key, List<String> values) {
		if (CollectionUtils.isEmpty(values)) {return;}
		redisTemplate.opsForList().rightPushAll(key, values.toArray());
	}
	@Override
	public void addList(String key, long timeout, TimeUnit timeUnit, List<String> values) {
		addList(key, values);
		redisTemplate.expire(key, timeout, timeUnit);
	}
	@Override
	public void setList(String key, long index, String value) {
		redisTemplate.opsForList().set(key, index, value);
	}
	@Override
	public void delList(String key, long count, String value) {
		redisTemplate.opsForList().remove(key, count, value);
	}
	
	@Override
	public DataList<String> getSet(RedisDto redisDto) {
		setPage(redisDto); //设置合适分页
		
		/* 手动扫描分页 */
		ScanOptions options = ScanOptions.scanOptions().match(redisDto.getPattern()).build();
		Cursor<Object> scan = redisTemplate.opsForSet().scan(redisDto.getKey(), options);
		long curIndex = 0, offset = (redisDto.getPageNum() - 1) * redisDto.getPageSize();
		
		/* 迭代获取分页数据 */
		List<String> datas = new ArrayList<>();
		while(scan.hasNext()) {
			Object next = scan.next(); //获取数据
			if (curIndex >= redisDto.getPageSize()) {break;} //已获取所有分页数据
			else if (curIndex > --offset) {datas.add(String.valueOf(next)); curIndex++;} //添加数据并自增数量
		}
		
		/* 分页后的数据 */
		return new DataList<>(datas, redisTemplate.opsForSet().size(redisDto.getKey()));
	}
	@Override
	public void addSet(String key, List<String> values) {
		if (CollectionUtils.isEmpty(values)) {return;}
		redisTemplate.opsForSet().add(key, values.toArray());
	}
	@Override
	public void addSet(String key, long timeout, TimeUnit timeUnit, List<String> values) {
		addSet(key, values);
		redisTemplate.expire(key, timeout, timeUnit);
	}
	@Override
	public void delSet(String key, List<String> values) {
		redisTemplate.opsForSet().remove(key, values.toArray());
	}
	
	@Override
	public DataList<String> getZSet(RedisDto redisDto) {
		setPage(redisDto); //设置合适分页
		
		/* 计算起始与终止位置 */
		long start = (redisDto.getPageNum() - 1) * redisDto.getPageSize();
		Long size = redisTemplate.opsForZSet().size(redisDto.getKey()) - 1; //总数量结束下标
		long end = redisDto.getPageNum() * redisDto.getPageSize() - 1;
		end = end > size ? size.intValue() : end; //取最小结束下标
		
		/* 分页后的值 */
		List<String> datas = redisTemplate.opsForZSet().range(redisDto.getKey(), start, end).stream().map(v -> String.valueOf(v)).collect(Collectors.toList());
		return new DataList<>(datas, size + 1);
	}
	@Override
	public double getScore(String key, String value) {
		return redisTemplate.opsForZSet().score(key, value).doubleValue();
	}
	@Override
	public void addZSet(String key, String value, double score) {
		redisTemplate.opsForZSet().add(key, value, score);
	}
	@Override
	public void addZSet(String key, String value, double score, long timeout, TimeUnit timeUnit) {
		addZSet(key, value, score);
		redisTemplate.expire(key, timeout, timeUnit);
	}
	@Override
	public void delZSet(String key, List<String> values) {
		redisTemplate.opsForZSet().remove(key, values.toArray());
	}
	
	@Override
	public void expire(String key, long timeout, TimeUnit timeUnit) {
		redisTemplate.expire(key, timeout, timeUnit);
	}
	@Override
	public long increment(String key, long delta) {
		return redisTemplate.opsForValue().increment(key, delta);
	}
	@Override
	public long decrement(String key, long delta) {
		return redisTemplate.opsForValue().decrement(key, delta);
	}
	
	@Override
	public <T> T tryLock(String key, int timeout, boolean mini, Supplier<T> run) {
		final String biasLock = key + "-" + localIpPort.toString() + "-" + Thread.currentThread().getId();
		long recordTime = System.currentTimeMillis(); //记录时间用于判断超时
		while((System.currentTimeMillis() - recordTime) < timeout) {
			Boolean isLock = redisTemplate.opsForValue().setIfAbsent(key, true, timeout, TimeUnit.MILLISECONDS);
			
			if (Boolean.TRUE.equals(isLock)) {
				try {
					setValue(biasLock, true, timeout, TimeUnit.MILLISECONDS); //记录偏向锁，用于嵌套锁执行
					return run.get(); //拿到锁直接执行方法并返回值
				} finally {delKey(biasLock); delKey(key);} //移除锁
			} else {
				boolean isBiasLock = getValue(biasLock, boolean.class);
				if (isBiasLock) {return run.get();} //有偏向锁可执行
			}
			
			try {Thread.sleep(mini ? 50L : 500L);} catch (InterruptedException e) {} //等待周期
		}
		throw new RuntimeException(String.format("获取[%s]锁超时！", key));
	}
	@Override
	public RedisTemplate<String, Object> getRedisTemplate() {return this.redisTemplate;}
	
	/* 设置分页大小 */
	private void setPage(BaseDto limit) {
		if (limit.getPageNum() == null || limit.getPageNum() < 1) {limit.setPageNum(1L);}
		if (limit.getPageSize() == null || limit.getPageSize() < 1) {limit.setPageSize(20L);}
	}
}
