package org.city.common.api.annotation.sql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @作者 ChengShi
 * @日期 2022-07-19 09:32:58
 * @版本 1.0
 * @描述 条件注解（针对[getNotEmpty]方法配套使用）
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditions {
	/**
	 * @描述 多个条件
	 */
	public Condition[] value();
}
