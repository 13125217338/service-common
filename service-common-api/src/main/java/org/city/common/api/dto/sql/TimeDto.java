package org.city.common.api.dto.sql;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-07-06 11:51:20
 * @版本 1.0
 * @描述 时间参数
 */
@Setter
@Getter
@Accessors(chain = true)
public class TimeDto extends BaseDto {
	/** 创建时间 */
	private String createTime;
	/** 更新时间 - 更新时自动设置当前时间 */
	private String updateTime;
}
