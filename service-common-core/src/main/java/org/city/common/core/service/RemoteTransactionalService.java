package org.city.common.core.service;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.dto.remote.RemoteTransactionalDto;
import org.city.common.api.exception.RemoteTransactionalException;
import org.city.common.api.in.redis.RedisMake;
import org.city.common.api.in.sql.RemoteTransactional;
import org.city.common.api.in.util.ThrowableMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

/**
 * @作者 ChengShi
 * @日期 2022年10月15日
 * @版本 1.0
 * @描述 远程分布式事务服务
 */
@Service
public class RemoteTransactionalService implements RemoteTransactional,ThrowableMessage {
	@Autowired
	private RemoteIpPortDto localIpPort;
	@Autowired
	private RedisMake redisMake;
	@Autowired
	private RemoteConfigDto remoteConfigDto;
	
	@Override
	public void add(String tranId) {
		JSONObject trans = new JSONObject().fluentPut(localIpPort.toString(), RemoteTransactionalDto.init());
		redisMake.setValue(CommonConstant.REDIS_REMOTE_TRANSACTIONAL_KEY + tranId, trans, remoteConfigDto.getReadTimeout(), TimeUnit.MILLISECONDS);
	}
	
	@Override
	public void append(String tranId) {
		redisMake.tryLock(CommonConstant.REDIS_TRANSACTIONAL_LOCK_KEY + tranId, remoteConfigDto.getReadTimeout(), true, () -> {
			Map<String, RemoteTransactionalDto> trans = get(tranId); //获取原值用于添加
			trans.put(localIpPort.toString(), RemoteTransactionalDto.init()); //添加初始等待状态
			redisMake.setValue(CommonConstant.REDIS_REMOTE_TRANSACTIONAL_KEY + tranId, trans, remoteConfigDto.getReadTimeout(), TimeUnit.MILLISECONDS);
			return null;
		});
	}
	
	@Override
	public void setState(String tranId, boolean state, Throwable throwable) throws Throwable {
		if(!redisMake.tryLock(CommonConstant.REDIS_TRANSACTIONAL_LOCK_KEY + tranId, remoteConfigDto.getReadTimeout(), true, () -> {
			if (!state) {if (verifyResult(tranId)) {return true;}} //待设置状态失败但执行成功则什么都不做
			Map<String, RemoteTransactionalDto> trans = get(tranId); //获取原值用于更新
			trans.get(localIpPort.toString()).setState(state).setErrorMsg(throwable == null ? null : getRealExcept(throwable).toString()); //设置事务状态与错误信息
			redisMake.setValue(CommonConstant.REDIS_REMOTE_TRANSACTIONAL_KEY + tranId, trans, remoteConfigDto.getReadTimeout(), TimeUnit.MILLISECONDS);
			if (!state) {return false;} else {return true;} //待设置状态失败且继续验证则抛出异常 - 否则什么都不做
		})) {throw throwable;} //返回false代表要抛出异常
	}
	
	@Override
	public boolean verifyResult(String tranId) {
		return redisMake.tryLock(CommonConstant.REDIS_TRANSACTIONAL_LOCK_KEY + tranId, remoteConfigDto.getReadTimeout(), true, () -> {
			boolean result = true;
			for (Entry<String, RemoteTransactionalDto> entry : get(tranId).entrySet()) {
				if (Boolean.FALSE.equals(entry.getValue().getState())) {
					throw new RemoteTransactionalException(entry.getValue()); //只要有失败则手动抛出远程事务异常
				} else if (result && entry.getValue().getState() == null) {result = false;} //标记为等待状态
			}
			return result;
		});
	}
	
	/* 获取事务数据，key=服务器地址端口，value=远程事务状态 */
	private Map<String, RemoteTransactionalDto> get(String tranId) {
		Map<String, RemoteTransactionalDto> trans = redisMake.getValue(CommonConstant.REDIS_REMOTE_TRANSACTIONAL_KEY + tranId, new ParameterizedType() {
			@Override
			public Type getRawType() {return Map.class;}
			@Override
			public Type getOwnerType() {return null;}
			@Override
			public Type[] getActualTypeArguments() {return new Type[] {String.class, RemoteTransactionalDto.class};}
		});
		if (trans == null) {throw new RemoteTransactionalException("通过事务ID未找到对应事务数据！");}
		return trans;
	}
}
