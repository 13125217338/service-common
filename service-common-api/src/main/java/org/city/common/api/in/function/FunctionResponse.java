package org.city.common.api.in.function;

/**
 * @作者 ChengShi
 * @日期 2022-07-25 15:32:52
 * @版本 1.0
 * @描述 方法响应
 */
@FunctionalInterface
public interface FunctionResponse<R> {
	/**
	 * @描述 自定义能抛出异常的方法
	 * @return 执行结果
	 */
	R get() throws Throwable;
}
