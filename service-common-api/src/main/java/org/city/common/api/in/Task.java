package org.city.common.api.in;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @作者 ChengShi
 * @日期 2022-06-23 13:06:50
 * @版本 1.0
 * @描述 任务池提供
 */
public interface Task {
	/**
	 * @描述 添加一次性任务，异步执行
	 * @param run 任务
	 */
	public void putTask(Runnable run);
	
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
	public <T> T putTaskSys(String masterId, long timeout, Supplier<T> sub, Function<Object, Object> master) throws Throwable;
	
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
	public <T> T runMaster(String masterId, long timeout, Object masterParam, Supplier<T> notExist) throws Throwable;
	
	/**
	 * @描述 启动一个定时任务线程
	 * @param id 唯一识别id，可通过该id获取定时任务线程（不能是NotifyRunTask和ClearRunTask，也不能是NULL）
	 * @param task 任务
	 * @param expression 表达式（Cron或者整数，整数=固定时间调用）
	 * @param isFrist 是否第一次就执行
	 * @return 定时任务线程
	 */
	public void schedula(String id, Runnable task, Supplier<String> expression, boolean isFrist);
	
	/**
	 * @描述 直接运行定时任务，跳过一次定时时间
	 * @param id 定时唯一ID
	 */
	public void NotifySchedula(String id);
	
	/**
	 * @描述 刷新定时任务时间
	 * @param id 定时唯一ID
	 */
	public void flushSchedula(String id);
	
	/**
	 * @描述 获取定时剩余待执行时间
	 * @param id 定时唯一ID
	 * @return 剩余时间（毫秒）
	 */
	public long getSchedulaOddTime(String id);
	
	/**
	 * @描述 设置当前线程为重要线程（线程钩子）
	 */
	public void setImport();
	
	/**
	 * @描述 克隆一个任务池
	 * @return 克隆的任务池
	 */
	public Task cloneTask();
}
