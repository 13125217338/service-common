package org.city.common.api.in.function;

/**
 * @作者 ChengShi
 * @日期 2022-07-25 14:37:51
 * @版本 1.0
 * @描述 方法请求（返回void）
 */
@FunctionalInterface
public interface FunctionRequestVoid<S> {
	/**
	 * @描述 自定义返回void能抛出异常的方法
	 * @param s 入参
	 */
	void apply(S s) throws Exception;
}
