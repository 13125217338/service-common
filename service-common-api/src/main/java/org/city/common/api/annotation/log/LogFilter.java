package org.city.common.api.annotation.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.constant.group.Default;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Level;

/**
 * @作者 ChengShi
 * @日期 2022-10-07 14:25:31
 * @版本 1.0
 * @描述 日志过滤
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface LogFilter {
	/**
	 * @描述 日志自定义分组接收（可以是某个业务类，也可以是某个异常类，获取其子类所有日志，默认所有都接收）
	 */
	public Class<?>[] groups() default {Default.class};
	/**
	 * @描述 日志接收固定等级（只接收这个等级日志，默认所有都接收，优先级大于value值）
	 */
	public int fixVal() default Level.ALL_INT;
	/**
	 * @描述 日志接收等级（接收小于等于这个等级的日志，默认所有都接收，优先级小于fixVal值）
	 */
	public int value() default Level.ALL_INT;
}
