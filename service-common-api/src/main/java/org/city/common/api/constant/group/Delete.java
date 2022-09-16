package org.city.common.api.constant.group;

/**
 * @作者 ChengShi
 * @日期 2022-07-26 10:30:16
 * @版本 1.0
 * @描述 组删除
 */
public interface Delete {
	/** 单个条件 */
	public final static int ONE = -2;
	/** 批量条件 */
	public final static int BATCH = -20;
	/** 连接条件 */
	public final static int JOIN = -23;
	/** 其他条件 */
	public final static int OTHER = -12000;
	/** 参数条件 */
	public final static int PARAMER = -12004;
	/** 信息条件 */
	public final static int INFO = -12008;
}
