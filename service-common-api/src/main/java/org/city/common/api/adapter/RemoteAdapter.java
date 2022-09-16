package org.city.common.api.adapter;

import java.util.List;

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
}
