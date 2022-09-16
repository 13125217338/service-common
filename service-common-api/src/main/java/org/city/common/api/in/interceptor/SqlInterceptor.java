package org.city.common.api.in.interceptor;

import org.city.common.api.constant.SqlType;

/**
 * @作者 ChengShi
 * @日期 2023-08-25 01:32:44
 * @版本 1.0
 * @描述 自定义全局Sql拦截处理
 */
public interface SqlInterceptor {
	/**
	 * @描述 执行语句回调
	 * @param sqlType 执行语句类型
	 * @param sql 执行语句
	 * @param param 执行语句参数
	 * @param execTime 执行语句时间
	 * @param execStack 执行类的当前栈
	 */
	public void exec(SqlType sqlType, String sql, Object param, long execTime, StackTraceElement execStack);
}
