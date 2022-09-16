package org.city.common.api.in.sql;

import org.city.common.api.dto.BaseDto;

/**
 * @作者 ChengShi
 * @日期 2022-08-28 09:24:05
 * @版本 1.0
 * @描述 分页限制
 */
public interface Limit {
	/**
	 * @描述 分页处理
	 * @param baseDto 分页信息
	 */
	public void handler(BaseDto baseDto);
}
