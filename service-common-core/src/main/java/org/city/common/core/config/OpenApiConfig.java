package org.city.common.core.config;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

/**
 * @作者 ChengShi
 * @日期 2023-09-09 13:57:18
 * @版本 1.0
 * @描述 开放Api配置
 */
@Configuration
public class OpenApiConfig {
	@Value("${knife4j.email:}")
	private String email;
	@Value("${knife4j.name:}")
	private String name;
	@Value("${knife4j.version:}")
	private String version;
	@Value("${knife4j.title:接口文档}")
	private String title;
	
	@Bean
	public OpenAPI openAPI() {
		OpenAPI openAPI = new OpenAPI();
		Contact contact = new Contact().email(email).name(name);
		String version = StringUtils.hasText(this.version) ? this.version : openAPI.getOpenapi();
		return openAPI.info(new Info().title(title).description("系统管理后台 - 接口文档").version(version).contact(contact));
	}
	
	@Bean
	public GroupedOpenApi groupedOpenApi() {
		return GroupedOpenApi.builder().group("默认分组").pathsToMatch("/**").build();
	}
}
