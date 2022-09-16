package org.city.common.api.spi;

import org.city.common.api.annotation.plug.Remote;
import org.city.common.api.dto.LogDto;

/**
 * @作者 ChengShi
 * @日期 2022-06-26 17:11:35
 * @版本 1.0
 * @描述 日志回调打印提供者（需要与@LogFilter注解配套使用）
 */
@Remote
public interface LogCallBackProvider {
	/**
	 * @描述 回调日志信息
	 * @param logDto 日志信息
	 */
	public void message(LogDto logDto);
}
