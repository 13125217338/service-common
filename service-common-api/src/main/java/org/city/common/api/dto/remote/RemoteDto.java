package org.city.common.api.dto.remote;

import java.lang.reflect.Type;
import java.util.Map;

import org.city.common.api.dto.Response;
import org.city.common.api.in.parse.TypeParse;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-07-27 15:52:09
 * @版本 1.0
 * @描述 远程参数
 */
@Data
@Accessors(chain = true)
public class RemoteDto {
	/* 请求唯一ID */
	private String requestId;
	/* 事务ID */
	private String tranId;
	/* 请求头信息 */
	private Map<String, String> headers;
	/* 对应Bean名称 */
	private String beanName;
	/* 远程方法信息 */
	private RemoteMethodDto remoteMethod;
	/* 方法参数 */
	private Object[] args;
	
	/**
	 * @描述 生成远程参数
	 * @param requestId 请求唯一ID
	 * @param tranId 事务ID
	 * @param headers 请求头信息
	 * @param beanName 对应Bean名称
	 * @param remoteMethod 远程方法信息
	 * @param args 方法参数
	 * @return 远程参数
	 */
	public static RemoteDto of(String requestId, String tranId, Map<String, String> headers,
			String beanName, RemoteMethodDto remoteMethod, Object...args) {
		return new RemoteDto().setRequestId(requestId).setTranId(tranId).setHeaders(headers)
				.setBeanName(beanName).setRemoteMethod(remoteMethod).setArgs(args);
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2023-02-23 10:19:39
	 * @版本 1.0
	 * @parentClass RemoteMethodDto
	 * @描述 响应结果
	 */
	@Data
	@Accessors(chain = true)
	public static class Result implements TypeParse {
		/* 请求唯一ID */
		private String requestId;
		/* 返回值类型 */
		private String returnType;
		/* 响应结果 */
		private Response<?> response;
		
		/**
		 * @描述 设置返回值类型
		 * @param returnType 返回值类型
		 */
		public Result $setReturnType(Type returnType) {this.returnType = returnType.getTypeName(); return this;}
		/**
		 * @描述 获取返回值类型
		 * @return 返回值类型
		 */
		public Type $getReturnType() {return forType(returnType);}
	}
}
