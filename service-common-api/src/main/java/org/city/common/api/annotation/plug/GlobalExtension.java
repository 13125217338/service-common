package org.city.common.api.annotation.plug;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2022-06-24 11:20:45
 * @版本 1.0
 * @描述 全局扩展（对应实现类必须添加@RequestMapping与@RestController注解，对应接口方法必须添加@PostMapping注解，对应接口方法参数必须添加@MutiBody注解）
 */
@Component
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GlobalExtension {
	/**
	 * @描述 唯一ID映射在哪个注解字段名上，同一个全局扩展ID只能有一个接口实现类，对应ID值不能为空（支持${key}取配置值，支持#{method}执行当前类无参方法获取String类型返回值）
	 */
	public String IdAs();
}
