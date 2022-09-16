package org.city.common.api.exception;

import org.city.common.api.dto.Response;

import lombok.Getter;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 17:00:32
 * @版本 1.0
 * @描述 跳过异常（该异常不进行全局拦截）
 */
@Getter
public class SkipException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private final Response<?> response;
	
	public SkipException(String msg, Response<?> response) {super(msg); this.response = response;}
	public SkipException(Throwable e, Response<?> response) {super(e); this.response = response;}
	public SkipException(String msg, Throwable e, Response<?> response) {super(msg, e); this.response = response;}
}
