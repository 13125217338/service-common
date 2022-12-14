package org.city.common.api.dto.remote;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import org.city.common.api.dto.TypeDto;
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
public class RemoteMethodDto implements TypeParse,AnnotationParse{
	/* 对应SpringBean名称 */
	private String beanName;
	/* 带参数类型的方法名 */
	private String name;
	/* 方法上所有注解 */
	private Map<String, Map<String, Object>> annotations;
	/* 返回值类型 */
	private TypeDto returnType;
	/* 参数所有类型 */
	private TypeDto[] parameterTypes;
	/* 异常所有类型 */
	private TypeDto[] exceptionTypes;
	/* 方法对应所有参数 */
	private Map<String, RemoteParameterDto> parameter;
	
	/**
	 * @描述 获取返回值类型
	 * @return 返回值类型
	 * @throws ClassNotFoundException
	 */
	public Class<?> $getReturnClass() throws ClassNotFoundException {return forName(returnType.getClassName());}
	/**
	 * @描述 设置返回值类型
	 * @param returnType 返回值类型
	 */
	public void $setReturnType(Type returnType) {this.returnType = product(returnType);}
	/**
	 * @描述 获取返回值类型
	 * @return 返回值类型
	 */
	public Type $getReturnType() {return parse(returnType);}
	
	/**
	 * @描述 获取参数类型
	 * @return 参数类型
	 * @throws ClassNotFoundException 
	 */
	public Class<?>[] $getParameterClasses() throws ClassNotFoundException {
		Class<?>[] classes = new Class[parameterTypes == null ? 0 : parameterTypes.length];
		for (int i = 0, j = classes.length; i < j; i++) {classes[i] = forName(parameterTypes[i].getClassName());}
		return classes;
	}
	/**
	 * @描述 设置参数类型
	 * @param parameterTypes 参数类型
	 */
	public void $setParameterTypes(Type[] parameterTypes) {
		TypeDto[] types = new TypeDto[parameterTypes == null ? 0 : parameterTypes.length];
		for (int i = 0, j = types.length; i < j; i++) {types[i] = product(parameterTypes[i]);}
		this.parameterTypes = types;
	}
	/**
	 * @描述 获取参数类型
	 * @return 参数类型
	 */
	public Type[] $getParameterTypes() {
		Type[] types = new Type[parameterTypes == null ? 0 : parameterTypes.length];
		for (int i = 0, j = types.length; i < j; i++) {types[i] = parse(parameterTypes[i]);}
		return types;
	}
	
	/**
	 * @描述 获取异常类型
	 * @return 异常类型
	 * @throws ClassNotFoundException
	 */
	public Class<?>[] $getExceptionClasses() throws ClassNotFoundException {
		Class<?>[] classes = new Class[exceptionTypes == null ? 0 : exceptionTypes.length];
		for (int i = 0, j = classes.length; i < j; i++) {classes[i] = forName(exceptionTypes[i].getClassName());}
		return classes;
	}
	/**
	 * @描述 设置异常类型
	 * @param exceptionTypes 异常类型
	 */
	public void $setExceptionTypes(Type[] exceptionTypes) {
		TypeDto[] types = new TypeDto[exceptionTypes == null ? 0 : exceptionTypes.length];
		for (int i = 0, j = types.length; i < j; i++) {types[i] = product(exceptionTypes[i]);}
		this.exceptionTypes = types;
	}
	/**
	 * @描述 获取异常类型
	 * @return 异常类型
	 */
	public Type[] $getExceptionTypes() {
		Type[] types = new Type[exceptionTypes == null ? 0 : exceptionTypes.length];
		for (int i = 0, j = types.length; i < j; i++) {types[i] = parse(exceptionTypes[i]);}
		return types;
	}
	
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
}
