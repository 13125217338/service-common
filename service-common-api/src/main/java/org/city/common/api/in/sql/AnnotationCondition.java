package org.city.common.api.in.sql;

import org.city.common.api.dto.sql.Condition;

/**
 * @作者 ChengShi
 * @日期 2022年8月7日
 * @版本 1.0
 * @描述 注解条件操作
 */
public interface AnnotationCondition {
	/**
	 * @描述 获取当前实体类字段值不为空的条件（只获取有@Conditions注解的字段）
	 * @param groups 与注解@Condition下的groups对应，不传获取所有默认条件
	 * @return 当前操作对象
	 */
	public AnnotationCondition getNotEmpty(int...groups);
	/**
	 * @描述 获取当前实体类字段条件（只获取有@Fields注解的字段）
	 * @param groups 与注解@Field下的groups对应，不传获取所有默认字段
	 * @return 当前操作对象
	 */
	public AnnotationCondition getField(int...groups);
	/**
	 * @描述 获取设置后的条件
	 * @return 条件对象
	 */
	public Condition cd();
}
