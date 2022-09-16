package org.city.common.api.in.parse;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import org.springframework.core.io.InputStreamSource;

/**
 * @作者 ChengShi
 * @日期 2022-11-25 15:20:20
 * @版本 1.0
 * @描述 验证是否可以解析JSON
 */
public interface AuthIsJsonParse extends TypeParse {
	/**
	 * @描述 验证解析
	 * @param type 待验证类型
	 * @return true=可以解析
	 */
	default boolean authParse(Type type) {
		if (type == null) {return false;}
		Class<?> ownCls = getClass(type);
		return !(InputStreamSource.class.isAssignableFrom(ownCls) ||
				InputStream.class.isAssignableFrom(ownCls) ||
				OutputStream.class.isAssignableFrom(ownCls) ||
				Reader.class.isAssignableFrom(ownCls) ||
				Writer.class.isAssignableFrom(ownCls) ||
				File.class.isAssignableFrom(ownCls) ||
				Throwable.class.isAssignableFrom(ownCls));
	}
	
	/**
	 * @描述 验证解析
	 * @param data 待验证对象
	 * @return true=可以解析
	 */
	default boolean authParse(Object data) {
		if (data == null) {return false;}
		return !(data instanceof InputStreamSource ||
				 data instanceof InputStream ||
				 data instanceof OutputStream ||
				 data instanceof Reader ||
				 data instanceof Writer ||
				 data instanceof File ||
				 data instanceof Throwable);
	}
}
