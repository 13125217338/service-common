package org.city.common.api.constant;

import java.lang.reflect.Type;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @作者 ChengShi
 * @日期 2022-10-08 14:48:20
 * @版本 1.0
 * @描述 基本类型（包含数组）
 */
@Getter
@AllArgsConstructor
public enum PrimitiveClass {
	VOID(void.class.getName(), Void.TYPE),
	/* 基本类型 */
	BYTE(byte.class.getName(), Byte.TYPE),
	SHORT(short.class.getName(), Short.TYPE),
	INT(int.class.getName(), Integer.TYPE),
	LONG(long.class.getName(), Long.TYPE),
	FLOAT(float.class.getName(), Float.TYPE),
	DOUBLE(double.class.getName(), Double.TYPE),
	CHAR(char.class.getName(), Character.TYPE),
	BOOLEAN(boolean.class.getName(), Boolean.TYPE),
	/* 基本类型数组 */
	BYTES(((Type) byte[].class).getTypeName(), byte[].class),
	SHORTS(((Type) short[].class).getTypeName(), short[].class),
	INTS(((Type) int[].class).getTypeName(), int[].class),
	LONGS(((Type) long[].class).getTypeName(), long[].class),
	FLOATS(((Type) float[].class).getTypeName(), float[].class),
	DOUBLES(((Type) double[].class).getTypeName(), double[].class),
	CHARS(((Type) char[].class).getTypeName(), char[].class),
	BOOLEANS(((Type) boolean[].class).getTypeName(), boolean[].class);
	
	/* 名称 */
	private final String name;
	/* 类型 */
	private final Class<?> type;
	
	/**
	 * @描述 基本类型查找（包含数组）
	 * @param name 待查找基本类型
	 * @return 基本类型（包含数组）
	 * @throws ClassNotFoundException
	 */
	public static Class<?> forName(String name) throws ClassNotFoundException {
		for (PrimitiveClass primitiveClass : PrimitiveClass.values()) {
			if (primitiveClass.name.equals(name)) {return primitiveClass.type;}
		}
		throw new ClassNotFoundException(String.format("根据[%s]未找到对应基本类型！", name));
	}
}
