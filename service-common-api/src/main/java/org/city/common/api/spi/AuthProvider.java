package org.city.common.api.spi;

import org.city.common.api.annotation.plug.Remote;
import org.city.common.api.dto.AuthResultDto;
import org.city.common.api.dto.AuthResultDto.AuthMethod;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 16:06:52
 * @版本 1.0
 * @描述 验证提供者（需要与@AuthFilter注解配套使用）
 */
@Remote
public interface AuthProvider {
	/**
	 * @描述 自定义验证
	 * @param authMethod 验证方法
	 * @param args 方法入参
	 * @param value 自定义参数（通常用作标记）
	 * @param values 已被替换的自定义参数
	 * @return 验证结果（返回NULL表示跳过该验证）
	 */
	public AuthResultDto auth(AuthMethod authMethod, Object[] args, int value, String[] values);
}
