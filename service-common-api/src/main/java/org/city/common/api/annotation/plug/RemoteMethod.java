package org.city.common.api.annotation.plug;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @作者 ChengShi
 * @日期 2022-10-07 13:07:45
 * @版本 1.0
 * @描述 标识方法为可远程调用（接口类必须先注解[@Remote]才有效）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RemoteMethod {
	/**
	 * @描述 方法定义名
	 */
	public String name();
	/**
	 * @描述 方法自定义值（支持${key}取配置值）
	 */
	public String value() default "";
}
