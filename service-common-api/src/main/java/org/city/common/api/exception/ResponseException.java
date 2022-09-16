package org.city.common.api.exception;

import org.city.common.api.dto.GlobalExceptionDto;
import org.city.common.api.dto.Response;
import org.city.common.api.in.parse.JSONParser;

import com.alibaba.fastjson.JSONObject;

import lombok.Getter;

/**
 * @作者 ChengShi
 * @日期 2022-08-09 18:53:31
 * @版本 1.0
 * @描述 响应异常
 */
@Getter
public class ResponseException extends RuntimeException implements JSONParser{
	private static final long serialVersionUID = 1L;
	private Response Response;
	private GlobalExceptionDto remote;
	public ResponseException(String msg) {super(msg);}
	
	public static ResponseException of(Response Response) {
		ResponseException ResponseException = new ResponseException(Response.getMsg());
		ResponseException.Response = Response;
		/* 获取其中的来源服务名称 */
		if (Response.getData() instanceof JSONObject) {
			JSONObject data = (JSONObject) Response.getData();
			if (data.containsKey("appName")) {ResponseException.remote = ResponseException.parse(data, GlobalExceptionDto.class);}
		}
		return ResponseException; 
	}
}
