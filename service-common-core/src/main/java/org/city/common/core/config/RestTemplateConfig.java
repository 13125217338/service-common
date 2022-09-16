package org.city.common.core.config;

import java.util.ArrayList;
import java.util.List;

import org.city.common.api.dto.remote.RemoteConfigDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * @作者 ChengShi
 * @日期 2022-07-27 16:54:06
 * @版本 1.0
 * @描述 远程调用配置
 */
@Configuration
public class RestTemplateConfig {
	@Autowired
	private RemoteConfigDto remoteConfigDto;
	
	@Bean
    public RestTemplate restTemplate(MvcConfig mvcConfig) {
		/* 配置数据解析 */
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		mvcConfig.setConverter(messageConverters);
		RestTemplate restTemplate = new RestTemplate(messageConverters);
		/* 链接请求工厂 */
		SSLConfig sslConfig = new SSLConfig();
		sslConfig.setConnectTimeout(remoteConfigDto.getConnectTimeout());
		sslConfig.setReadTimeout(remoteConfigDto.getReadTimeout());
		restTemplate.setRequestFactory(sslConfig);
        return restTemplate;
    }
}
