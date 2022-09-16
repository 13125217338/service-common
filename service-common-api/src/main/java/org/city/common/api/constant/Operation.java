package org.city.common.api.constant;

/**
 * @作者 ChengShi
 * @日期 2022-07-04 10:09:05
 * @版本 1.0
 * @描述 操作符
 */
public enum Operation {
	/** 大于操作 */
	Greater(" > "),
	/** 大于等于操作 */
	Greater_Equals(" >= "),
	/** 小于操作 */
	Less(" < "),
	/** 小于等于操作 */
	Less_Equals(" <= "),
	/** 等于操作 */
	Equals(" = "),
	/** 不等于操作 */
	Not_Equals(" != "),
	/** In操作 - 支持集合与数组（多个字段In使用英文逗号分割，值类型必须继承至BaseDto） */
	In(" in "),
	/** NotIn操作 - 支持集合与数组（多个字段In使用英文逗号分割，值类型必须继承至BaseDto） */
	Not_In(" not in "),
	/** 模糊匹配 */
	Like(" like "),
	/** 反向模糊匹配 */
	Not_Like(" not like "),
	/** 分割查找 */
	Find_In_Set(" find_in_set"),
	/** 是空条件 */
	Is_Null(" is null "),
	/** 不是空条件 */
	Is_Not_Null(" is not null ");
	
	/*操作值*/
	private final String optVal;
	public String getOptVal() {return this.optVal;}
	
	private Operation(String optVal) {this.optVal = optVal;}
}
