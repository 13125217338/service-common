package org.city.common.core.service;

import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.city.common.api.dto.HttpSevletRequestDto;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.core.task.Runnable;
import org.city.common.core.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
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
public final class TaskService implements org.city.common.api.in.Task {
	private final Task TASK; //任务池
	@Autowired
	public TaskService(RemoteConfigDto remoteConfigDto) {
		TASK = new Task(10, remoteConfigDto.getTaskThread(), remoteConfigDto.getTaskThread() * 2000, 0);
	}
	
	@Override
	public void putTask(org.city.common.api.in.Runnable run) {
		TASK.PutTask(getRun(run));
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
		TASK.Schedula(id, getRun(task), expression, isFrist);
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
	public void setImport() {
		TASK.setImport();
	}
	
	/* 公共运行 */
	private Runnable getRun(org.city.common.api.in.Runnable run) {
		final ServletRequestAttributes request = getRequest();
		final HttpServletRequest httpServletRequest = new HttpSevletRequestDto(request == null ? null : request.getRequest());
		final HttpServletResponse httpServletResponse = request == null ? null : request.getResponse();
		
		/* 异步执行对象 */
		return new Runnable() {
			@Override
			public void run() throws Throwable {
				/* 将原请求上下文设置到异步线程中 */
				RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest, httpServletResponse));
				run.run();
			}
		};
	}
	/* 获取当前请求属性 */
	private ServletRequestAttributes getRequest() {
		try {return (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();}
		catch (Exception e) {return null;}
	}
}
