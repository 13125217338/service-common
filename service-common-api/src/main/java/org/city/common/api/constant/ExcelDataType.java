package org.city.common.api.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @作者 ChengShi
 * @日期 2025-07-03 16:59:26
 * @版本 1.0
 * @描述 单元格数据类型
 */
@Getter
@AllArgsConstructor
public enum ExcelDataType {
	STRING("字符串"),
	FILE_IMAGE("文件图片"),
	URL_IMAGE("链接图片"),
	;
	
	/* 单元格数据类型描述 */
	private final String desc;
}
