package org.city.common.api.in.parse;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

/**
 * @作者 ChengShi
 * @日期 2022年8月20日
 * @版本 1.0
 * @描述 注解解析
 */
public interface AnnotationParse {
	/**
	 * @描述 解析参数为注解对象
	 * @param <A> 注解类型
	 * @param annotationData 注解数据
	 * @param annotationClass 待解析成的注解
	 * @return 解析后的注解对象
	 */
	@SuppressWarnings("unchecked")
	default <A extends Annotation> A parse(Map<String, Object> annotationData, Class<A> annotationClass) {
		return (A) java.lang.reflect.Proxy.newProxyInstance(this.getClass().getClassLoader(),
					new Class<?>[]{annotationClass}, new Proxy(annotationData));
	}
	
	/**
	 * @描述 解析参数为注解对象数组
	 * @param <A> 注解类型
	 * @param annotationDatas 多个注解数据
	 * @param annotationClass 待解析成的注解
	 * @return 解析后的注解对象数组
	 */
	@SuppressWarnings("unchecked")
	default <A extends Annotation> A[] parse(Collection<Map<String, Object>> annotationDatas, Class<A> annotationClass) {
		A[] vals = (A[]) Array.newInstance(annotationClass, annotationDatas.size());
		int i = 0;
		for (Map<String, Object> val : annotationDatas) {
			Array.set(vals, i, (A) java.lang.reflect.Proxy.newProxyInstance(this.getClass().getClassLoader(),
					new Class<?>[]{annotationClass}, new Proxy(val)));
			i++;
		}
		return vals;
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2022年8月20日
	 * @版本 1.0
	 * @描述 代理类
	 */
	public static class Proxy implements InvocationHandler,JSONParser{
		private final Map<String, Object> annotationData;
		public Proxy(Map<String, Object> annotationData) {this.annotationData = annotationData;}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return parse(annotationData.get(method.getName()), method.getGenericReturnType());
		}
		
	}
}
