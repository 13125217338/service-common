package org.city.common.api.util;

import org.city.common.api.in.parse.FirstCharParse;
import org.springframework.context.ApplicationContext;

/**
 * @作者 ChengShi
 * @日期 2022-09-16 15:18:11
 * @版本 1.0
 * @描述 Spring工具
 */
public final class SpringUtil implements FirstCharParse{
	private static ApplicationContext applicationContext;
	private static String appName;
	private final static SpringUtil SPRING_UTIL = new SpringUtil();
	private SpringUtil() {}
	
	/**
	 * @描述 初始化工具
	 * @param applicationContext 应用上下文
	 */
	static void init(ApplicationContext applicationContext) {
		SpringUtil.applicationContext = applicationContext;
		SpringUtil.appName = applicationContext.getEnvironment().getProperty("spring.application.name");
	}
	
	/**
	 * @描述 获取应用名称
	 * @return 应用名称
	 */
	public static String getAppName() {return SpringUtil.appName;}

    /**
     * @描述 获取applicationContext
     * @return applicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
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
     * @描述 通过class获取Bean
     * @param <T> 对应对象
     * @param cls 对应类（优先精简类名查找）
     * @return 对应Bean
     */
    public static <T> T getBean(Class<T> cls){
    	String lowerName = SPRING_UTIL.parseLower(cls);
    	return applicationContext.containsBean(lowerName) ?
    		   applicationContext.getBean(lowerName, cls) :
    		   applicationContext.getBean(cls);
    }
}
