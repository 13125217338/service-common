package org.city.common.api.in.parse;

import java.lang.reflect.Type;

import org.city.common.api.dto.Response;
import org.city.common.api.exception.ResponseException;

/**
 * @作者 ChengShi
 * @日期 2022年8月10日
 * @版本 1.0
 * @描述 请求响应解析
 */
public interface RestReponseParse extends JSONParser {
	/**
	 * @描述 请求响应体解析
	 * @param body 响应体
	 * @param 返回值类型
	 * @return 解析后的返回值
	 */
	default Object parseRp(Object body, Type returnType) {
		if (body == null) {return body;}
		else {
			try {
				Response parse = (Response) parse(body, Response.class);
				/* 如果是这种返回值类型则直接返回 */
				if (Response.class == returnType) {return parse;}
				/* 如果不是返回值类型 - 并且被全局异常捕获后返回响应异常 - 则抛出响应异常对象 */
				else if(!parse.isOk()){throw ResponseException.of(parse);}
				/* 如果不是返回值类型 - 并且还是ok响应 - 说明返回值类型map被强转为响应对象了 - 则重新返回真正的类型 */
			} catch (ResponseException e) {throw e;} catch (Exception e) {}
			/* 返回真正的类型 */
			return parse(body, returnType);
		}
	}
}
