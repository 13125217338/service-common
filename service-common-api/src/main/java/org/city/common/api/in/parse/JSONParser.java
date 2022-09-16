package org.city.common.api.in.parse;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;

/**
 * @作者 ChengShi
 * @日期 2022-07-22 18:19:48
 * @版本 1.0
 * @描述 自定义JSON转换解析
 */
public interface JSONParser extends AnnotationParse{
	/**
	 * @描述 转换JSON对象值，可以转换基本类型（包括String类型与注解类型）
	 * @param val 待转换类型对象数据
	 * @param target 被转换类型
	 * @return 转换后的类型对象数据
	 */
	@SuppressWarnings("unchecked")
	default <T> T parse(Object val, Type target) {
		if (val == null || target == null || void.class == target) {return null;}
		if (val.getClass() == target) {return (T) val;}
		
		/* 如果是字符串类型 - 验证是否是JsonStr，如果是则转成JSON类型 */
		if (val.getClass() == String.class) {
			JSONValidator validator = JSONValidator.from((String) val);
			if (validator.getType() != com.alibaba.fastjson.JSONValidator.Type.Value) {
				val = JSON.parse((String) val);
			}
		}
		
		/* 基本类型获取 */
		Class<?> param = target instanceof Class ? (Class<?>) target : null;
		boolean valBase = isBase(val.getClass()), pameraBase = param == null ? false : isBase(param);
		/* 根据类型进行转换 */
		if (valBase && pameraBase) {
			/* 获取基本类型值 */
			try {return (T) getBaseVal(val, param);}
			catch (Exception e) {throw new RuntimeException(e);}
		} else {
			try {
				if (param != null) {
					boolean isAnnotation = param.isAnnotation();
					if (param.isArray()) {isAnnotation = param.getComponentType().isAnnotation();}
					if (isAnnotation) {return (T) parseAnnotation(val, param);}
				}
				return JSONObject.parseObject(JSONObject.toJSONBytes(val), target);
			}catch (Exception e) {throw new IllegalArgumentException(String.format("值类型[%s]与转换类型[%s]不一致！", val.getClass().getName(), target.getTypeName()));}
		}
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
		else if(val instanceof Collection) {
			Object vl = ((Collection<?>) val).isEmpty() ? new HashMap<>() : ((Collection<?>) val).iterator().next();
			if (vl instanceof Map) {return parse((Map<String, Object>) vl, (Class<Annotation>) param);}
			else {return parse((Map<String, Object>) JSON.toJSON(vl), (Class<Annotation>) param);}
		}
		return null;
	}
	/* 获取基本类型转换值 */
	private static Object getBaseVal(Object val, Class<?> pamera) throws ParseException {
		String value = val.toString();
		if (String.class == pamera) {return value;}
		else if (Number.class == pamera) {
			return DecimalFormat.getInstance().parse(value);
		}else if(int.class == pamera || Integer.class == pamera){
			return Integer.valueOf(value);
		}else if(long.class == pamera || Long.class == pamera){
			return Long.valueOf(value);
		}else if(boolean.class == pamera || Boolean.class == pamera){
			return Boolean.valueOf(value);
		}else if(double.class == pamera || Double.class == pamera){
			return Double.valueOf(value);
		}else if(short.class == pamera || Short.class == pamera){
			return Short.valueOf(value);
		}else if(byte.class == pamera || Byte.class == pamera){
			return Byte.valueOf(value);
		}else if(float.class == pamera || Float.class == pamera){
			return Float.valueOf(value);
		}else if(char.class == pamera || Character.class == pamera){
			return value.toCharArray()[0];
		}else{return null;}
	}
	
	/**
	 * @描述 判断是否时基本类型
	 * @param verify 验证类
	 * @return true是基本类型（包括String类型）
	 */
	default boolean isBase(Class<?> verify) {
		if (verify == null) {return false;}
		else if(String.class == verify) {return true;}
		else if(Integer.class == verify) {return true;}
		else if(verify.isPrimitive()) {return true;}
		else if(Long.class == verify) {return true;}
		else if(Boolean.class == verify) {return true;}
		else if(Byte.class == verify) {return true;}
		else if(Double.class == verify) {return true;}
		else if(Short.class == verify) {return true;}
		else if(Character.class == verify) {return true;}
		else if(Float.class == verify) {return true;}
		else {return false;}
	}
}
