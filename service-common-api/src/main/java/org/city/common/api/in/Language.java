package org.city.common.api.in;

import java.util.Properties;

/**
 * @作者 ChengShi
 * @日期 2023年11月30日
 * @版本 1.0
 * @描述 多语言
 */
public interface Language {
	/**
	 * @描述 获取多语言属性配置
	 * @param language 多语言（NULL=由实现者提供）
	 * @return 多语言属性配置
	 */
	public Properties getLanguage(String language);
}
