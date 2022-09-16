package org.city.common.api.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @作者 ChengShi
 * @日期 2022-12-02 15:17:31
 * @版本 1.0
 * @描述 自定义属性工具
 */
public final class AttributeUtil {
	public final static String ATTR_HEADER_PREFIX = "AHP-"; //自定义互转的前缀
	private AttributeUtil() {}
	
	/**
	 * @描述 获取自定义属性值
	 * @param attrName 自定义属性名称
	 * @return 自定义属性值
	 */
	public static Object getAttr(String attrName) {
		if (attrName == null) {return null;}
		try {
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
			if (attrName.indexOf(ATTR_HEADER_PREFIX) == 0) {return request.getAttribute(attrName);}
			else {return request.getAttribute(ATTR_HEADER_PREFIX + attrName);}
		} catch (Exception e) {return null;}
	}
	
	/**
	 * @描述 获取所有自定义属性值
	 * @return 所有自定义属性值
	 */
	public static Map<String, Object> getAttrs() {
		try {
			Map<String, Object> attrs = new HashMap<>();
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
			Enumeration<String> attributeNames = request.getAttributeNames();
			while(attributeNames.hasMoreElements()) {
				String name = attributeNames.nextElement();
				if (name.indexOf(ATTR_HEADER_PREFIX) == 0) {
					attrs.put(name.substring(ATTR_HEADER_PREFIX.length()), request.getAttribute(name));
				}
			}
			return attrs;
		} catch (Exception e) {return null;}
	}
	
	/**
	 * @描述 设置自定义属性值
	 * @param attrName 自定义属性名称
	 * @param val 自定义属性值
	 */
	public static void setAttr(String attrName, Object val) {
		if (attrName == null) {return;}
		try {
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
			if (attrName.indexOf(ATTR_HEADER_PREFIX) == 0) {request.setAttribute(attrName, val);}
			else {request.setAttribute(ATTR_HEADER_PREFIX + attrName, val);}
		} catch (Exception e) {}
	}
	
	/**
	 * @描述 设置多个自定义属性值
	 * @param attrs 多个自定义属性值
	 */
	public static void setAttrs(Map<String, Object> attrs) {
		if (attrs == null) {return;}
		try {
			for (Entry<String, Object> entry : attrs.entrySet()) {
				HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
				if (entry.getKey().indexOf(ATTR_HEADER_PREFIX) == 0) {request.setAttribute(entry.getKey(), entry.getValue());}
				else {request.setAttribute(ATTR_HEADER_PREFIX + entry.getKey(), entry.getValue());}
			}
		} catch (Exception e) {}
	}
}
