package org.city.common.api.in.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import org.city.common.api.dto.ExceptionDto;

/**
 * @作者 ChengShi
 * @日期 2022-10-11 11:25:24
 * @版本 1.0
 * @描述 异常信息解析
 */
public interface ThrowableMessage {
	/**
	 * @描述 获取最内层真实异常，解决反射方法抛出的未解析异常
	 * @param throwable 待解析异常
	 * @return 解析后的异常
	 */
	default Throwable getRealExcept(Throwable throwable){
		if (throwable != null) {
			while(throwable instanceof InvocationTargetException || throwable instanceof UndeclaredThrowableException){
				if (throwable instanceof InvocationTargetException) {throwable = ((InvocationTargetException) throwable).getTargetException();}
				else{throwable = ((UndeclaredThrowableException) throwable).getUndeclaredThrowable();}
			}
		}
		return throwable;
	}
	
	/**
	 * @描述 获取简短异常信息
	 * @param throwable 待解析异常
	 * @return 简短异常信息
	 */
	default ExceptionDto getException(Throwable throwable) {
		/* 定义信息 */
		String msg = throwable.toString(), simple = null;
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
