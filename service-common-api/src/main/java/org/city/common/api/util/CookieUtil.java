package org.city.common.api.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @作者 ChengShi
 * @日期 2022-06-22 16:32:34
 * @版本 1.0
 * @描述 cookie工具
 */
public final class CookieUtil {
	private CookieUtil() {}
	
	/**
	 * @描述 通过cookie对应名称取出对应值
	 * @param cookieName 对应名称
	 * @return 对应值
	 */
	public static String getCookieVal(String cookieName) {
		if (cookieName == null) {return null;}
		for (Cookie cookie : getCookie()) {
			if (cookieName.equals(cookie.getName())) {return cookie.getValue();}
		}
		return null;
	}
	
	/**
	 * @描述 获取当前请求Cookie
	 * @return 当前请求Cookie（不会为NULL）
	 */
	public static Cookie[] getCookie() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		Cookie[] cookies = request.getCookies();
		return cookies == null ? new Cookie[0] : cookies;
	}
	
	/**
	 * @描述 设置cookie信息
	 * @param cookie 信息
	 */
	public static void setCookie(Cookie cookie) {
		if (cookie == null) {return;}
		HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
		response.addCookie(cookie);
	}
	
	/**
	 * @描述 设置cookie信息
	 * @param name 名称
	 * @param value 值
	 */
	public static void setCookie(String name, String value) {
		Cookie cookie = new Cookie(name, value);
		cookie.setHttpOnly(true);
		setCookie(cookie);
	}
	
	/**
	 * @描述 设置cookie信息
	 * @param name 名称
	 * @param value 值
	 * @param age 过期时间（秒计算）
	 */
	public static void setCookie(String name, String value, int age) {
		Cookie cookie = new Cookie(name, value);
		cookie.setHttpOnly(true);
		cookie.setMaxAge(age);
		setCookie(cookie);
	}
	
	/**
	 * @描述 设置cookie信息
	 * @param name 名称
	 * @param value 值
	 * @param domain 域
	 */
	public static void setCookie(String name, String value, String domain) {
		Cookie cookie = new Cookie(name, value);
		cookie.setHttpOnly(true);
		cookie.setDomain(domain);
		setCookie(cookie);
	}
	
	/**
	 * @描述 设置cookie信息
	 * @param name 名称
	 * @param value 值
	 * @param age 过期时间（秒计算）
	 * @param domain 域
	 */
	public static void setCookie(String name, String value, int age, String domain) {
		Cookie cookie = new Cookie(name, value);
		cookie.setHttpOnly(true);
		cookie.setMaxAge(age);
		cookie.setDomain(domain);
		setCookie(cookie);
	}
}
