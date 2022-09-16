package org.city.common.api.entity;

import java.sql.Timestamp;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;

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
public class TimeEntity extends BaseEntity {
	/** 创建时间 - 创建时自动设置当前时间 */
	@TableField(fill = FieldFill.INSERT)
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	private Timestamp createTime;
	/** 更新时间 - 更新时自动设置当前时间 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	private Timestamp updateTime;
}
