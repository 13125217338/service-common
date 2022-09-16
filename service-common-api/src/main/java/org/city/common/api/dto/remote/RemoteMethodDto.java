package org.city.common.api.dto.remote;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import org.city.common.api.in.parse.AnnotationParse;
import org.city.common.api.in.parse.TypeParse;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022年8月20日
 * @版本 1.0
 * @描述 远程方法信息
 */
@Data
@Accessors(chain = true)
public class RemoteMethodDto implements TypeParse,AnnotationParse {
	/* 实现类名称 */
	private String className;
	/* 带参数类型的方法名 */
	private String name;
	/* 方法上所有注解 */
	private Map<String, Map<String, Object>> annotations;
	/* 方法对应所有参数 */
	private Map<String, RemoteParameterDto> parameter;
	
	/**
	 * @描述 获取注解
	 * @param <A> 注解类型
	 * @param annotationClass 待获取的注解
	 * @return 注解
	 */
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		if (annotationClass == null) {throw new NullPointerException("传入的注解类型为NULL值！");}
		Map<String, Object> map = annotations.get(annotationClass.getName());
		if (map == null) {return null;}
		return parse(map, annotationClass);
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2023-02-23 10:19:39
	 * @版本 1.0
	 * @parentClass RemoteMethodDto
	 * @描述 返回值结果
	 */
	@Data
	@Accessors(chain = true)
	public static class ReturnResult implements TypeParse {
		/* 返回值类型 */
		private String returnType;
		/* 返回值 */
		private Object returnValue;
		
		/**
		 * @描述 设置返回值类型
		 * @param returnType 返回值类型
		 */
		public ReturnResult $setReturnType(Type returnType) {this.returnType = returnType.getTypeName(); return this;}
		/**
		 * @描述 获取返回值类型
		 * @return 返回值类型
		 */
		public Type $getReturnType() {return forType(returnType);}
	}
}
