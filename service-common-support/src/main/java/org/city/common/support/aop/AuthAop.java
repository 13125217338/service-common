package org.city.common.support.aop;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.adapter.RemoteSaveAdapter.RemoteInfo;
import org.city.common.api.annotation.auth.Auth;
import org.city.common.api.annotation.auth.AuthFilter;
import org.city.common.api.annotation.auth.Auths;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.AuthResultDto;
import org.city.common.api.dto.remote.RemoteClassDto;
import org.city.common.api.exception.AuthNotPassException;
import org.city.common.api.in.Replace;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.spi.AuthProvider;
import org.city.common.api.util.PlugUtil;
import org.city.common.api.util.PlugUtil.ClassProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
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
@DependsOn(CommonConstant.PLUG_UTIL_NAME)
public class AuthAop implements Replace,JSONParser{
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
	
	@Pointcut("@annotation(org.city.common.api.annotation.auth.Auths) || @within(org.city.common.api.annotation.auth.Auths)")
	private void authCut() {}
	
	@Before("authCut()")
	public void authBefore(JoinPoint jp) {
		Method method = ((MethodSignature)jp.getSignature()).getMethod();
		/*获取注解，使用@within无法直接入参*/
		Auths auths = getAuths(method, jp.getTarget().getClass());
		/*获取参数真实名称*/
		LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
		String[] names = discoverer.getParameterNames(method);
		
		AuthResultDto result = AuthResultDto.ok();Object[] datas = jp.getArgs();
		/*根据类型执行验证*/
		switch (auths.type()) {
			case AND: for(Auth auth : auths.auths()){
						  result = auth(auth, datas, names, method);
						  if (result == null) {continue;}
						  /*and条件不成功则直接抛出异常*/
						  if(!result.isSuccess()) {throw result.getAuthNotPassException();}
					  }
				/*验证结果*/
				if (result == null) {
					throw new AuthNotPassException("所有验证返回均为空，验证失败！"){
						private static final long serialVersionUID = 1L;
					};
				} else {return;}
			case OR: for(Auth auth : auths.auths()){
						 result = auth(auth, datas, names, method);
						 if (result == null) {continue;}
						 /*or条件成功则直接返回*/
						 if(result.isSuccess()) {return;}
					 }
				/*验证结果*/
				if (result == null) {
					throw new AuthNotPassException("所有验证返回均为空，验证失败！"){
						private static final long serialVersionUID = 1L;
					};
				} else {throw result.getAuthNotPassException();}
		}
	}
	
	/* 获取过滤唯一ID */
	private String getId(RemoteClassDto remoteClassDto) {
		AuthFilter authFilter = remoteClassDto.getAnnotation(AuthFilter.class);
		Assert.notNull(authFilter, String.format("验证实现类[%s]未有@AuthFilter注解！", remoteClassDto.getName()));
		return authFilter.id();
	}
	
	/*先通过方法获取，拿不到在通过类获取*/
	private Auths getAuths(Method method, Class<?> target) {
		Auths auths = method.getDeclaredAnnotation(Auths.class);
		if (auths == null) {
			auths = target.getDeclaredAnnotation(Auths.class);
		}
		return auths;
	}
	
	/* 验证参数 */
	private AuthResultDto auth(Auth auth, Object[] datas, String[] names, Method method) {
		/* 完善自定义values值 */
		String[] values = auth.values();
		replaceVals(environment, values, datas, names);
		
		/* 深度克隆 - 验证功能不允许操作方法入参 */
		Object[] clones = new Object[datas.length];
		Type[] parameterTypes = method.getGenericParameterTypes();
		for (int i = 0, j = datas.length; i < j; i++) {
			try {clones[i] = parse(JSONObject.toJSONString(datas[i]), parameterTypes[i]);}
			catch (Exception e) {/* 不能转换的不处理 */}
		}
		
		/* 验证结果 */
		return PlugUtil.invoke(auth.id(), idAdapter, AuthProvider.class, new ClassProcess<AuthProvider, AuthResultDto>() {
			@Override
			public AuthResultDto invoke(AuthProvider process) {
				return process.auth(clones, values);
			}
		});
	}
}
