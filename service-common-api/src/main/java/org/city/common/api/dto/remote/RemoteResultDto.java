package org.city.common.api.dto.remote;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-09-28 10:41:59
 * @版本 1.0
 * @描述 远程结果信息
 */
@Data
@Accessors(chain = true)
public class RemoteResultDto {
	/* 返回结果 */
	private Object result;
	/* 返回异常 */
	private Throwable throwable;
}
