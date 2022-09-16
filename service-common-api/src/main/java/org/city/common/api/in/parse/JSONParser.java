package org.city.common.api.in.parse;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;

/**
 * @作者 ChengShi
 * @日期 2022-07-22 18:19:48
 * @版本 1.0
 * @描述 自定义JSON转换解析
 */
public interface JSONParser extends AnnotationParse,AuthIsJsonParse {
	/**
	 * @描述 转换JSON对象值，可以转换基本类型（包括String类型与注解类型）
	 * @param <T> 转换后的类型
	 * @param val 待转换对象数据
	 * @param target 被转换类型
	 * @return 转换后的对象数据
	 */
	@SuppressWarnings("unchecked")
	default <T> T parse(Object val, Type target) {
		/* 如果是空值且类型是基本类型 - 则获取默认值 */
		if (val == null && target instanceof Class && ((Class<?>) target).isPrimitive()) {
			return (T) getPrimitiveValue((Class<?>) target);
		}
		if (val == null || target == null || void.class == target) {return null;}
		if (val.getClass() == target || (target instanceof Class && ((Class<?>) target).isAssignableFrom(val.getClass()))) {return (T) val;}
		Assert.isTrue(authParse(target), String.format("类型[%s]禁用解析转换！", target.toString()));
		
		try {
			/* 如果是字符串类型 - 因为判断过类型相等，所以val必须是JsonStr */
			if (val.getClass() == String.class) {
				val = JSON.parse((String) val, Feature.AllowComment, Feature.OrderedField);
			}
			
			/* 注解类型操作 */
			if (target instanceof Class) {
				Class<?> param = (Class<?>) target;
				boolean isAnnotation = param.isAnnotation();
				if (param.isArray()) {isAnnotation = param.getComponentType().isAnnotation();}
				if (isAnnotation) {return (T) parseAnnotation(val, param);}
			}
			return JSONObject.parseObject(JSONObject.toJSONBytes(val), target, Feature.OrderedField);
		} catch (Exception e) {throw new IllegalArgumentException(String.format("转换异常！值类型[%s]，转换类型[%s]", val.getClass().getName(), target.getTypeName()), e);}
	}
	
	/* 解析注解类型 */
	private Object parseAnnotation(Object val, Class<?> param) {
		if (param.isArray()) {
			param = param.getComponentType();
			Object annotations = parseAnnotationA(val, param);
			if (annotations == null) {annotations = parseAnnotationA(JSON.toJSON(val), param);}
			return annotations;
		} else {
			Object annotation = parseAnnotationO(val, param);
			if (annotation == null) {annotation = parseAnnotationO(JSON.toJSON(val), param);}
			return annotation;
		}
	}
	/* 数组注解 */
	@SuppressWarnings("unchecked")
	private Object parseAnnotationA(Object val, Class<?> param) {
		if (val instanceof Map) {return parse((Collection<Map<String, Object>>) Arrays.asList((Map<String, Object>) val), (Class<Annotation>) param);}
		else if (val instanceof Collection) {
			Collection<Map<String, Object>> vals = new ArrayList<>();
			for (Object vl : (Collection<?>) val) {
				if (vl instanceof Map) {vals.add((Map<String, Object>) vl);}
				else {vals.add((Map<String, Object>) JSON.toJSON(vl));}
			}
			return parse(vals, (Class<Annotation>) param);
		}
		return null;
	}
	/* 对象注解 */
	@SuppressWarnings("unchecked")
	private Object parseAnnotationO(Object val, Class<?> param) {
		if (val instanceof Map) {return parse((Map<String, Object>) val, (Class<Annotation>) param);}
		else if (val instanceof Collection) {
			Object vl = ((Collection<?>) val).isEmpty() ? new HashMap<>() : ((Collection<?>) val).iterator().next();
			if (vl instanceof Map) {return parse((Map<String, Object>) vl, (Class<Annotation>) param);}
			else {return parse((Map<String, Object>) JSON.toJSON(vl), (Class<Annotation>) param);}
		}
		return null;
	}
	
	/**
	 * @描述 获取基本类型默认值
	 * @param parmera 待获取的基本类型
	 * @return 基本类型默认值
	 */
	default Object getPrimitiveValue(Class<?> parmera){
		if (boolean.class == parmera) {return false;}
		else if (byte.class == parmera) {return (byte)0;}
		else if (short.class == parmera) {return (short)0;}
		else if (int.class == parmera) {return 0;}
		else if (long.class == parmera) {return 0L;}
		else if (char.class == parmera) {return (char)0;}
		else if (float.class == parmera) {return 0F;}
		else {return 0D;}
	}
	
	/**
	 * @描述 是否基本类型或引用基本类型
	 * @param parmera 待判断类型
	 * @return true=基本类型或引用基本类型
	 */
	default boolean isBaseType(Class<?> parmera) {
		return parmera.isPrimitive() ||
			   String.class == parmera ||
			   Integer.class == parmera ||
			   Long.class == parmera ||
			   Double.class == parmera ||
			   Boolean.class == parmera ||
			   Byte.class == parmera ||
			   Short.class == parmera ||
			   Float.class == parmera ||
			   Character.class == parmera;
	}
}
