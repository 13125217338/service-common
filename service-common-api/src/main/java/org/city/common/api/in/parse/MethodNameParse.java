package org.city.common.api.in.parse;

import java.lang.reflect.Method;

/**
 * @作者 ChengShi
 * @日期 2022-10-17 14:30:30
 * @版本 1.0
 * @描述 方法名称解析
 */
public interface MethodNameParse {
	/**
	 * @描述 解析方法名称（参数类型也展示出来）
	 * @param method 待获取的方法
	 * @return 带参数类型的方法名称
	 */
	default String parse(Method method) {
		StringBuilder sb = new StringBuilder(method.getName() + "(");
		if (method.getParameterCount() > 0) {
			for (Class<?> ptcls : method.getParameterTypes()) {
				sb.append(ptcls.getName() + ",");
			}
			sb.deleteCharAt(sb.length() - 1);
		}
		sb.append(")");
		return sb.toString();
	}
}
