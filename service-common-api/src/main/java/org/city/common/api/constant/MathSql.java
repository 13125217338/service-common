package org.city.common.api.constant;

/**
 * @作者 ChengShi
 * @日期 2022年8月3日
 * @版本 1.0
 * @描述 条件函数
 */
public enum MathSql {
	/* 正常 */
	Nomal("any_value(%s)", 0),
	/* 求最大 */
	Max("max(%s)", 0),
	/* 求最小 */
	Min("min(%s)", 0),
	/* 求平均 */
	Avg("avg(%s)", 0),
	/* 求和 */
	Sum("sum(%s)", 0),
	/* 求统计 */
	Count("count(%s)", 0),
	/* 求绝对值 */
	Abs("abs(%s)", 0),
	/* 求上浮整数 */
	Ceil("ceil(%s)", 0),
	/* 求下浮整数 */
	Floor("floor(%s)", 0),
	/* 求四舍五入 */
	Round("round(%s)", 0),
	/* 求字符长度 */
	Length("length(%s)", 0),
	/* 时间格式化 - 自动增加单引号变字符串 */
	DateFormat("date_format(%s, '%s')", 1),
	/* 如果为NULL判断 */
	IfNull("ifnull(%s, %s)", 1),
	/* 自定义Sql - 指定字段名作为别名，第一个值作为替换，别名Dto对象中有就行 */
	Sql("%s", 0);
	
	private final String val;
	private final int size;
	private MathSql(String val, int size) {this.val = val; this.size = size;}
	
	/**
	 * @描述 格式化函数值
	 * @param tableField 原表字段名
	 * @param vals 如果函数有需要填的值
	 * @return 格式化后的函数值
	 */
	public String format(String tableField, String...vals) {
		if (size > 0) {
			if (vals == null || vals.length != size) {throw new IllegalArgumentException(String.format("当前函数[%s]对应参数值个数不一致！", this.name()));}
			Object[] rep = new Object[vals.length + 1]; rep[0] = tableField;
			/* 使参数与函数一一对应 */
			for (int i = 0, j = vals.length; i < j; i++) {rep[i + 1] = vals[i];}
			return String.format(val, rep);
		} else {return String.format(val, tableField);}
	}
}
