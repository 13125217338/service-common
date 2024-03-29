package org.city.common.api.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @作者 ChengShi
 * @日期 2022-06-23 18:56:33
 * @版本 1.0
 * @描述 头信息工具
 */
public final class HeaderUtil {
	private HeaderUtil() {}
	
	/**
	 * @描述 通过headerName对应名称取出对应值
	 * @param headerName 对应名称
	 * @return 对应值
	 */
	public static String getHeaderVal(String headerName) {
		if (headerName == null) {return null;}
		try {
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
			return request.getHeader(headerName);
		} catch (Exception e) {return null;}
	}
	
	/**
	 * @描述 获取来源IP
	 * @return 来源IP
	 */
	public static String getRemoteIp() {
		try {
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
			String ip = request.getHeader("X-Real-IP");
			if (StringUtils.hasText(ip)) {return ip;}
			ip = request.getHeader("X-Forwarded-For");
			if (StringUtils.hasText(ip)) {return ip.split(",")[0];}
			return request.getRemoteAddr();
		} catch (Exception e) {return null;}
	}
	
	/**
	 * @描述 获取请求所有头信息
	 * @return 所有头信息
	 */
	public static HttpHeaders getHeader() {
		HttpHeaders headers = new HttpHeaders();
		
		/* 取出当前请求头 - 转发请求头时不一定有当前请求 */
		HttpServletRequest request = null;
		try {request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();}
		catch (Exception e) {return headers;}
		
		/* 取所有头名称进行添加 */
		Enumeration<String> headerNames = request.getHeaderNames();
		while(headerNames.hasMoreElements()) {
			List<String> headerVal = new ArrayList<>();
			String hName = headerNames.nextElement();
			
			/* 多个头值 */
			Enumeration<String> headerss = request.getHeaders(hName);
			while(headerss.hasMoreElements()){headerVal.add(headerss.nextElement());}
			/* 添加头信息 */
			headers.put(hName, headerVal);
		}
		return headers;
	}
}
