package org.city.common.api.in;

import java.lang.reflect.Method;
import java.util.Map.Entry;

import org.city.common.api.util.JsonUtil;
import org.city.common.api.util.MyUtil;
import org.springframework.core.env.Environment;

import com.alibaba.fastjson.JSONObject;

/**
 * @作者 ChengShi
 * @日期 2022-07-22 17:09:25
 * @版本 1.0
 * @描述 替换特殊字符接口
 */
public interface Replace {
	/**
	 * @描述 替换values中的特殊字符-${key}取配置与#{key}取方法入参
	 * @param environment 配置环境
	 * @param values 待替换数据
	 * @param datas 方法入参
	 * @param names 方法参数原名称
	 */
	default void replaceVals(Environment environment, String[] values, Object[] datas, String[] names) {
		if (values == null || datas == null || names == null) {return;}
		
		/*转换方法入参*/
		JSONObject dataJson = new JSONObject();
		for (int i = 0, j = datas.length; i < j; i++) {
			try {
				/*如果是字符串直接添加，其余转成JsonObject*/
				if (datas[i] instanceof String) {dataJson.put(names[i], datas[i]);} 
				else {dataJson.put(names[i], JSONObject.toJSON(datas[i]));}
			} catch (Exception e) {/* 不能转换的不处理 */}
		}
		
		/*通过转发参数替换注解的真实值*/
		for (int i = 0, j = values.length; i < j; i++) {
			/* 替换配置 */
			values[i] = replaceConfig(environment, values[i]);
			/* 替换方法入参 */
			values[i] = JsonUtil.getValue(values[i], dataJson);
		}
	}
	
	/**
	 * @描述 只替换配置
	 * @param environment 配置环境上下文
	 * @param val 待替换值
	 * @return 替换结果
	 */
	default String replaceConfig(Environment environment, String val) {
		for (Entry<String, String> idEntry :  MyUtil.getContainValue("${", "}", val).entrySet()) {
			String property = environment.getProperty(idEntry.getValue());
			if (property == null) {throw new RuntimeException(String.format("动态替换配置失败，替换key[%s]对应的值不存在！", idEntry.getKey()));}
			val = val.replace(idEntry.getKey(), property);
		}
		return val;
	}
	
	/**
	 * @描述 只替换执行方法
	 * @param curObjClass 执行方法的类
	 * @param val 待替换值
	 * @return 替换结果
	 */
	default String replaceMethod(Class<?> curObjClass, String val){
		Object curObj = null;
		try {curObj = curObjClass.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);}
		catch (Exception e) {throw new RuntimeException(e);} 
		
		/* 动态替换方法返回值 */
		for (Entry<String, String> cv : MyUtil.getContainValue("#{", "}", val).entrySet()) {
			String property = null;
			try {
				/* 获取当前类指定方法 */
				Method declaredMethod = curObjClass.getDeclaredMethod(cv.getValue(), new Class[0]);
				if (String.class != declaredMethod.getReturnType()) {
					throw new ClassNotFoundException(String.format("当前类[%s]对应注解的方法[%s]返回值类型<%s>不是<java.lang.String>，请检查注解参数值！", curObjClass.getName(), cv.getValue(), declaredMethod.getReturnType().getName()));
				}
				
				/* 设置权限执行方法 */
				declaredMethod.setAccessible(true);
				property = (String) declaredMethod.invoke(curObj, new Object[0]);
			} catch (Exception e) {throw new RuntimeException(String.format("当前类[%s]对应注解未找到方法[%s]，请检查注解参数值！", curObjClass.getName(), cv.getValue()));}
			val = val.replace(cv.getKey(), property);
		}
		return val;
	}
}
