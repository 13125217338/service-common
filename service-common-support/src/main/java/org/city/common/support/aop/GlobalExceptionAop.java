package org.city.common.support.aop;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.city.common.api.dto.ExceptionDto;
import org.city.common.api.dto.ExceptionDto.CustomException;
import org.city.common.api.dto.GlobalExceptionDto;
import org.city.common.api.dto.Response;
import org.city.common.api.exception.ResponseException;
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
	
	@SuppressWarnings("unchecked")
	@ResponseStatus(code = HttpStatus.OK)
	@ExceptionHandler(value = Throwable.class)
	public Object exceptionHandler(Throwable e) {
		e = getRealExcept(e); //获取真实异常
		final String trackId = UUID.randomUUID().toString(); //唯一通道ID
		
		try {
			/* 如果是远程异常特殊处理 */
			if (e instanceof ResponseException) {
				ResponseException responseExp = (ResponseException) e;
				if (responseExp.getRemote() == null) {return responseExp.getResponse();}
				else {
					GlobalExceptionDto remoteExp = responseExp.getRemote();
					/* 打印并返回 */
					log.error(String.format("%s》》》 [%s][%s] \r\n\tat %s", "远程服务调用异常", remoteExp.getAppName(), remoteExp.getTrackId(), remoteExp.getAppErroMsg()));
					return Response.error(responseExp.getResponse().getCode(), remoteExp, responseExp.getResponse().getMsg());
				}
			}
		/* 全局异常打印 */
		} finally {log.error(String.format("全局捕获异常》》》 [%s]", trackId), e);}
		
		/* 异常信息 */
		ExceptionDto exception = getException(e);
		GlobalExceptionDto globalExceptionDto = new GlobalExceptionDto(SpringUtil.getAppName(), exception.getAppMsg(), trackId);
		
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
					return Response.error(custom.getCode(), globalExceptionDto, custom.getMsg());
				}
			/* 自定义解析出错使用默认错误 */
			} catch (Throwable e2) {log.error(String.format("自定义解析异常》》》 [%s]", trackId), e2);}
		}
		/* 默认错误 */
		return Response.error(globalExceptionDto, exception.getErrorMsg());
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
	/* 获取异常信息 */
	private ExceptionDto getException(Throwable e) {
		/* 定义信息 */
		String msg = e.toString(), simple = null;
		/* 如果包含该异常 - 数据库操作异常不展示给前端 */
		if (msg.contains("org.springframework.dao.")) {
			String daoMsg = "数据库操作异常！";
			return new ExceptionDto("org.springframework.dao.*: " + daoMsg, daoMsg);
		}
		/* 如果包含该异常 - 数据库操作异常不展示给前端 */
		if (msg.contains("org.springframework.jdbc.")) {
			String daoMsg = "数据库操作异常！";
			return new ExceptionDto("org.springframework.jdbc.*: " + daoMsg, daoMsg);
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
