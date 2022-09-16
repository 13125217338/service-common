package org.city.common.api.in;

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
	 * @描述 添加一次性任务，同步执行
	 * @param run 任务
	 * @param timeout 执行超时时间
	 * @throws Exception
	 */
	public void putTaskSys(Runnable run, long timeout) throws Exception;
	
	/**
	 * @描述 添加一个定时任务
	 * @param id 定时唯一ID
	 * @param task 定时任务
	 * @param baseTime 基础时间yyyy-MM-dd HH:mm:ss
	 * @param timeout 定时时间
	 * @param isFrist 是否第一次就执行
	 */
	public void schedula(String id, Runnable task, String baseTime, long timeout, boolean isFrist);
	
	/**
	 * @描述 直接运行定时任务，跳过一次定时时间
	 * @param id 定时唯一ID
	 */
	public void NotifySchedula(String id);
	
	/**
	 * @描述 获取定时剩余待执行时间
	 * @param id 定时唯一ID
	 * @return 剩余时间（毫秒）
	 */
	public long getSchedulaOddTime(String id);
	
	/**
	 * @描述 重设定时时间
	 * @param id 定时唯一ID
	 * @param baseTime 基础时间，为null则直接用timeout
	 * @param timeout 定时时间
	 */
	public void resetSchedulaTimeout(String id, String baseTime, long timeout);
}
