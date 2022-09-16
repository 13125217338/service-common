package org.city.common.api.in.parse;

import java.util.Set;

/**
 * @作者 ChengShi
 * @日期 2022-08-21 15:08:41
 * @版本 1.0
 * @描述 远程调用模板解析
 */
public interface RestTemplateParse {
	/**
	 * @描述 将字段解析成RestTemplate可以拼接的Uri参数
	 * @param fieldNames 待解析字段名称
	 * @return 解析后的字符串
	 */
	default String parse(Set<String> fieldNames) {
		StringBuilder sb = new StringBuilder();
		for (String fieldName : fieldNames) {
			sb.append(String.format("%s={%s}&", fieldName, fieldName));
		}
		if (fieldNames.size() > 0) {sb.delete(sb.length() - 1, sb.length());}
		return sb.toString();
	}
}
