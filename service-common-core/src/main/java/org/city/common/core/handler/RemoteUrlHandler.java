package org.city.common.core.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.groups.Default;

import org.city.common.api.annotation.plug.RemoteUrl;
import org.city.common.api.exception.RemoteSpeedLimitException;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.remote.RemoteUrlHeaders;
import org.city.common.api.in.remote.RemoteUrlRequestBody;
import org.city.common.api.in.remote.RemoteUrlResponseBody;
import org.city.common.api.in.util.Replace;
import org.city.common.api.in.util.Validations;
import org.city.common.api.util.JsonUtil;
import org.city.common.api.util.SpeedLimitUtil;
import org.city.common.api.util.SpringUtil;
import org.city.common.core.config.MvcConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2023-04-06 17:15:08
 * @版本 1.0
 * @描述 远程Url调用处理
 */
@Slf4j
public class RemoteUrlHandler implements InvocationHandler,JSONParser,Replace,Validations {
	private final RestTemplate restTemplate = new RestTemplate(SpringUtil.getBean(MvcConfig.class).setConverter(new ArrayList<>()));
	private final String url;
	private final RemoteUrl remoteUrl;
	public RemoteUrlHandler(Class<?> orginClass, RemoteUrl remoteUrl) {
		this.remoteUrl = remoteUrl;
		this.url = replaceConfig(orginClass, SpringUtil.getEnvironment(), remoteUrl.url());
		
		/* 注解自定义错误处理 */
		if (remoteUrl.response() != ResponseErrorHandler.class) {
			restTemplate.setErrorHandler(SpringUtil.getBean(remoteUrl.response()));
		}
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return SpeedLimitUtil.limitExec(remoteUrl.speedLimit(), method, () -> new RemoteSpeedLimitException("远程服务限流！"), () -> invoke(method, args == null ? new Object[0] : args));
	}
	/* 执行远程调用 */
	private Object invoke(Method method, Object[] args) {
		verifys(method.getParameters(), args); //验证参数
		Map<String, Annotation> mapping = Arrays.asList(method.getDeclaredAnnotations()).stream().collect(Collectors.toMap(k -> k.annotationType().getName(), v -> v));
		String logFormat = String.format("远程方法[%s.%s()]开始调用，{}请求地址》》》 {}", method.getDeclaringClass().getSimpleName(), method.getName());
		Map<String, Object> uriVariables = getUriVariables(args, method.getParameters()); //地址参数
		
		/* 打印地址参数 */
		if (uriVariables.size() > 0) {
			String logFormatToPath = String.format("远程方法[%s.%s()]对应地址参数》》》 {}", method.getDeclaringClass().getSimpleName(), method.getName());
			log.info(logFormatToPath, JSON.toJSONString(uriVariables));
		}
		
		/* 不同映射不同处理 */
		if (mapping.containsKey(PostMapping.class.getName())) { //POST调用
			PostMapping postMapping = (PostMapping) mapping.get(PostMapping.class.getName());
			String reqPath = url + (postMapping.value().length == 0 ? "" : postMapping.value()[0]);
			log.info(logFormat, HttpMethod.POST.name(), reqPath); //打印请求日志 - POST
			ResponseEntity<String> response = restTemplate.exchange(reqPath, HttpMethod.POST, getHttpEntity(method, uriVariables, postMapping.headers(), args), String.class, uriVariables);
			return getResult(method, response.getHeaders(), response.getBody()); //获取结果 - POST
		
		} else if (mapping.containsKey(GetMapping.class.getName())) { //GET调用
			GetMapping getMapping = (GetMapping) mapping.get(GetMapping.class.getName());
			String reqPath = url + (getMapping.value().length == 0 ? "" : getMapping.value()[0]);
			log.info(logFormat, HttpMethod.GET.name(), reqPath); //打印请求日志 - GET
			ResponseEntity<String> response = restTemplate.exchange(reqPath, HttpMethod.GET, getHttpEntity(method, uriVariables, getMapping.headers(), args), String.class, uriVariables);
			return getResult(method, response.getHeaders(), response.getBody()); //获取结果 - GET
		
		} else if (mapping.containsKey(DeleteMapping.class.getName())) { //DELETE调用
			DeleteMapping deleteMapping = (DeleteMapping) mapping.get(DeleteMapping.class.getName());
			String reqPath = url + (deleteMapping.value().length == 0 ? "" : deleteMapping.value()[0]);
			log.info(logFormat, HttpMethod.DELETE.name(), reqPath); //打印请求日志 - DELETE
			ResponseEntity<String> response = restTemplate.exchange(reqPath, HttpMethod.DELETE, getHttpEntity(method, uriVariables, deleteMapping.headers(), args), String.class, uriVariables);
			return getResult(method, response.getHeaders(), response.getBody()); //获取结果 - DELETE
		
		} else if (mapping.containsKey(PutMapping.class.getName())) { //PUT调用
			PutMapping putMapping = (PutMapping) mapping.get(PutMapping.class.getName());
			String reqPath = url + (putMapping.value().length == 0 ? "" : putMapping.value()[0]);
			log.info(logFormat, HttpMethod.PUT.name(), reqPath); //打印请求日志 - PUT
			ResponseEntity<String> response = restTemplate.exchange(reqPath, HttpMethod.PUT, getHttpEntity(method, uriVariables, putMapping.headers(), args), String.class, uriVariables);
			return getResult(method, response.getHeaders(), response.getBody()); //获取结果 - PUT
		
		} else if (method.getName().equals("toString")) {
			return method.getDeclaringClass().getName();
		}
		throw new RuntimeException(String.format("远程方法[%s.%s()]没有映射注解！", method.getDeclaringClass().getSimpleName(), method.getName()));
	}
	/* 获取地址变量 */
	private Map<String, Object> getUriVariables(Object[] args, Parameter[] parameters) {
		Map<String, Object> uriVariables = new HashMap<String, Object>();
		for (int i = 0, j = args.length; i < j; i++) {
			PathVariable pathVariable = parameters[i].getDeclaredAnnotation(PathVariable.class);
			if (pathVariable == null) {continue;} //不处理地址参数
			
			/* 有值作为单个值 - 否则作为对象多个值 */
			if (StringUtils.hasText(pathVariable.value())) {
				uriVariables.put(pathVariable.value(), args[i]);
			} else {
				Object argJson = JSON.toJSON(args[i]);
				if (argJson instanceof JSONObject) {
					uriVariables.putAll((JSONObject) argJson);
				}
			}
		}
		return uriVariables;
	}
	/* 获取响应结果 */
	private Object getResult(Method method, HttpHeaders responseHeaders, String body) {
		/* 打印响应结果 */
		String logFormatToResponse = String.format("远程方法[%s.%s()]对应响应结果》》》 {}", method.getDeclaringClass().getSimpleName(), method.getName());
		log.info(logFormatToResponse, body);
		
		if (remoteUrl.responseBody() == RemoteUrlResponseBody.class) {return parse(body, method.getGenericReturnType());}
		else {return getRemoteUrlResponseBody().handler(method, responseHeaders, body);}
	}
	/* 获取请求实体类 */
	private HttpEntity<Object> getHttpEntity(Method method, Map<String, Object> uriVariables, String[] headers, Object[] args) {
		HttpHeaders header = new HttpHeaders();
		header.setContentType(MediaType.APPLICATION_JSON); //默认JSON体
		Object body = null;
		
		/* 参数头信息 */
		for (String hd : headers) {
			String[] hds = hd.split("=");
			if (hds.length > 1) {header.set(hds[0].trim(), hds[1].trim());}
		}
		
		/* 注解自定义头信息 */
		if (remoteUrl.commonHeaders() != RemoteUrlHeaders.class) {
			header.addAll(getRemoteUrlHeaders().getHeaders(method, uriVariables));
		}
		
		/* 注解自定义请求体 */
		if (remoteUrl.requestBody() == RemoteUrlRequestBody.class) {
			body = args.length > 0 ? args[0] : null;
		} else {
			body = getRemoteUrlRequestBody().getBody(method, header, args, uriVariables);
		}
		
		/* 打印请求参数并返回 */
		String logFormatToBody = String.format("远程方法[%s.%s()]对应请求参数》》》 {}", method.getDeclaringClass().getSimpleName(), method.getName());
		log.info(logFormatToBody, JsonUtil.toJSONString(body));
		return new HttpEntity<Object>(body, header);
	}
	/* 验证参数 */
	private void verifys(Parameter[] parameters, Object[] args) {
		for (int i = 0, j = parameters.length; i < j; i++) {
			Validated validated = parameters[i].getAnnotation(Validated.class);
			if (validated != null) {
				verify(args[i], validated.value().length == 0 ? new Class[] {Default.class} : validated.value());
			}
		}
	}
	
	/* 用于懒加载处理 */
	private RemoteUrlHeaders remoteUrlHeaders; //注解自定义请求头
	private RemoteUrlRequestBody remoteUrlRequestBody; //注解自定义请求体
	private RemoteUrlResponseBody remoteUrlResponseBody; //注解自定义响应体
	
	/* 获取自定义请求头实例 - 懒加载 */
	private RemoteUrlHeaders getRemoteUrlHeaders() {
		if (remoteUrlHeaders == null) {
			synchronized (remoteUrl.commonHeaders()) {
				if (remoteUrlHeaders == null) {
					remoteUrlHeaders = SpringUtil.getBean(remoteUrl.commonHeaders());
				}
			}
		}
		return remoteUrlHeaders;
	}
	/* 获取自定义请求体实例 - 懒加载 */
	private RemoteUrlRequestBody getRemoteUrlRequestBody() {
		if (remoteUrlRequestBody == null) {
			synchronized (remoteUrl.requestBody()) {
				if (remoteUrlRequestBody == null) {
					remoteUrlRequestBody = SpringUtil.getBean(remoteUrl.requestBody());
				}
			}
		}
		return remoteUrlRequestBody;
	}
	/* 获取自定义响应体实例 - 懒加载 */
	private RemoteUrlResponseBody getRemoteUrlResponseBody() {
		if (remoteUrlResponseBody == null) {
			synchronized (remoteUrl.responseBody()) {
				if (remoteUrlResponseBody == null) {
					remoteUrlResponseBody = SpringUtil.getBean(remoteUrl.responseBody());
				}
			}
		}
		return remoteUrlResponseBody;
	}
}
