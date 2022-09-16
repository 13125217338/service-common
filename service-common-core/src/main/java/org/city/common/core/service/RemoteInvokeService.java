package org.city.common.core.service;

import java.lang.reflect.Method;

import javax.management.RuntimeErrorException;

import org.city.common.api.dto.MethodCacheDto;
import org.city.common.api.dto.RemoteDto;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.open.RemoteInvokeApi;
import org.city.common.api.util.MyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;

/**
 * @作者 ChengShi
 * @日期 2022-07-27 15:18:20
 * @版本 1.0
 * @描述 远程执行服务实现
 */
@Service
public class RemoteInvokeService implements RemoteInvokeApi,JSONParser{
	@Value("${remote.invoke:false}")
	private boolean isInvoke;
	@Value("${remote.path:/remote/path}/invoke")
	private String remotePath;
	@Autowired
	private RestTemplate restTemplate;
	@Value("${remote.verify:RemoteInvokeService-Verify}")
	private String verifyKey;
	@Autowired
	private ApplicationContext applicationContext;

	@Override
	public <T> T invoke(HttpEntity<MethodCacheDto> httpEntity, Object...args) {
		MethodCacheDto methodCacheDto = httpEntity.getBody();
		String ipPort = methodCacheDto.getIp() + ":" + methodCacheDto.getPort();
		
		/* 远程调用 */
		Object postForObject = restTemplate.postForEntity("http://" + ipPort + remotePath,
				new HttpEntity<RemoteDto>(getRemoteDto(methodCacheDto, args), httpEntity.getHeaders()), Object.class);
		return parse(postForObject, methodCacheDto.getMethodInfo().$getReturnType());
	}
	/* 获取远程调用参数 */
	private RemoteDto getRemoteDto(MethodCacheDto methodCacheDto, Object...args) {
		try {return RemoteDto.of(MyUtil.md5(methodCacheDto.getClassInfo().getName() + methodCacheDto.getMethodInfo().getName() + verifyKey), methodCacheDto, args);}
		catch (Exception e) {throw new RuntimeException("远程调用加密失败！", e);}
	}
	
	/**
	 * @描述 被调用的服务器
	 * @param remoteDto 远程参数
	 * @return 调用结果
	 */
	public Object $invoke(RemoteDto remoteDto) throws Throwable {
		if (!isInvoke) {throw new RuntimeErrorException(new Error("服务器未开启远程调用功能，执行失败！"));}
		MethodCacheDto methodCacheDto = remoteDto.getMethodCacheDto();
		/* 加密验证 */
		String key = MyUtil.md5(methodCacheDto.getClassInfo().getName() + methodCacheDto.getMethodInfo().getName() + verifyKey);
		if (!key.equals(remoteDto.getVerify())) {throw new VerifyError("服务器Key认证失败，无法远程调用！");}
		
		/* 本地执行 */
		return localInvoke(methodCacheDto.getClassInfo().getName(), methodCacheDto.getMethodInfo().getName(), methodCacheDto, remoteDto.getArgs());
	}
	/* 本地执行 */
	private Object localInvoke(String className, String methodName, MethodCacheDto methodCacheDto, Object[] args) throws Exception {
		Object bean = null; Class<?> beanClass = Class.forName(className);
		try {bean = applicationContext.getBean(beanClass);}
		catch (Exception e) {bean = beanClass.getConstructor().newInstance();}
		
		/* 获取方法并执行 */
		Method declaredMethod = beanClass.getDeclaredMethod(methodName, methodCacheDto.getMethodInfo().$getParameterClasses());
		return ReflectionUtils.invokeMethod(declaredMethod, bean, args);
	}
}
