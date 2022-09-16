package org.city.common.support.aop;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.annotation.auth.Auth;
import org.city.common.api.annotation.auth.AuthFilter;
import org.city.common.api.annotation.auth.Auths;
import org.city.common.api.constant.AuthType;
import org.city.common.api.dto.AuthResultDto;
import org.city.common.api.dto.AuthResultDto.AuthMethod;
import org.city.common.api.dto.remote.RemoteClassDto;
import org.city.common.api.exception.AuthNotPassException;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.parse.MethodNameParse;
import org.city.common.api.in.remote.RemoteSave.RemoteInfo;
import org.city.common.api.in.util.Replace;
import org.city.common.api.spi.AuthProvider;
import org.city.common.api.util.PlugUtil;
import org.city.common.api.util.PlugUtil.ClassProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSONObject;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 15:07:56
 * @版本 1.0
 * @描述 验证拦截
 */
@Aspect
@Component
@Order(Short.MIN_VALUE)
public class AuthAop implements Replace,JSONParser,MethodNameParse {
	@Autowired
	private Environment environment;
	/* 唯一ID选择器 */
	private final RemoteAdapter idAdapter = new RemoteAdapter() {
		@Override
		public RemoteInfo select(List<RemoteInfo> invokes, Object id) {
			for (RemoteInfo remoteInfo : invokes) {
				if(getId(remoteInfo.getRemoteClassDto()).equals(id)) {return remoteInfo;}
			}
			return null;
		}
	};
	
	@Around("@within(org.city.common.api.annotation.auth.AuthFilter)")
	public Object authFilterAround(ProceedingJoinPoint jp) {
		if (!(jp.getTarget() instanceof AuthProvider)) {throw new RuntimeException(String.format("@AuthFilter注解对应类[%s]未实现AuthProvider接口！", jp.getTarget().getClass().getName()));}
		AuthProvider authProvider = (AuthProvider) jp.getTarget(); Object[] args = jp.getArgs();
		AuthMethod authMethod = (AuthMethod) args[0]; //验证的原方法信息
		
		/* 调用原验证方法进行验证 - 返回NULL跳过验证 */
		AuthResultDto auth = authProvider.auth(authMethod, (Object[]) args[1], (int) args[2], (String[]) args[3]);
		if (auth == null) {return null;}
		/* 当不成功时 - 且标记抛出异常 - 则抛出异常 */
		if (!auth.isSuccess() && authMethod.isThrow()) {throw auth.getAuthNotPassException();}
		return auth.setAuthNotPassException(null); //远程异常数据不传递
	}
	
	@Pointcut("@annotation(org.city.common.api.annotation.auth.Auths) || @within(org.city.common.api.annotation.auth.Auths)")
	private void authCut() {}
	
	@Before("authCut()")
	public void authBefore(JoinPoint jp) {
		Method method = ((MethodSignature) jp.getSignature()).getMethod();
		/* 获取注解，使用@within无法直接入参 */
		Auths auths = getAuths(method, jp.getTarget().getClass());
		/* 获取参数真实名称 */
		LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
		String[] names = discoverer.getParameterNames(method);
		
		/* 多个验证 */
		AuthResultDto result = null; Object[] datas = jp.getArgs();
		for (int i = 0, j = auths.value().length; i < j; i++) {
			boolean isThrow = ((i + 1) == j) ? true : auths.value()[i + 1].type() == AuthType.AND;
			result = auth(auths.value()[i], datas, names, jp.getTarget(), method, isThrow);
			if(!isThrow && result != null && result.isSuccess()) {return;}
		}
		/* 最后的验证不能跳过 */
		if (result == null) {throw new AuthNotPassException("最后的验证不能跳过，验证失败！");}
	}
	
	/* 获取过滤唯一ID */
	private String getId(RemoteClassDto remoteClassDto) {
		AuthFilter authFilter = remoteClassDto.getAnnotation(AuthFilter.class);
		Assert.notNull(authFilter, String.format("验证实现类[%s]未有@AuthFilter注解！", remoteClassDto.getName()));
		return authFilter.id();
	}
	/* 先通过方法获取，拿不到在通过类获取 */
	private Auths getAuths(Method method, Class<?> target) {
		Auths auths = method.getDeclaredAnnotation(Auths.class);
		if (auths == null) {
			auths = target.getDeclaredAnnotation(Auths.class);
		}
		return auths;
	}
	
	/* 验证参数 */
	private AuthResultDto auth(Auth auth, Object[] datas, String[] names, Object curObj, Method method, boolean isThrow) {
		/* 完善自定义values值 */
		String[] values = auth.values();
		replaceVals(environment, values, curObj, datas, names);
		
		/* 深度克隆 - 验证功能不允许操作方法入参 */
		Object[] clones = new Object[datas.length];
		Type[] parameterTypes = method.getGenericParameterTypes();
		for (int i = 0, j = datas.length; i < j; i++) {
			try {clones[i] = authParse(datas[i]) ? parse(JSONObject.toJSONString(datas[i]), parameterTypes[i]) : null;}
			catch (Throwable e) {/* 不能转换的不处理 */}
		}
		
		/* 验证结果 */
		return PlugUtil.invoke(auth.id(), idAdapter, AuthProvider.class, new ClassProcess<AuthProvider, AuthResultDto>() {
			@Override
			public AuthResultDto invoke(AuthProvider process) {
				return process.auth(toAuthMethod(method, auth, null).setThrow(isThrow), clones, auth.value(), values);
			}
		});
	}
}
