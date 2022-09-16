package org.city.common.api.annotation.make;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.in.MakeInvoke;

/**
 * @作者 ChengShi
 * @日期 2022-07-01 17:59:48
 * @版本 1.0
 * @描述 操作执行
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Make {
	/**
	 * @描述 在当前服务器操作（实现类必须交给Spring管理）
	 */
	public Class<? extends MakeInvoke> invoke();
	/**
	 * @描述 自定义参数（通常用作标记）
	 */
	public int value() default 0;
	/**
	 * @描述 自定义参数（支持${key}取配置值、#{parameter}取方法入参值、&{method}取执行方法返回值）
	 */
	public String[] values() default {};
}
