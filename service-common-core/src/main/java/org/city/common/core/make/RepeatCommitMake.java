package org.city.common.core.make;

import java.util.concurrent.TimeUnit;

import org.city.common.api.in.MakeInvoke;
import org.city.common.api.in.redis.RedisMake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2023年6月28日
 * @版本 1.0
 * @描述 重复提交操作
 */
@Slf4j
@Component
public class RepeatCommitMake implements MakeInvoke {
	@Autowired
    private RedisMake redisMake;
	
	@Override
	public void invoke(Process process, int value, String[] values) throws Throwable {
		if (values.length == 0) {throw new RuntimeException("Redis重复操作对应[values]值不能为空！");}
		int unlockTime = value < 1 ? 3 : value;
		
		/* 获取到锁等待自动解锁 - 未获取到锁抛出异常 */
		if (redisMake.getRedisTemplate().opsForValue().setIfAbsent(values[0], true, unlockTime, TimeUnit.SECONDS)) {
			NotReturn(process);
		} else {
			log.error(String.format("对应Redis[%s]锁重复提交！", values[0]));
			throw new RuntimeException("请勿重复提交！");
		}
	}
}
