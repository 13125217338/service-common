package org.city.common.api.dto;

import javax.validation.constraints.NotBlank;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-07-27 15:52:09
 * @版本 1.0
 * @描述 远程参数
 */
@Data
@Accessors(chain = true)
public class RemoteDto {
	/* 验证参数 */
	@NotBlank(message = "验证参数不能为空！")
	private String verify;
	/* 方法参数 */
	private Object[] args;
	/* 方法缓存数据 */
	private MethodCacheDto methodCacheDto;
	
	public static RemoteDto of(String verify, MethodCacheDto methodCacheDto, Object...args) {
		return new RemoteDto().setVerify(verify).setMethodCacheDto(methodCacheDto).setArgs(args);
	}
}
