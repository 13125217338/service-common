package org.city.common.support.aop;

import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.dto.Response;
import org.city.common.api.in.interceptor.ControllerInterceptor;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.redis.RedisMake;
import org.city.common.api.in.sql.Crud;
import org.city.common.api.util.FormatUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2023年4月19日
 * @版本 1.0
 * @描述 拦截所有控制层并清除自定义Sql与自定义事务
 */
@Aspect
@Component
@Order(Integer.MIN_VALUE)
public class ControllerAop implements JSONParser {
	@Autowired(required = false)
	private List<ControllerInterceptor> interceptors;
	@Autowired
	private RedisMake redisMake;
	
	@Around("@within(org.springframework.web.bind.annotation.RestController) || @within(org.springframework.stereotype.Controller)")
	public Object authBefore(ProceedingJoinPoint jp) throws Throwable {
		Crud.clearSql(jp.getArgs()); //清除自定义Sql
		RemoteAdapter.removeTranId(redisMake); //清除自定义事务
		
		FormatUtil.format(jp.getArgs()); //格式化入参
		if (preHandlers(jp)) {return new Response<>(403, "拒绝请求！");} //执行前处理
		Object result = jp.proceed(); //执行原方法
		postHandlers(jp, result); //执行后处理
		FormatUtil.format(result); //格式化返回值
		
		return result; //返回格式化后的结果
	}
	/* 执行前处理 */
	private boolean preHandlers(JoinPoint jp) {
		if (interceptors != null) {
			for (ControllerInterceptor interceptor : interceptors) {
				if (!interceptor.preHandler(jp)) {return true;}
			}
		}
		return false;
	}
	/* 执行后处理 */
	private void postHandlers(JoinPoint jp, Object result) {
		if (interceptors != null) {
			for (ControllerInterceptor interceptor : interceptors) {
				interceptor.postHandle(jp, result);
			}
		}
	}
}
