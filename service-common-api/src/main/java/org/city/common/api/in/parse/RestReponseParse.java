package org.city.common.api.in.parse;

import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.city.common.api.dto.Response;
import org.city.common.api.dto.remote.RemoteMethodDto.ReturnResult;
import org.city.common.api.exception.ResponseException;
import org.city.common.api.util.AttributeUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * @作者 ChengShi
 * @日期 2022年8月10日
 * @版本 1.0
 * @描述 请求响应解析
 */
public interface RestReponseParse extends JSONParser {
	/**
	 * @描述 Post请求解析结果
	 * @param <T> 调用数据类型
	 * @param url 调用地址
	 * @param data 调用数据
	 * @param restTemplate 调用对象
	 * @return 解析结果
	 */
	default <T> Object parsePostRp(String url, HttpEntity<T> data, RestTemplate restTemplate) {
		/* 远程调用 */
		ResponseEntity<Response> responseEntity = restTemplate.postForEntity(url, data, Response.class);
		/* 如果调用不成功直接抛出异常 */
		if (HttpStatus.OK != responseEntity.getStatusCode()) {
			throw ResponseException.of(Response.error(responseEntity.getStatusCodeValue(),
					responseEntity.getBody(), responseEntity.getStatusCode().getReasonPhrase()));
		}
		/* 不允许返回NULL值 */
		Response response = responseEntity.getBody();
		Assert.notNull(response, String.format("远程地址[%s]调用返回值为NULL！", url));
		
		try {
			/* 将头转成临时属性值 */
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
			for (Entry<String, List<String>> entry : responseEntity.getHeaders().entrySet()) {
				if (entry.getKey().indexOf(AttributeUtil.ATTR_HEADER_PREFIX) == 0 && entry.getValue().size() == 1) {
					request.setAttribute(entry.getKey(), JSON.parse(entry.getValue().get(0)));
				}
			}
		} catch (Exception e) {/* 不处理异常 */}
		
		if(response.isOk()) {
			/* 返回真正的类型 - 返回值一定是JSONObject */
			ReturnResult returnResult = ((JSONObject) response.getData()).toJavaObject(ReturnResult.class);
			return parse(returnResult.getReturnValue(), returnResult.$getReturnType());
		}
		throw ResponseException.of(response);
	}
}
