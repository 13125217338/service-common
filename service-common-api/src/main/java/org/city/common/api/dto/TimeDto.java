package org.city.common.api.dto;

import org.city.common.api.constant.CommonConstant;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-07-06 11:51:20
 * @版本 1.0
 * @描述 时间实体类
 */
@Setter
@Getter
@Accessors(chain = true)
public class TimeDto extends BaseDto{
	private static final long serialVersionUID = 1L;
	/** 创建时间 */
	private String createTime;
	/** 更新时间 */
	private String updateTime = CommonConstant.getNowTime();
}
