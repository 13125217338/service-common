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
 * @描述 字段（只对查询有效，使用被注解的字段名用做当前表字段名）
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Field {
	/**
	 * @描述 字段添加顺序
	 */
	public int order();
	/**
	 * @描述 自定义接收值字段名（Join无效）
	 */
	public String receiveFieldName() default "";
	/**
	 * @描述 字段分组标识
	 */
	public int[] groups() default {Default.VALUE};
	/**
	 * @描述 自定义函数
	 */
	public MathSql mathSql() default MathSql.Normal;
	/**
	 * @描述 自定义函数值（优先级小于mathSqlVal方法）
	 */
	public String[] vals() default {};
	/**
	 * @描述 自定义函数值（需交给Spring管理）
	 */
	public Class<? extends MathSqlValue> mathSqlVal() default MathSqlValue.class;
	/**
	 * @描述 连接条件（只对查询有效，使用连接表字段名做接收值，优先级大于当前表字段名）
	 */
	public Join joinTable() default @Join(join = Crud.class);
}
