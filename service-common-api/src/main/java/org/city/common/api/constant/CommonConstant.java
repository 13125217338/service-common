package org.city.common.api.constant;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @作者 ChengShi
 * @日期 2022-06-24 13:02:30
 * @版本 1.0
 * @描述 公共常量
 */
public interface CommonConstant {
	/** Redis远程存取适配 */
	public final static String REDIS_REMOTE_ADAPTER_NAME = "RedisRemoteAdapter";
	/** 当前适配器使用的实现 */
	public final static String REMOTE_ADAPTER_USER_NAME = REDIS_REMOTE_ADAPTER_NAME;
	
	/** 远程分布式事务头记录ID */
	public final static String REMOTE_TRANSACTIONAL_HEADER_NAME = "Remote-Transactional-Id";
	
	/** 插件标记名称 */
	public final static String PLUG_UTIL_NAME = "PlugUtilName";
	
	/** 自定义sql追加字段 - 如：sys.create_time as createTime,xxx等 - 只有查询有用（不要逗号结尾） */
	public final static String SQL_FIELD = "$SQL-FIELD$";
	/** 自定义sql追加连接表 - 如：left join system sys on c.id = sys.id - 只有查询有用 */
	public final static String SQL_JOIN = "$SQL-JOIN$";
	/** 自定义sql条件 - 如：and sys.name = '插件' - 查询、删除、更新有用 */
	public final static String SQL_WHERE = "$SQL-WHERE$";
	
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
}
