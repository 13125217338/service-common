package org.city.common.api.annotation.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.annotation.plug.GlobalExtension;
import org.city.common.api.constant.group.Default;

/**
 * @作者 ChengShi
 * @日期 2022-06-26 17:16:35
 * @版本 1.0
 * @描述 日志回调实现
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@GlobalExtension(IdAs = "id")
public @interface LogCallBackImpl {
	/**
	 * @描述 提供者唯一ID值
	 */
	public String id();
	
	/**
	 * @描述 日志自定义分组接收（可以是某个业务类，也可以是某个异常类，获取其子类所有日志，默认所有都接收）
	 */
	public Class<?>[] groups() default {Default.class};
}
