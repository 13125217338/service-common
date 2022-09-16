package org.city.common.core.config;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.city.common.api.annotation.plug.Remote;
import org.city.common.api.annotation.plug.RemoteMethod;
import org.city.common.api.annotation.plug.RemoteUrl;
import org.city.common.api.dto.remote.RemoteClassDto;
import org.city.common.api.dto.remote.RemoteMethodDto;
import org.city.common.api.dto.remote.RemoteParameterDto;
import org.city.common.api.in.GlobalConfig;
import org.city.common.api.in.Scanner;
import org.city.common.api.in.parse.FirstCharParse;
import org.city.common.api.in.parse.MethodNameParse;
import org.city.common.api.in.util.Replace;
import org.city.common.api.util.MyUtil;
import org.city.common.api.util.PlugUtil;
import org.city.common.api.util.SpringUtil;
import org.city.common.core.handler.RemoteProxyHandler;
import org.city.common.core.handler.RemoteUrlHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @作者 ChengShi
 * @日期 2022-09-28 10:46:34
 * @版本 1.0
 * @描述 远程调用配置
 */
public class RemoteConfig extends AppenderConfig implements GlobalConfig,Replace,FirstCharParse,MethodNameParse {
	@Autowired
	private ServletWebServerApplicationContext context;
	@PostConstruct
	protected void init() {
		Object mainBean = context.getBeansWithAnnotation(SpringBootApplication.class).entrySet().iterator().next().getValue();
		String[] scanBasePackages = ClassUtils.getUserClass(mainBean).getDeclaredAnnotation(SpringBootApplication.class).scanBasePackages();
		Assert.notEmpty(scanBasePackages, "未找到启动入口类@SpringBootApplication注解对应scanBasePackages值，请检查注解参数对应值！");
		PlugUtil.REMOTE_SAVE.init(); //提前初始化远程调用保存
		
		/* 提前扫描远程注解 - 并记录所有扫描到的类信息 */
		List<Class<?>> allClass = new ArrayList<>(); Map<Class<?>, Remote> remotes = new HashMap<>(); Map<Class<?>, RemoteUrl> remoteUrls = new HashMap<>();
		new ClassPathBeanDefinitionScanner(context.getDefaultListableBeanFactory()) {
			@Override
			protected boolean isCandidateComponent(MetadataReader metadataReader) throws IOException {
				try {
					Class<?> scanClass = Class.forName(metadataReader.getClassMetadata().getClassName());
					allClass.add(scanClass); //添加所有扫描到的类信息
					
					if (scanClass.isInterface()) { //只扫描接口且注解有远程标志
						Remote remote = scanClass.getDeclaredAnnotation(Remote.class);
						if (remote != null) {remotes.put(scanClass, remote);}
						
						RemoteUrl remoteUrl = scanClass.getDeclaredAnnotation(RemoteUrl.class);
						if (remoteUrl != null) {remoteUrls.put(scanClass, remoteUrl);}
					}
				} catch (ClassNotFoundException e) {/* 不处理类找不到异常 */}
				return false;
			}
		}.scan(scanBasePackages);
		
		initConfig(remotes, remoteUrls); //初始化远程配置
		super.init(); //初始化日志信息
		for (Scanner scanner : context.getBeansOfType(Scanner.class).values()) {
			for (Class<?> scanClass : allClass) {scanner.scan(scanClass);} //最后自定义扫描
		}
	}
	
	/* 初始化配置 */
	private void initConfig(Map<Class<?>, Remote> remotes, Map<Class<?>, RemoteUrl> remoteUrls) {
		/* 必定是接口 */
		for (Entry<Class<?>, Remote> entry : remotes.entrySet()) {
			Set<Method> methodInfos = new HashSet<>();
			String key = getKey(entry.getKey(), methodInfos);
			/* 上报当前实现类 */
			for (String beanName : SpringUtil.getApplicationContext().getBeanNamesForType(entry.getKey())) {
				Class<?> curObjClass = ClassUtils.getUserClass(SpringUtil.getApplicationContext().getType(beanName, false));
				/* 保存远程类信息 */
				if (curObjClass != null) {
					PlugUtil.REMOTE_SAVE.save(key, setRemoteMethodDto(curObjClass, beanName, entry.getKey().getName(), methodInfos));
				}
			}
			
			/* 注册单例Bean */
			Object bean = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {entry.getKey()},
					new RemoteProxyHandler(entry.getKey(), entry.getValue().adapter()));
			register(bean, entry.getKey(), entry.getValue().beanName());
		}
		/* 必定是接口 */
		for (Entry<Class<?>, RemoteUrl> entry : remoteUrls.entrySet()) {
			/* 注册单例Bean */
			Object bean = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {entry.getKey()},
					new RemoteUrlHandler(entry.getKey(), entry.getValue()));
			register(bean, entry.getKey(), entry.getValue().beanName());
		}
	}
	
	@Override
	public RemoteMethodDto getRemoteMethod(Class<?> cls, Method method) {
		RemoteMethodDto remoteMethodDto = new RemoteMethodDto();
		/* 设置基础信息 */
		remoteMethodDto.setClassName(cls.getName());
		remoteMethodDto.setName(parse(method));
		
		/* 设置注解参数 */
		Map<String, Map<String, Object>> annotations = getAnnotations(method.getDeclaredAnnotations());
		RemoteMethod remoteMethod = method.getDeclaredAnnotation(RemoteMethod.class);
		if (remoteMethod != null) {annotations.put(RemoteMethod.class.getName(), getMetodAnnotation(cls, remoteMethod));}
		remoteMethodDto.setAnnotations(annotations);
		
		/* 添加远程方法入参 */
		return remoteMethodDto.setParameter(setParameter(method));
	}
	
	/* 设置方法信息 */
	private RemoteClassDto setRemoteMethodDto(Class<?> curObjClass, String beanName, String interfaceName, Set<Method> methodInfos) {
		Map<String, RemoteMethodDto> methods = new HashMap<>();
		for (Method method : methodInfos) { //处理所有远程方法
			RemoteMethodDto remoteMethodDto = getRemoteMethod(curObjClass, method);
			methods.put(remoteMethodDto.getName(), remoteMethodDto);
		}
		
		/* 添加远程类信息 */
		return new RemoteClassDto().setMethods(methods).setName(curObjClass.getName()).setBeanName(beanName)
				.setInterfaceName(interfaceName).setAnnotations(getAnnotations(curObjClass.getDeclaredAnnotations()));
	}
	/* 设置方法入参信息 */
	private Map<String, RemoteParameterDto> setParameter(Method method) {
		Map<String, RemoteParameterDto> ptms = new HashMap<>();
		/* 获取接口方法参数名 - 接口Spring的获取不了 */
		Parameter[] parameters = method.getParameters();
		for (Parameter parameter : parameters) {
			ptms.put(parameter.getName(), new RemoteParameterDto().setName(parameter.getName())
					.setAnnotations(getAnnotations(parameter.getAnnotations())));
		}
		return ptms;
	}
	/* 获取所有注解值 */
	private Map<String, Map<String, Object>> getAnnotations(Annotation[] annotations) {
		Map<String, Map<String, Object>> annotationVals = new HashMap<>();
		for (Annotation annotation : annotations) {
			annotationVals.put(annotation.annotationType().getName(), MyUtil.getAnnotationVal(annotation));
		}
		return annotationVals;
	}
	
	/* 获取唯一Key */
	private String getKey(Class<?> interfaceCls, Set<Method> methodInfos) {
		StringBuilder sb = new StringBuilder(interfaceCls.getName());
		for (Method method : interfaceCls.getMethods()) { //只处理公开方法 - 包括继承的公开方法
			RemoteMethod remoteMethod = method.getDeclaredAnnotation(RemoteMethod.class);
			/* 只拼接有远程方法注解的方法名 */
			if (remoteMethod != null && StringUtils.hasText(remoteMethod.name())) {
				String methodName = "-" + remoteMethod.name();
				Assert.isTrue(sb.indexOf(methodName) == -1, String.format("同一接口[%s]远程方法名[%s]重复！", interfaceCls.getName(), remoteMethod.name()));
				/* 追加并添加方法信息 */
				sb.append(methodName);
			}
			methodInfos.add(method);
		}
		return sb.toString();
	}
	
	/* 获取远程方法注解参数 */
	private Map<String, Object> getMetodAnnotation(Class<?> curObjClass, RemoteMethod remoteMethod) {
		Map<String, Object> methodAnnotation = MyUtil.getAnnotationVal(remoteMethod);
		String value = replaceConfig(curObjClass, SpringUtil.getEnvironment(), remoteMethod.value());
		methodAnnotation.put("value", value);
		return methodAnnotation;
	}
	
	/* 注册单例Bean */
	private void register(Object bean, Class<?> beanCls, String beanName) {
		ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) SpringUtil.getApplicationContext();
		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getAutowireCapableBeanFactory();
		/* 首字母小写 */
		beanFactory.registerSingleton(StringUtils.hasText(beanName) ? beanName : parseLower(beanCls), bean);
	}
}
