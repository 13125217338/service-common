package org.city.common.api.annotation.plug;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.adapter.impl.LoopAdapter;

/**
 * @作者 ChengShi
 * @日期 2022-09-27 09:28:54
 * @版本 1.0
 * @描述 标识接口为可远程调用
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Remote {
	/**
	 * @描述 Spring注册的beanName，默认接口精简名首字母小写
	 */
	public String beanName() default "";
	/**
	 * @描述 远程调用适配实现类（实现类需交给Spring管理）
	 */
	public Class<? extends RemoteAdapter> adapter() default LoopAdapter.class;
	/**
	 * @描述 远程方法调用限流
	 */
	public int speedLimit() default -1;
}
