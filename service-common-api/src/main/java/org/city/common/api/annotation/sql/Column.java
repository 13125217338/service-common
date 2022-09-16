package org.city.common.api.annotation.sql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @作者 ChengShi
 * @日期 2022-06-22 10:12:42
 * @版本 1.0
 * @描述 列申明（只对[Entity]字段有效）
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Column {
	/**
	 * @描述 手动指定列名
	 */
	public String name() default "";
	/**
	 * @描述 当参数没有值时的默认值（默认值将原样输出，请慎用，只对添加有效）
	 */
	public String value() default "";
}
