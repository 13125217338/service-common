package org.city.common.api.util;

import java.util.ArrayList;
import java.util.List;

import org.city.common.api.in.parse.FirstCharParse;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * @作者 ChengShi
 * @日期 2022-09-16 15:18:11
 * @版本 1.0
 * @描述 Spring工具
 */
public final class SpringUtil {
	private final static FirstCharParse FIRST_CHAR_PARSE = new FirstCharParse() {};
	private static ApplicationContext applicationContext;
	private static String appName;
	private SpringUtil() {}
	
	/**
	 * @描述 初始化工具（只能执行一次）
	 * @param applicationContext 应用上下文
	 */
	public synchronized static void init(ApplicationContext applicationContext) {
		if (SpringUtil.applicationContext == null) {
			SpringUtil.applicationContext = applicationContext;
			SpringUtil.appName = getEnvironment().getProperty("spring.application.name");
		}
	}
	
	/**
	 * @描述 获取应用名称
	 * @return 应用名称
	 */
	public static String getAppName() {
		return SpringUtil.appName;
	}
	
    /**
     * @描述 获取应用上下文
     * @return 应用上下文
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
    
    /**
     * @描述 获取环境信息
     * @return 环境信息
     */
    public static Environment getEnvironment() {
    	return applicationContext.getEnvironment();
    }
    
    /**
     * @描述 通过name获取Bean
     * @param name 名称
     * @return Bean
     */
    public static Object getBean(String name){
        return applicationContext.getBean(name);
    }
    
    /**
     * @描述 通过名称加类型获取
     * @param <T> 对应对象
     * @param name 名称
     * @param cls 类型
     * @return Bean
     */
    public static <T> T getBean(String name, Class<T> cls) {
    	return applicationContext.getBean(name, cls);
    }
    
    /**
     * @描述 通过class获取Bean
     * @param <T> 对应对象
     * @param cls 对应类（优先精简类名查找）
     * @return 对应Bean
     */
    public static <T> T getBean(Class<T> cls){
    	String lowerName = FIRST_CHAR_PARSE.parseLower(cls);
    	return applicationContext.containsBean(lowerName) ? getBean(lowerName, cls) : applicationContext.getBean(cls);
    }
    
    /**
     * @描述 通过class获取多个实现Bean
     * @param <T> 对应对象
     * @param cls 类型
     * @return 多个实现Bean
     */
    public static <T> List<T> getBeans(Class<T> cls) {
    	List<T> beans = new ArrayList<>();
    	for (String beanName : getBeanNames(cls)) {
			beans.add(getBean(beanName, cls));
		}
    	return beans;
    }
    
    /**
     * @描述 通过类型获取多个实现Bean名称
     * @param cls 类型
     * @return 多个实现Bean名称
     */
    public static String[] getBeanNames(Class<?> cls) {
    	return applicationContext.getBeanNamesForType(cls);
    }
    
    /**
     * @描述 只获取一个BeanName
     * @param cls 类型
     * @return 一个BeanName
     */
    public static String getBeanName(Class<?> cls) {
    	String[] beanNames = getBeanNames(cls);
    	if (beanNames == null || beanNames.length == 0) {
			throw new NullPointerException(String.format("通过[%s]类并未找到名称！", cls.getName()));
		}
    	if (beanNames.length > 1) {
			throw new IndexOutOfBoundsException(String.format("通过[%s]类找到多个[%d]名称！", cls.getName(), beanNames.length));
		}
    	return beanNames[0];
    }
}
