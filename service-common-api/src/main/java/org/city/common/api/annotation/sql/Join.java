package org.city.common.api.annotation.sql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.constant.JoinType;
import org.city.common.api.constant.group.Default;

/**
 * @作者 ChengShi
 * @日期 2022年8月6日
 * @版本 1.0
 * @描述 连接条件（只对查询有效）
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Join {
	/**
	 * @描述 连接的表别名（为空使用join表别名）
	 */
	public String alias() default "";
	/**
	 * @描述 连接的表
	 */
	public Class<?> join();
	/**
	 * @描述 连接表忽略索引名称（只对Joins注解有效）
	 */
	public String ignore() default "";
	/**
	 * @描述 连接实体类字段名（只对Fields与Conditions注解有效）
	 */
	public String joinFieldName() default "";
	/**
	 * @描述 连接实体类忽略字段名（只对Fields注解有效）
	 */
	public String[] ignoreFieldNames() default {};
	/**
	 * @描述 分组字段（只对Fields注解有效）
	 */
	public GroupField[] groupFields() default {};
	/**
	 * @描述 分组分页数量（只对Fields注解有效）
	 */
	public int limit() default Integer.MAX_VALUE;
	/**
	 * @描述 连接分组标识（在@Condition注解中表明查询子对象条件分组）
	 */
	public int[] groups() default {Default.VALUE};
	/**
	 * @描述 连接类型（只对Joins注解有效）
	 */
	public JoinType joinType() default JoinType.Inner_Join;
	/**
	 * @描述 join连接条件（只对Joins注解有效）
	 */
	public ON[] ons() default {};
}
