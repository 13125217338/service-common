package org.city.common.api.annotation.excel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.constant.ExcelDataType;

/**
 * @作者 ChengShi
 * @日期 2025-07-03 17:06:25
 * @版本 1.0
 * @描述 单元格导出注解
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelExport {
	/**
	 * @描述 导出字段名
	 */
	public String name() default "";
	/**
	 * @描述 字段顺序
	 */
	public int order() default 0;
	/**
	 * @描述 数据类型
	 */
	public ExcelDataType dataType() default ExcelDataType.STRING;
	/**
	 * @描述 工作表名称（类上才有效）
	 */
	public String sheetName() default "详细数据";
}
