package org.city.common.core.task;

import java.io.Closeable;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.function.Supplier;

import org.city.common.api.in.Runnable;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2020-04-17 13:41:50
 * @版本 1.0
 * @描述 定时调度线程（可被手动Notify解开运行）
 */
@Slf4j
public final class Schedula extends Thread implements Closeable {
	private final Runnable task;
	private final Task taskMain;
	private final String id;
	private final Supplier<String> expression;
	private long nextTime = Long.MAX_VALUE;
	private boolean isReWait = true, isRun = true, isFrist = true;
	private CronExpression cronExpression;
	private String param;
	Schedula(Task taskMain, String id, Runnable task, Supplier<String> expression, boolean isFrist) {
		this.taskMain = taskMain;
		this.id = id;
		this.task = task;
		this.expression = expression;
		this.isFrist = isFrist;
	}
	
	/**
	 * @描述 获取剩余多少毫秒开始运行任务
	 * @return 剩余多少毫秒
	 */
	public long getOddTime() {
		long oddTime = nextTime - System.currentTimeMillis();
		return oddTime < 1 ? 1 : oddTime;
	}
	/**
	 * @描述 请使用此方法来notify（已加锁）
	 */
	public synchronized void Notify() {
		this.isReWait = false;
		this.notifyAll();
	}
	/**
	 * @描述 刷新定时时间
	 */
	public synchronized void flush() {
		this.isReWait = true;
		this.notifyAll();
	}
	/**
	 * @描述 停止线程并移除该线程
	 */
	@Override
	public void close() {
		this.isRun = false;
		taskMain.dataCenter.SCHEDULA.remove(id);
		this.Notify();
	}
	@Override
	public void run() {
		while(isRun){
			try {
				if (isFrist) {try {task.run();} finally {Thread.interrupted();}} else {isFrist = true;}
				/* 加锁定时跑 */
				synchronized (this) {
					while(isReWait){
						isReWait = false; nextTime = getNextTime();
						this.wait(getOddTime());
					}
					isReWait = true;
				}
			} catch (Throwable e) {log.error(String.format("定时任务[%s]运行异常！", id), e); isFrist = false;}
		}
	}
	
	/* 获取下次运行时间点 */
	private long getNextTime() {
		String newParam = expression.get().trim();
		if (param == null || !param.equals(newParam)) {
			long time = selectRun(newParam, (isCron) -> {
				if (isCron) {
					Assert.isTrue(CronExpression.isValidExpression(newParam), String.format("Cron表达式[%s]有误！", newParam));
					cronExpression = CronExpression.parse(newParam);
					return Timestamp.valueOf(cronExpression.next(LocalDateTime.now())).getTime();
				} else {
					try {return Long.parseLong(newParam);}
					catch (NumberFormatException e) {throw new NumberFormatException(String.format("表达式[%s]有误！", newParam));}
				}
			});
			param = newParam; //解析成功需要保存原参数
			return time;
		}
		return selectRun(newParam, (isCron) -> {
			return isCron ? Timestamp.valueOf(cronExpression.next(LocalDateTime.now())).getTime() : Long.parseLong(newParam);
		});
	}
	/* 选择运行 */
	private long selectRun(String param, Function<Boolean, Long> function) {
		if (param.split(" ").length > 1) {return function.apply(true).longValue();}
		else {return System.currentTimeMillis() + function.apply(false).longValue();}
	}
}
