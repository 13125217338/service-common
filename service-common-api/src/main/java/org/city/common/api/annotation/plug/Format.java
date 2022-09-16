package org.city.common.api.annotation.plug;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.in.FormatFieldValue;

/**
 * @作者 ChengShi
 * @日期 2022-08-17 15:43:09
 * @版本 1.0
 * @描述 格式化字段值
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Format {
	/**
	 * @描述 自定义格式化实现类（需交给Spring管理）
	 */
	@SuppressWarnings("rawtypes")
	public Class<? extends FormatFieldValue> format() default FormatFieldValue.class;
	/**
	 * @描述 自定义参数（支持${key}取配置值）
	 */
	public String[] values() default {};
}
