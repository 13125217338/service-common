package org.city.common.support.aop;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.city.common.api.dto.ExceptionDto;
import org.city.common.api.dto.ExceptionDto.CustomException;
import org.city.common.api.dto.GlobalExceptionDto;
import org.city.common.api.dto.Response;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.exception.ResponseException;
import org.city.common.api.exception.SkipException;
import org.city.common.api.in.interceptor.LanguageExceptionInterceptor;
import org.city.common.api.in.parse.ExceptionParse;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.util.ThrowableMessage;
import org.city.common.api.util.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.alibaba.fastjson.JSON;

import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 10:07:52
 * @版本 1.0
 * @描述 全局异常拦截
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionAop implements JSONParser,ThrowableMessage {
	@Autowired(required = false)
	private List<ExceptionParse<? extends Throwable>> exceptionParses;
	@Autowired(required = false)
	private LanguageExceptionInterceptor languageExceptionInterceptor;
	
	@PostConstruct
	private void init() {
		/* 自定义异常排序 */
		if (!CollectionUtils.isEmpty(exceptionParses)) {
			exceptionParses.sort(new Comparator<ExceptionParse<? extends Throwable>>() {
				@Override
				public int compare(ExceptionParse<? extends Throwable> o1, ExceptionParse<? extends Throwable> o2) {
					Class<? extends Throwable> t1 = getThrowable(o1), t2 = getThrowable(o2);
					return t2.isAssignableFrom(t1) ? 1 : (t1.isAssignableFrom(t2) ? -1 : 0);
				} //范围较大的异常放后面，先处理小异常
			});
		}
	}
	
	@ResponseStatus(code = HttpStatus.OK)
	@ExceptionHandler(value = Throwable.class)
	public Object exceptionHandler(Throwable e) {
		Response<?> response = getResponse(e);
		if (languageExceptionInterceptor != null) { //使用多语言
			Properties language = languageExceptionInterceptor.getLanguage();
			if (language != null) { //必须有多语言属性配置
				response.setMsg(language.getProperty(response.getMsg(), response.getMsg()));
			}
		}
		return response;
	}
	
	/* 获取响应 */
	@SuppressWarnings("unchecked")
	private Response<?> getResponse(Throwable e) {
		/* 获取真实异常 */
		e = getRealExcept(e);
		/* 跳过异常 */
		if (e instanceof SkipException) {
			log.error(String.format("该异常手动跳过，异常消息》》》 %s", e.toString()));
			return ((SkipException) e).getResponse();
		}
		
		/* 如果是远程异常特殊处理 */
		if (e instanceof ResponseException) {
			ResponseException responseExp = (ResponseException) e;
			RemoteIpPortDto remoteIpPort = responseExp.getRemoteIpPort();
			Response<?> response = responseExp.getResponse();
			
			/* 打印并返回 */
			log.error(String.format("%s》》》 [%s] \r\n\tat %s", "远程服务调用异常", remoteIpPort, JSON.toJSONString(response.getData())));
			return response;
		}
		
		/* 全局打印 */
		final String trackId = UUID.randomUUID().toString();
		log.error(String.format("全局捕获异常》》》 [%s]", trackId), e);
		/* 异常信息 */
		ExceptionDto exception = getException(e);
		GlobalExceptionDto globalException = new GlobalExceptionDto(SpringUtil.getAppName(), exception.getAppMsg(), trackId);
		
		/* 自定义异常 */
		if (!CollectionUtils.isEmpty(exceptionParses)) {
			try {
				ExceptionParse<Throwable> exceptionParse = null;
				for (ExceptionParse<? extends Throwable> parse : exceptionParses) {
					/* 异常从小到大匹配，匹配成功则直接赋值返回 */
					if (getThrowable(parse).isAssignableFrom(e.getClass())) {exceptionParse = (ExceptionParse<Throwable>) parse; break;}
				}
				
				if (exceptionParse != null) { //如果赋值了则直接使用这个实现类
					CustomException custom = exceptionParse.parse(e, exception.getErrorMsg());
					return new Response<>(custom.getCode(), custom.getMsg(), globalException);
				}
			/* 自定义解析出错使用默认错误 */
			} catch (Throwable e2) {log.error(String.format("自定义解析异常》》》 [%s]", trackId), e2);}
		}
		/* 默认错误 */
		return new Response<>(exception.getErrorMsg(), globalException);
	}
	/* 获取自定义异常 */
	@SuppressWarnings("unchecked")
	private Class<? extends Throwable> getThrowable(ExceptionParse<? extends Throwable> parse) {
		Type[] types = parse.getClass().getGenericInterfaces();
		for (Type type : types) {
			/* 只取泛型对应实现异常 */
			if (type instanceof ParameterizedType) {
				ParameterizedType parseType = (ParameterizedType) type;
				/* 如果是异常接口 */
				if (ExceptionParse.class == parseType.getRawType()) {
					return (Class<? extends Throwable>) parseType.getActualTypeArguments()[0];
				}
			}
		}
		throw new NullPointerException(String.format("自定义解析异常错误，[%s]未找到！", parse.getClass().getName()));
	}
}
