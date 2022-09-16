package org.city.common.api.annotation.plug;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.constant.group.Default;

/**
 * @作者 ChengShi
 * @日期 2022-08-17 15:43:09
 * @版本 1.0
 * @描述 赋值别名
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AliasTo {
	/**
	 * @描述 别名字段（JSONPath写法，数组只能一级）
	 */
	public String[] value() default "";
	/**
	 * @描述 别名分组标识
	 */
	public int[] groups() default {Default.VALUE};
}
