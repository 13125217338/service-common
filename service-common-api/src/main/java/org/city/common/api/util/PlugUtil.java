package org.city.common.api.util;

import java.lang.annotation.Annotation;
import java.util.List;

import org.city.common.api.adapter.ExtensionIOAdapter;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.MethodCacheDto;
import org.city.common.api.in.GlobalConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2022-06-24 12:48:59
 * @版本 1.0
 * @描述 插件工具（只能获取添加@GlobalExtension注解的实现类）
 */
@Component(CommonConstant.PLUG_UTIL_NAME)
public final class PlugUtil{
	/*扩展点适配*/
	private static ExtensionIOAdapter extensionIOAdapter;
	@Autowired
	private void init(GlobalConfig globalConfig, ApplicationContext applicationContext) throws Exception {
		PlugUtil.extensionIOAdapter = globalConfig.init(applicationContext);
	}
	
	/**
	 * @描述 获取存储的Bean
	 * @param id 全局扩展ID
	 * @param proxyInterface 需要获取的代理接口
	 * @return 对应Bean
	 */
	public static <T> T getBean(String id, Class<T> proxyInterface) {
		return extensionIOAdapter.getBean(id, proxyInterface);
	}
	
	/**
	 * @描述 获取存储的集合Bean
	 * @param proxyInterface 需要获取的代理接口
	 * @return 对应Bean集合（实现类返回不会为NULL）
	 */
	public static <T> List<T> getAllBean(Class<T> proxyInterface) {
		return extensionIOAdapter.getAllBean(proxyInterface);
	}
	
	/**
	 * @描述 获取对应注解
	 * @param id 全局扩展点ID
	 * @param proxyInterface 需要获取的代理接口
	 * @param annotationClass 需要获取的注解
	 * @return 对应注解
	 */
	public static <A extends Annotation> A getValue(String id, Class<?> proxyInterface, Class<A> annotationClass) {
		return extensionIOAdapter.getValue(id, proxyInterface, annotationClass);
	}
	
	/**
	 * @描述 获取对应注解集合
	 * @param proxyInterface 需要获取的代理接口
	 * @param annotationClass 需要获取的注解
	 * @return 对应注解集合
	 */
	public static <A extends Annotation> List<A> getAllValue(Class<?> proxyInterface, Class<A> annotationClass) {
		return extensionIOAdapter.getAllValue(proxyInterface, annotationClass);
	}
	
	/**
	 * @描述 通过ID获取缓存初始方法信息
	 * @param id 缓存ID
	 * @return 缓存初始方法信息
	 */
	public static MethodCacheDto getInitMethod(String id) {
		return extensionIOAdapter.getInitMethod(id);
	}
}
