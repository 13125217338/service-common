package org.city.common.core.scanner;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.city.common.api.annotation.auth.Auths;
import org.city.common.api.dto.AuthResultDto.AuthMethod;
import org.city.common.api.in.Scanner;
import org.city.common.api.in.parse.MethodNameParse;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2022-10-07 10:27:52
 * @版本 1.0
 * @描述 自定义验证注解扫描
 */
@Component
public class AuthScanner implements Scanner,MethodNameParse {
	private final Set<AuthMethod> authMethods = new HashSet<>();
	
	@Override
	public void scan(Class<?> scanClass) {
		if (scanClass.getModifiers() == 1 || scanClass.getModifiers() == 17) { //常规类和最终常规类
			Auths auths = scanClass.getDeclaredAnnotation(Auths.class);
			for (Method method : scanClass.getDeclaredMethods()) {
				/* 不保留顶级父类方法 */
				if (method.getDeclaringClass() == Object.class) {continue;}
				int modifiers = method.getModifiers(); //私有、最终、静态都不保留
				if (Modifier.isPrivate(modifiers) || Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {continue;}
				
				/* 如果类或者方法上有验证注解则直接添加 - 验证参数方法上优先使用 */
				Auths ats = method.getDeclaredAnnotation(Auths.class);
				if (ats != null) {authMethods.add(toAuthMethod(method, null, ats));}
				else if(auths != null) {authMethods.add(toAuthMethod(method, null, auths));}
			}
		}
	}
	
	/**
	 * @描述 获取所有验证方法
	 * @return 所有验证方法
	 */
	public Set<AuthMethod> getAuthMethods() {
		return this.authMethods;
	}
}
