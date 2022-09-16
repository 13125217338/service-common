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
 * @描述 多个连接的表（针对getJoin方法配套使用）
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JoinTable {
	/**
	 * @描述 多个连接的表（只对查询有效）
	 */
	public Join[] value();
}
