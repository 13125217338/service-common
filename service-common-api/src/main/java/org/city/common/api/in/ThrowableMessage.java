package org.city.common.api.in;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

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
				if (throwable instanceof InvocationTargetException) {throwable = ((InvocationTargetException)throwable).getTargetException();}
				else{throwable = ((UndeclaredThrowableException)throwable).getUndeclaredThrowable();}
			}
		}
		return throwable;
	}
}
