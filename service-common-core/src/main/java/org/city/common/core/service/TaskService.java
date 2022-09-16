package org.city.common.core.service;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.city.common.api.dto.HttpSevletRequestDto;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.core.task.Runnable;
import org.city.common.core.task.Task;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @作者 ChengShi
 * @日期 2022-06-23 13:37:50
 * @版本 1.0
 * @描述 任务实现
 */
@Service
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public final class TaskService implements org.city.common.api.in.Task {
	/* 当前代理方法 */
	private final Method AOP_METHOD = getAopMehtod();
	/* 任务池 */
	private final Task TASK;
	@Autowired
	public TaskService(RemoteConfigDto remoteConfigDto) {
		TASK = new Task(10, remoteConfigDto.getTaskThread(), remoteConfigDto.getTaskThread() * 2000, 0);
	}
	
	/* 公共运行 */
	private Runnable getRun(org.city.common.api.in.Runnable run, ServletRequestAttributes request) {
		final HttpServletRequest httpServletRequest = new HttpSevletRequestDto(request == null ? null : request.getRequest());
		final HttpServletResponse httpServletResponse = request == null ? null : request.getResponse();
		final Object currentProxy = getAop();
		
		/* 异步执行对象 */
		return new Runnable() {
			@Override
			public void run() throws Throwable {
				/* 将原代理对象设置到异步线程中 */
				AOP_METHOD.invoke(AopContext.class, currentProxy);
				RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest, httpServletResponse));
				run.run();
			}
		};
	}
	
	@Override
	public void putTask(org.city.common.api.in.Runnable run) {
		TASK.PutTask(getRun(run, getRequest()));
	}
	@Override
	public <T> T putTaskSys(String masterId, long timeout, Supplier<T> sub, Function<Object, Object> master) throws Throwable {
		return TASK.PutTaskSys(masterId, timeout, sub, master);
	}
	@Override
	public <T> T runMaster(String masterId, long timeout, Object masterParam, Supplier<T> notExist) throws Throwable {
		return TASK.runMaster(masterId, timeout, masterParam, notExist);
	}

	@Override
	public void schedula(String id, org.city.common.api.in.Runnable task, Supplier<String> expression, boolean isFrist) {
		TASK.Schedula(id, getRun(task, getRequest()), expression, isFrist);
	}
	@Override
	public void NotifySchedula(String id) {
		TASK.getSchedula(id).Notify();
	}
	@Override
	public void flushSchedula(String id) {
		TASK.getSchedula(id).flush();
	}
	@Override
	public long getSchedulaOddTime(String id) {
		return TASK.getSchedula(id).getOddTime();
	}
	
	@Override
	public void setImport() {TASK.setImport();}
	
	/* 获取当前请求属性 */
	private ServletRequestAttributes getRequest() {
		try {return (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();}
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
