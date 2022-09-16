package org.city.common.api.util;

import java.util.Map.Entry;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 16:51:04
 * @版本 1.0
 * @描述 Json工具
 */
public final class JsonUtil {
	private JsonUtil() {}
	
	/**
	 * @描述 通过JSONPath替换value中特殊字符的数据，取数为data，条件为#{key}
	 * @param value 待替换数据
	 * @param data 取数为原数据
	 * @return 替换后的数据
	 */
	public static String getValue(String value, JSONObject data) {
		for (Entry<String, String> entry : MyUtil.getContainValue("#{", "}", value).entrySet()) {
			Object val = JSONPath.eval(data, entry.getValue());
			value = value.replace(entry.getKey(), String.valueOf(val));
		}
		return value;
	}
}
