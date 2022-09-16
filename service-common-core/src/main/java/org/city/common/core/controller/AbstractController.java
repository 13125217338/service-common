package org.city.common.core.controller;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.city.common.api.dto.Response;
import org.city.common.api.in.function.FunctionRequestVoid;
import org.city.common.api.in.parse.TypeParse;
import org.city.common.api.util.SpringUtil;

/**
 * @作者 ChengShi
 * @日期 2022-07-25 12:37:42
 * @版本 1.0
 * @描述 公共控制方法
 */
public abstract class AbstractController<S> implements TypeParse {
	/* 当前服务对象 */
	protected S service;
	@PostConstruct
	private void init() {service = SpringUtil.getBean(getCurClass());}
	/* 获取当前类 */
	@SuppressWarnings("unchecked")
	private Class<S> getCurClass() {
		Map<String, Class<?>> genericSuperClass = getGenericSuperClass(this.getClass());
		return (Class<S>) genericSuperClass.get("S");
	}
	
	/**
	 * @描述 成功响应
	 * @param <R> 响应类型
	 * @param data 成功数据（code=200）
	 * @return 响应对象
	 */
	protected <R> Response<R> OK(R data) {
		return new Response<R>(data);
	}
	
	/**
	 * @描述 成功响应
	 * @param request 请求对象（返回空数据）
	 * @return 响应对象
	 * @throws Throwable 执行异常
	 */
	protected Response<Void> OKV(FunctionRequestVoid<S> request) throws Throwable {
		request.apply(service);
		return new Response<Void>();
	}
	
	/**
	 * @描述 错误响应
	 * @param <R> 响应类型
	 * @param errorMsg 错误信息（code=500）
	 * @return 响应对象
	 */
	protected <R> Response<R> ERROR(String errorMsg) {
		return new Response<R>(errorMsg);
	}
	
	/**
	 * @描述 错误响应
	 * @param <R> 响应类型
	 * @param errorMsg 错误信息（code=500）
	 * @param errorData 错误数据
	 * @return 响应对象
	 */
	protected <R> Response<R> ERROR(String errorMsg, R errorData) {
		return new Response<R>(errorMsg, errorData);
	}
	
	/**
	 * @描述 自定义响应
	 * @param <R> 响应类型
	 * @param code 状态码
	 * @param msg 消息
	 * @return 响应对象
	 */
	protected <R> Response<R> RSP(int code, String msg) {
		return new Response<R>(code, msg);
	}
	
	/**
	 * @描述 自定义响应
	 * @param <R> 响应类型
	 * @param code 状态码
	 * @param msg 消息
	 * @param data 数据
	 * @return 响应对象
	 */
	protected <R> Response<R> RSP(int code, String msg, R data) {
		return new Response<R>(code, msg, data);
	}
}
