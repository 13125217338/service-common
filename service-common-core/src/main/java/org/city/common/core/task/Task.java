package org.city.common.core.task;

import java.io.Closeable;
import java.io.IOException;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * @作者 ChengShi
 * @日期 2020-04-17 18:37:45
 * @版本 1.0
 * @描述 读写任务线程池
 */
public final class Task implements Closeable{
	private final String NotifyRunTask = "NotifyRunTask";
	private final String ClearRunTask = "ClearRunTask";
	private int isClearSum = 0;
	final DataCenter dataCenter;
	/**
	 * @描述 初始化任务线程池
	 * @param core 核心线程（如果小于1则用默认1个）
	 * @param max 最大线程（如果小于核心用核心数量）
	 * @param maxTask 最大任务数量（超过该数会抛出异常信息）
	 * @param maxTimeout 最大jvm退出线程执行的等待时间，毫秒（小于一分钟则为一分钟）
	 */
	public Task(int core, int max, int maxTask, long maxTimeout){
		dataCenter = new DataCenter(maxTimeout);
		dataCenter.CORE = core < dataCenter.CORE ? dataCenter.CORE : core;
		dataCenter.MAX = max < dataCenter.CORE ? dataCenter.CORE : max;
		dataCenter.TASKMAX = maxTask < dataCenter.TASKMAX ? dataCenter.TASKMAX : maxTask;
		for (int i = 0; i < core; i++) {
			synchronized (dataCenter.RUNS) {
				dataCenter.RUNS.add(createRun());
			}
		}
		
		/*自增线程数并且通知任务线程运行（默认五分钟）*/
		Schedula(NotifyRunTask, new Runnable() {
			@Override
			public void run() {
				/*获取等待线程*/
				int runSum = dataCenter.RUNS.size();
				int taskSum = dataCenter.TASKS.size();
				List<Run> waits = new ArrayList<>(runSum);
				for (Run run : dataCenter.RUNS) {if (run.isCurWait) {waits.add(run);}}
				
				/*判断并自增线程*/
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
				/*唤醒等待线程*/
				for(Run run : waits){if (run != null) {run.Notify();}}
			}
		}, null, 1000 * 60 * 5, true);
		/*十秒判断一次清理线程（如果统计数大于五就清理线程）*/
		Schedula(ClearRunTask, new Runnable() {
			@Override
			public void run() {
				if (isClearSum > 5) {
					if (dataCenter.RUNS.size() > dataCenter.CORE) {
						for(Run run : new ArrayList<Run>(dataCenter.RUNS)){
							if (run == null || State.TERMINATED.equals(run.getState())) {synchronized (dataCenter.RUNS) {dataCenter.RUNS.remove(run);}}
							else{if (run.isCurWait && dataCenter.RUNS.size() > dataCenter.CORE) {run.close();}}
						}
					}
					isClearSum = 0;
				}else{isClearSum++;}
			}
		}, null, 10000, false);
	}
	/**
	 * @描述 创建运行线程
	 */
	private Run createRun(){
		Run run = new Run(this);
		run.setName("Task("+run.getId()+")");;
		run.setPriority(Thread.MAX_PRIORITY);
		run.start();
		return run;
	}
	
	/**
	 * @描述 启动一个定时任务线程
	 * @param id 唯一识别id，可通过该id获取定时任务线程（不能是NotifyRunTask和ClearRunTask，也不能是NULL）
	 * @param task 任务
	 * @param baseTime 基本时间，以此时间为基数按timeout进行定时跑，如果为null或空字符则直接使用timeout，该时间必须为yyyy-MM-dd HH:mm:ss格式（如：2019-01-05 18:30:00）
	 * @param timeout 定时时间，毫秒（小于等于0默认一直跑）
	 * @param isFrist 是否第一次就执行
	 * @return 定时任务线程
	 */
	public synchronized Schedula Schedula(String id, Runnable task, String baseTime, long timeout, boolean isFrist){
		if (dataCenter.SCALUDES.containsKey(id)) {throw new RuntimeException("有Scalude调度线程唯一ID《"+id+"》存在，请勿重新设置ID！");}
		Schedula schedula = new Schedula(this, id, task, baseTime, timeout, isFrist);
		try {
			schedula.setName("Scalude("+id+")");
			schedula.setPriority(8);
			schedula.start();
			dataCenter.SCALUDES.put(id, schedula);
		} catch (Throwable e) {schedula.close();throw e;}
		return schedula;
	}
	/**
	 * @描述 通过唯一id获取定时任务线程
	 * @param id 唯一识别id
	 * @return 定时任务线程
	 */
	public Schedula getSchedula(String id){return dataCenter.SCALUDES.get(id);}
	
	/**
	 * @描述 添加任务（队列任务超最大会抛出运行异常，多线程可能会超出一些任务，但不影响）
	 * @param task 任务对象
	 */
	public void PutTask(Runnable task){
		if (dataCenter.isClose) {return;}
		if (dataCenter.TASKS.size() >= dataCenter.TASKMAX) {
			throw new RuntimeException("队列任务数量已超过最大（"+dataCenter.TASKS.size()+"）！");
		}
		dataCenter.TASKS.add(task);
		getSchedula(NotifyRunTask).Notify();
	}
	
	/**
	 * @描述 异步任务同步结果-添加任务（队列任务超最大会抛出运行异常，多线程可能会超出一些任务，但不影响）
	 * @param task 任务对象
	 * @param timeout 允许超时时间，小于1默认Short.MAX_VALUE（毫秒单位）
	 * @throws Exception
	 */
	public void PutTaskSys(Runnable task, long timeout) throws Exception{
		task.SYS = Thread.currentThread();
		timeout = timeout < 1 ? Short.MAX_VALUE : timeout;
		synchronized (task.SYS) {
			PutTask(task);
			long start = System.currentTimeMillis();
			task.SYS.wait(timeout);
			if ((System.currentTimeMillis() - start) >= timeout) {throw new TimeoutException("执行异步任务同步结果超时！");}
		}
	}
	
	/**
	 * @描述 重新设置最大任务线程数
	 * @param max 最大线程数
	 */
	public void setMax(int max){
		dataCenter.MAX = max < dataCenter.CORE ? dataCenter.CORE : max;
	}
	
	/**
	 * @描述 重新设置核心线程数
	 * @param sum 核心线程数
	 */
	public void setCore(int sum){
		dataCenter.CORE = sum < 1 ? 1 : sum;
	}
	
	/**
	 * @描述 重新设置最大任务数
	 * @param maxTask 最大任务数
	 */
	public void setMaxTask(int maxTask){
		dataCenter.TASKMAX = maxTask < Short.MAX_VALUE ? Short.MAX_VALUE : maxTask;
	}
	
	/**
	 * @描述 获取最大线程数
	 * @return 最大线程数
	 */
	public int getMax(){return dataCenter.MAX;}
	
	/**
	 * @描述 获取正在等待的线程数
	 * @return 等待的线程数
	 */
	public int getWait(){
		int sum = 0;
		for (Run run : new ArrayList<>(dataCenter.RUNS)) {if (run.isCurWait) {sum++;}}
		return sum;
	}
	
	/**
	 * @描述 获取正在运行的线程数
	 * @return 运行的线程数
	 */
	public int getRun(){return dataCenter.RUNS.size() - getWait();}
	
	/**
	 * @描述 获取核心线程数
	 * @return 核心线程
	 */
	public int getCore(){return dataCenter.CORE;}
	
	/**
	 * @描述 获取最大任务数量
	 * @return 任务数量
	 */
	public int getMaxTask(){return dataCenter.TASKMAX;}
	
	/**
	 * @描述 获取当前任务数量
	 * @return 当前任务数量
	 */
	public int getCurTask(){return dataCenter.TASKS.size();}
	
	/**
	 * @描述 清空任务数据
	 * @throws IOException
	 */
	public void clearTask() {dataCenter.TASKS.removeAll();}
	
	/** 关闭所有由线程池拉起的线程（报错重试一次） */
	@Override
	public void close(){
		try {remove();} catch (Throwable e) {remove();}
	}
	/*关闭所有线程*/
	private void remove(){
		for(Schedula schedula : new ArrayList<Schedula>(dataCenter.SCALUDES.values())){schedula.close();};
		for (Run run : new ArrayList<Run>(dataCenter.RUNS)) {run.close();}
		clearTask();
	}
	
	/**
	 * @描述 设置重要线程（线程安全）
	 */
	public void setImport(){
		int indexOf = dataCenter.RUNS.indexOf(Thread.currentThread());
		if (indexOf != -1) {dataCenter.jvmCloseHandler.setImport();dataCenter.RUNS.get(indexOf).isCore = true;}
	}
	/**
	 * @描述 移除做完的重要线程（线程安全）
	 */
	void removeImport(){dataCenter.jvmCloseHandler.removeImport();}
	
	/**
	 * @描述 获取任务
	 * @return 任务
	 */
	Runnable getTask(){return dataCenter.TASKS.getHead();}
}
