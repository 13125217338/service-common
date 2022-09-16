package org.city.common.api.in.parse;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.city.common.api.constant.PrimitiveClass;

/**
 * @作者 ChengShi
 * @日期 2022年8月20日
 * @版本 1.0
 * @描述 类型解析
 */
public interface TypeParse {
	/**
	 * @描述 类型解析（可解析泛型、基本类型、原类型）
	 * @param typeName 待解析的类名
	 * @return 解析后的类型
	 */
	default Type forType(String typeName) {
		if (typeName == null) {return null;}
		final String name = typeName.trim(); //防止因前后空格导致解析失败
		
		try {
			int before = name.indexOf("<");
			if (before == -1) {
				/* 原类查找返回 */
				try {return Class.forName(name.contains("[]") ? "[L" + name.replace("[]", ";") : name);}
				catch (ClassNotFoundException e) {return PrimitiveClass.forName(name);}
			} else {
				int after = name.indexOf(">"), arr = name.lastIndexOf("[]");
				if (arr > after) {
					/* 数组标志比结束标志大代表是数组泛型 */
					return new GenericArrayType() {
						@Override
						public Type getGenericComponentType() {return forType(name.substring(0, arr));}
					};
				} else {
					return new ParameterizedType() {
						@Override
						public Type getRawType() {return forType(name.substring(0, before));}
						@Override
						public Type getOwnerType() {return null;}
						@Override
						public Type[] getActualTypeArguments() {
							List<Integer> index = new ArrayList<>();
							String ptdType = name.substring(before + 1, after);
							setIndex(index, ptdType, 0); //将对应分割位置记录
							
							/* 通过分割位置分割字符串在递归解析 */
							Type[] types = new Type[index.size()];
							for (int i = 0, j = 0; i < types.length; i++,j++) {
								types[i] = forType(ptdType.substring(j, (j = index.get(i))));
							}
							return types;
						}
						/* 将对应分割位置记录 */
						private void setIndex(List<Integer> index, String ptdType, int startIndex) {
							int after = ptdType.indexOf(">", startIndex), dh = ptdType.indexOf(",", startIndex);
							/* 只有逗号标志大于结束标志代表在右边可截取 - 找不到代表后续无数据 */
							if (dh == -1) {index.add(ptdType.length()); return;} else if (dh > after) {index.add(dh);}
							setIndex(index, ptdType, dh + 1); //递归寻找
						}
					};
				}
			}
		} catch (Exception e) {throw new RuntimeException(e);}
	}
	
	/**
	 * @描述 获取方法返回值类型
	 * @param orgin 源类
	 * @param method 方法
	 * @return 方法返回值类型
	 */
	default Type getReturnType(Class<?> orgin, Method method) {
		Class<?> curClass = method.getDeclaringClass();
		TypeVariable<?>[] types = curClass.getTypeParameters();
		if (curClass == orgin || types.length == 0) {return method.getGenericReturnType();}
		
		while (orgin != Object.class) {
			Type superCls = orgin.getGenericSuperclass(); //父类必须为泛型
			if ((orgin = getClass(superCls)) == curClass && superCls instanceof ParameterizedType) {
				String typeName = method.getGenericReturnType().getTypeName();
				for (int i = 0, j = types.length; i < j; i++) { //泛型名称一致则返回对应下标类型
					if (typeName.equals(types[i].getTypeName())) {return ((ParameterizedType) superCls).getActualTypeArguments()[i];}
				}
			}
		}
		return method.getGenericReturnType();
	}
	
	/**
	 * @描述 根据当前类获取父类泛型信息
	 * @param curClass 当前类
	 * @return key=泛型名称，value=泛型类
	 */
	default Map<String, Class<?>> getGenericSuperClass(Class<?> curClass) {
		ParameterizedType superClass = (ParameterizedType) curClass.getGenericSuperclass();
		Type[] types = superClass.getActualTypeArguments(); //父类泛型信息
		TypeVariable<?>[] typeVars = ((Class<?>) superClass.getRawType()).getTypeParameters(); //当前泛型信息
		
		/* key=泛型名称，value=泛型类 */
		Map<String, Class<?>> result = new HashMap<>();
		for (int i = 0, j = types.length; i < j; i++) {
			result.put(typeVars[i].getName(), getClass(types[i]));
		}
		return result;
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
}
