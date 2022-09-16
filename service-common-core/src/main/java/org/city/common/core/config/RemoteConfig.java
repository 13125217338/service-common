package org.city.common.core.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.city.common.api.adapter.RemoteSaveAdapter;
import org.city.common.api.annotation.plug.Remote;
import org.city.common.api.annotation.plug.RemoteMethod;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.remote.RemoteClassDto;
import org.city.common.api.dto.remote.RemoteMethodDto;
import org.city.common.api.dto.remote.RemoteParameterDto;
import org.city.common.api.in.GlobalConfig;
import org.city.common.api.in.ProxyType;
import org.city.common.api.in.Replace;
import org.city.common.api.in.parse.FirstCharParse;
import org.city.common.api.in.parse.MethodNameParse;
import org.city.common.api.util.MyUtil;
import org.city.common.core.handler.RemoteProxyHandler;
import org.city.common.core.handler.RemoteProxyHandler.RemoteProxyInfo;
import org.city.common.core.scanner.RemoteScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @作者 ChengShi
 * @日期 2022-09-28 10:46:34
 * @版本 1.0
 * @描述 远程调用配置
 */
@Component
@Import(RemoteConfig.class)
public class RemoteConfig extends AppenderConfig implements GlobalConfig,ImportBeanDefinitionRegistrar,Replace,ProxyType,FirstCharParse,MethodNameParse{
	private static Map<Class<?>, Remote> scanClasses = new HashMap<>();
	@Autowired
	@Qualifier(CommonConstant.REMOTE_ADAPTER_USER_NAME)
	private RemoteSaveAdapter remoteSaveAdapter;
	@Autowired
	private Environment environment;
	
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		String[] scanPackage = null;
		for (String name : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(name);
			/* 通过扫描获取入口类注解值 */
			if (beanDefinition instanceof AbstractBeanDefinition) {
				Class<?> beanClass = ((AbstractBeanDefinition) beanDefinition).getBeanClass();
				if (beanClass.isAnnotationPresent(SpringBootApplication.class)) {
					scanPackage = beanClass.getDeclaredAnnotation(SpringBootApplication.class).scanBasePackages();
					break;
				}
			}
		}
		
		Assert.notEmpty(scanPackage, "未找到启动入口类@SpringBootApplication注解对应scanBasePackages值，请检查注解参数对应值！");
		/* 自定义扫描，只存放含有Remote注解的接口 */
		RemoteScanner remoteScanner = new RemoteScanner(registry, scanClasses);
		remoteScanner.scan(scanPackage);
	}
	
	@Override
	public RemoteSaveAdapter init(ApplicationContext applicationContext) throws Exception {
		/* 初始化日志信息 */
		super.init();
		/* 必定是接口 */
		for (Entry<Class<?>, Remote> entry : scanClasses.entrySet()) {
			Map<Method, RemoteMethod> methodInfos = new HashMap<>();
			String key = getKey(entry.getKey(), methodInfos);
			/* 上报当前实现类 */
			for (String name : applicationContext.getBeanNamesForType(entry.getKey())) {
				Class<?> curObjClass = getType(applicationContext.getType(name, false));
				/* 保存远程类信息 */
				remoteSaveAdapter.save(key, setRemoteMethodDto(curObjClass, name, entry.getKey().getName(), methodInfos), name);
			}
			
			/* 注册单例Bean */
			Object bean = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {entry.getKey()},
					new RemoteProxyHandler(new RemoteProxyInfo(entry.getKey(), entry.getValue().adapter())));
			register(applicationContext, bean, entry.getKey(), entry.getValue().value());
		}
		RemoteConfig.scanClasses = null;
		return remoteSaveAdapter;
	}
	
	/* 设置方法信息 */
	private RemoteClassDto setRemoteMethodDto(Class<?> curObjClass, String beanName, String interfaceName, Map<Method, RemoteMethod> methodInfos) {
		Map<String, RemoteMethodDto> methods = new HashMap<>();
		for (Entry<Method, RemoteMethod> entry : methodInfos.entrySet()) {
			RemoteMethodDto remoteMethodDto = new RemoteMethodDto();
			
			/* 设置基础信息 */
			remoteMethodDto.setBeanName(beanName);
			remoteMethodDto.setName(parse(entry.getKey()));
			
			/* 设置注解参数 */
			Map<String, Map<String, Object>> annotations = getAnnotations(entry.getKey().getDeclaredAnnotations());
			annotations.put(RemoteMethod.class.getName(), entry.getValue() == null ? null : getMetodAnnotation(curObjClass, entry.getValue()));
			remoteMethodDto.setAnnotations(annotations);
			
			/* 添加方法类型 */
			remoteMethodDto.$setParameterTypes(entry.getKey().getGenericParameterTypes());
			remoteMethodDto.$setExceptionTypes(entry.getKey().getGenericExceptionTypes());
			remoteMethodDto.$setReturnType(entry.getKey().getGenericReturnType());
			
			/* 添加方法入参 */
			remoteMethodDto.setParameter(setParameter(entry.getKey()));
			
			/* 存储 */
			methods.put(remoteMethodDto.getName(), remoteMethodDto);
		}
		return new RemoteClassDto().setMethods(methods).setName(curObjClass.getName()).setInterfaceName(interfaceName)
				.setAnnotations(getAnnotations(curObjClass.getDeclaredAnnotations()));
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
	private String getKey(Class<?> interfaceCls, Map<Method, RemoteMethod> methodInfos) {
		StringBuilder sb = new StringBuilder(interfaceCls.getName());
		Method[] methods = interfaceCls.getDeclaredMethods();
		for (Method method : methods) {
			RemoteMethod remoteMethod = method.getDeclaredAnnotation(RemoteMethod.class);
			/* 只拼接有远程方法注解的方法名 */
			if (remoteMethod != null && StringUtils.hasText(remoteMethod.name())) {
				String methodName = RemoteSaveAdapter.REMOTE_KEY_SPLITE + remoteMethod.name();
				Assert.isTrue(sb.indexOf(methodName) == -1, String.format("同一接口[%s]远程方法名[%s]重复！", interfaceCls.getName(), remoteMethod.name()));
				/* 追加并添加方法信息 */
				sb.append(methodName);
				methodInfos.put(method, remoteMethod);
			} else {methodInfos.put(method, null);}
		}
		return sb.toString();
	}
	
	/* 获取远程方法注解参数 */
	private Map<String, Object> getMetodAnnotation(Class<?> curObjClass, RemoteMethod remoteMethod) {
		Map<String, Object> methodAnnotation = MyUtil.getAnnotationVal(remoteMethod);
		String value = replaceMethod(curObjClass, replaceConfig(environment, remoteMethod.value()));
		methodAnnotation.put("value", value);
		return methodAnnotation;
	}

	/* 注册单例Bean */
	private void register(ApplicationContext applicationContext, Object bean, Class<?> beanCls, String beanName) {
		ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getAutowireCapableBeanFactory();
		/* 首字母小写 */
		beanFactory.registerSingleton(StringUtils.hasText(beanName) ? beanName : parseLower(beanCls), bean);
	}
}
