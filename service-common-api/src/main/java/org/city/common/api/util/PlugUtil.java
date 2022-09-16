package org.city.common.api.util;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.annotation.plug.RemoteMethod;
import org.city.common.api.dto.remote.RemoteClassDto;
import org.city.common.api.dto.remote.RemoteMethodDto;
import org.city.common.api.exception.ServiceNotFoundException;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.parse.MethodNameParse;
import org.city.common.api.in.remote.RemoteSave;
import org.city.common.api.in.remote.RemoteSave.RemoteInfo;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2022-06-24 12:48:59
 * @版本 1.0
 * @描述 插件工具
 */
@Slf4j
public final class PlugUtil {
	public final static RemoteSave REMOTE_SAVE = SpringUtil.getBean(RemoteSave.class);
	private PlugUtil() {}
	
	/**
	 * @描述 获取指定远程信息
	 * @param param 自定义参数
	 * @param remoteAdapter 远程调用选择适配
	 * @param interfaceCls 对象的接口
	 * @return 远程信息
	 */
	public static RemoteInfo getRemote(Object param, RemoteAdapter remoteAdapter, Class<?> interfaceCls) {
		RemoteInfo select = remoteAdapter.select(REMOTE_SAVE.get(interfaceCls), param);
		if (select == null) {
			log.error("接口[{}]使用参数[{}]的选择器[{}]未选择任何实现类！", interfaceCls.getName(), param, remoteAdapter.getClass().getName());
			throw new ServiceNotFoundException(param, remoteAdapter, interfaceCls);
		}
		return select.setRemoteAdapter(remoteAdapter);
	}
	/**
	 * @描述 通过指定远程信息执行方法获取返回值
	 * @param <P> 接口对象
	 * @param <R> 返回值
	 * @param param 自定义参数
	 * @param remoteAdapter 远程调用选择适配
	 * @param interfaceCls 对象的接口
	 * @param process 对象接口执行器
	 * @return 返回值（禁用的远程会再调用一次）
	 */
	@SuppressWarnings("unchecked")
	public static <P, R> R invoke(Object param, RemoteAdapter remoteAdapter, Class<P> interfaceCls, ClassProcess<P, R> process) {
		RemoteInfo remote = getRemote(param, remoteAdapter, interfaceCls);
		try {return process.invoke((P) remote.getBean());}
		catch (Throwable e) {if (remote.getTurnOnTime() > System.currentTimeMillis()) {return invoke(param, remoteAdapter, interfaceCls, process);} else {throw e;}}
	}
	/**
	 * @描述 获取所有远程信息
	 * @param interfaceCls 对象的接口
	 * @return 所有远程信息
	 */
	public static List<RemoteInfo> getRemotes(Class<?> interfaceCls) {
		return REMOTE_SAVE.get(interfaceCls);
	}
	
	/**
	 * @描述 获取指定接口对象
	 * @param <T> 接口对象
	 * @param param 自定义参数
	 * @param remoteAdapter 远程调用选择适配
	 * @param interfaceCls 对象的接口
	 * @return 接口对象
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getBean(Object param, RemoteAdapter remoteAdapter, Class<T> interfaceCls) {
		return (T) getRemote(param, remoteAdapter, interfaceCls).getBean();
	}
	/**
	 * @描述 获取所有接口对象
	 * @param <T> 接口对象
	 * @param interfaceCls 对象的接口
	 * @return 所有接口对象
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getBeans(Class<T> interfaceCls) {
		return getRemotes(interfaceCls).stream().map(v -> (T) v.getBean()).collect(Collectors.toList());
	}
	
	/**
	 * @描述 获取指定远程类信息
	 * @param param 自定义参数
	 * @param remoteAdapter 远程调用选择适配
	 * @param interfaceCls 对象的接口
	 * @return 远程类信息
	 */
	public static RemoteClassDto getValue(Object param, RemoteAdapter remoteAdapter, Class<?> interfaceCls) {
		return getRemote(param, remoteAdapter, interfaceCls).getRemoteClassDto();
	}
	/**
	 * @描述 获取所有远程类信息
	 * @param interfaceCls 对象的接口
	 * @return 所有远程类信息
	 */
	public static List<RemoteClassDto> getValues(Class<?> interfaceCls) {
		return getRemotes(interfaceCls).stream().map(v -> v.getRemoteClassDto()).collect(Collectors.toList());
	}
	
	/**
	 * @描述 获取指定方法执行器
	 * @param param 自定义参数
	 * @param remoteAdapter 远程调用选择适配
	 * @param methodName 方法名称
	 * @return 方法执行器
	 */
	public static MethodProcess getMethod(Object param, RemoteAdapter remoteAdapter, String methodName) {
		RemoteInfo select = remoteAdapter.select(REMOTE_SAVE.get(methodName), param);
		Assert.notNull(select, String.format("方法[%s]使用参数[%s]的选择器[%s]返回NULL未选择任何实现类！", methodName, param, remoteAdapter.getClass().getName()));
		/* 取出相应参数 */
		RemoteMethodDto remoteMethodDto = select.setRemoteAdapter(remoteAdapter).getRemoteClassDto().getMethods().values().stream()
			.filter(v -> equalsRemoteMethod(v, methodName)).findFirst().get();
		RemoteClassDto remoteClassDto = select.getRemoteClassDto();
		return new MethodProcess(select.getBean(), remoteClassDto.getInterfaceName(), remoteClassDto.getName(), remoteMethodDto);
	}
	/* 对比远程方法值 */
	private static boolean equalsRemoteMethod(RemoteMethodDto remoteMethodDto, String methodName) {
		RemoteMethod remoteMethod = remoteMethodDto.getAnnotation(RemoteMethod.class);
		return remoteMethod == null ? false : (remoteMethod.name().equals(methodName));
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2022-10-07 15:27:14
	 * @版本 1.0
	 * @parentClass PlugUtil
	 * @描述 方法执行器
	 */
	@Data
	public static class MethodProcess implements JSONParser,MethodNameParse{
		/* 实现对象 */
		private final Object bean;
		/* 实现接口 */
		private final String interfaceName;
		/* 实现类 */
		private final String beanClass;
		/* 远程方法信息 */
		private final RemoteMethodDto remoteMethodDto;
		
		/**
		 * @描述 执行方法
		 * @param args 方法入参
		 * @return 执行结果
		 */
		public final Object invoke(Object...args){
			Method curMethod = null;
			Class<?>[] interfaces = bean.getClass().getInterfaces();
			flag:
			for (Class<?> interfaceCls : interfaces) {
				/* 找接口 */
				if (interfaceCls.getName().equals(interfaceName)) {
					Method[] declaredMethods = interfaceCls.getDeclaredMethods();
					/* 找方法 */
					for (Method method : declaredMethods) {
						if (parse(method).equals(remoteMethodDto.getName())) {
							curMethod = method; break flag;
						}
					}
				}
			}
			Assert.notNull(curMethod, String.format("根据接口[%s]对应方法名[%s]未找到方法！", interfaceName, remoteMethodDto.getName()));
			
			/* 转换接口入参调用时的类型 - 传入是Object所以类型可能对不上 */
			Type[] types = curMethod.getGenericParameterTypes();
			for (int i = 0, j = types.length; i < j; i++) {args[i] = parse(args[i], types[i]);}
			/* 执行方法 */
			return ReflectionUtils.invokeMethod(curMethod, bean, args);
		}
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2022年12月3日
	 * @版本 1.0
	 * @描述 执行回调
	 */
	public interface ClassProcess<P, R> {
		/**
		 * @描述 待执行方法
		 * @param process 执行器
		 * @return 执行结果
		 */
		public R invoke(P process);
	}
}
