package org.city.common.core.make;

import org.city.common.api.in.MakeInvoke;
import org.city.common.api.in.redis.RedisMake;
import org.city.common.api.in.util.ThrowableMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2023年6月28日
 * @版本 1.0
 * @描述 Redis加锁操作
 */
@Component
public class RedisLockMake implements MakeInvoke,ThrowableMessage {
	@Autowired
    private RedisMake redisMake;
	
	@Override
	public void invoke(Process process, int value, String[] values) throws Throwable {
		if (values.length == 0) {throw new RuntimeException("Redis加锁操作对应[values]值不能为空！");}
		Integer waitTime = value < 10 ? Short.MAX_VALUE : value;
		
		/* 分布式加锁执行 */
		redisMake.tryLock(values[0], waitTime, false, () -> {
			try {
				process.nextInvoke();
				NotReturn(process);
				return null;
			} catch (Throwable e) {
				Throwable realExcept = getRealExcept(e);
				throw new RuntimeException(realExcept.getMessage(), realExcept);
			}
		});
	}
}
