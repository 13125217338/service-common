package org.city.common.api.in.parse;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.city.common.api.dto.Response;
import org.city.common.api.exception.ResponseException;
import org.city.common.api.util.AttributeUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;

/**
 * @作者 ChengShi
 * @日期 2022年8月10日
 * @版本 1.0
 * @描述 请求响应解析
 */
public interface RestReponseParse extends JSONParser {
	/**
	 * @描述 Post请求解析结果
	 * @param url 调用地址
	 * @param data 调用数据
	 * @param returnType 返回值类型
	 * @param restTemplate 调用对象
	 * @return 解析结果
	 */
	default <T> Object parsePostRp(String url, HttpEntity<T> data, Type returnType, RestTemplate restTemplate) {
		/* 远程调用 */
		ResponseEntity<Object> responseEntity = restTemplate.postForEntity(url, data, Object.class);
		/* 如果调用不成功直接抛出异常 */
		if (HttpStatus.OK != responseEntity.getStatusCode()) {
			throw ResponseException.of(Response.error(responseEntity.getStatusCodeValue(),
					responseEntity.getBody(), responseEntity.getStatusCode().getReasonPhrase()));
		}
		
		try {
			/* 将头转成临时属性值 */
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
			for (Entry<String, List<String>> entry : responseEntity.getHeaders().entrySet()) {
				if (entry.getKey().indexOf(AttributeUtil.ATTR_HEADER_PREFIX) == 0 && entry.getValue().size() == 1) {
					request.setAttribute(entry.getKey(), JSON.parse(entry.getValue().get(0), Feature.SupportAutoType));
				}
			}
		} catch (Exception e) {/* 不处理异常 */}
		
		/* 结果解析 */
		return parseRp(responseEntity.getBody(), returnType);
	}
	
	/**
	 * @描述 只解析返回值
	 * @param body 返回值
	 * @param returnType 返回值类型
	 * @return 解析后的返回值
	 */
	default <T> Object parseRp(Object body, Type returnType) {
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
