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
 * @描述 操作执行
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MakeInvoke {
	/**
	 * @描述 在当前服务器操作（实现类必须交给Spring管理）
	 */
	public Class<? extends org.city.common.api.in.MakeInvoke> invoke();
	/**
	 * @描述 自定义参数（支持${key}取配置值与#{key}取方法入参）
	 */
	public String[] values() default {};
}
