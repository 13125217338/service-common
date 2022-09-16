package org.city.common.core.service;

import java.io.BufferedReader;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.city.common.api.annotation.request.MutiBody;
import org.city.common.api.in.parse.JSONParser;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.alibaba.fastjson.JSONObject;

/**
 * @作者 ChengShi
 * @日期 2022年7月24日
 * @版本 1.0
 * @描述 多参数自定义解析（使用FastJson解析）
 */
public class MutiBodyResolverService implements HandlerMethodArgumentResolver,JSONParser{
	/* 记录Body */
	private final ThreadLocal<JSONObject> body = new ThreadLocal<>();
	@Override
	public boolean supportsParameter(MethodParameter parameter) {return parameter.hasParameterAnnotation(MutiBody.class);}
	
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		if (parameter.getParameterName() == null) {
			throw new NullPointerException("方法入参添加了@MutiBody注解，但获取参数名称为NULL！");
		}
		/* 自定义解析 */
		return parse(jsonBody(webRequest).get(parameter.getParameterName()), parameter.getGenericParameterType());
	}

	/* 参数体必须是一个JSON对象 */
	private JSONObject jsonBody(NativeWebRequest webRequest) throws Exception {
		HttpServletRequest httpServletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		if (!httpServletRequest.getContentType().toLowerCase().trim().startsWith("application/json")) {
			throw new ServletRequestBindingException("该请求非'application/json'类型，请勿使用@MutiBody注解！");
		}
		
		/* 获取流读取内容 */
		BufferedReader reader = httpServletRequest.getReader();
		if (reader.ready()) {
			String jsonStr = IOUtils.toString(reader);
			Object parse = JSONObject.parse(jsonStr);
			if (!(parse instanceof JSONObject)) {
				throw new IllegalArgumentException("原参数体非JSONObject对象》》》" + jsonStr);
			}
			body.set((JSONObject) parse);
		}
		return body.get();
	}
}
