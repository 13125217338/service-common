package org.city.common.core.listener;

import org.city.common.api.in.parse.FirstCharParse;
import org.city.common.api.util.SpringUtil;
import org.city.common.core.config.RemoteConfig;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @作者 ChengShi
 * @日期 2023年4月20日
 * @版本 1.0
 * @描述 自定义配置监听
 */
public class ConfigListener implements SpringApplicationRunListener,FirstCharParse {
	public ConfigListener(SpringApplication application, String[] args) {}
	
	@Override
	public void contextLoaded(ConfigurableApplicationContext context) {
		if (context instanceof ServletWebServerApplicationContext) {
			ServletWebServerApplicationContext webContext = (ServletWebServerApplicationContext) context;
			SpringUtil.init(webContext); //初始化工具
			webContext.registerBeanDefinition(parseLower(RemoteConfig.class),new RootBeanDefinition(RemoteConfig.class)); //注册远程配置
		}
	}
}
