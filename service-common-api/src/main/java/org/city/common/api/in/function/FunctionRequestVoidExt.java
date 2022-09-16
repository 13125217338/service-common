package org.city.common.api.in.function;

/**
 * @作者 ChengShi
 * @日期 2022-07-25 14:37:51
 * @版本 1.0
 * @描述 方法请求（返回void）
 */
@FunctionalInterface
public interface FunctionRequestVoidExt<S> {
	/**
	 * @描述 自定义返回void方法
	 * @param s 入参
	 */
	void apply(S s);
}
