package org.city.common.api.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 13:16:09
 * @版本 1.0
 * @描述 字段工具
 */
public final class FieldUtil {
	private FieldUtil() {}

	/**
	 * @描述 获取原类所有字段信息，包括继承的父类，自动去安全检查（不获取常量字段）
	 * @param orgin 原类
	 * @return 所有字段，包括父类（不获取常量字段）
	 */
	public static List<Field> getAllDeclaredField(Class<?> orgin) {
		List<Field> fields = new ArrayList<>();
		do {
			Field[] declaredFields = orgin.getDeclaredFields();
			for (Field field : declaredFields) {
				if (!Modifier.isFinal(field.getModifiers())) {
					field.setAccessible(true);
					fields.add(field);
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
