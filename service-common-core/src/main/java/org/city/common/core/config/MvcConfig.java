package org.city.common.core.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.city.common.core.service.MutiBodyResolverService;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;

/**
 * @作者 ChengShi
 * @日期 2022年7月24日
 * @版本 1.0
 * @描述 自定义参数解析器
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer{
	private static final boolean jackson2XmlPresent;
	private static final boolean jackson2SmilePresent;
	private static final boolean jackson2CborPresent;
	private static final boolean jackson2Present;
	static {
		ClassLoader classLoader = WebMvcConfigurationSupport.class.getClassLoader();
		jackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
		jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
		jackson2CborPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
				ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
	}
	
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new MutiBodyResolverService());
	}
	
	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		setConverter(converters);
	}
	
	/**
	 * @描述 自定义解析器
	 * @param converters 解析器
	 */
	public void setConverter(List<HttpMessageConverter<?>> converters) {
		converters.clear();
		/* FastJson序列化配置 */
		FastJsonConfig fastJsonConfig = new FastJsonConfig();
		fastJsonConfig.setFeatures(Feature.SupportAutoType);
		fastJsonConfig.setDateFormat("yyyy-MM-dd HH:mm:ss");
		
		/* FastJson设置解析 */
		FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        converter.setSupportedMediaTypes(getTypes());
        converter.setDefaultCharset(StandardCharsets.UTF_8);
		converter.setFastJsonConfig(fastJsonConfig);
		
        /* 添加自定义Converter */
		converters.add(converter); //第一个使用FastJson
		converters.add(new FormHttpMessageConverter());
		if (jackson2CborPresent) {converters.add(new MappingJackson2CborHttpMessageConverter());}
		if (jackson2XmlPresent) {converters.add(new MappingJackson2XmlHttpMessageConverter());}
		if (jackson2SmilePresent) {converters.add(new MappingJackson2SmileHttpMessageConverter());}
		if (jackson2Present) {converters.add(new MappingJackson2HttpMessageConverter());}
    }
	/* 获取类型 */
	private List<MediaType> getTypes() {
		List<MediaType> mediaTypes = new ArrayList<>();
		/* 第一个为默认类型 */
		mediaTypes.add(MediaType.APPLICATION_JSON);
		mediaTypes.add(MediaType.APPLICATION_PDF);
		mediaTypes.add(MediaType.APPLICATION_PROBLEM_JSON);
		mediaTypes.add(MediaType.APPLICATION_STREAM_JSON);
		mediaTypes.add(MediaType.IMAGE_GIF);
		mediaTypes.add(MediaType.IMAGE_JPEG);
		mediaTypes.add(MediaType.IMAGE_PNG);
		mediaTypes.add(MediaType.MULTIPART_RELATED);
		mediaTypes.add(MediaType.TEXT_HTML);
		mediaTypes.add(MediaType.TEXT_MARKDOWN);
		return mediaTypes;
	}
}
