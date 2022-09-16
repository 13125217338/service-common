package org.city.common.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

/**
 * @作者 ChengShi
 * @日期 2023-09-26 20:13:45
 * @版本 1.0
 * @描述 分页拦截配置
 */
@Configuration
public class MybatisPlusConfig {
	
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
    	MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
    	mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return mybatisPlusInterceptor;
    }
}
