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
 * @描述 分组字段
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GroupField {
	/**
	 * @描述 字段表
	 */
	public Class<?> field();
	/**
	 * @描述 字段表别名
	 */
	public String alias() default "";
	/**
	 * @描述 字段名称（必须非基本类型，必须非数组类型）
	 */
	public String fieldName();
}
