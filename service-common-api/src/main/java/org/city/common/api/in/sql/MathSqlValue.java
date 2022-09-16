package org.city.common.api.in.sql;

import org.city.common.api.entity.BaseEntity;

/**
 * @作者 ChengShi
 * @日期 2023-08-26 18:59:25
 * @版本 1.0
 * @描述 自定义字段函数值
 */
public interface MathSqlValue {
	/**
	 * @描述 获取自定义函数值
	 * @param cur 当前表服务
	 * @param join 连接表服务（未连接则为NULL值）
	 * @param vals 注解自定义函数值（不会为NULL值）
	 * @return 返回自定义函数值
	 */
	public String[] getVals(Crud<? extends BaseEntity> cur, Crud<? extends BaseEntity> join, String[] vals);
}
