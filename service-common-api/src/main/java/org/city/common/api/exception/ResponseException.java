package org.city.common.api.exception;

import org.city.common.api.dto.Response;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.in.parse.JSONParser;

import lombok.Getter;

/**
 * @作者 ChengShi
 * @日期 2022-08-09 18:53:31
 * @版本 1.0
 * @描述 响应异常
 */
@Getter
public class ResponseException extends RuntimeException implements JSONParser {
	private static final long serialVersionUID = 1L;
	private final Response<?> response;
	private final RemoteIpPortDto remoteIpPort;
	
	public ResponseException(Response<?> response, RemoteIpPortDto remoteIpPort) {
		super(response.getMsg()); this.response = response; this.remoteIpPort = remoteIpPort;
	}
}
