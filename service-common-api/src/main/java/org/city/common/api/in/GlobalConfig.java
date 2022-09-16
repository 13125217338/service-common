package org.city.common.api.in;

import org.city.common.api.adapter.ExtensionIOAdapter;
import org.springframework.context.ApplicationContext;

/**
 * @作者 ChengShi
 * @日期 2022-07-29 10:38:55
 * @版本 1.0
 * @描述 全局配置
 */
public interface GlobalConfig {
	/**
	 * @描述 初始化配置
	 * @param applicationContext 应用上下文
	 * @throws Exception
	 */
	public ExtensionIOAdapter init(ApplicationContext applicationContext) throws Exception;
}
