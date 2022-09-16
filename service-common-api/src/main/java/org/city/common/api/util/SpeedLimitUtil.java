package org.city.common.api.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.AllArgsConstructor;

/**
 * @作者 ChengShi
 * @日期 2022-11-02 09:54:25
 * @版本 1.0
 * @描述 速度限制
 */
public final class SpeedLimitUtil {
	private SpeedLimitUtil() {}
	private final static Map<Object, LimitInfo> LIMIT = new ConcurrentHashMap<>(8);
	
	/**
	 * @描述 限制速度（秒单位限制多少）
	 * @param sum 限制大小
	 * @param timeSec 秒单位
	 * @param syncObj 需要被同步的对象（防止多线程访问同一对象）
	 */
	public static void limitSec(int sum, int timeSec, Object syncObj) {
		if (sum < 1 || timeSec < 1) {return;} else {timeSec = timeSec * 1000;} //小于1不限制，时间换成毫秒计算增加精准度
		/* 只能一个对象执行 */
		synchronized (syncObj) {
			LimitInfo limitInfo = LIMIT.computeIfAbsent(syncObj, k -> new LimitInfo(0, System.currentTimeMillis())); //限制信息
			double interval = (double) timeSec / sum; //限制间隔
			/* 循环直到少于限制即可 */
			while(sub(limitInfo, sum, interval)) {
				try {Thread.sleep((long) interval);} //睡眠间隔的时间
				catch (InterruptedException e) {throw new RuntimeException("速度限制线程被中断！");}
			}
			limitInfo.sum++; //放行数量加一
		}
	}
	/* 限制减少计算 */
	private static boolean sub(LimitInfo limitInfo, int sum, double interval) {
		double subSize = (System.currentTimeMillis() - limitInfo.recordTime) / interval; //计算时间差需要减少限制的数量
		limitInfo.sum -= subSize; //总数量减少
		limitInfo.recordTime += (subSize * interval); //校准总量减少对应时间误差
		if (limitInfo.sum < 0) {limitInfo.sum = 0; limitInfo.recordTime = System.currentTimeMillis();}
		return limitInfo.sum > sum;
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2022-11-02 09:59:45
	 * @版本 1.0
	 * @parentClass Cmpp2SendLimit
	 * @描述 限制信息
	 */
	@AllArgsConstructor
	private static class LimitInfo {
		/* 限制数量 */
		private double sum;
		/* 记录时间 */
		private double recordTime;
	}
}
