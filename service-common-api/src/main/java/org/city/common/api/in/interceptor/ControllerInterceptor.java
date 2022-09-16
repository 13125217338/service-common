package org.city.common.api.in.interceptor;

import org.aspectj.lang.JoinPoint;

/**
 * @作者 ChengShi
 * @日期 2023年11月30日
 * @版本 1.0
 * @描述 自定义全局控制拦截处理
 */
public interface ControllerInterceptor {
	/**
	 * @描述 进入控制前处理
	 * @param jp 拦截点
	 * @return true=放行
	 */
	public boolean preHandler(JoinPoint jp);
	/**
	 * @描述 执行方法后处理
	 * @param jp 拦截点
	 * @param result 执行结果
	 */
	public void postHandle(JoinPoint jp, Object result);
}
