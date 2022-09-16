package org.city.common.api.in.parse;

import org.city.common.api.dto.ExceptionDto.CustomException;

/**
 * @作者 ChengShi
 * @日期 2022-07-26 11:57:17
 * @版本 1.0
 * @描述 自定义解析异常错误
 */
public interface ExceptionParse<T extends Throwable> {
	/**
	 * @描述 自定义解析异常错误（泛型类及子类都会进入，多个匹配只会进入一个实现者）
	 * @param throwable 待解析的异常错误
	 * @param defaultMsg 原默认异常信息
	 * @return 自定义异常
	 * @throws Throwable
	 */
	public CustomException parse(T throwable, String defaultMsg) throws Throwable;
}
