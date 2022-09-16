package org.city.common.api.dto.remote;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.city.common.api.in.parse.AnnotationParse;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-09-28 08:58:01
 * @版本 1.0
 * @描述 远程类信息
 */
@Data
@Accessors(chain = true)
public class RemoteClassDto implements AnnotationParse {
	/* 接口类名 */
	private String interfaceName;
	/* 实现类名 */
	private String name;
	/* 对应SpringBean名称 */
	private String beanName;
	/* 类上所有注解 */
	private Map<String, Map<String, Object>> annotations;
	/* 类对应所有方法 */
	private Map<String, RemoteMethodDto> methods;
	
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
