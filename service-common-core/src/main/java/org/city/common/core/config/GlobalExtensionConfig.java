package org.city.common.core.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.city.common.api.adapter.ExtensionIOAdapter;
import org.city.common.api.annotation.plug.GlobalExtension;
import org.city.common.api.annotation.plug.InitMethodCache;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.GlobalExtensionDto;
import org.city.common.api.dto.MethodCacheDto;
import org.city.common.api.dto.MethodCacheDto.ClassInfo;
import org.city.common.api.dto.MethodCacheDto.MethodInfo;
import org.city.common.api.in.GlobalConfig;
import org.city.common.api.in.ProxyType;
import org.city.common.api.in.Replace;
import org.city.common.api.util.MyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2022-06-24 14:23:08
 * @版本 1.0
 * @描述 全局扩展点配置
 */
@Slf4j
@Component
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class GlobalExtensionConfig implements GlobalConfig,Replace,ProxyType{
	@Autowired
	@Qualifier(CommonConstant.EXT_ADAPTER_USER_NAME)
	private ExtensionIOAdapter extensionIOAdapter;
	@Autowired
	private Environment environment;
	@Value("${spring.application.name}")
	private String zone;
	@Value("${server.port}")
	private int port;
	@Value("${spring.datasource.url:jdbc?serverTimezone=UTC}")
	private String timeZone;
	@PostConstruct
	private void setTimezone() {
		String timezoneId = "UTC";
		try {
			String[] params = timeZone.split("[?]")[1].split("[&]");
			for (String param : params) {
				if (param.contains("serverTimezone")) {
					timezoneId = param.split("=")[1].trim(); break;
				}
			}
		} catch (Exception e) {log.error("自定义设置FastJson时区失败！", e);}
		
		/* 全局配置FastJson */
		JSONObject.defaultTimeZone = TimeZone.getTimeZone(timezoneId);
		ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
	}
	
	/* 获取全局Beans定义 */
	private GlobalBeans getGlobalBeans(ApplicationContext applicationContext) {
		GlobalBeans beans = new GlobalBeans();
		/* 获取所有定义Bean名称 */
		String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
		for (String beanName : beanDefinitionNames) {
			Class<?> beanType = getType(applicationContext.getType(beanName, false));
			if (beanType != null) {
				
				/* 全局扩展点 - 必须有远程地址才能存放 */
				RequestMapping requestMapping = beanType.getDeclaredAnnotation(RequestMapping.class);
				if (requestMapping != null) {
					/*类上所有注解*/
					Annotation[] annotations = beanType.getDeclaredAnnotations();
					for (Annotation att : annotations) {
						
						/*获取自定义注解上的全局注解*/
						GlobalExtension gb = att.annotationType().getDeclaredAnnotation(GlobalExtension.class);
						if (gb != null) {
							beans.annotation.put(beanType, new GlobalAnnotation(beanName, gb, requestMapping, att));
							break;
						}
					}
				}
				
				/* 初始方法 */
				Method[] methods = beanType.getDeclaredMethods();
				for (Method method : methods) {
					InitMethodCache initMethodCache = method.getDeclaredAnnotation(InitMethodCache.class);
					if (initMethodCache != null) {
						
						/*初始方法缓存*/
						Map<Method, InitMethodCache> methodInit = beans.initAnnotation.get(beanType);
						if (methodInit == null) {
							methodInit = new HashMap<>();
							beans.initAnnotation.put(beanType, methodInit);
						}
						methodInit.put(method, initMethodCache);
					}
				}
			}
		}
		return beans;
	}
	
	@Override
	public ExtensionIOAdapter init(ApplicationContext applicationContext) throws Exception {
		try {
			String ipPort = MyUtil.getIpPort(port);
			log.info("开始移除当前分区扩展点：" + zone);
			/* 先移除已存储的Bean */
			extensionIOAdapter.removeBean(zone);
			
			/* 获取全局Beans定义 */
			GlobalBeans globalBeans = getGlobalBeans(applicationContext);
			
			/* 扫描扩展注解 */
			log.info("开始添加当前分区-扩展点个数：" + globalBeans.annotation.size());
			for (Entry<Class<?>, GlobalAnnotation> entry : globalBeans.annotation.entrySet()) {
				/*获取自定义注解所有值*/
				Map<String, Object> annotationVal = MyUtil.getAnnotationVal(entry.getValue().userAnnotation);
				if (annotationVal != null) {
					
					/*获取全局注解申明的唯一ID*/
					Object attFieldVal = annotationVal.get(entry.getValue().globalExtension.IdAs());
					if (!ObjectUtils.isEmpty(attFieldVal)) {
						attFieldVal = attFieldVal instanceof String ? attFieldVal : JSONObject.toJSONString(attFieldVal);
						/*动态替换key*/
						String attFieldValStr = replaceMethod(entry.getKey(), replaceConfig(environment, (String) attFieldVal));
						annotationVal.put(entry.getValue().globalExtension.IdAs(), attFieldValStr);
						
						/*获取对象所有接口参数*/
						Map<String, GlobalExtensionDto> param = getParam(attFieldValStr, entry.getKey(), entry.getValue(), annotationVal, ipPort);
						if (param.size() > 0) {extensionIOAdapter.saveBean(zone, entry.getValue().beanName, param);}
					}
				}
			}
			
			/* 扫描初始化方法缓存注解 */
			log.info("开始添加当前分区-初始化方法缓存信息个数：" + globalBeans.initAnnotation.size());
			for (Entry<Class<?>, Map<Method, InitMethodCache>> entry : globalBeans.initAnnotation.entrySet()) {
				Class<?> curObjClass = entry.getKey();
				/* 生成类信息 */
				ClassInfo classInfo = new ClassInfo();
				classInfo.setName(curObjClass.getName());
				classInfo.setAnnotations(getAnnotations(curObjClass.getDeclaredAnnotations()));
				
				/* 扫描类所有方法 */
				for (Entry<Method, InitMethodCache> method : entry.getValue().entrySet()) {
					Map<String, Object> initAnnotation = MyUtil.getAnnotationVal(method.getValue());
					String id = replaceMethod(curObjClass, replaceConfig(environment, method.getValue().id()));
					String value = replaceMethod(curObjClass, replaceConfig(environment, method.getValue().value()));
					initAnnotation.put("id", id); initAnnotation.put("value", value);
					
					/* 保存方法缓存信息 */
					extensionIOAdapter.saveInitMethod(id, getMethodCacheDto(method.getKey(), classInfo, initAnnotation));
				}
			}
		} catch (Exception e) {throw new RuntimeException(e);}
		return extensionIOAdapter;
	}
	/* 获取方法缓存 */
	private MethodCacheDto getMethodCacheDto(Method method, ClassInfo classInfo, Map<String, Object> initAnnotation) throws UnknownHostException {
		MethodCacheDto methodCacheDto = new MethodCacheDto();
		methodCacheDto.setIp(InetAddress.getLocalHost().getHostAddress());
		methodCacheDto.setPort(port);
		
		/* 方法信息 */
		MethodInfo methodInfo = new MethodInfo();
		methodInfo.setName(method.getName());
		Map<String, Map<String, Object>> annotations = getAnnotations(method.getDeclaredAnnotations());
		annotations.put(InitMethodCache.class.getName(), initAnnotation);
		methodInfo.setAnnotations(annotations);
		
		/* 添加方法类型 */
		methodInfo.$setParameterTypes(method.getGenericParameterTypes());
		methodInfo.$setExceptionTypes(method.getGenericExceptionTypes());
		methodInfo.$setReturnType(method.getGenericReturnType());
		
		/* 设置类与方法信息 */
		methodCacheDto.setClassInfo(classInfo);
		methodCacheDto.setMethodInfo(methodInfo);
		return methodCacheDto;
	}
	/* 获取所有注解值 */
	private Map<String, Map<String, Object>> getAnnotations(Annotation[] annotations) {
		Map<String, Map<String, Object>> annotationVals = new HashMap<>();
		for (Annotation annotation : annotations) {
			annotationVals.put(annotation.annotationType().getName(), MyUtil.getAnnotationVal(annotation));
		}
		return annotationVals;
	}
	
	/*获取对象所有接口参数*/
	private Map<String, GlobalExtensionDto> getParam(String id, Class<?> curObjClass, GlobalAnnotation globalAnnotation, Map<String, Object> annotationVal, String ipPort) throws Exception {
		Map<String, GlobalExtensionDto> param = new HashMap<>();
		/* key方法名，value方法本身 - 获取方法参数名称的方法不能是接口方法，只能是实现类 */
		Map<String, Method> nameMethod = Arrays.asList(curObjClass.getMethods()).stream().collect(Collectors.toMap(k -> k.getName(), v -> v, (v1, v2) -> v1));
		/*所有接口*/
		Class<?>[] interfaces = curObjClass.getInterfaces();
		
		for (Class<?> itf : interfaces) {
			Map<String, String> methodPath = new HashMap<>();
			Map<String, String[]> methodParamNames = new HashMap<>();
			/* 迭代接口方法 */
			for (Method method : itf.getMethods()) {
				
				/*只获取post路径*/
				PostMapping postMapping = method.getAnnotation(PostMapping.class);
				if (postMapping != null) {
					methodPath.put(method.getName(), getPsPath(postMapping));
				}
				
				/*获取参数真实名称*/
				LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
				/* 获取实现类方法的参数名称 */
				methodParamNames.put(method.getName(), discoverer.getParameterNames(nameMethod.get(method.getName())));
			}
			
			/* 扩展参数 */
			GlobalExtensionDto globalExtensionDto = new GlobalExtensionDto(extensionIOAdapter.getOnlyId(id, itf), getRqPath(globalAnnotation.requestMapping, ipPort),
					methodPath, methodParamNames, globalAnnotation.userAnnotation.annotationType().getName(), annotationVal, 0L);
			param.put(zone + ExtensionIOAdapter.MH + globalExtensionDto.getOnlyId(), globalExtensionDto);
		}
		return param;
	}
	
	/*获取路径地址*/
	private String getRqPath(RequestMapping requestMapping, String ipPort) throws UnknownHostException {
		String path = "";
		if (requestMapping.value().length > 0) {
			path = requestMapping.value()[0];
		}
		if (!StringUtils.hasText(path) && requestMapping.path().length > 0) {
			path = requestMapping.path()[0];
		}
		if (path.length() > 0) {
			path = path.startsWith("/") ? path : "/" + path;
		}
		return "http://" + ipPort + path;
	}
	
	/*获取路径地址*/
	private String getPsPath(PostMapping postMapping) {
		String path = "";
		if (postMapping.value().length > 0) {
			path = postMapping.value()[0];
		}
		if (!StringUtils.hasText(path) && postMapping.path().length > 0) {
			path = postMapping.path()[0];
		}
		if (path.length() > 0) {
			path = path.startsWith("/") ? path : "/" + path;
		}
		return path;
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2022-08-21 10:08:14
	 * @版本 1.0
	 * @parentClass GlobalExtensionConfig
	 * @描述 全局Bean对象
	 */
	private class GlobalBeans {
		/* 全局扩展点 */
		private final Map<Class<?>, GlobalAnnotation> annotation = new HashMap<>();
		/* 初始化方法 */
		private final Map<Class<?>, Map<Method, InitMethodCache>> initAnnotation = new HashMap<>();
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2022-08-21 10:46:48
	 * @版本 1.0
	 * @parentClass GlobalExtensionConfig
	 * @描述 全局扩展点对象
	 */
	@AllArgsConstructor
	private class GlobalAnnotation {
		/* 对应Bean名称 */
		private final String beanName;
		/* 全局扩展点注解 */
		private final GlobalExtension globalExtension;
		/* 请求路径注解 */
		private final RequestMapping requestMapping;
		/* 用户自定义注解 */
		private final Annotation userAnnotation;
	}
}
