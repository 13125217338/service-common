package org.city.common.api.dto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.tomcat.util.http.FastHttpDateFormat;
import org.city.common.api.util.AttributeUtil;
import org.city.common.api.util.HeaderUtil;
import org.springframework.http.HttpHeaders;

import lombok.Data;

/**
 * @作者 ChengShi
 * @日期 2023-02-01 10:56:50
 * @版本 1.0
 * @描述 自定义请求参数
 */
@Data
public class HttpSevletRequestDto implements HttpServletRequest {
	private final HttpServletRequest request;
	private final Cookie[] cookie;
	private final HttpHeaders header = HeaderUtil.getHeader();
	private final Map<String, Object> attr = getAHPAttrs();
	public HttpSevletRequestDto(HttpServletRequest request) {
		this.request = request;
		this.cookie = request == null || request.getCookies() == null ? new Cookie[0] : request.getCookies();
	}
	
	/* 获取前缀属性的所有值 - 当前线程操作的请求属性所以线程安全 */
	private Map<String, Object> getAHPAttrs() {
		Map<String, Object> attrs = AttributeUtil.getAttrs();
		if (attrs == null) {return new ConcurrentHashMap<>();}
		
		/* 将原key添加前缀 */
		for (String key : new ArrayList<>(attrs.keySet())) {
			Object value = attrs.get(key);
			attrs.remove(key);
			attrs.put(AttributeUtil.ATTR_HEADER_PREFIX + key, value);
		}
		return attrs;
	}
	
	@Override
	public Object getAttribute(String name) {return attr.get(name);}
	@Override
	public Enumeration<String> getAttributeNames() {return new MyEnumeration<>(attr.keySet().iterator());}
	@Override
	public void setAttribute(String name, Object o) {attr.put(name, o);}
	@Override
	public void removeAttribute(String name) {attr.remove(name);}
	
	@Override
	public Cookie[] getCookies() {return cookie;}
	
	@Override
	public long getDateHeader(String name) {
        String value = getHeader(name);
        if (value == null) { return -1L;}
        
        // Attempt to convert the date header in a variety of formats
        long result = FastHttpDateFormat.parseDate(value);
        if (result != (-1L)) {return result;}
        throw new IllegalArgumentException(value);
    }
	@Override
	public String getHeader(String name) {return header.getFirst(name);}
	@Override
	public Enumeration<String> getHeaders(String name) {return new MyEnumeration<>(header.get(name).iterator());}
	@Override
	public Enumeration<String> getHeaderNames() {return new MyEnumeration<>(header.keySet().iterator());}
	@Override
	public int getIntHeader(String name) {
        String value = getHeader(name);
        if (value == null) {return -1;}
        return Integer.parseInt(value);
    }
	
	@Override
	public String getCharacterEncoding() {return request.getCharacterEncoding();}
	@Override
	public void setCharacterEncoding(String env) throws UnsupportedEncodingException {request.setCharacterEncoding(env);}
	@Override
	public int getContentLength() {return request.getContentLength();}
	@Override
	public long getContentLengthLong() {return request.getContentLengthLong();}
	@Override
	public String getContentType() {return request.getContentType();}
	@Override
	public ServletInputStream getInputStream() throws IOException {return request.getInputStream();}
	@Override
	public String getParameter(String name) {return request.getParameter(name);}
	@Override
	public Enumeration<String> getParameterNames() {return request.getParameterNames();}
	@Override
	public String[] getParameterValues(String name) {return request.getParameterValues(name);}
	@Override
	public Map<String, String[]> getParameterMap() {return request.getParameterMap();}
	@Override
	public String getProtocol() {return request.getProtocol();}
	@Override
	public String getScheme() {return request.getScheme();}
	@Override
	public String getServerName() {return request.getServerName();}
	@Override
	public int getServerPort() {return request.getServerPort();}
	@Override
	public BufferedReader getReader() throws IOException {return request.getReader();}
	@Override
	public String getRemoteAddr() {return request.getRemoteAddr();}
	@Override
	public String getRemoteHost() {return request.getRemoteHost();}
	@Override
	public Locale getLocale() {return request.getLocale();}
	@Override
	public Enumeration<Locale> getLocales() {return request.getLocales();}
	@Override
	public boolean isSecure() {return request.isSecure();}
	@Override
	public RequestDispatcher getRequestDispatcher(String path) {return request.getRequestDispatcher(path);}
	@Override
	@SuppressWarnings("deprecation")
	public String getRealPath(String path) {return request.getRealPath(path);}
	@Override
	public int getRemotePort() {return request.getRemotePort();}
	@Override
	public String getLocalName() {return request.getLocalName();}
	@Override
	public String getLocalAddr() {return request.getLocalAddr();}
	@Override
	public int getLocalPort() {return request.getLocalPort();}
	@Override
	public ServletContext getServletContext() {return request.getServletContext();}
	@Override
	public AsyncContext startAsync() throws IllegalStateException {return request.startAsync();}
	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {return request.startAsync(servletRequest, servletResponse);}
	@Override
	public boolean isAsyncStarted() {return request.isAsyncStarted();}
	@Override
	public boolean isAsyncSupported() {return request.isAsyncSupported();}
	@Override
	public AsyncContext getAsyncContext() {return request.getAsyncContext();}
	@Override
	public DispatcherType getDispatcherType() {return request.getDispatcherType();}
	@Override
	public String getAuthType() {return request.getAuthType();}
	@Override
	public String getMethod() {return request.getMethod();}
	@Override
	public String getPathInfo() {return request.getPathInfo();}
	@Override
	public String getPathTranslated() {return request.getPathTranslated();}
	@Override
	public String getContextPath() {return request.getContextPath();}
	@Override
	public String getQueryString() {return request.getQueryString();}
	@Override
	public String getRemoteUser() {return request.getRemoteUser();}
	@Override
	public boolean isUserInRole(String role) {return request.isUserInRole(role);}
	@Override
	public Principal getUserPrincipal() {return request.getUserPrincipal();}
	@Override
	public String getRequestedSessionId() {return request.getRequestedSessionId();}
	@Override
	public String getRequestURI() {return request.getRequestURI();}
	@Override
	public StringBuffer getRequestURL() {return request.getRequestURL();}
	@Override
	public String getServletPath() {return request.getServletPath();}
	@Override
	public HttpSession getSession(boolean create) {return request.getSession(create);}
	@Override
	public HttpSession getSession() {return request.getSession();}
	@Override
	public String changeSessionId() {return request.changeSessionId();}
	@Override
	public boolean isRequestedSessionIdValid() {return request.isRequestedSessionIdValid();}
	@Override
	public boolean isRequestedSessionIdFromCookie() {return request.isRequestedSessionIdFromCookie();}
	@Override
	public boolean isRequestedSessionIdFromURL() {return request.isRequestedSessionIdFromURL();}
	@Override
	@SuppressWarnings("deprecation")
	public boolean isRequestedSessionIdFromUrl() {return request.isRequestedSessionIdFromUrl();}
	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {return request.authenticate(response);}
	@Override
	public void login(String username, String password) throws ServletException {request.login(username, password);}
	@Override
	public void logout() throws ServletException {request.logout();}
	@Override
	public Collection<Part> getParts() throws IOException, ServletException {return request.getParts();}
	@Override
	public Part getPart(String name) throws IOException, ServletException {return request.getPart(name);}
	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> httpUpgradeHandlerClass) throws IOException, ServletException {return request.upgrade(httpUpgradeHandlerClass);}
	
	/**
	 * @作者 ChengShi
	 * @日期 2023年6月19日
	 * @版本 1.0
	 * @描述 我的枚举
	 */
	private class MyEnumeration<T> implements Enumeration<T> {
		private final Iterator<T> iterator; 
		private MyEnumeration(Iterator<T> iterator) {this.iterator = iterator;}
		@Override
		public boolean hasMoreElements() {return iterator.hasNext();}
		@Override
		public T nextElement() {return iterator.next();}
	}
}
