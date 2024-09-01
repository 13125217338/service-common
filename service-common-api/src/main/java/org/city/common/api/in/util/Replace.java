package org.city.common.api.in.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.city.common.api.in.parse.AuthIsJsonParse;
import org.city.common.api.util.JsonUtil;
import org.city.common.api.util.MyUtil;
import org.springframework.core.env.Environment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * @作者 ChengShi
 * @日期 2022-07-22 17:09:25
 * @版本 1.0
 * @描述 替换特殊字符接口
 */
public interface Replace extends AuthIsJsonParse {
	/**
	 * @描述 替换values中的特殊字符 - 支持${key}取配置值与#{key}取方法入参与&{method}取执行方法返回值
	 * @param environment 配置环境
	 * @param values 待替换数据
	 * @param curObj 当前对象（非代理对象）
	 * @param datas 方法入参
	 * @param names 方法参数原名称
	 */
	default void replaceVals(Environment environment, String[] values, Object curObj, Object[] datas, String[] names) {
		if (values == null || datas == null || names == null) {return;}
		
		/* 转换方法入参 */
		JSONObject dataJson = new JSONObject();
		for (int i = 0, j = datas.length; i < j; i++) {
			try {
				/* 如果是字符串直接添加，其余验证通过转成JsonObject */
				if (datas[i] instanceof String) {dataJson.put(names[i], datas[i]);} 
				else if(authParse((datas[i]))) {dataJson.put(names[i], JSON.toJSON(datas[i]));}
			} catch (Exception e) {/* 不能转换的不处理 */}
		}
		
		/* 通过转发参数替换注解的真实值 */
		for (int i = 0, j = values.length; i < j; i++) {
			/* 替换方法入参 */
			values[i] = JsonUtil.getValue(values[i], dataJson);
			/* 替换配置 */
			values[i] = replaceConfig(curObj.getClass(), environment, values[i]);
			/* 替换执行方法 */
			values[i] = replaceMethod(curObj, values[i], datas);
		}
	}
	
	/**
	 * @描述 只替换配置
	 * @param curObjClass 当前对象类
	 * @param environment 配置环境上下文
	 * @param val 待替换值
	 * @return 替换结果
	 */
	default String replaceConfig(Class<?> curObjClass, Environment environment, String val) {
		/* 动态替换配置值 */
		for (Entry<String, String> cv : MyUtil.getContainValue("${", "}", val).entrySet()) {
			int mh = cv.getValue().indexOf(":"); 
			
			/* 查找配置Key */
			String eKey = mh == -1 ? cv.getValue() : cv.getValue().substring(0, mh);
			String property = environment.getProperty(eKey, mh == -1 ? null : cv.getValue().substring(mh + 1));
			
			/* 没有默认值则必须得有配置值 */
			if (property == null) {throw new RuntimeException(String.format("当前类[%s]配置键[%s]不存在！", curObjClass.getName(), cv.getValue()));}
			val = val.replace(cv.getKey(), property);
		}
		return val;
	}
	
	/**
	 * @描述 只替换执行方法
	 * @param curObj 当前对象（非代理对象）
	 * @param val 待替换值
	 * @param args 原方法入参
	 * @return 替换结果
	 */
	default String replaceMethod(Object curObj, String val, Object[] args){
		Class<?> curObjClass = curObj.getClass();
		Map<String, Method> methods = Arrays.asList(curObjClass.getDeclaredMethods()).stream().collect(Collectors.toMap(k -> k.getName(), v -> v, (ov, nv) -> ov));
		
		/* 动态替换方法返回值 */
		for (Entry<String, String> cv : MyUtil.getContainValue("&{", "}", val).entrySet()) {
			Object property = null;
			try {
				/* 获取当前类指定方法 */
				Method method = methods.get(cv.getValue());
				/* 设置权限执行方法 */
				method.setAccessible(true);
				/* 执行结果 */
				property = method.invoke(curObj, args);
			} catch (Exception e) {throw new RuntimeException(String.format("当前类[%s]执行方法[%s]不存在！", curObjClass.getName(), cv.getValue()));}
			
			/* 必须得有方法返回值 */
			if (property == null) {throw new RuntimeException(String.format("当前类[%s]执行方法[%s]返回值为空！", curObjClass.getName(), cv.getValue()));}
			val = val.replace(cv.getKey(), property.toString());
		}
		return val;
	}
}
