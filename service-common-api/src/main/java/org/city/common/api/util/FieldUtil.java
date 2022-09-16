package org.city.common.api.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 13:16:09
 * @版本 1.0
 * @描述 字段工具
 */
public final class FieldUtil {
	private final static Map<String, Map<String, Field>> CACHE_FIELD = new ConcurrentHashMap<>();
	private FieldUtil() {}
	
	/**
	 * @描述 获取原类所有字段信息，包括继承的父类，自动去除安全检查
	 * @param orgin 原类
	 * @param getFinal 是否获取常量字段
	 * @param ignoreClss 忽略的类
	 * @return key=字段名称，value=字段
	 */
	public static Map<String, Field> getAllDeclaredField(Class<?> orgin, boolean getFinal, Class<?>...ignoreClss) {
		List<Class<?>> igClss = ignoreClss == null ? Collections.emptyList() : Arrays.asList(ignoreClss); //不允许为空
		return CACHE_FIELD.computeIfAbsent(orgin.getName() + "-" + getFinal + "-" + igClss.toString(), (k) -> {
			Map<String, Field> fields = new HashMap<>(); Class<?> curCls = orgin;
			do {
				if (igClss.contains(curCls)) {continue;} //忽略的类跳过
				Field[] declaredFields = curCls.getDeclaredFields();
				for (Field field : declaredFields) {
					if (getFinal || !Modifier.isFinal(field.getModifiers())) {
						try {
							field.setAccessible(true);
							/* 不覆盖 - 相同则取子类字段 */
							if (!fields.containsKey(field.getName())) {
								fields.put(field.getName(), field);
							}
						} catch (Throwable e) {} //不能操作的字段跳过
					}
				}
			} while ((curCls = curCls.getSuperclass()) !=  Object.class);
			return fields;
		});
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
