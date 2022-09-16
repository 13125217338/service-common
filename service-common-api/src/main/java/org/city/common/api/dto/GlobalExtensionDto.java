package org.city.common.api.dto;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.city.common.api.in.parse.AnnotationParse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @作者 ChengShi
 * @日期 2022年7月23日
 * @版本 1.0
 * @描述 全局扩展点参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalExtensionDto implements AnnotationParse{
	/* 标记的唯一ID */
	private String onlyId;
	/* 前缀路径 */
	private String prifixPath;
	/* 方法地址 */
	private Map<String, String> methodPath;
	/* 方法参数名称 */
	private Map<String, String[]> methodParamNames;
	/* 自定义注解类型 */
	private String annotationClass;
	/* 注解值 */
	private Map<String, Object> annotationVal;
	/* 记录时间（毫秒） */
	private long recordTime;
	
	/**
	 * @描述 获取注解
	 * @param <A> 注解类型
	 * @param annotationClass 待获取的注解
	 * @return 注解
	 */
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		if (annotationClass == null) {throw new NullPointerException("传入的注解类型为NULL值！");}
		if (!annotationClass.getName().equals(this.annotationClass)) {
			throw new IllegalArgumentException(String.format("传入的注解类型[%s]与实现者的注解类型[%s]不一致！", annotationClass.getName(), this.annotationClass));
		}
		return parse(annotationVal, annotationClass);
	}
}
