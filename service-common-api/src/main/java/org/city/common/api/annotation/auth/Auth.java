package org.city.common.api.annotation.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 14:55:19
 * @版本 1.0
 * @描述 验证注解
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auth {
	/**
	 * @描述 验证回调对应@AuthImpl中的id值（支持${key}取配置值）
	 */
	public String id();
	/**
	 * @描述 自定义参数（支持${key}取配置值与#{key}取方法入参）
	 */
	public String[] values() default {};
}
