package org.city.common.api.annotation.plug;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.city.common.api.in.remote.RemoteUrlHeaders;
import org.city.common.api.in.remote.RemoteUrlRequestBody;
import org.city.common.api.in.remote.RemoteUrlResponseBody;
import org.city.common.api.remote.request.CommonRequestBody;
import org.springframework.web.client.ResponseErrorHandler;

/**
 * @作者 ChengShi
 * @日期 2023-04-06 16:06:25
 * @版本 1.0
 * @描述 远程调用
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RemoteUrl {
	/**
	 * @描述 获取请求地址（支持${key}取配置值）
	 */
	public String url();
	/**
	 * @描述 Spring注册的beanName，默认接口精简名首字母小写
	 */
	public String beanName() default "";
	/**
	 * @描述 获取公共请求头（实现类需交给Spring管理）
	 */
	public Class<? extends RemoteUrlHeaders> commonHeaders() default RemoteUrlHeaders.class;
	/**
	 * @描述 获取请求体（实现类需交给Spring管理）
	 */
	public Class<? extends RemoteUrlRequestBody> requestBody() default CommonRequestBody.class;
	/**
	 * @描述 响应处理（实现类需交给Spring管理）
	 */
	public Class<? extends ResponseErrorHandler> response() default ResponseErrorHandler.class;
	/**
	 * @描述 业务响应处理（实现类需交给Spring管理）
	 */
	public Class<? extends RemoteUrlResponseBody> responseBody() default RemoteUrlResponseBody.class;
	/**
	 * @描述 远程方法调用限流
	 */
	public int speedLimit() default -1;
}
