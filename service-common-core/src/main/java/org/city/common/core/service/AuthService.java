package org.city.common.core.service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.city.common.api.annotation.auth.Auths;
import org.city.common.api.dto.remote.RemoteMethodDto;
import org.city.common.api.in.scanner.AuthScanner;
import org.city.common.api.util.MyUtil;
import org.city.common.core.config.RemoteConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2022-10-07 10:27:52
 * @版本 1.0
 * @描述 自定义验证注解扫描
 */
@Component
public class AuthService implements AuthScanner {
	private final Map<Method, RemoteMethodDto> authMethods = new HashMap<>();
	@Autowired
	private RemoteConfig remoteConfig;
	
	@Override
	public void scan(Class<?> scanClass) {
		if (scanClass.getModifiers() == 1 || scanClass.getModifiers() == 17) { //常规类和最终常规类
			for (Method method : scanClass.getDeclaredMethods()) {
				/* 不保留顶级父类方法 */
				if (method.getDeclaringClass() == Object.class) {continue;}
				int modifiers = method.getModifiers(); //私有、最终、静态都不保留
				if (Modifier.isPrivate(modifiers) || Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {continue;}
				
				/* 如果类或者方法上有验证注解则直接添加 - 验证参数方法上优先使用 */
				Auths methodAuths = method.getDeclaredAnnotation(Auths.class), classAuths = scanClass.getDeclaredAnnotation(Auths.class);
				if (methodAuths != null) {authMethods.put(method, remoteConfig.getRemoteMethod(scanClass, method));}
				else if(classAuths != null) {
					RemoteMethodDto remoteMethod = remoteConfig.getRemoteMethod(scanClass, method);
					remoteMethod.getAnnotations().put(classAuths.annotationType().getName(), MyUtil.getAnnotationVal(classAuths));
					authMethods.put(method, remoteMethod);
				}
			}
		}
	}
	
	@Override
	public Collection<RemoteMethodDto> getAuthMethods() {
		return this.authMethods.values();
	}
	
	@Override
	public RemoteMethodDto getByMethod(Method method) {
		return this.authMethods.get(method);
	}
}
