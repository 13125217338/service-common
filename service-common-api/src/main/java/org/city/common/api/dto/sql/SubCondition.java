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
 * @描述 子查询条件
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
				.setVals(new String[] {MathSql.Normal.sqlFormat(String.format("%s.%s", curAlias, v))})));
	}
	
	/**
	 * @描述 解析成JSON对象字符串
	 * @return 当前子查询条件
	 */
	public SubCondition parseJsonObject() {
		this.sql = parseJson("");
		return this;
	}
	
	/**
	 * @描述 解析成JSON数组字符串
	 * @return 当前子查询条件
	 */
	public SubCondition parseJsonArray() {
		this.sql = parseJson("json_arrayagg");
		return this;
	}
	
	/* 解析成JSON格式字符串 */
	private String parseJson(String prefix) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("select %s(json_object(", prefix));
		for (String fieldName : subFieldName) {
			sb.append(String.format("'%s',%s.%s,", fieldName, curAlias, fieldName));
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(String.format(")) json from (%s) %s", sql, curAlias));
		return sb.toString();
	}
}
