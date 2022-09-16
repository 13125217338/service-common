package org.city.common.api.dto;

import org.city.common.api.exception.AuthNotPassException;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 15:40:24
 * @版本 1.0
 * @描述 验证结果
 */
@Data
@Accessors(chain = true)
public class AuthResultDto {
	/*是否验证成功*/
	private boolean isSuccess;
	/*不成功时异常*/
	private AuthNotPassException authNotPassException;
	
	/**
	 * @描述 验证成功
	 * @return 成功结果
	 */
	public static AuthResultDto ok() {
		return new AuthResultDto().setSuccess(true);
	}
	
	/**
	 * @描述 验证失败
	 * @param authNotPassException 失败异常
	 * @return 失败结果
	 */
	public static AuthResultDto error(AuthNotPassException authNotPassException) {
		return new AuthResultDto().setAuthNotPassException(authNotPassException);
	}
}
