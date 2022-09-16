package org.city.common.api.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 13:16:09
 * @版本 1.0
 * @描述 字段工具
 */
public final class FieldUtil {
	private FieldUtil() {}
	
	/**
	 * @描述 获取原类所有字段信息，包括继承的父类，自动去除安全检查
	 * @param orgin 原类
	 * @param getFinal 是否获取常量字段
	 * @return key=字段名称，value=字段
	 */
	public static Map<String, Field> getAllDeclaredField(Class<?> orgin, boolean getFinal) {
		Map<String, Field> fields = new HashMap<>();
		do {
			Field[] declaredFields = orgin.getDeclaredFields();
			for (Field field : declaredFields) {
				if (getFinal || !Modifier.isFinal(field.getModifiers())) {
					field.setAccessible(true);
					/* 不覆盖 - 相同取子类字段 */
					if (!fields.containsKey(field.getName())) {
						fields.put(field.getName(), field);
					}
				}
			}
		} while ((orgin = orgin.getSuperclass()) !=  Object.class);
		return fields;
	}
	
	/**
	 * @描述 获取对象下某个字段值
	 * @param data 对象
	 * @param fieldName 字段名称
	 * @return 字段值
	 */
	public static Object get(Object data, String fieldName) {
		try {
			Field field = data.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(data);
		} catch (Exception e) {throw new RuntimeException(e);}
	}
	
	/**
	 * @描述 设置对象下某个字段值
	 * @param data 对象
	 * @param fieldName 字段名称
	 * @param value 字段值
	 */
	public static void set(Object data, String fieldName, Object value) {
		try {
			Field field = data.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(data, value);
		} catch (Exception e) {throw new RuntimeException(e);}
	}
}
