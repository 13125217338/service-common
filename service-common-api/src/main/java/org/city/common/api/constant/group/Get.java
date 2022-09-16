package org.city.common.api.constant.group;

/**
 * @作者 ChengShi
 * @日期 2022-07-26 10:30:16
 * @版本 1.0
 * @描述 组查询
 */
public interface Get {
	/** 单个条件 */
	public final static int ONE = -3;
	/** 批量条件 */
	public final static int LIST = -30;
	/** 连接条件 */
	public final static int JOIN = -34;
	/** 其他条件 */
	public final static int OTHER = -13000;
	/** 参数条件 */
	public final static int PARAMER = -13004;
	/** 信息条件 */
	public final static int INFO = -13008;
}
