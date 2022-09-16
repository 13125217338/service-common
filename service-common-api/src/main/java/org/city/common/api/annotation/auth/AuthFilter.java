package org.city.common.api.annotation.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2022-10-07 14:39:06
 * @版本 1.0
 * @描述 验证过滤
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface AuthFilter {
	/**
	 * @描述 验证唯一ID
	 */
	public String id();
}
