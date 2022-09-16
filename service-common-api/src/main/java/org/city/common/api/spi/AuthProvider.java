package org.city.common.api.spi;

import org.city.common.api.annotation.request.MutiBody;
import org.city.common.api.dto.AuthResultDto;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 16:06:52
 * @版本 1.0
 * @描述 验证提供者（需要与@AuthImpl注解配套使用）
 */
public interface AuthProvider {
	/**
	 * @描述 自定义验证
	 * @param args 方法入参
	 * @param values 自定义参数
	 * @return 验证结果（返回NULL表示跳过该验证，当所有验证都为NULL时则验证不通过）
	 */
	@PostMapping("/auth")
	public AuthResultDto auth(@MutiBody Object[] args, @MutiBody String[] values);
}
