package org.city.common.api.annotation.sql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @作者 ChengShi
 * @日期 2022年8月6日
 * @版本 1.0
 * @描述 自定义数据源
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyDataSource {
	/**
	 * @描述 指定数据源ID
	 */
	public String dsId();
}
