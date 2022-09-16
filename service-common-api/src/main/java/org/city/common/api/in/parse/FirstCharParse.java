package org.city.common.api.in.parse;

/**
 * @作者 ChengShi
 * @日期 2022-10-13 14:48:00
 * @版本 1.0
 * @描述 第一个字符解析
 */
public interface FirstCharParse {
	/**
	 * @描述 首字符小写
	 * @param parse 待转换字符串
	 * @return 首字符小写
	 */
	default String parseLower(String parse) {
		char[] charArray = parse.toCharArray();
		charArray[0] = Character.toLowerCase(charArray[0]);
		return String.valueOf(charArray);
	}
	
	/**
	 * @描述 解析精简类名首字符小写
	 * @param cls 待转换类名
	 * @return 精简类名首字符小写
	 */
	default String parseLower(Class<?> cls) {
		return parseLower(cls.getSimpleName());
	}
	
	/**
	 * @描述 首字符大写
	 * @param parse 待转换字符串
	 * @return 首字符大写
	 */
	default String parseUpper(String parse) {
		char[] charArray = parse.toCharArray();
		charArray[0] = Character.toUpperCase(charArray[0]);
		return String.valueOf(charArray);
	}
}
