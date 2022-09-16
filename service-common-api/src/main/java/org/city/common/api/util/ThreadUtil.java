package org.city.common.api.util;

import java.lang.reflect.Method;

/**
 * @作者 ChengShi
 * @日期 2022年8月25日
 * @版本 1.0
 * @描述 线程工具
 */
public final class ThreadUtil {
	private ThreadUtil() {}
	
	/**
	 * @描述 获取上层方法
	 * @return 上层方法
	 */
	public static Method getUpMethod() {
		try {
			StackTraceElement upStackTrack = getUpStackTrack();
			/* 获取上层类 */
			Class<?> forName = Class.forName(upStackTrack.getClassName());
			Method[] declaredMethods = forName.getDeclaredMethods();
			for (Method method : declaredMethods) {
				/* 通过对比上层方法名返回指定方法 */
				if (method.getName().equals(upStackTrack.getMethodName())) {return method;}
			}
		} catch (Exception e) {}
		return null;
	}
	
	/**
	 * @描述 获取上层类
	 * @return 上层类
	 */
	public static Class<?> getUpClass() {
		try {return Class.forName(getUpStackTrack().getClassName());}
		catch (Exception e) {return null;}
	}
	
	/* 获取上一层栈信息 */
	private static StackTraceElement getUpStackTrack() {
		return  Thread.currentThread().getStackTrace()[4];
	}
}
