package org.city.common.api.in.sql;

import org.city.common.api.dto.remote.RemoteTransactionalDto;

/**
 * @作者 ChengShi
 * @日期 2022-12-01 13:56:31
 * @版本 1.0
 * @描述 远程分布式事务
 */
public interface RemoteTransactional {
	/**
	 * @描述 添加一个事务
	 * @param transactionalId 事务ID
	 */
	public void add(String transactionalId);
	
	/**
	 * @描述 获取事务参数
	 * @param transactionalId 事务ID
	 * @return 事务参数
	 */
	public RemoteTransactionalDto get(String transactionalId);
	
	/**
	 * @描述 移除事务
	 * @param transactionalId 事务ID
	 */
	public void remove(String transactionalId);
}
