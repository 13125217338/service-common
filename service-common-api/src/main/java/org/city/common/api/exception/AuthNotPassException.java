package org.city.common.api.exception;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 17:00:32
 * @版本 1.0
 * @描述 验证不通过异常（通过继承该类实现自定义验证错误异常）
 */
public abstract class AuthNotPassException extends RuntimeException{
	private static final long serialVersionUID = 1L;
	
	public AuthNotPassException(String msg) {super(msg);}
}
