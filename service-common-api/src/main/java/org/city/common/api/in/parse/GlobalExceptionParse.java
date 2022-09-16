package org.city.common.api.in.parse;

import org.city.common.api.dto.Response;

/**
 * @作者 ChengShi
 * @日期 2022-07-26 11:57:17
 * @版本 1.0
 * @描述 全局异常解析
 */
public interface GlobalExceptionParse {
	/**
	 * @描述 全局异常解析
	 * @param throwable 待解析的异常错误
	 * @return 解析结果
	 */
	public Response<?> getResponse(Throwable throwable);
}
