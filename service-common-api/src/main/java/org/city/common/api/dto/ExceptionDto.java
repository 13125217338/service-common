package org.city.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @作者 ChengShi
 * @日期 2022-06-25 01:14:15
 * @版本 1.0
 * @描述 异常信息
 */
@Getter
@AllArgsConstructor
public class ExceptionDto {
	/* App内展示信息 */
	private final String appMsg;
	/* 外部展示错误信息 */
	private final String errorMsg;
	
	/**
	 * @作者 ChengShi
	 * @日期 2022年8月22日
	 * @版本 1.0
	 * @描述 自定义异常
	 */
	@Getter
	@AllArgsConstructor
	public static class CustomException {
		/* 自定义异常码 */
		private final int code;
		/* 自定义异常信息 */
		private final String msg;
	}
}
