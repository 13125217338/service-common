package org.city.common.api.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;

import org.city.common.api.in.parse.JSONParser;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ValueFilter;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 16:51:04
 * @版本 1.0
 * @描述 Json工具
 */
public final class JsonUtil {
	private JsonUtil() {}
	private final static JSONParser PARSER = new JSONParser() {};
	private final static ValueFilter VALUE_FILTER = new ValueFilter() {
		@Override
		public Object process(Object object, String name, Object value) {return parse(value);}
	};
	
	/**
	 * @描述 通过JSONPath替换value中特殊字符的数据，取数为data，条件为#{key}
	 * @param value 待替换数据
	 * @param data 取数为原数据
	 * @return 替换后的数据
	 */
	public static String getValue(String value, JSONObject data) {
		for (Entry<String, String> entry : MyUtil.getContainValue("#{", "}", value).entrySet()) {
			Object val = JSONPath.eval(data, entry.getValue());
			value = value.replace(entry.getKey(), JSON.toJSONString(val, SerializerFeature.SortField));
		}
		return value;
	}
	
	/**
	 * @描述 根据路径设置值
	 * @param root 待设置对象
	 * @param path 路径（数组只支持一级）
	 * @param value 设置值
	 */
	public static void setValue(JSONObject root, String path, Object value) {
		if (value == null) {return;} //不处理空值
		String[] fieldNames = path.split("\\.");
		JSON cur = root, before = null;
		
		/* 字段地址查询 */
		for (String fieldName : fieldNames) {
			if (fieldName.equals("$")) {continue;}
			int k = fieldName.indexOf("[");
			cur = set(before = cur, k >= 0, k < 0 ? fieldName : fieldName.substring(0, k));
		}
		
		/* 追加最后一个字段值 */
		if (cur instanceof JSONArray) {
			if (value instanceof Collection) {
				((JSONArray) cur).addAll((Collection<?>) value);
			} else if (value.getClass().isArray()) {
				int len = Array.getLength(value);
				for (int i = 0; i < len; i++) {((JSONArray) cur).add(Array.get(value, i));}
			} else {((JSONArray) cur).add(value);}
		} else {
			if (before instanceof JSONArray) {
				JSONObject data = ((JSONArray) before).getJSONObject(0);
				((JSONArray) before).clear();
				/* 对原数组或集合重新解析 */
				if (value instanceof Collection) {
					for (Object val : (Collection<?>) value) {
						JSONObject cloneData = JSONObject.parseObject(data.toJSONString());
						((JSONArray) before).add(cloneData.fluentPut(fieldNames[fieldNames.length - 1], val));
					}
				} else if (value.getClass().isArray()) {
					int len = Array.getLength(value);
					for (int i = 0; i < len; i++) {
						JSONObject cloneData = JSONObject.parseObject(data.toJSONString());
						((JSONArray) before).add(cloneData.fluentPut(fieldNames[fieldNames.length - 1], Array.get(value, i)));
					}
				} else {((JSONArray) before).add(data.fluentPut(fieldNames[fieldNames.length - 1], value));}
			} else {
				((JSONObject) before).put(fieldNames[fieldNames.length - 1], value);
			}
		}
	}
	
	/**
	 * @描述 序列化成JSON字符串（一般用作日志打印）
	 * @param value 待序列化对象
	 * @return JSON字符串
	 */
	public static String toJSONString(Object value) {
		return JSON.toJSONString(parse(value), VALUE_FILTER);
	}
	
	/* 不同类型不同处理 */
	@SuppressWarnings("unchecked")
	private static Object parse(Object value) {
		if (value == null) {return null;}
		if (value instanceof Collection) {return parseCollection((Collection<Object>) value);}
		if (value.getClass().isArray()) {return parseArray(value);}
		return parseObject(value);
	}
	/* 集合处理 */
	private static Object parseCollection(Collection<Object> vals) {
		try {
			Collection<Object> datas = new ArrayList<Object>(vals.size());
			for (Object val : vals) {datas.add(parseObject(val));}
			return datas;
		} catch (Exception e) {return null;}
	}
	/* 数组处理 */
	private static Object parseArray(Object vals) {
		int length = Array.getLength(vals);
		Object datas = Array.newInstance(Object.class, length);
		for (int i = 0; i < length; i++) {
			Array.set(datas, i, parseObject(Array.get(vals, i)));
		}
		return datas;
	}
	/* 对象处理 */
	private static Object parseObject(Object val) {
		if (val == null) {return null;}
		if (val instanceof InputStreamSource) {
			if (val instanceof Resource) {return ((Resource) val).getDescription();}
			if (val instanceof MultipartFile) {return ((MultipartFile) val).getOriginalFilename();}
			return val.getClass().getName();
		}
		if (val instanceof InputStream || val instanceof OutputStream || val instanceof Reader || val instanceof Writer) {
			return val.getClass().getName();
		}
		if (val instanceof Throwable) {
			return val.toString();
		}
		return val;
	}
	
	/* 创建并设置路径 */
	private static JSON set(JSON data, boolean isArray, String fieldName) {
		if (data instanceof JSONObject) {
			return setObj((JSONObject) data, isArray, fieldName);
		} else if (data instanceof JSONArray) {
			return setArray((JSONArray) data, isArray, fieldName);
		} else {throw new NullPointerException(String.format("当前对象[%s]非JSON对象！", data.getClass().getName()));}
	}
	/* 设置JSON对象 */
	private static JSON setObj(JSONObject data, boolean isArray, String fieldName) {
		Object dt = ((JSONObject) data).get(fieldName);
		if (dt == null || PARSER.isBaseType(dt.getClass())) {
			JSON value = isArray ? new JSONArray() : new JSONObject();
			data.put(fieldName, value);
			return value;
		} else if (dt instanceof JSON) {return (JSON) dt;}
		else {throw new NullPointerException(String.format("当前对象[%s]非JSON对象！", dt.getClass().getName()));}
	}
	/* 设置JSON数组 */
	private static JSON setArray(JSONArray data, boolean isArray, String fieldName) {
		Object dt = data.size() > 0 ? data.get(0) : null;
		if (dt == null || PARSER.isBaseType(dt.getClass())) {
			JSON value = isArray ? new JSONArray() : new JSONObject();
			data.clear(); data.add(new JSONObject().fluentPut(fieldName, value));
			return value;
		} else if (dt instanceof JSONObject) {return setObj((JSONObject) dt, isArray, fieldName);}
		else {throw new NullPointerException(String.format("当前对象[%s]非JSON对象！", dt.getClass().getName()));}
	}
}
