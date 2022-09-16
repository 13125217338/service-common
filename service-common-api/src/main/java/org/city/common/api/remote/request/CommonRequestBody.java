package org.city.common.api.remote.request;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import org.city.common.api.in.remote.RemoteUrlRequestBody;
import org.city.common.api.util.FormatUtil;
import org.city.common.api.util.JsonUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;

/**
 * @作者 ChengShi
 * @日期 2023-04-07 18:12:03
 * @版本 1.0
 * @描述 公共请求体
 */
@Component
public class CommonRequestBody implements RemoteUrlRequestBody {
	
	@Override
	public Object getBody(Method method, HttpHeaders requestHeaders, Object[] args, Map<String, Object> uriVariables) {
		if (args.length == 0) {return null;} //无参数
		
		Parameter[] parameters = method.getParameters();
		for (int i = 0, j = args.length; i < j; i++) {
			if (parameters[i].isAnnotationPresent(PathVariable.class)) {continue;} //不处理地址参数
			if (parameters[i].isAnnotationPresent(RequestBody.class)) {return args[i];} //JSON体参数
			if (parameters[i].isAnnotationPresent(RequestPart.class)) {return handlerPart(parameters[i], requestHeaders, args[i]);} //多媒体参数
			if (parameters[i].isAnnotationPresent(RequestParam.class)) {return handlerParam(parameters[i], requestHeaders, args[i]);} //UrlEncoded参数
			return handlerText(requestHeaders, args[i]); //文本参数
		}
		
		return null; //默认空
	}
	
	/* 处理文本数据 */
	private String handlerText(HttpHeaders requestHeaders, Object arg) {
		requestHeaders.setContentType(MediaType.TEXT_PLAIN);
		return JsonUtil.toJSONString(arg);
	}
	/* 处理UrlEncoded数据 */
	private MultiValueMap<String, Object> handlerParam(Parameter parameter, HttpHeaders requestHeaders, Object arg) {
		RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
		requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		/* 指定名称单个返回 - 否则对象多个返回 */
		if (StringUtils.hasText(requestParam.value())) {
			MultiValueMap<String, Object> content = new LinkedMultiValueMap<String, Object>();
			content.add(requestParam.value(), arg);
			return content;
		} else {return FormatUtil.getMutiValueMap(arg);}
	}
	/* 处理多媒体数据 */
	private MultiValueMap<String, Object> handlerPart(Parameter parameter, HttpHeaders requestHeaders, Object arg) {
		RequestPart requestPart = parameter.getAnnotation(RequestPart.class);
		requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
		/* 指定名称单个返回 - 否则对象多个返回 */
		if (StringUtils.hasText(requestPart.value())) {
			MultiValueMap<String, Object> content = new LinkedMultiValueMap<String, Object>();
			content.add(requestPart.value(), arg);
			return content;
		} else {return FormatUtil.getMutiValueMap(arg);}
	}
}
