package org.city.common.api.in.parse;

import java.lang.reflect.Field;
import java.util.Collection;

import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;

/**
 * @作者 ChengShi
 * @日期 2023年5月5日
 * @版本 1.0
 * @描述 Sql查询子对象解析
 */
public interface SqlQuerySubObjectParse extends JSONParser {
	/**
	 * @描述 解析子对象类型
	 * @param <D> 解析后的顶级类型
	 * @param subFields 子对象字段
	 * @param data 待解析数据
	 * @param parseType 顶级对象类型
	 * @param split 子对象字段分隔符
	 * @return 解析后的顶级对象
	 */
	default <D> D parseSub(Collection<Field> subFields, Object data, Class<?> parseType, String split) {
		D parse = parse(data, parseType); //最顶级对象数据解析
		if (CollectionUtils.isEmpty(subFields)) {return parse;}
		
		JSONObject oldDate = (JSONObject) parse(data, JSONObject.class);
		for (Field subField : subFields) {
			try {
				Object value = oldDate.get(subField.getName() + split); //如果字段名一致且有数据 - 则直接转换对象
				if (value != null) {subField.set(parse, parse(value.toString().replace("\\", "\\\\"), subField.getGenericType()));}
			} catch (Exception e) {
				throw new RuntimeException(String.format("Sql查询解析子对象失败，类[%s]当前字段[%s]！", parseType.getName(), subField.getName()), e);
			}
		}
		return parse;
	}
}
