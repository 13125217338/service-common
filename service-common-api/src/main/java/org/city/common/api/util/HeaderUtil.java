package org.city.common.api.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;

/**
 * @作者 ChengShi
 * @日期 2022-06-23 18:56:33
 * @版本 1.0
 * @描述 头信息工具
 */
public final class HeaderUtil {
	private final static ThreadLocal<Map<String, String>> HEADERS = new ThreadLocal<>();
	private HeaderUtil() {}
	
	/**
	 * @描述 设置头信息来自当前请求
	 * @param request 当前请求
	 */
	public static void setByRequest(HttpServletRequest request) {
		/* 当前请求头 */
		Map<String, String> headers = new HashMap<>();
		headers.put("x-cur-ip", request.getRemoteAddr());
		
		/* 取出所有头名称进行添加 */
		Enumeration<String> names = request.getHeaderNames();
		while(names.hasMoreElements()) {
			String name = names.nextElement().toLowerCase();
			headers.put(name, request.getHeader(name));
		}
		/* 设置请求头 */
		HEADERS.set(headers);
	}
	
	/**
	 * @描述 设置头信息来自参数
	 * @param headers 头信息
	 */
	public static void set(Map<String, String> headers) {
		HEADERS.set(headers);
	}
	/**
	 * @描述 获取头信息
	 * @return 头信息
	 */
	public static Map<String, String> get() {
		return HEADERS.get();
	}
	
	/**
	 * @描述 设置对应值来自头名称
	 * @param name 头名称
	 * @param value 对应值
	 */
	public static void setValue(String name, String value) {
		if (name == null || value == null) {return;}
		Map<String, String> headers = HEADERS.get();
		if (headers != null) {headers.put(name.toLowerCase(), value);}
	}
	/**
	 * @描述 根据头名称获取对应值
	 * @param name 头名称
	 * @return 对应值
	 */
	public static String getValue(String name) {
		if (name == null) {return null;}
		Map<String, String> headers = HEADERS.get();
		return headers == null ? null : headers.get(name.toLowerCase());
	}
	
	/**
	 * @描述 获取来源IP
	 * @return 来源IP
	 */
	public static String getRemoteIp() {
		Map<String, String> headers = HEADERS.get();
		if (headers == null) {return null;}
		/* 真实IP - 代理前请求IP */
		String ip = headers.get("x-real-ip");
		if (StringUtils.hasText(ip)) {return ip;}
		/* IP链路 - 多层代理IP */
		ip = headers.get("x-forwarded-for");
		if (StringUtils.hasText(ip)) {return ip.split(",")[0];}
		/* 当前请求IP */
		ip = headers.get("x-cur-ip");
		if (StringUtils.hasText(ip)) {return ip;}
		return null;
	}
	
	/**
	 * @描述 删除头信息
	 */
	public static void remove() {
		HEADERS.remove();
	}
}
