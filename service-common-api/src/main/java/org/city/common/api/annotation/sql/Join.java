package org.city.common.api.annotation.sql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.constant.JoinType;
import org.city.common.api.constant.group.Default;
import org.city.common.api.in.sql.Crud;

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
	 * @描述 连接实体类字段名（只对Fields与Conditions注解有效）
	 */
	public String joinFieldName() default "";
	
	/**
	 * @描述 条件分组标识（只对JoinTable注解有效）
	 */
	public int[] groups() default {Default.VALUE};
	/**
	 * @描述 连接类型（只对JoinTable注解有效）
	 */
	public JoinType joinType() default JoinType.Inner_Join;
	/**
	 * @描述 指明当前表（只对JoinTable注解有效）
	 */
	public Cur cur() default @Cur(service = Crud.class);
	/**
	 * @描述 join连接条件（只对JoinTable注解有效）
	 */
	public ON[] ons() default {};
}
