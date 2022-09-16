package org.city.common.api.dto;

import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.util.SpringUtil;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 17:40:23
 * @版本 1.0
 * @描述 公共响应
 */
@Data
@NoArgsConstructor
@Schema(description = "公共响应")
public class Response<R> {
	private final static String SUCCESS = SpringUtil.getBean(RemoteConfigDto.class).getMsg(); //默认显示成功字符串
	
	@Schema(description = "状态码 - 200=OK")
	private int code = HttpStatus.OK.value();
	
	@Schema(description = "数据")
	private R data;
	
	@Schema(description = "消息")
	private String msg = SUCCESS;
	
	/**
	 * @param data 成功数据（code=200）
	 */
	public Response(R data) {
		this.data = data;
	}
	
	/**
	 * @param errorMsg 错误信息（code=500）
	 */
	public Response(String errorMsg) {
		this.code = HttpStatus.INTERNAL_SERVER_ERROR.value();
		this.msg = errorMsg;
	}
	
	/**
	 * @param errorMsg 错误信息（code=500）
	 * @param errorData 错误数据
	 */
	public Response(String errorMsg, R errorData) {
		this.code = HttpStatus.INTERNAL_SERVER_ERROR.value();
		this.msg = errorMsg;
		this.data = errorData;
	}
	
	/**
	 * @param code 状态码
	 * @param msg 消息
	 */
	public Response(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}
	
	/**
	 * @param code 状态码
	 * @param msg 消息
	 * @param data 数据
	 */
	public Response(int code, String msg, R data) {
		this.code = code;
		this.msg = msg;
		this.data = data;
	}
}
