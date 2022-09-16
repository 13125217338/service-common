package org.city.common.api.util;

import java.util.Calendar;
import java.util.Map.Entry;

import org.city.common.api.constant.CommonConstant;

/**
 * @作者 ChengShi
 * @日期 2024-04-03 14:32:20
 * @版本 1.0
 * @描述 日期工具
 */
public final class DateUtil {
	private DateUtil() {}
	
	/**
	 * @描述 获取当天时间（yyyy-MM-dd HH:mm:ss）
	 * @return key=开始日期，value=结束日期
	 */
	public static Entry<String, String> getCurDay() {
		Calendar instance = getDayOfZero();
		/* 开始时间 */
		String startTime = CommonConstant.getSimpleDateFormat().format(instance.getTime());
		instance.add(Calendar.DAY_OF_MONTH, 1);
		instance.add(Calendar.SECOND, -1);
		String endTime = CommonConstant.getSimpleDateFormat().format(instance.getTime());
		return getEntry(startTime, endTime);
	}
	
	/**
	 * @描述 获取当月时间（yyyy-MM-dd HH:mm:ss）
	 * @return key=开始日期，value=结束日期
	 */
	public static Entry<String, String> getCurMonth() {
		Calendar instance = getDayOfZero();
		instance.set(Calendar.DAY_OF_MONTH, 1);
		/* 开始时间 */
		String startTime = CommonConstant.getSimpleDateFormat().format(instance.getTime());
		instance.add(Calendar.MONTH, 1);
		instance.add(Calendar.SECOND, -1);
		String endTime = CommonConstant.getSimpleDateFormat().format(instance.getTime());
		return getEntry(startTime, endTime);
	}
	
	/* 获取当天时间为0的实例 */
	private static Calendar getDayOfZero() {
		Calendar instance = Calendar.getInstance();
		instance.setTimeInMillis(System.currentTimeMillis());
		instance.set(Calendar.HOUR_OF_DAY, 0);
		instance.set(Calendar.MINUTE, 0);
		instance.set(Calendar.SECOND, 0);
		instance.set(Calendar.MILLISECOND, 0);
		return instance;
	}
	/* 获取Entry */
	private static Entry<String, String> getEntry(String start, String end) {
		/* key=开始日期，value=结束日期 */
		return new Entry<String, String>() {
			@Override
			public String setValue(String value) {return value;}
			@Override
			public String getKey() {return start;}
			@Override
			public String getValue() {return end;}
		};
	}
}
