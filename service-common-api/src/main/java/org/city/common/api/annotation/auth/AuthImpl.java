package org.city.common.api.annotation.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.annotation.plug.GlobalExtension;

/**
 * @作者 ChengShi
 * @日期 2022-06-24 23:19:34
 * @版本 1.0
 * @描述 验证实现
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@GlobalExtension(IdAs = "id")
public @interface AuthImpl {
	/**
	 * @描述 提供者唯一ID值
	 */
	public String id();
}
