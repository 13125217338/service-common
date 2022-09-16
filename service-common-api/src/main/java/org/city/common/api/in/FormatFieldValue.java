package org.city.common.api.in;

import java.lang.reflect.Field;

/**
 * @作者 ChengShi
 * @日期 2022-08-17 15:41:12
 * @版本 1.0
 * @描述 自定义格式化字段值
 */
public interface FormatFieldValue {
	/**
	 * @描述 自定义格式化
	 * @param field 待格式字段
	 * @param fieldVal 原字段值
	 * @param type 自定义类型
	 * @return 格式化结果
	 * @throws Exception
	 */
	public Object format(Field field, Object fieldVal, int type) throws Exception;
}
