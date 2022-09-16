package org.city.common.api.in;

/**
 * @作者 ChengShi
 * @日期 2022-08-18 17:33:38
 * @版本 1.0
 * @描述 支持抛出异常的运行
 */
public interface Runnable {
	/**
	 * @描述 带抛出异常的回调
	 */
	public void run() throws Throwable;
}
