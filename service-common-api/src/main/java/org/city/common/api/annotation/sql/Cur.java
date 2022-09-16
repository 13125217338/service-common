package org.city.common.api.annotation.sql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @作者 ChengShi
 * @日期 2022-08-15 17:55:50
 * @版本 1.0
 * @描述 当前表
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cur {
	/**
	 * @描述 指明当前表
	 */
	public Class<?> service();
	/**
	 * @描述 当前表别名
	 */
	public String alias() default "";
}
