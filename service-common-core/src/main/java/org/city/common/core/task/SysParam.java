package org.city.common.core.task;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2024年3月2日
 * @版本 1.0
 * @描述 同步参数
 */
@Data
@Accessors(chain = true)
public class SysParam {
	
	private Object subReturn;
	
	private Object masterParam;
	
	private Object masterReturn;
	
	private boolean isContinue;
}
