package org.city.common.api.in.sql;

import org.city.common.api.dto.BaseDto;

/**
 * @作者 ChengShi
 * @日期 2022年8月7日
 * @版本 1.0
 * @描述 函数条件值
 */
public interface MathSqlValue {
	/**
	 * @描述 自定义设置函数值
	 * @param cur 当前表操作对象
	 * @param join 连接表操作对象（如果有连接）
	 * @return 自定义设置的值
	 * @throws Exception
	 */
	public String[] setVals(Crud<? extends BaseDto> cur, Crud<? extends BaseDto> join) throws Exception;
}
