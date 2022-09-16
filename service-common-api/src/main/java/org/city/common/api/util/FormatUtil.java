package org.city.common.api.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.city.common.api.annotation.plug.Format;
import org.city.common.api.dto.DataList;
import org.city.common.api.dto.Response;
import org.city.common.api.in.FormatFieldValue;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.util.Replace;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @作者 ChengShi
 * @日期 2022-08-17 15:47:49
 * @版本 1.0
 * @描述 自定义格式化字段值工具
 */
public final class FormatUtil {
	private final static JSONParser PARSER = new JSONParser() {};
	private final static Replace REPLACE = new Replace() {};
	private final static Map<Class<?>, Map<Field, Entry<String[], FormatFieldValue<?>>>> FIELD_CACHE = new ConcurrentHashMap<>();
	private final static String URL_PARAMER = "?";
	private FormatUtil() {}
	
	/**
	 * @描述 格式化对象上的字段值（与注解@Format配套使用）
	 * @param param 待格式化对象（支持Map与集合与数组）
	 */
	public static void format(Object param) {
		if (param == null || PARSER.isBaseType(param.getClass())) {return;}
		if (param instanceof Response<?>) {format(((Response<?>) param).getData()); return;}
		if (param instanceof DataList<?>) {format(((DataList<?>) param).getRows()); return;}
		if (param instanceof Map) {format((Map<?, ?>) param); return;}
		if (param instanceof Collection) {format((Collection<?>) param); return;}
		if (param.getClass().isArray()) {formatArray(param); return;}
		
		/* 获取所有缓存字段解析 */
		Map<Field, Entry<String[], FormatFieldValue<?>>> fields = FIELD_CACHE.computeIfAbsent(param.getClass(), pcls -> {
			Map<Field, Entry<String[], FormatFieldValue<?>>> cache = new HashMap<>();
			/* 获取所有非常量字段 */
			for (Field field : FieldUtil.getAllDeclaredField(pcls, false).values()) {
				Format format = field.getDeclaredAnnotation(Format.class);
				if (format == null) {continue;} //无格式化注解不解析
				
				/* 嵌套解析 */
				if (format.format() == FormatFieldValue.class) {cache.put(field, null);}
				else {
					FormatFieldValue<?> bean = SpringUtil.getBean(format.format());
					Assert.notNull(bean, String.format("实现类[%s]未交给Spring管理！", format.format().getName()));
					/* 字段解析 */
					cache.put(field, new Entry<String[], FormatFieldValue<?>>() {
						private final String[] values = getValues();
						private final FormatFieldValue<?> ffv = bean;
						@Override
						public FormatFieldValue<?> setValue(FormatFieldValue<?> value) {return null;}
						@Override
						public FormatFieldValue<?> getValue() {return this.ffv;}
						@Override
						public String[] getKey() {return this.values;}
						/* 获取已替换值 */
						private String[] getValues() {
							String[] values = format.values();
							Environment environment = SpringUtil.getEnvironment();
							for (int i = 0, j = values.length; i < j; i++) {
								values[i] = REPLACE.replaceConfig(pcls, environment, values[i]);
							}
							return values;
						}
					});
				}
			}
			return cache;
		});
		
		/* 处理待解析字段 */
		for (Entry<Field, Entry<String[], FormatFieldValue<?>>> entry : fields.entrySet()) {
			if (entry.getValue() == null) { //嵌套解析
				try {format(entry.getKey().get(param));}
				catch (Exception e) {throw new RuntimeException("自定义格式化特定类型错误！", e);}
			} else { //字段解析
				Entry<String[], FormatFieldValue<?>> ey = entry.getValue();
				try {entry.getKey().set(param, ey.getValue().format(param, entry.getKey(), getCopyValues(ey.getKey())));}
				catch (Throwable e) {throw new RuntimeException("自定义格式化字段值错误！", e);}
			}
		}
	}
	/* 格式化数组每个对象上的字段值 */
	private static void formatArray(Object params) {
		int len = Array.getLength(params);
		for (int i = 0; i < len; i++) {format(Array.get(params, i));} //解析每个对象
	}
	/* 格式化集合每个对象上的字段值 */
	private static void format(Collection<?> params) {
		for (Object param : params) {format(param);} //解析每个对象
	}
	/* 格式化Map每个对象上的字段值 */
	private static void format(Map<?, ?> params) {
		for (Entry<?, ?> param : params.entrySet()) { //解析每个对象
			format(param.getKey()); //格式化key
			format(param.getValue()); //格式化value
		}
	}
	/* 获取拷贝后的字符串数组 */
	private static String[] getCopyValues(String[] values) {
		String[] result = new String[values.length];
		for (int i = 0, j = values.length; i < j; i++) {
			result[i] = values[i];
		}
		return result;
	}
	
	/**
	 * @描述 获取Rest地址拼接数据
	 * @param url 原地址参数
	 * @param data 待拼接格式化数据
	 * @param charset Url的编码格式
	 * @return Rest地址拼接数据
	 */
	@SuppressWarnings("unchecked")
	public static String getUrlSetValue(String url, Object data, Charset charset) {
		if (data == null) {return url;}
		
		StringBuilder sb = new StringBuilder();
		/* Map直接添加 */
		if (data instanceof Map) {
			for (Entry<String, ?> entry : ((Map<String, ?>) data).entrySet()) {
				Object dt = entry.getValue();
				if (dt != null) {sb.append(String.format("&%s=%s", entry.getKey(), URLEncoder.encode(String.valueOf(dt), charset)));}
			}
		} else {
			try {
				for (Field field : FieldUtil.getAllDeclaredField(data.getClass(), true).values()) {
					Object dt = field.get(data);
					if (dt != null) {sb.append(String.format("&%s=%s", field.getName(), URLEncoder.encode(String.valueOf(dt), charset)));}
				}
			} catch (Exception e) {throw new RuntimeException(e);}
		}
		return sb.length() > 0 ? url + URL_PARAMER + sb.substring(1) : url;
	}
	
	/**
	 * @描述 通过对象转Spring多媒体数据
	 * @param data 对象
	 * @return 多媒体数据
	 */
	@SuppressWarnings("unchecked")
	public static MultiValueMap<String, Object> getMutiValueMap(Object data) {
		MultiValueMap<String, Object> value = new LinkedMultiValueMap<>();
		if (data == null) {return value;}
		
		/* Map直接添加 */
		if (data instanceof Map) {
			for (Entry<String, ?> v : ((Map<String, ?>) data).entrySet()) {
				mutiValueMapAdd(v.getKey(), v.getValue(), value);
			}
		} else {
			try {
				for (Field field : FieldUtil.getAllDeclaredField(data.getClass(), true).values()) {
					mutiValueMapAdd(field.getName(), field.get(data), value);
				}
			} catch (Exception e) {throw new RuntimeException(e);}
		}
		return value;
	}
	/* 数组添加 */
	private static void mutiValueMapAdd(String name, Object dt, MultiValueMap<String, Object> value) {
		/* 集合添加 */
		if (dt instanceof Collection) {
			for (Object d : (Collection<?>) dt) {
				value.add(name, d);
			}
		/* 数组添加 */
		} else if(dt.getClass().isArray()) {
			int length = Array.getLength(dt);
			for (int i = 0; i < length; i++) {
				value.add(name, Array.get(dt, i));
			}
		/* 对象添加 */
		} else {value.add(name, dt);}
	}
}
