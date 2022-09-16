package org.city.common.support.aop;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.city.common.api.annotation.sql.MyDataSource;
import org.city.common.api.constant.CommonConstant;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2023年6月20日
 * @版本 1.0
 * @描述 自定义数据源拦截
 */
@Aspect
@Component
@Order(Integer.MAX_VALUE - 1)
public class MyDataSourceAop {
	
	@Pointcut("@annotation(org.city.common.api.annotation.sql.MyDataSource) || @within(org.city.common.api.annotation.sql.MyDataSource)")
	private void slaveDataSourceCut() {}
	
	@Around("slaveDataSourceCut()")
	public Object slaveDataSourceAround(ProceedingJoinPoint jp) throws Throwable {
		Deque<String> deque = CommonConstant.OTHER_DATA_SOURCE.get();
		if (deque == null) {deque = new ArrayDeque<String>(); CommonConstant.OTHER_DATA_SOURCE.set(deque);}
		
		try {
			Method method = ((MethodSignature) jp.getSignature()).getMethod();
			deque.push(method.getDeclaredAnnotation(MyDataSource.class).dsId());
			return jp.proceed();
		} finally {deque.pollFirst();}
	}
}
