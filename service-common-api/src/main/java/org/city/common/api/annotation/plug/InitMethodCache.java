package org.city.common.api.annotation.plug;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @作者 ChengShi
 * @日期 2022年8月20日
 * @版本 1.0
 * @描述 项目启动初始化方法缓存
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InitMethodCache {
	/**
	 * @描述 全局唯一获取ID（支持${key}取配置值，支持#{method}执行当前类无参方法获取String类型返回值）
	 */
	public String id();
	/**
	 * @描述 自定义参数（支持${key}取配置值，支持#{method}执行当前类无参方法获取String类型返回值）
	 */
	public String value() default "";
}
