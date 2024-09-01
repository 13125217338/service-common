package org.city.common.core.make;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.city.common.api.in.MakeInvoke;
import org.city.common.api.in.redis.RedisMake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2023年6月28日
 * @版本 1.0
 * @描述 Redis加锁操作
 */
@Component
public class RedisLockMake implements MakeInvoke {
	@Autowired
    private RedisMake redisMake;
	private final ThreadLocal<List<Entry<String, Integer>>> CUR_KEYS = new ThreadLocal<>(); //当前锁
	
	@Override
	public void invoke(Process process, int value, String[] values) throws Throwable {
		if (values.length == 0) {throw new RuntimeException("Redis加锁操作对应[values]值不能为空！");}
		if (CUR_KEYS.get() == null) {CUR_KEYS.set(new ArrayList<>());}
		Integer waitTime = value < 10 ? Short.MAX_VALUE : value;
		
		/* 添加当前锁信息 */
		CUR_KEYS.get().add(new Entry<String, Integer>() {
			@Override
			public Integer setValue(Integer value) {return null;}
			@Override
			public Integer getValue() {return waitTime;}
			@Override
			public String getKey() {return values[0];}
		});
	}
	
	@Override
	public void finallys(Process process, int value, String[] values) throws Throwable {
		List<Entry<String, Integer>> keys = CUR_KEYS.get();
		if (keys == null || keys.isEmpty()) {return;}
		exec(process, keys.iterator()); //加锁执行 - 多个锁顺序执行
	}
	
	/* 加锁顺序执行 */
	private void exec(Process process, Iterator<Entry<String, Integer>> keys) {
		if (keys.hasNext()) {
			Entry<String, Integer> key = keys.next(); //当前锁信息
			redisMake.tryLock(key.getKey(), key.getValue(), false, () -> {
				keys.remove(); //获取锁后删除当前信息
				exec(process, keys);  //递归操作其他锁信息
				return null;
			});
		} else {
			CUR_KEYS.remove();
			try {NotReturn(process);} //已获取全部锁 - 开始执行原方法
			catch (Throwable e) {throw new RuntimeException(e);}
		}
	}
}
