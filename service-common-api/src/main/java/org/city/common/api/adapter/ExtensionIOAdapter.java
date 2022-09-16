package org.city.common.api.adapter;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import org.city.common.api.dto.GlobalExtensionDto;
import org.city.common.api.dto.MethodCacheDto;

/**
 * @作者 ChengShi
 * @日期 2022-06-24 12:52:18
 * @版本 1.0
 * @描述 扩展点适配
 */
public interface ExtensionIOAdapter {
	/** 定时调度更新唯一ID */
	public final static String TIME_UPDATE_ID = "TIME_UPDATE_ID";
	/** 分割符 */
	public final static String MH = ":";
	
	/**
	 * @描述 获取唯一ID
	 * @param id 全局扩展点ID
	 * @param proxyInterface 需要获取的代理接口
	 * @return 唯一ID
	 */
	default String getOnlyId(String id, Class<?> proxyInterface) {
		return proxyInterface.getName() + MH + id;
	}
	/**
	 * @描述 获取实现者的系统时间
	 */
	public long getCurTime();
	/**
	 * @描述 获取已存在的ID全称
	 * @param onlyId 需要获取的唯一ID
	 * @return 返回已存在的ID全称，返回NULL代表不存在
	 */
	public String getId(String onlyId);
	
	/**
	 * @描述 移除已经存储的Bean
	 * @param zone 分区前缀名称
	 * @throws Exception
	 */
	public void removeBean(String zone) throws Exception;
	/**
	 * @描述 存储Bean到指定位置
	 * @param zone 分区前缀名称
	 * @param beanName 当前被扫描的Bean名称
	 * @param param 对象所有接口参数，key=ID全称（携带了zone分区前缀名称）
	 * @throws Exception
	 */
	public void saveBean(String zone, String beanName, Map<String, GlobalExtensionDto> param) throws Exception;
	
	/**
	 * @描述 获取已存在的扩展参数
	 * @param onlyId 需要获取的唯一ID
	 * @return 返回已存在的扩展参数，返回NULL代表不存在
	 */
	public GlobalExtensionDto getGlobalExtensionDto(String onlyId);
  	/**
	 * @描述 获取已存在的所有扩展参数
	 * @param proxyInterfaceName 需要获取的代理接口名称
	 * @return 返回已存在的所有扩展参数（不会为NULL）
	 */
	public List<GlobalExtensionDto> getGlobalExtensionDtos(String proxyInterfaceName);
	
	/**
	 * @描述 获取存储的Bean
	 * @param id 全局扩展点ID
	 * @param proxyInterface 需要获取的代理接口
	 * @return 对应Bean
	 */
	public <T> T getBean(String id, Class<T> proxyInterface);
	/**
	 * @描述 获取存储的集合Bean
	 * @param proxyInterface 需要获取的代理接口
	 * @return 对应Bean集合
	 */
	public <T> List<T> getAllBean(Class<T> proxyInterface);
	
	/**
	 * @描述 获取对应注解
	 * @param id 全局扩展点ID
	 * @param proxyInterface 需要获取的代理接口
	 * @param annotationClass 需要获取的注解
	 * @return 对应注解
	 */
	public <A extends Annotation> A getValue(String id, Class<?> proxyInterface, Class<A> annotationClass);
	/**
	 * @描述 获取对应注解集合
	 * @param proxyInterface 需要获取的代理接口
	 * @param annotationClass 需要获取的注解
	 * @return 对应注解集合
	 */
	public <A extends Annotation> List<A> getAllValue(Class<?> proxyInterface, Class<A> annotationClass);
	
	/**
	 * @描述 通过ID获取缓存初始方法信息
	 * @param id 缓存ID
	 * @return 缓存初始方法信息
	 */
	public MethodCacheDto getInitMethod(String id);
	/**
	 * @描述 存储初始化方法信息
	 * @param id 缓存ID
	 * @param methodCacheDto 缓存初始方法信息
	 */
	public void saveInitMethod(String id, MethodCacheDto methodCacheDto);
}
