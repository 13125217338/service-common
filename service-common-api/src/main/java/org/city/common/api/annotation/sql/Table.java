package org.city.common.api.annotation.sql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 11:51:28
 * @版本 1.0
 * @描述 表申明
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Table {
	/**
	 * @描述 对应表名
	 */
	public String name();
	/**
	 * @描述 表别名
	 */
	public String alias() default "";
	/**
	 * @描述 序列化时间格式，如为空字符则使用默认序列化方式
	 */
	public String dateFormat() default "yyyy-MM-dd HH:mm:ss";
}
