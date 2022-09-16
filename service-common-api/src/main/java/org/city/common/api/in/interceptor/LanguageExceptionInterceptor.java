package org.city.common.api.in.interceptor;

import java.util.Properties;

/**
 * @作者 ChengShi
 * @日期 2023年11月30日
 * @版本 1.0
 * @描述 自定义异常多语言拦截处理
 */
public interface LanguageExceptionInterceptor {
	/**
	 * @描述 获取多语言属性配置
	 * @return 多语言属性配置
	 */
	public Properties getLanguage();
}
