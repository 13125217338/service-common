package org.city.common.support.aop;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;

import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.ExceptionDto;
import org.city.common.api.dto.GlobalExceptionDto;
import org.city.common.api.dto.Response;
import org.city.common.api.dto.ExceptionDto.CustomException;
import org.city.common.api.exception.ResponseException;
import org.city.common.api.in.parse.ExceptionParse;
import org.city.common.api.in.parse.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 10:07:52
 * @版本 1.0
 * @描述 全局异常拦截
 */
@Slf4j
@RestControllerAdvice
@DependsOn(CommonConstant.PLUG_UTIL_NAME)
public class GlobalExceptionAop implements JSONParser{
	@Value("${spring.application.name}")
	private String appName;
	@Autowired(required = false)
	private List<ExceptionParse<? extends Throwable>> exceptionParses;
	
	@SuppressWarnings("unchecked")
	@ResponseStatus(code = HttpStatus.OK)
	@ExceptionHandler(value = Throwable.class)
	public Object exceptionHandler(Throwable e) {
		/* 唯一通道ID */
		final String trackId = UUID.randomUUID().toString();
		
		/* 如果是Mas异常特殊处理 */
		if (e instanceof ResponseException) {
			ResponseException mase = (ResponseException) e;
			if (mase.getRemote() == null) {
				/* 全局异常打印 */
				log.error(String.format("全局捕获异常》》》 [%s]", trackId), e);
				return mase.getResponse();
			} else {
				GlobalExceptionDto parse = mase.getRemote();
				/*打印并返回*/
				log.error(String.format("%s》》》 [%s] \r\n\tat %s", "远程服务调用异常", parse.getTrackId(), parse.getAppErroMsg()));
				return Response.error(parse, mase.getResponse().getMsg());
			}
		/* 全局异常打印 */
		} else {log.error(String.format("全局捕获异常》》》 [%s]", trackId), e);}
		
		/*异常信息*/
		ExceptionDto exception = getException(e);
		GlobalExceptionDto globalExceptionDto = new GlobalExceptionDto(appName, exception.getAppMsg(), trackId);
		
		/*自定义异常*/
		if (!CollectionUtils.isEmpty(exceptionParses)) {
			try {
				ExceptionParse<Throwable> exceptionParse = null;
				exp:
				for (ExceptionParse<? extends Throwable> parse : exceptionParses) {
					Type[] types = parse.getClass().getGenericInterfaces();
					for (Type type : types) {
						/* 只取泛型对应实现异常 */
						if (type instanceof ParameterizedType) {
							ParameterizedType parseType = (ParameterizedType) type;
							
							/* 如果是异常接口且是异常类或子类 */
							if (ExceptionParse.class == parseType.getRawType() &&
									((Class<?>) parseType.getActualTypeArguments()[0]).isAssignableFrom(e.getClass())) {
								exceptionParse = (ExceptionParse<Throwable>) parse;
								/* 如果赋值了则使用这个实现类，多个匹配只会进入一个实现者 */
								break exp;
							}
						}
					}
				}
				
				if (exceptionParse != null) {
					CustomException custom = exceptionParse.parse(e, exception.getErrorMsg());
					return Response.error(custom.getCode(), globalExceptionDto, custom.getMsg());
				}
			/* 自定义解析出错使用默认错误 */
			} catch (Throwable e2) {log.error(String.format("自定义解析异常》》》 [%s]", trackId), e2);}
		}
		/* 默认错误 */
		return Response.error(globalExceptionDto, exception.getErrorMsg());
	}
	/* 获取异常信息 */
	private ExceptionDto getException(Throwable e) {
		/* 定义信息 */
		String msg = e.toString(), simple = null;
		/* 如果包含该异常 - 数据库操作异常不展示给前端 */
		if (msg.contains("org.springframework.dao.")) {
			String daoMsg = "数据库操作异常！";
			return new ExceptionDto("org.springframework.dao.*: " + daoMsg, daoMsg);
		}
		
		/* 定义下标 - 只取一行 */
		int hh = msg.indexOf("\n"), mh = msg.indexOf(":") + 1;
		/* 截取信息 */
		if (hh > 0) {msg = msg.substring(0, hh).trim();}
		simple = mh > 0 ? msg.substring(mh).trim() : msg;
		/* 精简信息过大时限制大小并使用“...”做后缀 */
		if (simple.length() > Byte.MAX_VALUE) {simple = simple.substring(0, Byte.MAX_VALUE) + "...";}
		
		/* 生成的异常 */
		return new ExceptionDto(msg, simple);
	}
}
