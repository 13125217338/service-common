package org.city.common.api.annotation.sql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.constant.Operation;
import org.city.common.api.in.sql.Crud;

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
	 * @描述 指明当前表（主要用于多个连接表条件）
	 */
	public Cur cur() default @Cur(service = Crud.class);
	/**
	 * @描述 当前表实体字段名（找不到会直接用该值代替，意思是可以用固定值）
	 */
	public String curField();
	/**
	 * @描述 连接表实体字段名（找不到会直接用该值代替，意思是可以用固定值）
	 */
	public String joinField();
	/**
	 * @描述 是否and查询，默认and查询
	 */
	public boolean isAnd() default true;
	/**
	 * @描述 操作符
	 */
	public Operation make() default Operation.Equals;
}
