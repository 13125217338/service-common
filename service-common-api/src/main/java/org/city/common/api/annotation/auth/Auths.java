package org.city.common.api.annotation.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.constant.AuthContidionType;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 15:05:01
 * @版本 1.0
 * @描述 多个验证
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auths {
	/**
	 * @描述 多个验证条件
	 */
	public AuthContidionType type() default AuthContidionType.AND;
	/**
	 * @描述 验证
	 */
	public Auth[] auths();
}
