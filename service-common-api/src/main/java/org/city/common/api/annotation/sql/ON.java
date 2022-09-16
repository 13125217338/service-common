package org.city.common.api.annotation.sql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @作者 ChengShi
 * @日期 2022年8月6日
 * @版本 1.0
 * @描述 连接条件
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ON {
	/**
	 * @描述 当前表实体字段名（找不到会直接用该值代替，意思是可以用固定值）
	 */
	public String curField();
	/**
	 * @描述 连接表实体字段名（找不到会直接用该值代替，意思是可以用固定值）
	 */
	public String joinField();
}
