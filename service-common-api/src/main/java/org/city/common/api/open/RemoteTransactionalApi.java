package org.city.common.api.open;

import org.city.common.api.annotation.plug.Remote;
import org.city.common.api.dto.remote.RemoteTransactionalDto;

/**
 * @作者 ChengShi
 * @日期 2022年10月15日
 * @版本 1.0
 * @描述 远程分布式事务
 */
@Remote
public interface RemoteTransactionalApi {
	/**
	 * @描述 远程执行事务Sql
	 * @param transactionalId 事务ID
	 * @param dto 事务参数
	 * @return 执行结果
	 * @throws Throwable
	 */
	public Object exec(String transactionalId, RemoteTransactionalDto dto) throws Throwable;
}
