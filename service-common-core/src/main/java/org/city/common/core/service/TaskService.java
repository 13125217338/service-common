package org.city.common.core.service;

import java.util.function.Function;
import java.util.function.Supplier;

import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.in.Runnable;
import org.city.common.core.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @作者 ChengShi
 * @日期 2022-06-23 13:37:50
 * @版本 1.0
 * @描述 任务实现
 */
@Service
public final class TaskService implements org.city.common.api.in.Task {
	private final RemoteConfigDto REMOTE_CONFIG; //配置信息
	private final Task TASK; //任务池
	@Autowired
	public TaskService(RemoteConfigDto remoteConfigDto) {
		REMOTE_CONFIG = remoteConfigDto;
		TASK = new Task(10, REMOTE_CONFIG.getTaskThread(), REMOTE_CONFIG.getTaskThread() * 2000, 0);
	}
	
	@Override
	public void putTask(Runnable run) {
		TASK.PutTask(run);
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
	public void schedula(String id, Runnable task, Supplier<String> expression, boolean isFrist) {
		TASK.Schedula(id, task, expression, isFrist);
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
	
	@Override
	public org.city.common.api.in.Task cloneTask() {
		return new TaskService(REMOTE_CONFIG);
	}
}
