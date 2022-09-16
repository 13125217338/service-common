package org.city.common.api.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @作者 ChengShi
 * @日期 2025-07-03 16:59:26
 * @版本 1.0
 * @描述 单元格类型
 */
@Getter
@AllArgsConstructor
public enum ExcelType {
	SYEC("同步导出当前分页数据"),
	SYEA("同步导出全部数据"),
	ASYEC("异步导出当前分页数据"),
	ASYEA("异步导出所有数据"),
	DIT("下载导入模板"),
	;
	
	/* 单元格类型描述 */
	private final String desc;
}
