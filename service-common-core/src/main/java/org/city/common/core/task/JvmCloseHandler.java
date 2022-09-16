package org.city.common.core.task;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @作者 ChengShi
 * @日期 2020-07-17 12:44:41
 * @版本 1.0
 * @描述 jvm关闭处理线程
 */
class JvmCloseHandler extends Thread{
	private final DataCenter dataCenter;
	private final long timeout = 1000 * 30;
	private long maxTimeout = 0;
	private boolean isWait = true;
	private AtomicInteger curCoreThread = new AtomicInteger(0);
	JvmCloseHandler(DataCenter dataCenter, long maxTimeout) {
		this.dataCenter = dataCenter;
		this.maxTimeout = maxTimeout < 60000 ? 60000 : maxTimeout;
		Runtime.getRuntime().addShutdownHook(this);
	}
	/*唤醒当前线程*/
	private synchronized void Notify(){
		isWait = false;
		this.notifyAll();
	}
	/*原子操作自增与自减*/
	void setImport(){curCoreThread.incrementAndGet();}
	void removeImport(){curCoreThread.decrementAndGet();Notify();}
	
	@Override
	public void run() {
		dataCenter.isClose = true;
		maxTimeout += System.currentTimeMillis();
		while(curCoreThread.intValue() > 0 && maxTimeout > System.currentTimeMillis()){
			try {synchronized (this) {if (isWait) {this.wait(timeout);}isWait = true;}} 
			catch (Throwable e) {}
		}
	}
}
