package org.city.common.core.task;

/**
 * @作者 ChengShi
 * @日期 2020-05-14 22:57:55
 * @版本 1.0
 * @描述 带抛出问题的run方法
 */
public abstract class Runnable {
	Thread SYS = null;
	public abstract void run() throws Throwable;
}
