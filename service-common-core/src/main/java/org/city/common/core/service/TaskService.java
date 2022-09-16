package org.city.common.core.service;

import org.city.common.core.task.Runnable;
import org.city.common.core.task.Task;
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
public final class TaskService implements org.city.common.api.in.Task{
	/* 任务池 */
	private final Task TASK = new Task(10, 20, Integer.MAX_VALUE, 0);
	
	@Override
	public void putTask(org.city.common.api.in.Runnable run) {
		final RequestAttributes request = getRequest();
		TASK.PutTask(new Runnable() {
			@Override
			public void run() throws Throwable {
				RequestContextHolder.setRequestAttributes(request);
				run.run();
			}
		});
	}
	@Override
	public void putTaskSys(org.city.common.api.in.Runnable run, long timeout) throws Exception {
		final RequestAttributes request = getRequest();
		TASK.PutTaskSys(new Runnable() {
			@Override
			public void run() throws Throwable {
				RequestContextHolder.setRequestAttributes(request);
				run.run();
			}
		}, timeout);
	}

	@Override
	public void schedula(String id, org.city.common.api.in.Runnable task, String baseTime, long timeout, boolean isFrist) {
		final RequestAttributes request = getRequest();
		TASK.Schedula(id, new Runnable() {
			@Override
			public void run() throws Throwable {
				RequestContextHolder.setRequestAttributes(request);
				task.run();
			}
		}, baseTime, timeout, isFrist);
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
	
	/* 获取当前请求属性 */
	private RequestAttributes getRequest() {
		try {return RequestContextHolder.currentRequestAttributes();}
		catch (Exception e) {return null;}
	}
}
