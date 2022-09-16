package org.city.common.api.in.parse;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.city.common.api.annotation.auth.Auths;
import org.city.common.api.dto.AuthResultDto.AuthMethod;
import org.city.common.api.dto.AuthResultDto.AuthMethod.Auth;

/**
 * @作者 ChengShi
 * @日期 2022-10-17 14:30:30
 * @版本 1.0
 * @描述 方法名称解析
 */
public interface MethodNameParse {
	/**
	 * @描述 解析方法名称（参数类型也展示出来）
	 * @param method 待解析的方法
	 * @return 带参数类型的方法名称
	 */
	default String parse(Method method) {
		StringBuilder sb = new StringBuilder(method.getName() + "(");
		if (method.getParameterCount() > 0) {
			for (Class<?> ptcls : method.getParameterTypes()) {
				sb.append(ptcls.getName() + ",");
			}
			sb.deleteCharAt(sb.length() - 1);
		}
		sb.append(")");
		return sb.toString();
	}
	
	/**
	 * @描述 转换成验证方法
	 * @param method 带转换的方法
	 * @param auth 验证参数（优先使用该参数）
	 * @param auths 验证参数数组
	 * @return 验证方法
	 */
	default AuthMethod toAuthMethod(Method method, org.city.common.api.annotation.auth.Auth auth, Auths auths) {
		List<Auth> ats = new ArrayList<>();
		/* 解析验证注解 */
		if (auth == null) {
			for (org.city.common.api.annotation.auth.Auth at : auths.value()) {
				ats.add(new Auth(at.id(), at.values()));
			}
		} else {ats.add(new Auth(auth.id(), auth.values()));}
		
		/* 将方法参数类型和返回值类型赋值 */
		String[] parameterType = new String[method.getParameterCount()];
		for (int i = 0; i < parameterType.length; i++) {parameterType[i] = method.getGenericParameterTypes()[i].getTypeName();}
		return new AuthMethod(method.getDeclaringClass().getName(), method.getName(), parameterType, method.getGenericReturnType().getTypeName(), ats, true);
	}
}
