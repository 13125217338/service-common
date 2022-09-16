package org.city.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 10:25:35
 * @版本 1.0
 * @描述 全局异常信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalExceptionDto {
	/* 服务名 */
	private String appName;
	/* 错误消息 */
	private String appErroMsg;
	/* 唯一ID */
	private String trackId;
}
