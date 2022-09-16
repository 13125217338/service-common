package org.city.common.api.annotation.sql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.constant.MathSql;
import org.city.common.api.constant.group.Default;
import org.city.common.api.in.sql.Crud;
import org.city.common.api.in.sql.MathSqlValue;

/**
 * @作者 ChengShi
 * @日期 2022年8月6日
 * @版本 1.0
 * @描述 添加字段（只对查询有效）
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Field {
	/**
	 * @描述 条件分组标识
	 */
	public int[] groups() default {Default.VALUE};
	/**
	 * @描述 字段添加顺序
	 */
	public int order();
	/**
	 * @描述 函数类型
	 */
	public MathSql mathSql() default MathSql.Nomal;
	/**
	 * @描述 接收值字段名（有joinTable时该值无效）
	 */
	public String receiveName() default "";
	/**
	 * @描述 函数需要时的值（优先使用）
	 */
	public String[] vals() default {};
	/**
	 * @描述 函数需要时的值（如果vals没有指定值会使用该值，需交给Spring管理）
	 */
	public Class<? extends MathSqlValue> val() default MathSqlValue.class;
	/**
	 * @描述 连接条件（只对查询有效）
	 */
	public Join joinTable() default @Join(join = Crud.class);
}
