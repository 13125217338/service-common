package org.city.common.api.in.excel;

import java.util.List;

/**
 * @作者 ChengShi
 * @日期 2025-07-03 16:47:00
 * @版本 1.0
 * @描述 单元格导入
 */
public interface ExcelImport<T> {
	/**
	 * @描述 初始化（同步导入时会调用）
	 */
	default void init() {}
	
	/**
	 * @描述 处理多行数据
	 * @param rowDatas 多行数据
	 */
	public void handlerRowDatas(List<T> rowDatas);
}
