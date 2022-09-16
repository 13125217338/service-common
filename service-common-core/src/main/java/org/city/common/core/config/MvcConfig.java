package org.city.common.core.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.util.SpringUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;

/**
 * @作者 ChengShi
 * @日期 2022年7月24日
 * @版本 1.0
 * @描述 自定义参数解析器
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
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
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		setConverter(converters);
	}
	
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedHeaders("*")
				.allowedMethods("*")
				.allowedOrigins("*");
	}
	
	/**
	 * @描述 自定义解析器
	 * @param converters 解析器
	 * @return 解析器
	 */
	public List<HttpMessageConverter<?>> setConverter(List<HttpMessageConverter<?>> converters) {
		converters.clear();
		/* FastJson设置解析 */
		FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        converter.setSupportedMediaTypes(getTypes());
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        
		/* 验证是否返回空字符 */
        RemoteConfigDto remoteConfigDto = SpringUtil.getBean(RemoteConfigDto.class);
        if (remoteConfigDto.isWriteNull()) {converter.getFastJsonConfig().setSerializerFeatures(SerializerFeature.WriteMapNullValue);}
		/* 验证是否Long转String */
        if (remoteConfigDto.isLongToString()) {
        	SerializeConfig serializeConfig = converter.getFastJsonConfig().getSerializeConfig();
        	serializeConfig.put(Long.class, ToStringSerializer.instance);
        	serializeConfig.put(Long.TYPE, ToStringSerializer.instance);
        }
        /* 验证是否Double转String */
        if (remoteConfigDto.isDoubleToString()) {
        	SerializeConfig serializeConfig = converter.getFastJsonConfig().getSerializeConfig();
        	serializeConfig.put(Double.class, ToStringSerializer.instance);
        	serializeConfig.put(Double.TYPE, ToStringSerializer.instance);
        }
		
        /* 添加自定义Converter */
        converters.add(new FormHttpMessageConverter());
        converters.add(new ByteArrayHttpMessageConverter());
        converters.add(new ResourceHttpMessageConverter());
		converters.add(converter); //使用FastJson消息解析
		if (jackson2CborPresent) {converters.add(new MappingJackson2CborHttpMessageConverter());}
		if (jackson2XmlPresent) {converters.add(new MappingJackson2XmlHttpMessageConverter());}
		if (jackson2SmilePresent) {converters.add(new MappingJackson2SmileHttpMessageConverter());}
		if (jackson2Present) {converters.add(new MappingJackson2HttpMessageConverter());}
		return converters;
    }
	/* 获取类型 */
	private List<MediaType> getTypes() {
		List<MediaType> mediaTypes = new ArrayList<>();
		/* 第一个为默认类型 */
		mediaTypes.add(MediaType.APPLICATION_JSON);
		mediaTypes.add(MediaType.APPLICATION_PDF);
		mediaTypes.add(MediaType.APPLICATION_PROBLEM_JSON);
		mediaTypes.add(MediaType.APPLICATION_NDJSON);
		mediaTypes.add(MediaType.IMAGE_GIF);
		mediaTypes.add(MediaType.IMAGE_JPEG);
		mediaTypes.add(MediaType.IMAGE_PNG);
		mediaTypes.add(MediaType.MULTIPART_RELATED);
		mediaTypes.add(MediaType.TEXT_HTML);
		mediaTypes.add(MediaType.TEXT_PLAIN);
		mediaTypes.add(MediaType.TEXT_MARKDOWN);
		return mediaTypes;
	}
}
