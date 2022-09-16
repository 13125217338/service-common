package org.city.common.api.constant.group;

/**
 * @作者 ChengShi
 * @日期 2022-07-26 11:09:17
 * @版本 1.0
 * @描述 默认组
 */
public interface Default extends javax.validation.groups.Default{
	/** 默认条件 */
	public final static int VALUE = Integer.MIN_VALUE;
	/** 相等条件 */
	public final static int EQUALS = -101;
	/** 不相等条件 */
	public final static int NOT_EQUALS = -201;
	/** 模糊条件 */
	public final static int LIKE = -301;
	/** 大于条件 */
	public final static int GREATER = -401;
	/** 小于条件 */
	public final static int LESS = -501;
	/** In条件 */
	public final static int IN = -601;
	/** 查询设置条件 */
	public final static int FIND_IN_SET = -701;
	/** 连接条件 */
	public final static int JOIN = -801;
	/** 其他条件 */
	public final static int OTHER = -10000;
	/** 参数条件 */
	public final static int PARAMER = -10004;
	/** 信息条件 */
	public final static int INFO = -10008;
}
