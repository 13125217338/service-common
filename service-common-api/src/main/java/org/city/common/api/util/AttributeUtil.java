package org.city.common.api.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

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
			RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
			if (attrName.indexOf(ATTR_HEADER_PREFIX) == 0) {return requestAttributes.getAttribute(attrName, RequestAttributes.SCOPE_REQUEST);}
			else {return requestAttributes.getAttribute(ATTR_HEADER_PREFIX + attrName, RequestAttributes.SCOPE_REQUEST);}
		} catch (Exception e) {return null;}
	}
	
	/**
	 * @描述 获取所有自定义属性值
	 * @return 所有自定义属性值
	 */
	public static Map<String, Object> getAttrs() {
		try {
			Map<String, Object> attrs = new HashMap<>();
			RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
			for (String name : requestAttributes.getAttributeNames(RequestAttributes.SCOPE_REQUEST)) {
				if (name.indexOf(ATTR_HEADER_PREFIX) == 0) {
					attrs.put(name.substring(ATTR_HEADER_PREFIX.length()), requestAttributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST));
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
			RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
			if (attrName.indexOf(ATTR_HEADER_PREFIX) == 0) {requestAttributes.setAttribute(attrName, val, RequestAttributes.SCOPE_REQUEST);}
			else {requestAttributes.setAttribute(ATTR_HEADER_PREFIX + attrName, val, RequestAttributes.SCOPE_REQUEST);}
		} catch (Exception e) {}
	}
	
	/**
	 * @描述 设置多个自定义属性值
	 * @param attrs 多个自定义属性值
	 */
	public static void setAttrs(Map<String, Object> attrs) {
		if (attrs == null) {return;}
		try {
			RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
			for (Entry<String, Object> entry : attrs.entrySet()) {
				if (entry.getKey().indexOf(ATTR_HEADER_PREFIX) == 0) {requestAttributes.setAttribute(entry.getKey(), entry.getValue(), RequestAttributes.SCOPE_REQUEST);}
				else {requestAttributes.setAttribute(ATTR_HEADER_PREFIX + entry.getKey(), entry.getValue(), RequestAttributes.SCOPE_REQUEST);}
			}
		} catch (Exception e) {}
	}
}
