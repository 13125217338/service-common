package org.city.common.api.adapter;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.DataList;
import org.city.common.api.dto.RedisDto;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.in.redis.RedisMake;
import org.city.common.api.in.remote.RemoteSave.RemoteInfo;

/**
 * @作者 ChengShi
 * @日期 2022-09-27 09:15:34
 * @版本 1.0
 * @描述 远程调用适配器
 */
public interface RemoteAdapter {
	/** 远程调用自定义事务 */
	public final static ThreadLocal<String> REMOTE_TRANSACTIONAL = new ThreadLocal<>();
	
	/**
	 * @描述 选择适配远程对象信息
	 * @param invokes 待选择集合
	 * @param param 自定义参数（PlugUtil调用可传入）
	 * @return 选择的调用对象
	 */
	public RemoteInfo select(List<RemoteInfo> invokes, Object param);
	
	/**
	 * @描述 执行后回调信息
	 * @param invokedTime 执行时间（毫秒）
	 * @param remoteInfo 远程信息
	 */
	default void invokedInfo(int invokedTime, RemoteInfo remoteInfo) {}
	
	/**
	 * @描述 设置事务ID
	 * @param redisMake 缓存操做对象
	 * @param localIpPort 本地地址端口
	 * @param remoteConfig 远程配置信息
	 * @param tranId 事务ID
	 */
	public static void setTranId(RedisMake redisMake, RemoteIpPortDto localIpPort, RemoteConfigDto remoteConfig, String tranId) {
		if (tranId != null) {
			REMOTE_TRANSACTIONAL.set(tranId);
			DataList<String> tranIpPorts = redisMake.getSet(RedisDto.of(CommonConstant.REDIS_REMOTE_TRANSACTIONAL_SERVICE_KEY + tranId, "*", 1, Long.MAX_VALUE));
			if (!tranIpPorts.getRows().contains(localIpPort.toString())) { //无当前服务才添加执行链路
				tranIpPorts.getRows().add(localIpPort.toString());
				redisMake.addSet(CommonConstant.REDIS_REMOTE_TRANSACTIONAL_SERVICE_KEY + tranId, remoteConfig.getReadTimeout() << 1, TimeUnit.MILLISECONDS, tranIpPorts.getRows());
			}
		}
	}
	
	/**
	 * @描述 移除事务ID
	 * @param redisMake 缓存操做对象
	 */
	public static void removeTranId(RedisMake redisMake) {
		String tranId = REMOTE_TRANSACTIONAL.get();
		if (tranId != null) {
			REMOTE_TRANSACTIONAL.remove();
			redisMake.delKey(CommonConstant.REDIS_REMOTE_TRANSACTIONAL_SERVICE_KEY + tranId);
		}
	}
}
