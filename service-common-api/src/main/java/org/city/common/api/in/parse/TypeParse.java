package org.city.common.api.in.parse;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.city.common.api.constant.PrimitiveClass;
import org.city.common.api.dto.TypeDto;

/**
 * @作者 ChengShi
 * @日期 2022年8月20日
 * @版本 1.0
 * @描述 类型解析
 */
public interface TypeParse {
	/**
	 * @描述 生成一个类型参数
	 * @param type 待生成类型
	 * @return 类型参数
	 */
	default TypeDto product(Type type) {
		if(type instanceof Class) {
			/* 原类 */
			return new TypeDto().setTypeName(((Class<?>) type).getName());
		} else if(type instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) type;
			TypeDto[] types = new TypeDto[ptype.getActualTypeArguments().length];
			/* 解析多个泛型类 */
			for (int i = 0, j = types.length; i < j; i++) {
				types[i] = product(ptype.getActualTypeArguments()[i]);
			}
			/* 泛型 */
			return new TypeDto().setTypeName(ParameterizedType.class.getName())
					.setRawType(product(ptype.getRawType()))
					.setOwnerType(product(ptype.getOwnerType()))
					.setActualType(types);
		} else if(type instanceof GenericArrayType) {
			GenericArrayType atype = (GenericArrayType) type;
			/* 数组泛型 */
			return new TypeDto().setTypeName(GenericArrayType.class.getName())
					.setGenericArray(product(atype.getGenericComponentType()));
		} else {return null;}
	}
	
	/**
	 * @描述 类型解析
	 * @param typeDto 待解析的参数
	 * @return 解析后的类型
	 */
	default Type parse(TypeDto typeDto) {
		if (typeDto == null || typeDto.getTypeName() == null) {return null;}
		try {
			/* 泛型 */
			if(typeDto.getTypeName().equals(ParameterizedType.class.getName())) {
				return new ParameterizedType() {
					@Override
					public Type getRawType() {return parse(typeDto.getRawType());}
					@Override
					public Type getOwnerType() {return parse(typeDto.getOwnerType());}
					@Override
					public Type[] getActualTypeArguments() {
						Type[] types = new Type[typeDto.getActualType().length];
						for (int i = 0, j = types.length; i < j; i++) {types[i] = parse(typeDto.getActualType()[i]);}
						return types;
					}
				};
			/* 数组泛型 */
			} else if(typeDto.getTypeName().equals(GenericArrayType.class.getName())) {
				return new GenericArrayType() {
					@Override
					public Type getGenericComponentType() {return parse(typeDto.getGenericArray());}
				};
			/* 原类 */
			} else {return forName(typeDto.getTypeName());}
		} catch (Exception e) {throw new RuntimeException(e);}
	}
	
	
	/**
	 * @描述 获取原类
	 * @param type 待生成类型
	 * @return 原类
	 */
	default Class<?> getClass(Type type) {
		if(type instanceof ParameterizedType) {return (Class<?>) ((ParameterizedType) type).getRawType();}
		else if(type instanceof GenericArrayType) {return getClass(((GenericArrayType) type).getGenericComponentType());}
		else {return (Class<?>) type;}
	}
	
	/**
	 * @描述 通过类名找类（支持查找基本类型）
	 * @param name 类名
	 * @return 对应类
	 * @throws ClassNotFoundException
	 */
	default Class<?> forName(String name) throws ClassNotFoundException {
		try {return Class.forName(name);}
		catch (ClassNotFoundException e) {return PrimitiveClass.forName(name);}
	}
}
