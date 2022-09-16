package org.city.common.api.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @作者 ChengShi
 * @日期 2022-08-06 14:49:20
 * @版本 1.0
 * @描述 连接类型
 */
@Getter
@AllArgsConstructor
public enum JoinType {
	Left_Join(" left join "),
	Right_Join(" right join "),
	Inner_Join(" inner join ");
	
	/* 连接值 */
	private final String val;
}
