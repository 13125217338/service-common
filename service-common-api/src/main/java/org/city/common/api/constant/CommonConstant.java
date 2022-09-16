package org.city.common.api.constant;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @作者 ChengShi
 * @日期 2022-06-24 13:02:30
 * @版本 1.0
 * @描述 公共常量
 */
public interface CommonConstant {
	/** 定时验证过期操作唯一ID */
	public final static String REDIS_TOKEN_TIME_EXPIRE_ID = "REDIS_TOKEN_TIME_EXPIRE_ID";
	/** Redis监控分布锁唯一ID */
	public final static String REDIS_MONITOR_LOCK_ID = "REDIS_MONITOR_LOCK_ID";
	
	/** 记录Redis远程接口信息 */
	public final static String REDIS_REMOTE_PREFIX_KEY = "RedisRemoteKey:";
	/** 记录Redis事务锁 */
	public final static String REDIS_TRANSACTIONAL_LOCK_KEY = "RedisTransactionalLock:";
	/** 记录Redis远程事务信息 */
	public final static String REDIS_REMOTE_TRANSACTIONAL_KEY = "RedisRemoteTransactional:";
	/** 记录Redis令牌信息 */
	public final static String REDIS_TOKEN_HKEY = "RedisToken";
	/** 记录Redis令牌过期时间信息 */
	public final static String REDIS_TOKEN_EXPIRE_TIME_HKEY = "RedisTokenExpireTime";
	
	/** 远程分布式事务头记录ID */
	public final static String REMOTE_TRANSACTIONAL_HEADER_NAME = "Remote-Transactional-Id";
	
	/** 启动时间 */
	public final static long START_TIME = System.currentTimeMillis();
	
	/** 记录时间 */
	public final static ThreadLocal<SimpleDateFormat> LOCAL_TIME = new ThreadLocal<>();
	/** 记录时间 - 紧凑版 */
	public final static ThreadLocal<SimpleDateFormat> LOCAL_TIME$ = new ThreadLocal<>();
	
	/**
	 * @描述 获取当前时间（格式yyyy-MM-dd HH:mm:ss时间）
	 * @return 当前时间
	 */
	public static String getNowTime() {
		return getSimpleDateFormat().format(new Date());
	}
	/**
	 * @描述 获取当前日期格式（格式yyyy-MM-dd HH:mm:ss时间）
	 * @return 当前日期格式
	 */
	public static SimpleDateFormat getSimpleDateFormat() {
		SimpleDateFormat simpleDateFormat = LOCAL_TIME.get();
		if (simpleDateFormat == null) {simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); LOCAL_TIME.set(simpleDateFormat);}
		return simpleDateFormat;
	}
	
	/**
	 * @描述 获取当前时间（格式yyyyMMddHHmmss时间）
	 * @return 当前时间
	 */
	public static String getNowTime$() {
		return getSimpleDateFormat$().format(new Date());
	}
	
	/**
	 * @描述 获取当前日期格式（格式yyyyMMddHHmmss时间）
	 * @return 当前日期格式
	 */
	public static SimpleDateFormat getSimpleDateFormat$() {
		SimpleDateFormat simpleDateFormat = LOCAL_TIME$.get();
		if (simpleDateFormat == null) {simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss"); LOCAL_TIME$.set(simpleDateFormat);}
		return simpleDateFormat;
	}
	
	/**
	 * @描述 判断异常是否是连接超时
	 * @param throwable 异常
	 * @return true=是
	 */
	public static boolean isConnectTimeout(Throwable throwable) {
		if (throwable instanceof ConnectException) {return true;}
		if (throwable instanceof SocketTimeoutException && throwable.getMessage().contains("connect timed out")) {return true;}
		return false;
	}
}
