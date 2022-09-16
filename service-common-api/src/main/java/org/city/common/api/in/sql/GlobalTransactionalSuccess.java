package org.city.common.api.in.sql;

import java.lang.reflect.Method;

/**
 * @作者 ChengShi
 * @日期 2023年4月22日
 * @版本 1.0
 * @描述 全局分布式事务成功操作
 */
public interface GlobalTransactionalSuccess {
	/**
	 * @描述 自定义处理成功逻辑
	 * @param tranId 事务ID
	 * @param method 原方法
	 * @param args 方法入参
	 * @param returnVal 方法返回值
	 */
	public void success(String tranId, Method method, Object[] args, Object returnVal) throws Throwable;
}
