package org.city.common.api.dto;

import org.springframework.http.HttpStatus;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 17:40:23
 * @版本 1.0
 * @描述 统一响应消息
 */
@Data
@Accessors(chain = true)
public class Response {
	/*标识码*/
	private int code = HttpStatus.OK.value();
	/*数据*/
	private Object data;
	/*消息*/
	private String msg = HttpStatus.OK.getReasonPhrase();
	
	/**
	 * @描述 是否成功请求
	 * @return true为成功请求
	 */
	public boolean isOk() {return HttpStatus.OK.value() == code;}
	
	/**
	 * @描述 成功请求
	 * @return 成功对象
	 */
	public static Response ok() {
		return new Response();
	}
	
	/**
	 * @描述 成功请求
	 * @param data 成功数据
	 * @return 成功对象
	 */
	public static Response ok(Object data) {
		return new Response().setData(data);
	}
	
	/**
	 * @描述 成功请求
	 * @param data 成功数据
	 * @param msg 成功信息
	 * @return 成功对象
	 */
	public static Response ok(Object data, String msg) {
		return new Response().setMsg(msg).setData(data);
	}
	
	/**
	 * @描述 响应默认错误信息
	 * @return 错误对象
	 */
	public static Response error() {
		return new Response()
				.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.setMsg(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
	}
	
	/**
	 * @描述 响应指定错误信息
	 * @param 错误消息
	 * @return 错误对象
	 */
	public static Response error(String msg) {
		return new Response()
				.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.setMsg(msg);
	}
	
	/**
	 * @描述 响应默认错误信息
	 * @param data 错误数据
	 * @return 错误对象
	 */
	public static Response error(Object data) {
		return new Response()
				.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.setMsg(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
				.setData(data);
	}
	
	/**
	 * @描述 响应指定的错误信息
	 * @param code 错误码
	 * @param msg 错误消息
	 * @return 错误对象
	 */
	public static Response error(int code, String msg) {
		return new Response().setCode(code).setMsg(msg);
	}
	
	/**
	 * @描述 响应指定的错误信息
	 * @param data 错误数据
	 * @param msg 错误消息
	 * @return 错误对象
	 */
	public static Response error(Object data, String msg) {
		return new Response().setData(data).setMsg(msg)
				.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
	}
	
	/**
	 * @描述 响应默认错误信息
	 * @param code 错误码
	 * @param data 错误数据
	 * @return 错误对象
	 */
	public static Response error(int code, Object data) {
		return new Response().setCode(code).setData(data)
				.setMsg(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
	}
	
	/**
	 * @描述 响应默认错误信息
	 * @param code 错误码
	 * @param data 错误数据
	 * @param msg 错误消息
	 * @return 错误对象
	 */
	public static Response error(int code, Object data, String msg) {
		return new Response().setCode(code).setData(data).setMsg(msg);
	}
}
