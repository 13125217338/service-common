package org.city.common.api.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.city.common.api.in.function.FunctionResponse;

import lombok.AllArgsConstructor;

/**
 * @作者 ChengShi
 * @日期 2022-11-02 09:54:25
 * @版本 1.0
 * @描述 速度限制
 */
public final class SpeedLimitUtil {
	private final static Map<Object, LimitInfo> LIMIT_SEC = new ConcurrentHashMap<>(8);
	private final static Map<Object, Integer> LIMIT_EXEC = new ConcurrentHashMap<>(8);
	private SpeedLimitUtil() {}
	
	/**
	 * @描述 限制速度（执行限制多少）
	 * @param <T> 返回类型
	 * @param sum 限制大小
	 * @param syncObj 需要被同步的对象（防止多线程访问同一对象）
	 * @param speedEx 超速异常（NULL=同步限制）
	 * @param passExec 放行执行逻辑（NULL=返回空）
	 * @throws Throwable
	 */
	public static <T> T limitExec(int sum, Object syncObj, Supplier<RuntimeException> speedEx, FunctionResponse<T> passExec) throws Throwable {
		if (passExec == null) {return null;} //无执行直接返回空
		if (sum < 1) {return passExec.get();} //小于1不限制，直接执行
		boolean isAdd = false; //是否数量加1
		
		try {
			while (true) {
				/* 只能一个对象执行 */
				synchronized (syncObj) {
					Integer curSum = LIMIT_EXEC.computeIfAbsent(syncObj, k -> 0);
					curSum = curSum > sum ? sum : curSum; //当前数量不能大于限制大小
					if (curSum == sum) {
						if (speedEx == null) {
							try {syncObj.wait(Short.MAX_VALUE);} //睡眠32秒的时间
							catch (InterruptedException e) {throw new RuntimeException("速度限制线程被中断！");}
						} else {throw speedEx.get();} //自定义超速异常
					} else {
						LIMIT_EXEC.put(syncObj, curSum + 1); //请求数量加1
						isAdd = true;
						break;
					}
				}
			}
			return passExec.get(); //放行后执行逻辑
		} finally {
			if (isAdd) {
				synchronized (syncObj) {
					Integer curSum = LIMIT_EXEC.get(syncObj) - 1;
					curSum = curSum < 0 ? 0 : curSum; //当前数量不能小于0
					LIMIT_EXEC.put(syncObj, curSum); //请求数量减1
					syncObj.notifyAll(); //等待线程重新验证
				}
			}
		}
	}
	
	/**
	 * @描述 限制速度（秒单位限制多少）
	 * @param sum 限制大小
	 * @param timeSec 秒单位
	 * @param syncObj 需要被同步的对象（防止多线程访问同一对象）
	 * @param speedEx 超速异常（NULL=同步限制）
	 */
	public static void limitSec(int sum, int timeSec, Object syncObj, Supplier<RuntimeException> speedEx) {
		if (sum < 1 || timeSec < 1) {return;} else {timeSec = timeSec * 1000;} //小于1不限制，时间换成毫秒计算增加精准度
		/* 只能一个对象执行 */
		synchronized (syncObj) {
			LimitInfo limitInfo = LIMIT_SEC.computeIfAbsent(syncObj, k -> new LimitInfo(0, System.currentTimeMillis())); //限制信息
			double interval = (double) timeSec / sum; //限制间隔
			/* 循环直到少于限制即可 */
			while(sub(limitInfo, sum, interval)) {
				if (speedEx == null) {
					try {Thread.sleep((long) interval);} //睡眠间隔的时间
					catch (InterruptedException e) {throw new RuntimeException("速度限制线程被中断！");}
				} else {throw speedEx.get();} //自定义超速异常
			}
			limitInfo.sum++; //放行数量加1
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
