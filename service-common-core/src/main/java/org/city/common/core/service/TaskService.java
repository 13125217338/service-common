package org.city.common.core.service;

import java.lang.reflect.Method;

import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.core.task.Runnable;
import org.city.common.core.task.Task;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @作者 ChengShi
 * @日期 2022-06-23 13:37:50
 * @版本 1.0
 * @描述 任务实现
 */
@Service
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public final class TaskService implements org.city.common.api.open.Task{
	/* 当前代理方法 */
	private final Method AOP_METHOD = getAopMehtod();
	/* 任务池 */
	private final Task TASK;
	@Autowired
	public TaskService(RemoteConfigDto remoteConfigDto) {
		TASK = new Task(10, remoteConfigDto.getTaskThread(), remoteConfigDto.getTaskThread() * 2000, 0);
	}
	
	/* 公共运行 */
	private Runnable getRun(org.city.common.api.in.Runnable run, RequestAttributes request) {
		final Object currentProxy = getAop();
		return new Runnable() {
			@Override
			public void run() throws Throwable {
				/* 将原代理对象设置到异步线程中 */
				AOP_METHOD.invoke(AopContext.class, currentProxy);
				RequestContextHolder.setRequestAttributes(request);
				run.run();
			}
		};
	}
	
	@Override
	public void putTask(org.city.common.api.in.Runnable run) {
		TASK.PutTask(getRun(run, getRequest()));
	}
	@Override
	public void putTaskSys(org.city.common.api.in.Runnable run, long timeout) throws Exception {
		TASK.PutTaskSys(getRun(run, getRequest()), timeout);
	}

	@Override
	public void schedula(String id, org.city.common.api.in.Runnable task, String baseTime, long timeout, boolean isFrist) {
		TASK.Schedula(id, getRun(task, getRequest()), baseTime, timeout, isFrist);
	}
	@Override
	public void NotifySchedula(String id) {
		TASK.getSchedula(id).Notify();
	}
	@Override
	public long getSchedulaOddTime(String id) {
		return TASK.getSchedula(id).getOddTime();
	}
	@Override
	public void resetSchedulaTimeout(String id, String baseTime, long timeout) {
		TASK.getSchedula(id).setTimeout(baseTime, timeout);
	}
	@Override
	public void setImport() {TASK.setImport();}
	
	/* 获取当前请求属性 */
	private RequestAttributes getRequest() {
		try {return RequestContextHolder.currentRequestAttributes();}
		catch (Exception e) {return null;}
	}
	
	/* 获取原代理对象 */
	private Object getAop() {
		/* 原代理对象 */
		try {return AopContext.currentProxy();} catch (Exception e) {return null;}
	}
	
	/* 获取aop中的方法 */
	private Method getAopMehtod() {
		Method methodAop = null;
		try {
			methodAop = AopContext.class.getDeclaredMethod("setCurrentProxy", Object.class);
			methodAop.setAccessible(true);
		} catch (Exception e) {throw new NullPointerException("未找到AopContext类中对应setCurrentProxy方法！");} 
		return methodAop;
	}
}
