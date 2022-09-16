package org.city.common.api.in;

/**
 * @作者 ChengShi
 * @日期 2023年11月30日
 * @版本 1.0
 * @描述 自定义扫描类
 */
public interface Scanner {
	/**
	 * @描述 回调扫描到的类
	 * @param scanClass 扫描到的类
	 */
	public void scan(Class<?> scanClass);
}
