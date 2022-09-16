package org.city.common.api.annotation.make;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @作者 ChengShi
 * @日期 2022-07-01 17:59:48
 * @版本 1.0
 * @描述 操作注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Makes {
	/**
	 * @描述 多个操作执行
	 */
	public MakeInvoke[] value() default {};
}
