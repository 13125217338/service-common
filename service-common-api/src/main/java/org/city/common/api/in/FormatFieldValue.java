package org.city.common.api.in;

import java.lang.reflect.Field;

/**
 * @作者 ChengShi
 * @日期 2022-08-17 15:41:12
 * @版本 1.0
 * @描述 自定义格式化字段值
 */
public interface FormatFieldValue<R> {
	/**
	 * @描述 自定义格式化
	 * @param orgin 原数据对象
	 * @param field 待格式化字段
	 * @param values 已被替换的自定义参数
	 * @return 格式化后的字段值
	 * @throws Throwable
	 */
	public R format(Object orgin, Field field, String[] values) throws Throwable;
}
