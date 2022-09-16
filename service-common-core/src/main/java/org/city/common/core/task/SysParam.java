package org.city.common.core.task;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2023年3月2日
 * @版本 1.0
 * @描述 同步参数
 */
@Data
@Accessors(chain = true)
public class SysParam {
	/* 子线程返回值 */
	private Object subReturn;
	/* 主线程参数 */
	private Object masterParam;
	/* 主线程返回值 */
	private Object masterReturn;
	/* 是否继续等待 */
	private boolean isContinue;
}
