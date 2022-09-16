package org.city.common.api.annotation.sql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.in.sql.DefaultDataSource;
import org.city.common.api.in.sql.MyDataSource;

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
	 * @描述 默认表名
	 */
	public String name();
	/**
	 * @描述 默认表别名
	 */
	public String alias() default "";
	/**
	 * @描述 自定义数据源（实现类需交给Spring管理）
	 */
	public Class<? extends MyDataSource> dataSource() default DefaultDataSource.class;
}
