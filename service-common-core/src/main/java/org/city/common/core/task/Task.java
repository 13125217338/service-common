package org.city.common.core.task;

import java.io.Closeable;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import org.city.common.api.in.Runnable;

/**
 * @作者 ChengShi
 * @日期 2020-04-17 18:37:45
 * @版本 1.0
 * @描述 执行任务线程池
 */
public final class Task implements Closeable {
	private final String NotifyRunTask = "NotifyRunTask";
	private final String ClearRunTask = "ClearRunTask";
	private final String NTime = "300000", CTime = "10000";
	private int isClearSum = 0;
	final DataCenter dataCenter;
	
	/**
	 * @描述 初始化任务线程池
	 * @param core 核心线程（如果小于1则用默认1个）
	 * @param max 最大线程（如果小于核心用核心数量）
	 * @param maxTask 最大任务数量（超过该数会抛出异常信息）
	 * @param maxTimeout 最大jvm退出线程执行的等待时间，毫秒（小于一分钟则为一分钟）
	 */
	public Task(int core, int max, int maxTask, long maxTimeout) {
		dataCenter = new DataCenter(maxTimeout);
		dataCenter.CORE = core < dataCenter.CORE ? dataCenter.CORE : core;
		dataCenter.MAX = max < dataCenter.CORE ? dataCenter.CORE : max;
		dataCenter.TASKMAX = maxTask < dataCenter.TASKMAX ? dataCenter.TASKMAX : maxTask;
		for (int i = 0; i < core; i++) {
			synchronized (dataCenter.RUNS) {
				dataCenter.RUNS.add(createRun());
			}
		}
		
		/* 自增线程数并且通知任务线程运行（默认五分钟） */
		Schedula(NotifyRunTask, new Runnable() {
			@Override
			public void run() {
				/* 获取等待线程 */
				int runSum = dataCenter.RUNS.size();
				int taskSum = dataCenter.TASKS.size();
				List<Run> waits = new ArrayList<>(runSum);
				for (Run run : dataCenter.RUNS) {if (run.isCurWait) {waits.add(run);}}
				
				/* 判断并自增线程 */
				int add = taskSum - waits.size();
				if (add > 0) {
					add = (runSum + add) > dataCenter.MAX ? (dataCenter.MAX - runSum) : add;
					for (int i = 0; i < add; i++) {
						Run createRun = createRun();
						synchronized (dataCenter.RUNS) {dataCenter.RUNS.add(createRun);}
						waits.add(createRun);
					}
					isClearSum = 0;
				}
				/* 唤醒等待线程 */
				for (Run run : waits) {if (run != null) {run.Notify();}}
			}
		}, () -> NTime, true);
		/* 十秒判断一次清理线程（如果统计数大于五就清理线程） */
		Schedula(ClearRunTask, new Runnable() {
			@Override
			public void run() {
				if (isClearSum > 5) {
					if (dataCenter.RUNS.size() > dataCenter.CORE) {
						for (Run run : new ArrayList<Run>(dataCenter.RUNS)) {
							if (run == null || State.TERMINATED.equals(run.getState())) {synchronized (dataCenter.RUNS) {dataCenter.RUNS.remove(run);}}
							else {if (run.isCurWait && dataCenter.RUNS.size() > dataCenter.CORE) {run.close();}}
						}
					}
					isClearSum = 0;
				} else {isClearSum++;}
			}
		}, () -> CTime, false);
	}
	/* 创建运行线程 */
	private Run createRun() {
		Run run = new Run(this);
		run.setName(String.format("Task(%d)", run.getId()));
		run.setPriority(Thread.MAX_PRIORITY);
		run.start();
		return run;
	}
	
	/**
	 * @描述 启动一个定时任务线程
	 * @param id 唯一识别id，可通过该id获取定时任务线程（不能是NotifyRunTask和ClearRunTask，也不能是NULL）
	 * @param task 任务
	 * @param expression 表达式（Cron或者整数，整数=固定时间调用）
	 * @param isFrist 是否第一次就执行
	 * @return 定时任务线程
	 */
	public Schedula Schedula(String id, Runnable task, Supplier<String> expression, boolean isFrist) {
		if (dataCenter.SCHEDULA.containsKey(id)) {throw new RuntimeException(String.format("调度线程唯一ID[%s]已存在，请勿重复设置ID！", id));}
		Schedula schedula = new Schedula(this, id, task, expression, isFrist);
		try {
			schedula.setName(String.format("Schedula(%s)", id));
			schedula.setPriority(8);
			schedula.start();
			dataCenter.SCHEDULA.put(id, schedula);
		} catch (Throwable e) {schedula.close();throw e;}
		return schedula;
	}
	/**
	 * @描述 通过唯一id获取定时任务线程
	 * @param id 唯一识别id
	 * @return 定时任务线程
	 */
	public Schedula getSchedula(String id) {return dataCenter.SCHEDULA.get(id);}
	
	/**
	 * @描述 添加任务（队列任务超最大会抛出运行异常，多线程可能会超出一些任务，但不影响）
	 * @param task 任务对象
	 */
	public void PutTask(Runnable task) {
		if (dataCenter.isClose) {return;}
		if (dataCenter.TASKS.size() >= dataCenter.TASKMAX) {
			throw new RuntimeException(String.format("当前队列任务数量[%d]已超过最大值[%d]！", dataCenter.TASKS.size(), dataCenter.TASKMAX));
		}
		dataCenter.TASKS.add(task);
		getSchedula(NotifyRunTask).Notify();
	}
	
	/**
	 * @描述 添加任务（异步子线程运行，主线程等待执行）
	 * @param <T> 子线程返回值
	 * @param masterId 主线程唯一ID
	 * @param timeout 主线程等待最大时间（毫秒）
	 * @param sub 子线程执行的方法
	 * @param master 主线程执行的方法
	 * @return 子线程执行的返回值
	 * @throws Throwable
	 */
	@SuppressWarnings("unchecked")
	public <T> T PutTaskSys(String masterId, long timeout, Supplier<T> sub, Function<Object, Object> master) throws Throwable {
		timeout = timeout < 1 ? Short.MAX_VALUE : timeout;
		SysParam sysParam = new SysParam().setContinue(true);
		
		try {
			/* 异步子线程执行方法 */
			dataCenter.SYS_DATA.put(masterId, sysParam);
			PutTask(new Runnable() {
				@Override
				public void run() throws Throwable {
					try {sysParam.setSubReturn(sub.get());} catch (Throwable e) {sysParam.setSubReturn(e);}
					finally {synchronized (sysParam) {sysParam.setContinue(false); sysParam.notifyAll();}}
				}
			});
			
			/* 主线程等待判断执行 */
			long recordTime = System.currentTimeMillis();
			while (sysParam.isContinue()) {
				synchronized (sysParam) {
					if (sysParam.getMasterParam() != null) {
						try {sysParam.setMasterReturn(master.apply(sysParam.getMasterParam()));}
						catch (Throwable e) {sysParam.setMasterReturn(e);} finally {sysParam.notifyAll();}
					}
					sysParam.wait(timeout);
				}
				if ((System.currentTimeMillis() - recordTime) >= timeout) {
					throw new TimeoutException(String.format("主线程[%s]下的子线程执行超时！", masterId));
				}
			}
			
			/* 返回子线程执行结果 */
			if (sysParam.getSubReturn() instanceof Throwable) {throw (Throwable) sysParam.getSubReturn();}
			else {return (T) sysParam.getSubReturn();}
		} finally {dataCenter.SYS_DATA.remove(masterId);} //清空参数
	}
	
	/**
	 * @描述 执行主线程
	 * @param <T> 主线程返回值
	 * @param masterId 主线程唯一ID
	 * @param timeout 当前线程等待最大时间（毫秒）
	 * @param masterParam 主线程执行参数
	 * @param notExist 主线程不存在时执行
	 * @return 主线程执行的返回值
	 * @throws Throwable
	 */
	@SuppressWarnings("unchecked")
	public <T> T runMaster(String masterId, long timeout, Object masterParam, Supplier<T> notExist) throws Throwable {
		if (masterId == null) {return notExist.get();}
		SysParam sysParam = dataCenter.SYS_DATA.get(masterId);
		if (sysParam == null) {return notExist.get();}
		
		synchronized (sysParam) {
			timeout = timeout < 1 ? Short.MAX_VALUE : timeout;
			if (!sysParam.isContinue()) {throw new RuntimeException(String.format("主线程[%s]已结束，无法执行其他逻辑！", masterId));}
			try {
				sysParam.setMasterParam(masterParam); sysParam.notifyAll();
				long recordTime = System.currentTimeMillis(); //记录时间 - 判断是否超时
				
				sysParam.wait(timeout);
				if ((System.currentTimeMillis() - recordTime) >= timeout) {
					throw new TimeoutException(String.format("主线程[%s]执行超时！", masterId));
				}
			} finally {sysParam.setMasterParam(null);} //清空参数
			
			/* 返回主线程执行结果 */
			if (sysParam.getMasterReturn() instanceof Throwable) {throw (Throwable) sysParam.getMasterReturn();}
			else {return (T) sysParam.getMasterReturn();}
		}
	}
	
	/**
	 * @描述 重新设置最大任务线程数
	 * @param max 最大线程数
	 */
	public void setMax(int max) {
		dataCenter.MAX = max < dataCenter.CORE ? dataCenter.CORE : max;
	}
	
	/**
	 * @描述 重新设置核心线程数
	 * @param sum 核心线程数
	 */
	public void setCore(int sum) {
		dataCenter.CORE = sum < 1 ? 1 : sum;
	}
	
	/**
	 * @描述 重新设置最大任务数
	 * @param maxTask 最大任务数
	 */
	public void setMaxTask(int maxTask) {
		dataCenter.TASKMAX = maxTask < Short.MAX_VALUE ? Short.MAX_VALUE : maxTask;
	}
	
	/**
	 * @描述 获取最大线程数
	 * @return 最大线程数
	 */
	public int getMax() {return dataCenter.MAX;}
	
	/**
	 * @描述 获取正在等待的线程数
	 * @return 等待的线程数
	 */
	public int getWait() {
		int sum = 0;
		for (Run run : new ArrayList<>(dataCenter.RUNS)) {if (run.isCurWait) {sum++;}}
		return sum;
	}
	
	/**
	 * @描述 获取正在运行的线程数
	 * @return 运行的线程数
	 */
	public int getRun() {return dataCenter.RUNS.size() - getWait();}
	
	/**
	 * @描述 获取核心线程数
	 * @return 核心线程
	 */
	public int getCore() {return dataCenter.CORE;}
	
	/**
	 * @描述 获取最大任务数量
	 * @return 任务数量
	 */
	public int getMaxTask() {return dataCenter.TASKMAX;}
	
	/**
	 * @描述 获取当前任务数量
	 * @return 当前任务数量
	 */
	public int getCurTask() {return dataCenter.TASKS.size();}
	
	/**
	 * @描述 清空任务数据
	 */
	public void clearTask() {dataCenter.TASKS.removeAll();}
	
	/**
	 * @描述 关闭所有由线程池拉起的线程（报错重试一次）
	 */
	@Override
	public void close() {
		try {remove();} catch (Throwable e) {remove();}
	}
	/* 关闭所有线程 */
	private void remove() {
		for(Schedula schedula : new ArrayList<Schedula>(dataCenter.SCHEDULA.values())){schedula.close();}
		for (Run run : new ArrayList<Run>(dataCenter.RUNS)) {run.close();}
		clearTask();
	}
	
	/**
	 * @描述 设置重要线程（线程安全）
	 */
	public void setImport() {
		int indexOf = dataCenter.RUNS.indexOf(Thread.currentThread());
		if (indexOf != -1) {dataCenter.jvmCloseHandler.setImport(); dataCenter.RUNS.get(indexOf).isCore = true;}
	}
	/**
	 * @描述 移除做完的重要线程（线程安全）
	 */
	void removeImport() {dataCenter.jvmCloseHandler.removeImport();}
	
	/**
	 * @描述 获取任务
	 * @return 任务
	 */
	Runnable getTask() {return dataCenter.TASKS.getHead();}
}
