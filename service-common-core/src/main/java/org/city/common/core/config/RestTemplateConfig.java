package org.city.common.core.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @作者 ChengShi
 * @日期 2022-07-27 16:54:06
 * @版本 1.0
 * @描述 远程调用配置
 */
@Configuration
public class RestTemplateConfig {
	@Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, MvcConfig mvcConfig) {
		RestTemplate restTemplate = builder.build();
		/* 配置数据解析 */
        mvcConfig.setConverter(restTemplate.getMessageConverters());
        return restTemplate;
    }
}
