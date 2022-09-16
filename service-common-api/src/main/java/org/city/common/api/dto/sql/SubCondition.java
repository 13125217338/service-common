package org.city.common.api.dto.sql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.city.common.api.constant.MathSql;
import org.city.common.api.dto.sql.Condition.Field;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @作者 ChengShi
 * @日期 2023-09-14 12:00:10
 * @版本 1.0
 * @描述 子查询条件参数
 */
@Data
@AllArgsConstructor
public class SubCondition {
	/* 子查询Sql */
	private String sql;
	/* 子查询参数 */
	private final List<Object> params;
	/* 查询所有接收字段名 */
	private final Set<String> subFieldName;
	/* 当前表别名 */
	private String curAlias;
	/* 是否解析过字段 */
	private boolean isParseField;
	
	/**
	 * @描述 解析成接收字段
	 * @return key=接收字段名称，value=接收字段
	 */
	public Map<String, Field> parseField() {
		if (this.isParseField) {return new HashMap<>();} else {this.isParseField = true;} //只解析一次
		return subFieldName.stream().collect(Collectors.toMap(k -> k, v -> new Field().setFieldName(v).setMathSql(MathSql.Sql)
				.setVals(new String[] {MathSql.Normal.format(String.format("%s.%s", curAlias, v))})));
	}
}
