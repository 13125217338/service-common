package org.city.common.core.task;

import java.io.Closeable;

/**
 * @作者 ChengShi
 * @日期 2020-04-17 17:08:31
 * @版本 1.0
 * @描述 处理线程
 */
class Run extends Thread implements Closeable{
	private boolean isWait = true;
	private Task taskMain = null;
	private boolean isRun = true;
	private short timeout = Short.MAX_VALUE;
	boolean isCore = false;
	boolean isCurWait = true;
	Run(Task taskMain) {
		this.taskMain = taskMain;
	}
	
	/**
	 * @描述 带状态的notify（已加锁）
	 */
	synchronized void Notify(){
		isWait = false;
		this.notifyAll();
	}
	/**
	 * @描述 停止线程并移除该线程
	 */
	@Override
	public void close(){
		isRun = false;
		synchronized (taskMain.dataCenter.RUNS) {taskMain.dataCenter.RUNS.remove(this);}
		this.Notify();
	}
	@Override
	public void run() {
		while(isRun){
			try {
				synchronized(this){if (isWait) {isCurWait = true;this.wait(timeout);}isWait = true;isCurWait = false;}
				/*如果停止线程或jvm退出则不用接收任务了*/
				if (taskMain.dataCenter.isClose || !isRun) {return;}
				/*任务*/
				Runnable task = null;
				while(!isCore && (task = taskMain.getTask()) != null){
					try {task.run();} catch (Throwable e) {}
					finally {if (task.SYS != null) {synchronized (task.SYS) {task.SYS.notifyAll();}}}
				}
			} catch (Throwable e) {}
			finally {try {if (isCore) {taskMain.removeImport();}isCore = false;} catch (Throwable e1) {}}
		}
	}
}
