package org.city.common.api.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @作者 ChengShi
 * @日期 2022-07-04 10:09:05
 * @版本 1.0
 * @描述 操作符
 */
@Getter
@AllArgsConstructor
public enum Operation {
	/** 大于等于操作 */
	Greater_Equals(" >= "),
	/** 大于操作 */
	Greater(" > "),
	/** 小于等于操作 */
	Less_Equals(" <= "),
	/** 小于操作 */
	Less(" < "),
	/** 不等于操作 */
	Not_Equals(" != "),
	/** 等于操作 */
	Equals(" = "),
	/** NotIn操作 - 支持集合与数组（多个字段In使用英文逗号分割，值类型必须继承至BaseDto） */
	Not_In(" not in "),
	/** In操作 - 支持集合与数组（多个字段In使用英文逗号分割，值类型必须继承至BaseDto） */
	In(" in "),
	/** 反向模糊匹配 */
	Not_Like(" not like "),
	/** 模糊匹配 */
	Like(" like "),
	/** 分割查找 */
	Find_In_Set(" find_in_set"),
	/** 不是空条件 */
	Is_Not_Null(" is not null "),
	/** 是空条件 */
	Is_Null(" is null ");
	
	/* 操作值 */
	private final String optVal;
}
