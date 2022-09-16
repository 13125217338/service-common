package org.city.common.api.in.function;

/**
 * @作者 ChengShi
 * @日期 2022-07-25 13:58:14
 * @版本 1.0
 * @描述 方法请求
 */
@FunctionalInterface
public interface FunctionRequest<S, R> {
	/**
	 * @描述 自定义能抛出异常的方法
	 * @param s 入参
	 * @return 执行结果
	 */
	R apply(S s) throws Exception;
}
