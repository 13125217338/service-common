package org.city.common.api.annotation.sql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @作者 ChengShi
 * @日期 2022年8月7日
 * @版本 1.0
 * @描述 字段注解（针对[getField]方法配套使用）
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Fields {
	/**
	 * @描述 多个字段（只对查询有效）
	 */
	public Field[] value();
}
