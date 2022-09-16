package org.city.common.api.annotation.sql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.constant.Operation;
import org.city.common.api.constant.group.Default;
import org.city.common.api.in.sql.Crud;

/**
 * @作者 ChengShi
 * @日期 2022-07-19 09:27:23
 * @版本 1.0
 * @描述 条件（使用被注解的字段名用做当前表字段名）
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Condition {
	/**
	 * @描述 条件排序，数字越小越先执行
	 */
	public int order();
	/**
	 * @描述 条件分组标识
	 */
	public int[] groups() default {Default.VALUE};
	/**
	 * @描述 字段别名（取别名的值）
	 */
	public String alias() default "";
	/**
	 * @描述 操作符
	 */
	public Operation make() default Operation.Equals;
	/**
	 * @描述 条件固定值（优先使用，会尝试格式化转成int类型，所以没有字符串型int类型）
	 */
	public String fixVal() default "";
	/**
	 * @描述 枚举条件固定值（需配合fixVal使用，当有此值时会将fixVal当成name获取枚举）
	 * @return 枚举的toString方法（如果fixVal没有值则不生效）
	 */
	@SuppressWarnings("rawtypes")
	public Class<? extends Enum> fixValEnum() default Enum.class;
	/**
	 * @描述 是否左模糊（只对Like操作符有用）
	 */
	public boolean isLeft() default true;
	/**
	 * @描述 是否右模糊（只对Like操作符有用）
	 */
	public boolean isRight() default true;
	/**
	 * @描述 是否and查询，默认and查询
	 */
	public boolean isAnd() default true;
	/**
	 * @描述 连接条件（只对查询有效，使用连接表字段名做条件，优先级大于当前表字段名）
	 */
	public Join joinTable() default @Join(join = Crud.class);
}
