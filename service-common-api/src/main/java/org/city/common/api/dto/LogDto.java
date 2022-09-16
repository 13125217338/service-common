package org.city.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @作者 ChengShi
 * @日期 2022年7月26日
 * @版本 1.0
 * @描述 日志参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogDto {
	/* 用户打印信息 */
	private String customMsg;
	/* 异常信息（如果有） */
	private String throwMsg;
	/* 所有打印信息 */
	private String allMsg;
	/* 日志等级 */
	private int levelInt;
	/* 用户打印位置的类名 */
	private String customClass;
	/* 抛出异常的类名（如果有） */
	private String throwClass;
	/* 用户打印的服务名 */
	private String appName;
}
