package org.city.common.core.controller;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.city.common.api.dto.Response;
import org.city.common.api.in.function.FunctionRequest;
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
	 * @描述 自定义执行成功请求（抛出异常）
	 * @param func 回调方法
	 * @return 执行结果
	 * @throws Throwable 
	 */
	protected Response ok(FunctionRequest<S, Object> func) throws Throwable {
		return Response.ok(func.apply(service));
	}
	/**
	 * @描述 自定义执行成功请求（不抛异常）
	 * @param func 回调方法
	 * @return 执行结果
	 */
	protected Response OK(Function<S, Object> func) {
		return Response.ok(func.apply(service));
	}
	/**
	 * @描述 自定义执行成功请求
	 * @param error 异常时指定返回错误信息（为NULL不处理）
	 * @param func 回调方法
	 * @return 执行结果
	 */
	protected Response OK(Response error, FunctionRequest<S, Object> func) {
		try {return Response.ok(func.apply(service));}
		catch (Throwable e) {return error == null ? Response.ok() : error;}
	}
	
	/**
	 * @描述 自定义执行成功请求（抛出异常）
	 * @param func 回调方法（返回为void）
	 * @return 执行结果
	 * @throws Throwable 
	 */
	protected Response okv(FunctionRequestVoid<S> func) throws Throwable {
		func.apply(service); return Response.ok();
	}
	/**
	 * @描述 自定义执行成功请求（不抛异常）
	 * @param func 回调方法（返回为void）
	 * @return 执行结果
	 */
	protected Response OKV(Consumer<S> func) {
		func.accept(service); return Response.ok();
	}
	/**
	 * @描述 自定义执行成功请求
	 * @param error 异常时指定返回错误信息（为NULL不处理）
	 * @param func 回调方法（返回为void）
	 * @return 执行结果
	 */
	protected Response OKV(Response error, FunctionRequestVoid<S> func) {
		try {func.apply(service); return Response.ok();}
		catch (Throwable e) {return error == null ? Response.ok() : error;}
	}
	
	/**
	 * @描述 自定义布尔执行判断请求（布尔判断，抛出异常）
	 * @param func 回调方法
	 * @return 执行结果（回调返回true为成功请求，否则都是失败请求）
	 * @throws Throwable 
	 */
	protected Response bl(FunctionRequest<S, Object> func) throws Throwable {
		Object apply = func.apply(service);
		return Boolean.TRUE.equals(apply) ? Response.ok() : Response.error();
	}
	/**
	 * @描述 自定义布尔执行判断请求（布尔判断，不抛异常）
	 * @param func 回调方法
	 * @return 执行结果（回调返回true为成功请求，否则都是失败请求）
	 */
	protected Response BL(Function<S, Object> func) {
		Object apply = func.apply(service);
		return Boolean.TRUE.equals(apply) ? Response.ok() : Response.error();
	}
	/**
	 * @描述 自定义布尔执行判断请求（布尔判断）
	 * @param error 判断为false时失败请求对象（为NULL不处理）
	 * @param func 回调方法
	 * @return 执行结果（回调返回true为成功请求，否则都是失败请求）
	 */
	protected Response BL(Response error, FunctionRequest<S, Object> func) {
		try {
			Object apply = func.apply(service);
			return Boolean.TRUE.equals(apply) ? Response.ok() : error == null ? Response.ok() : error;
		} catch (Throwable e) {return error == null ? Response.ok() : error;}
	}
	
	/**
	 * @描述 自定义值判断请求（equals判断，抛出异常）
	 * @param verify 验证判断值
	 * @param func 回调方法
	 * @return 执行结果（回调返回判断值相等为成功请求，否则都是失败请求）
	 * @throws Throwable 
	 */
	protected Response eq(Object verify, FunctionRequest<S, Object> func) throws Throwable {
		Object apply = func.apply(service);
		return (verify == null ? verify == apply : verify.equals(apply)) ? Response.ok() : Response.error();
	}
	/**
	 * @描述 自定义值判断请求（equals判断，不抛异常）
	 * @param verify 验证判断值
	 * @param func 回调方法
	 * @return 执行结果（回调返回判断值相等为成功请求，否则都是失败请求）
	 */
	protected Response EQ(Object verify, Function<S, Object> func) {
		Object apply = func.apply(service);
		return (verify == null ? verify == apply : verify.equals(apply)) ? Response.ok() : Response.error();
	}
	/**
	 * @描述 自定义值判断请求（equals判断）
	 * @param verify 验证判断值
	 * @param error 判断为不相等时失败请求对象（为NULL不处理）
	 * @param func 回调方法
	 * @return 执行结果（回调返回判断值相等为成功请求，否则都是失败请求）
	 */
	protected Response EQ(Object verify, Response error, FunctionRequest<S, Object> func) {
		try {
			Object apply = func.apply(service);
			return (verify == null ? verify == apply : verify.equals(apply)) ? Response.ok() : error == null ? Response.ok() : error;
		} catch (Throwable e) {return error == null ? Response.ok() : error;}
	}
	
	/**
	 * @描述 响应指定的错误信息
	 * @param code 错误码
	 * @param msg 错误消息
	 * @return 错误对象
	 */
	protected Response error(int code, String msg) {return Response.error(code, msg);}
	/**
	 * @描述 响应指定的错误信息
	 * @param msg 错误消息
	 * @return 错误对象
	 */
	protected Response error(String msg) {return Response.error(msg);}
}
