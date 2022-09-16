package org.city.common.api.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.city.common.api.annotation.plug.Format;
import org.city.common.api.in.FormatFieldValue;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @作者 ChengShi
 * @日期 2022-08-17 15:47:49
 * @版本 1.0
 * @描述 自定义格式化字段值工具
 */
public final class FormatUtil {
	private FormatUtil() {}
	
	/**
	 * @描述 格式化对象上字段值（与注解@Format配套使用）
	 * @param param 待格式化对象
	 */
	public static void format(Object param) {
		if (param == null) {return;}
		
		/* 获取所有字段解析 */
		List<Field> allDeclaredField = FieldUtil.getAllDeclaredField(param.getClass());
		for (Field field : allDeclaredField) {
			Format format = field.getDeclaredAnnotation(Format.class);
			if (format != null && format.format() != FormatFieldValue.class) {
				/* 获取实现类 */
				FormatFieldValue bean = SpringUtil.getBean(format.format());
				Assert.notNull(bean, String.format("实现类[%s]未交给Spring管理！", format.format().getName()));
				
				/* 解析并设置新值 */
				try {field.set(param,  bean.format(field, field.get(param), format.fixVal()));}
				catch (Exception e) {throw new RuntimeException("自定义格式化字段值错误！", e);}
			}
		}
	}
	
	/**
	 * @描述 格式化集合每个对象上字段值（与注解@Format配套使用）
	 * @param params 待格式化集合对象
	 */
	public static void format(Collection<?> params) {
		if (CollectionUtils.isEmpty(params)) {return;}
		
		/* 解析每个对象 */
		for (Object param : params) {format(param);}
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
				List<Field> declaredField = FieldUtil.getAllDeclaredField(data.getClass());
				for (Field field : declaredField) {
					Object dt = field.get(data);
					if (dt != null) {sb.append(String.format("&%s=%s", field.getName(), URLEncoder.encode(String.valueOf(dt), charset)));}
				}
			} catch (Exception e) {throw new RuntimeException(e);}
		}
		return sb.length() > 0 ? url + "?" + sb.substring(1) : url;
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
				value.add(v.getKey(), v.getValue());
			}
		} else {
			try {
				List<Field> declaredField = FieldUtil.getAllDeclaredField(data.getClass());
				for (Field field : declaredField) {
					Object dt = field.get(data);
					/* 集合添加 */
					if (dt instanceof Collection) {
						for (Object d : (Collection<?>) dt) {
							value.add(field.getName(), d);
						}
					/* 数组添加 */
					} else if(dt.getClass().isArray()) {
						int length = Array.getLength(dt);
						for (int i = 0; i < length; i++) {
							value.add(field.getName(), Array.get(dt, i));
						}
					/* 对象添加 */
					} else {value.add(field.getName(), dt);}
				}
			} catch (Exception e) {throw new RuntimeException(e);}
		}
		return value;
	}
}
