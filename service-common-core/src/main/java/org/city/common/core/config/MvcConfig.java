package org.city.common.core.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;

import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2022年7月24日
 * @版本 1.0
 * @描述 自定义参数解析器
 */
@Slf4j
@Configuration
public class MvcConfig implements WebMvcConfigurer{
	@Value("${spring.datasource.url:jdbc?serverTimezone=UTC}")
	private String timeZone;
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
		setFastJsonTimezone();
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
		converters.add(new ByteArrayHttpMessageConverter());
		converters.add(new StringHttpMessageConverter());
		converters.add(new ResourceHttpMessageConverter());
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
		mediaTypes.add(MediaType.APPLICATION_NDJSON);
		mediaTypes.add(MediaType.IMAGE_GIF);
		mediaTypes.add(MediaType.IMAGE_JPEG);
		mediaTypes.add(MediaType.IMAGE_PNG);
		mediaTypes.add(MediaType.MULTIPART_RELATED);
		mediaTypes.add(MediaType.TEXT_HTML);
		mediaTypes.add(MediaType.TEXT_MARKDOWN);
		return mediaTypes;
	}
	/* 设置FastJson时区 */
	private void setFastJsonTimezone() {
		String timezoneId = TimeZone.getDefault().getID();
		try {
			String[] params = timeZone.split("[?]")[1].split("[&]");
			for (String param : params) {
				if (param.contains("serverTimezone")) {
					timezoneId = param.split("=")[1].trim(); break;
				}
			}
		} catch (Exception e) {log.error("自定义设置FastJson时区失败！", e);}
		
		/* 全局配置FastJson */
		JSONObject.defaultTimeZone = TimeZone.getTimeZone(timezoneId);
		ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
	}
}
