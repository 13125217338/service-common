package org.city.common.api.annotation.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.constant.AuthType;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 14:55:19
 * @版本 1.0
 * @描述 验证
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auth {
	/**
	 * @描述 验证回调对应@AuthFilter中的id值
	 */
	public String id();
	/**
	 * @描述 自定义参数（通常用作标记）
	 */
	public int value() default 0;
	/**
	 * @描述 自定义参数（支持${key}取配置值、#{parameter}取方法入参值、&{method}取执行方法返回值）
	 */
	public String[] values() default {};
	/**
	 * @描述 验证条件
	 */
	public AuthType type() default AuthType.AND;
}
