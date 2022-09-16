package org.city.common.core.config;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.city.common.api.util.HeaderUtil;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * @作者 ChengShi
 * @日期 2022-09-28 10:46:34
 * @版本 1.0
 * @描述 过滤拦截配置
 */
@Configuration
public class WebFilterConfig {
	
	@Bean
	public FilterRegistrationBean<Filter> myFilter() {
	    FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
	    bean.setFilter(new Filter() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				try {
					HeaderUtil.setByRequest((HttpServletRequest) request); //设置头信息
					chain.doFilter(request, response); //放行请求
				} finally {HeaderUtil.remove();} //删除头信息
			}
		});
	    bean.addUrlPatterns("/*"); //拦截所有请求
	    bean.setOrder(Ordered.HIGHEST_PRECEDENCE); //最高优先级
	    return bean;
	}
}
