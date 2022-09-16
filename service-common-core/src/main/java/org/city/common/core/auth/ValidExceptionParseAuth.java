package org.city.common.core.auth;

import org.city.common.api.dto.ExceptionDto.CustomException;
import org.city.common.api.in.parse.ExceptionParse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;

/**
 * @作者 ChengShi
 * @日期 2022-07-26 11:45:05
 * @版本 1.0
 * @描述 Spring验证异常自定义实现
 */
@Component
public class ValidExceptionParseAuth implements ExceptionParse<BindException> {
	@Override
	public CustomException parse(BindException throwable, String defaultMsg) throws Throwable {
		return new CustomException(HttpStatus.INTERNAL_SERVER_ERROR.value(), throwable.getBindingResult().getFieldError().getDefaultMessage());
	}
}
