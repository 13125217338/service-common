package org.city.common.core.entity;

import java.sql.Timestamp;

import lombok.Getter;
import lombok.Setter;

/**
 * @作者 ChengShi
 * @日期 2022-07-06 11:51:20
 * @版本 1.0
 * @描述 时间实体类
 */
@Setter
@Getter
public class TimeEntity extends BaseEntity {
	/** 创建时间 */
	private Timestamp createTime;
	/** 更新时间 */
	private Timestamp updateTime;
}
