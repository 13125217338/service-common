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
	/** 全局扩展点在Redis中的key名称 */
	public final static String REDIS_EXT_KEY_NAME = "Mas:GlobalExtension";
	/** 初始方法缓存信息在Redis中的key名称 */
	public final static String REDIS_INIT_METHOD_CACHE_NAME = "Mas:InitMethodCache";
	
	/** 远程调用识别头信息 */
	public final static String REMOTE_IP_PORT_HEADER = "RemoteIpPort";
	
	/** Redis扩展点存取适配 */
	public final static String REDIS_EXT_ADAPTER_NAME = "RedisExtAdapter";
	/** 当前适配器使用的实现 */
	public final static String EXT_ADAPTER_USER_NAME = REDIS_EXT_ADAPTER_NAME;
	
	/** 插件标记名称 */
	public final static String PLUG_UTIL_NAME = "PlugUtilName";
	
	/** 自定义sql追加字段 - 如：sys.create_time as createTime,xxx等 - 只有查询有用（不要逗号结尾） */
	public final static String SQL_FIELD = "$SQL-FIELD$";
	/** 自定义sql追加连接表 - 如：left join system sys on c.id = sys.id - 只有查询有用 */
	public final static String SQL_JOIN = "$SQL-JOIN$";
	/** 自定义sql条件 - 如：and sys.name = '插件' - 查询、删除、更新有用 */
	public final static String SQL_WHERE = "$SQL-WHERE$";
	
	/**
	 * @描述 获取当前时间（格式yyyy-MM-dd HH:mm:ss时间）
	 * @return 当前时间
	 */
	public static String getNowTime() {return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());}
}
