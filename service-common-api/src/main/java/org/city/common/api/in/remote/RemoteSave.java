package org.city.common.api.in.remote;

import java.util.List;
import java.util.Map;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.dto.remote.RemoteClassDto;
import org.city.common.api.dto.remote.RemoteIpPortDto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-09-27 14:20:44
 * @版本 1.0
 * @描述 远程调用保存
 */
public interface RemoteSave {
	/**
	 * @描述 初始化操作
	 */
	public void init();
	
	/**
	 * @描述 自定义保存远程类信息（只会在初始化时执行）
	 * @param key 由接口类加方法生成的唯一ID（可被接口或方法名范围查询）
	 * @param remoteClassDto 远程实现类信息
	 */
	public void save(String key, RemoteClassDto remoteClassDto);
	
	/**
	 * @描述 刷新缓存操作
	 */
	public void flush();
	
	/**
	 * @描述 获取自定义保存的远程类信息（每次调用都会执行）
	 * @param interfaceCls 接口对应类型
	 * @return 远程对象信息
	 */
	public List<RemoteInfo> get(Class<?> interfaceCls);
	/**
	 * @描述 获取自定义保存的远程类信息（每次调用都会执行）
	 * @param methodName 方法名称
	 * @return 远程对象信息
	 */
	public List<RemoteInfo> get(String methodName);
	
	/**
	 * @描述 通过范围Key获取远程类信息（远程调用）
	 * @param scanKey 范围Key
	 * @return key=Redis拼接的save对应Key，value=《key=RemoteIpPortDto，value=远程类信息》
	 */
	public Map<String, Map<String, List<RemoteClassDto>>> getScopeRemote(String scanKey);
	
	/**
	 * @作者 ChengShi
	 * @日期 2022-09-27 18:07:23
	 * @版本 1.0
	 * @parentClass RemoteSaveAdapter
	 * @描述 远程信息
	 */
	@Data
	@Accessors(chain = true)
	public static class RemoteInfo {
		/* 当前对象 */
		private Object bean;
		/* 远程调用接通时间点 */
		private long turnOnTime;
		/* 远程方法调用限流 */
		private final int speedLimit;
		/* 远程方法参数 */
		private final RemoteClassDto remoteClassDto;
		/* 远程地址端口信息 */
		private final RemoteIpPortDto remoteIpPortDto;
		/* 远程调用适配器 */
		private RemoteAdapter remoteAdapter;
	}
}
