package org.city.common.api.constant;

/**
 * @作者 ChengShi
 * @日期 2022-08-06 14:49:20
 * @版本 1.0
 * @描述 连接类型
 */
public enum JoinType {
	Left_Join(" left join "),
	Right_Join(" right join "),
	Inner_Join(" inner join ");
	
	public final String val;
	private JoinType(String val) {this.val = val;}
}
