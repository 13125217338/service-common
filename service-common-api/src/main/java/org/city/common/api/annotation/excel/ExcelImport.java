package org.city.common.api.annotation.excel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @作者 ChengShi
 * @日期 2025-07-03 17:06:25
 * @版本 1.0
 * @描述 单元格导入注解
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelImport {
	/**
	 * @描述 导入字段名
	 */
	public String name() default "";
	/**
	 * @描述 字段顺序
	 */
	public int order() default 0;
	/**
	 * @描述 示例
	 */
	public String demo() default "xxx";
	/**
	 * @描述 是否嵌套解析
	 */
	public boolean nesting() default false;
	/**
	 * @描述 工作表名称（类上才有效 - 必填）
	 */
	public String sheetName() default "";
	/**
	 * @描述 批量导入大小（类上才有效）
	 */
	public short batchSize() default 1000;
}
