package org.city.common.core.service;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.DataList;
import org.city.common.api.dto.RedisDto;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.in.Runnable;
import org.city.common.api.in.Task;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.redis.RedisMake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import lombok.AllArgsConstructor;

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
	@Autowired
	private Task task;
	/* 待监控数据 */
	private final Set<Entry<Monitor, Integer>> MONITORS = new TreeSet<>((o1, o2) -> o1.getValue().compareTo(o2.getValue()));
	
	@PostConstruct
	private void init() {
		task.schedula(CommonConstant.REDIS_MONITOR_LOCK_ID, new Runnable() {
			@Override
			public void run() throws Throwable {
				while (MONITORS.size() > 0) {
					/* 获取第一个最小时间监控数据 */
					Iterator<Entry<Monitor, Integer>> iterator = MONITORS.iterator();
					Entry<Monitor, Integer> first = iterator.next();
					long recordTime = System.currentTimeMillis(); //记录时间 - 计算等待时间
					if (first.getValue() > 0) {synchronized (MONITORS) {MONITORS.wait(first.getValue());}}
					
					/* 验证监控状态 */
					Monitor monitor = first.getKey();
					synchronized (monitor) {
						if (monitor.isRun && !expire(monitor.key, monitor.timeout, TimeUnit.MILLISECONDS)) { //运行中且过期未设置成功 - 退出监控
							monitor.isRun = false;
						}
						if (monitor.isRun) { //运行中 - 偏向锁更新过期时间
							expire(monitor.biasLock, monitor.timeout, TimeUnit.MILLISECONDS);
						}
					}
					/* 计算等待时间 - 消除expire远程调用误差时间 */
					caculateTime((int) (System.currentTimeMillis() - recordTime), first.getKey().key);
				}
			}
			/* 计算时间 */
			private void caculateTime(int waitTime, String key) {
				List<Entry<Monitor, Integer>> clone = new ArrayList<>();
				synchronized (MONITORS) {clone.addAll(MONITORS); MONITORS.clear();}
				/* 计算剩余时间并重新添加 */
				for (Entry<Monitor, Integer> entry : clone) {
					if (entry.getKey().isRun) {
						/* 如果是第一个key则重新计时 - 否则计算剩余时间 */
						if (key.equals(entry.getKey().key)) {entry.setValue(entry.getKey().timeout >> 2);}
						else {
							int lastTime = entry.getValue() - waitTime;
							entry.setValue(lastTime < 0 ? 0 : lastTime);
						}
						/* 重新添加排序 - 未运行不添加 */
						synchronized (MONITORS) {MONITORS.add(entry);}
					}
				}
			}
		}, () -> String.valueOf(Integer.MAX_VALUE), false);
	}
	
	@Override
	public DataList<String> keys(RedisDto redisDto) {
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
	public boolean expire(String key, long timeout, TimeUnit timeUnit) {
		return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, timeUnit));
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
	public RedisTemplate<String, Object> getRedisTemplate() {
		return redisTemplate;
	}
	
	@Override
	public long getNowTime() {
		return redisTemplate.execute(new RedisCallback<Long>() {
			@Override
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.time();
			}
		});
	}
	
	@Override
	public <T> T tryLock(String key, int timeout, boolean mini, Supplier<T> run) {
		Assert.isTrue(timeout > 99, "同步锁过期时间不能小于100毫秒！"); //不能小于100毫秒
		String tranId = RemoteAdapter.REMOTE_TRANSACTIONAL.get(), curLock = CommonConstant.START_TIME + "-" + Thread.currentThread().getId() + "$" + localIpPort.toString();
		final String biasLock = key + "-" + (tranId == null ? curLock : tranId); //偏向锁
		final long recordTime = System.currentTimeMillis(); //记录时间用于判断超时
		
		/* 未超时循环获取分布式锁 */
		while((System.currentTimeMillis() - recordTime) < timeout) {
			Boolean isLock = redisTemplate.opsForValue().setIfAbsent(key, true, timeout, TimeUnit.MILLISECONDS);
			if (Boolean.TRUE.equals(isLock)) { //拿到分布式锁则设置偏向锁
				final Monitor monitor = setMonitor(key, biasLock, timeout); //监控更新过期时间
				try {
					setValue(biasLock, true, timeout, TimeUnit.MILLISECONDS); //设置偏向锁，用于嵌套锁执行
					return run.get(); //当前锁直接执行
				} finally {closeMonitor(monitor); delKey(biasLock); delKey(key);} //关闭监控 - 移除偏向锁 - 移除分布式锁
			} else { //未拿到锁则查询有无偏向锁
				boolean isBiasLock = getValue(biasLock, boolean.class);
				if (isBiasLock) {return run.get();} //有偏向锁可执行
			}
			
			/* 等待周期 */
			double sleep = mini ? (Math.random() * 80 + 20) : (Math.random() * 800 + 200);
			try {Thread.sleep((long) sleep);} catch (InterruptedException e) {} //等待周期
		}
		throw new RuntimeException(String.format("获取[%s]锁超时！", key));
	}
	
	/* 设置监控数据 */
	private Monitor setMonitor(String key, String biasLock, int timeout) {
		Monitor monitor = new Monitor(true, key, biasLock, timeout);
		synchronized (MONITORS) {
			MONITORS.add(new Entry<RedisMakeService.Monitor, Integer>() {
				private int $timeout = timeout >> 2;
				@Override
				public Integer setValue(Integer value) {return $timeout = value;}
				@Override
				public Integer getValue() {return $timeout;}
				@Override
				public Monitor getKey() {return monitor;}
			});
			MONITORS.notifyAll();
		}
		/* 通知定时任务执行 */
		task.NotifySchedula(CommonConstant.REDIS_MONITOR_LOCK_ID);
		return monitor;
	}
	/* 关闭监控数据 */
	private void closeMonitor(Monitor monitor) {
		synchronized (monitor) {monitor.isRun = false;}
		synchronized (MONITORS) {MONITORS.notifyAll();}
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2024-08-23 13:31:21
	 * @版本 1.0
	 * @parentClass RedisMakeService
	 * @描述 监控类
	 */
	@AllArgsConstructor
	private class Monitor {
		/* 是否在运行 */
		private boolean isRun;
		/* 加锁键 */
		private String key;
		/* 偏向锁 */
		private String biasLock;
		/* 过期时间 - 毫秒 */
		private int timeout;
	}
}
