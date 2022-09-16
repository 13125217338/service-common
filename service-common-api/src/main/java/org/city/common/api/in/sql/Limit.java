package org.city.common.api.in.sql;

import org.city.common.api.entity.BaseEntity;

/**
 * @作者 ChengShi
 * @日期 2022-08-28 09:24:05
 * @版本 1.0
 * @描述 分页限制
 */
public interface Limit {
	/**
	 * @描述 分页处理
	 * @param baseEntity 公共实体类
	 */
	public void handler(BaseEntity baseEntity);
}
