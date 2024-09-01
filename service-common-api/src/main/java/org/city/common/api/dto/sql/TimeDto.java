package org.city.common.api.dto.sql;

import java.sql.Timestamp;

import com.alibaba.fastjson.annotation.JSONField;

import io.swagger.v3.oas.annotations.Hidden;
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
	@Hidden
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	private Timestamp createTime; //创建时间 - 创建时自动设置当前时间
	@Hidden
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	private Timestamp updateTime; //更新时间 - 更新时自动设置当前时间
}
