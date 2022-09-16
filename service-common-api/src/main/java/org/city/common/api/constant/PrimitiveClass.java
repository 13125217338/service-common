package org.city.common.api.constant;

/**
 * @作者 ChengShi
 * @日期 2022-10-08 14:48:20
 * @版本 1.0
 * @描述 基本类型
 */
public enum PrimitiveClass {
	VOID(void.class.getName(), Void.TYPE),
	BYTE(byte.class.getName(), Byte.TYPE),
	SHORT(short.class.getName(), Short.TYPE),
	INT(int.class.getName(), Integer.TYPE),
	LONG(long.class.getName(), Long.TYPE),
	FLOAT(float.class.getName(), Float.TYPE),
	DOUBLE(double.class.getName(), Double.TYPE),
	CHAR(char.class.getName(), Character.TYPE),
	BOOLEAN(boolean.class.getName(), Boolean.TYPE),
	;
	
	/* 名称 */
	private final String name;
	/* 类型 */
	private final Class<?> type;
	private PrimitiveClass(String name, Class<?> type) {
		this.name = name; this.type = type;
	}
	
	/**
	 * @描述 基本类型查找
	 * @param name 待查找基本类型
	 * @return 基本类型
	 * @throws ClassNotFoundException
	 */
	public static Class<?> forName(String name) throws ClassNotFoundException {
		for (PrimitiveClass primitiveClass : PrimitiveClass.values()) {
			if (primitiveClass.name.equals(name)) {return primitiveClass.type;}
		}
		throw new ClassNotFoundException(String.format("根据[%s]未找到对应基本类型！", name));
	}
}
