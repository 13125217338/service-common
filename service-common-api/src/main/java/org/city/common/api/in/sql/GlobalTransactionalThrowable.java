package org.city.common.api.in.sql;

import java.lang.reflect.Method;

/**
 * @作者 ChengShi
 * @日期 2023年4月22日
 * @版本 1.0
 * @描述 全局分布式事务异常操作
 */
public interface GlobalTransactionalThrowable {
	/**
	 * @描述 自定义处理异常逻辑
	 * @param tranId 事务ID
	 * @param method 原方法
	 * @param args 方法入参
	 * @param e 事务异常
	 */
	public void throwable(String tranId, Method method, Object[] args, Throwable e) throws Throwable;
}
