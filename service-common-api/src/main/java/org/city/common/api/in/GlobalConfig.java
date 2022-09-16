package org.city.common.api.in;

import java.lang.reflect.Method;

import org.city.common.api.dto.remote.RemoteMethodDto;

/**
 * @作者 ChengShi
 * @日期 2022-07-29 10:38:55
 * @版本 1.0
 * @描述 全局配置
 */
public interface GlobalConfig {
	/**
	 * @描述 获取远程方法参数
	 * @param cls 待获取类
	 * @param method 待获取方法
	 * @return 远程方法参数
	 */
	public RemoteMethodDto getRemoteMethod(Class<?> cls, Method method);
}
