package org.city.common.api.in.function;

/**
 * @作者 ChengShi
 * @日期 2022-07-25 14:37:51
 * @版本 1.0
 * @描述 方法请求（返回void）
 */
@FunctionalInterface
public interface FunctionRequestVoid<T> {
	/**
	 * @描述 自定义能抛出异常的方法
	 * @param t 入参
	 */
	void apply(T t) throws Throwable;
}
