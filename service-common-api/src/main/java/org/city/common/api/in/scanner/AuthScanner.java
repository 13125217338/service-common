package org.city.common.api.in.scanner;

import java.lang.reflect.Method;
import java.util.Collection;

import org.city.common.api.dto.remote.RemoteMethodDto;

/**
 * @作者 ChengShi
 * @日期 2023年11月30日
 * @版本 1.0
 * @描述 验证扫描类
 */
public interface AuthScanner extends Scanner {
	/**
	 * @描述 获取所有验证方法
	 * @return 所有验证方法
	 */
	public Collection<RemoteMethodDto> getAuthMethods();
	
	/**
	 * @描述 根据本地方法获取验证方法
	 * @return 验证方法
	 */
	public RemoteMethodDto getByMethod(Method method);
}
