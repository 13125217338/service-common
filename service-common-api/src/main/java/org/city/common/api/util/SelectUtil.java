package org.city.common.api.util;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.city.common.api.annotation.plug.SelectInvoke;
import org.city.common.api.in.parse.JSONParser;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * @作者 ChengShi
 * @日期 2023-3-11 12:59:50
 * @版本 1.0
 * @描述 选择工具
 */
public final class SelectUtil {
	private SelectUtil() {}
	/* 选择执行的缓存数据 */
	private final static Map<Class<?>, Map<Integer, Entry<Object, Method>>> CACHE = new HashMap<>();
	/* JSON解析对象 */
	private final static JSONParser PARSER = new JSONParser() {};
	
	/**
	 * @描述 选择执行方法（与注解@SelectInvoke配套使用）
	 * @param invokeClass 待执行类
	 * @param value 选择执行注解唯一值
	 * @param args 执行方法入参
	 * @return 执行方法返回值
	 */
	public static Object invoke(Class<?> invokeClass, Integer value, Object...args) {
		/* 获取待执行列表 */
		Map<Integer, Entry<Object, Method>> invokes = CACHE.get(invokeClass);
		if (invokes == null) { //加锁初始执行列表
			synchronized (invokeClass) {
				invokes = CACHE.computeIfAbsent(invokeClass, k -> {
					return initMethod(k, new HashMap<>()); //初始化选择执行方法
				});
			}
		}
		
		/* 获取待执行方法 */
		Entry<Object, Method> invoke = invokes.get(value);
		if (invoke == null) {return null;} //直接返回空
		else {
			Type[] gpTypes = invoke.getValue().getGenericParameterTypes();
			Assert.isTrue(gpTypes.length == args.length, String.format("执行方法入参个数[%d]与实际入参个数[%d]不一致！", gpTypes.length, args.length));
			for (int i = 0, j = gpTypes.length; i < j; i++) {args[i] = PARSER.parse(args[i], gpTypes[i]);} //将入参数据转换
			
			try {return invoke.getValue().invoke(invoke.getKey(), args);} //反射选择执行方法
			catch (Exception e) {throw new RuntimeException(String.format("选择执行方法[%s]失败！", invoke.getValue().toGenericString()), e);}
		}
	}
	/* 初始化选择执行方法 */
	private static Map<Integer, Entry<Object, Method>> initMethod(Class<?> invokeClass, Map<Integer, Entry<Object, Method>> invokes) {
		final Object invokeBean = SpringUtil.getBean(invokeClass); //获取代理对象
		invokeClass = ClassUtils.getUserClass(AopProxyUtils.ultimateTargetClass(invokeBean)); //获取原类型
		
		for (Method method : invokeClass.getDeclaredMethods()) {
			SelectInvoke selectInvoke = method.getDeclaredAnnotation(SelectInvoke.class);
			if (selectInvoke != null) { //找到了就初始化
				method.setAccessible(true); //可被反射执行
				
				/* 添加当前方法缓存 */
				invokes.put(selectInvoke.value(), new Entry<Object, Method>() {
					private final Method value = method;
					@Override
					public Object getKey() {return invokeBean;}
					@Override
					public Method getValue() {return value;}
					@Override
					public Method setValue(Method value) {return value;}
				});
			}
		}
		return invokes;
	}
}
