package org.city.common.core.task;

import java.io.Closeable;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * @作者 ChengShi
 * @日期 2020-04-17 13:41:50
 * @版本 1.0
 * @描述 定时调度线程（可被手动Notify解开运行）
 */
public final class Schedula extends Thread implements Closeable{
	private final Runnable task;
	private final Task taskMain;
	private final String id;
	private long timeout = 1, baseTime = 0, recordTime = System.currentTimeMillis();
	private boolean isReWait = true, isRun = true, isFrist = true;
	Schedula(Task taskMain, String id, Runnable task, String baseTime, long timeout, boolean isFrist) {
		this.baseTime = parseBaseTime(baseTime);
		this.taskMain = taskMain;
		this.id = id;
		this.task = task;
		this.timeout = timeout < 1 ? this.timeout : timeout;
		this.isFrist = isFrist;
	}
	
	/*将yyyy-MM-dd HH:mm:ss格式日期转化long时间*/
	private long parseBaseTime(String baseTime){
		long baseTime_ = 0;
		/*用于计算的基本时间*/
		if (baseTime != null && baseTime.length() > 0) {
			try {
				baseTime_ = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(baseTime).getTime();
			} catch (ParseException e) {
				throw new RuntimeException("你输入的baseTime时间格式不对！");
			}
		}
		return baseTime_;
	}
	
	/**
	 * @描述 获取剩余多少毫秒开始运行任务
	 * @return 剩余多少毫秒
	 */
	public long getOddTime() {
		long oddTime = timeout - (System.currentTimeMillis() - recordTime);
		return oddTime < 0 ? timeout : oddTime;
	}
	
	/**
	 * @描述 请使用此方法来notify（已加锁）
	 */
	public synchronized void Notify(){
		this.isReWait = false;
		this.notifyAll();
	}
	/**
	 * @描述 停止线程并移除该线程
	 */
	@Override
	public void close(){
		this.isRun = false;
		taskMain.dataCenter.SCALUDES.remove(id);
		this.Notify();
	}
	/**
	 * @描述 重新设置定时时间（已加锁）
	 * @param baseTime 基本时间，以此时间为基数按timeout进行定时跑，如果为null或空字符则直接使用timeout，该时间必须为yyyy-MM-dd HH:mm:ss格式（如：2019-01-05 18:30:00）
	 * @param timeout 定时时间，毫秒（小于等于0默认一直跑）
	 */
	public synchronized void setTimeout(String baseTime, long timeout){
		this.isReWait = true;
		this.baseTime = parseBaseTime(baseTime);
		this.timeout = timeout < 1 ? 1 : timeout;
		this.notifyAll();
	}
	@Override
	public void run() {
		while(isRun){
			try {
				if (isFrist) {task.run();}
				else{isFrist = true;}
				/*加锁定时跑*/
				synchronized (this) {
					while(isReWait){
						isReWait = false;recordTime = System.currentTimeMillis();
						if (baseTime == 0) {this.wait(timeout);}
						else {this.wait(timeout - (Math.abs(System.currentTimeMillis() - baseTime) % timeout));}
					}
					isReWait = true;
				}
			} catch (Throwable e) {isFrist = false;}
		}
	}
}