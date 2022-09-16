package org.city.common.api.in.excel;

import java.io.File;

/**
 * @作者 ChengShi
 * @日期 2025-07-04 12:32:50
 * @版本 1.0
 * @描述 单元格进度（异步处理时必须实现该接口）
 */
public interface ExcelProcess {
	/**
	 * @描述 初始阶段
	 * @param isImport 是否导入（true=导入，false=导出）
	 * @return 进度唯一ID
	 */
	public String init(boolean isImport);
	
	/**
	 * @描述 准备阶段
	 * @param processId 进度唯一ID
	 * @param sheetName 单元格名称
	 * @param total 预估总数
	 */
	public void start(String processId, String sheetName, long total);
	
	/**
	 * @描述 处理阶段
	 * @param processId 进度唯一ID
	 * @param cur 当前处理数
	 */
	public void handler(String processId, long cur);
	
	/**
	 * @描述 完成阶段
	 * @param processId 进度唯一ID
	 * @param realTotal 实际总数
	 * @param temp 处理后的临时文件（导入=错误信息，导出=详细数据）
	 */
	public void finish(String processId, long realTotal, File temp);
	
	/**
	 * @描述 异常阶段
	 * @param processId 进度唯一ID
	 * @param e 异常信息
	 */
	public void error(String processId, Throwable e);
}
