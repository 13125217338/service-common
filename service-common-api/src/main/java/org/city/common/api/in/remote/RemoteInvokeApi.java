package org.city.common.api.in.remote;

import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.dto.remote.RemoteMethodDto;

/**
 * @作者 ChengShi
 * @日期 2022-07-27 15:08:22
 * @版本 1.0
 * @描述 远程执行代码
 */
public interface RemoteInvokeApi {
	/**
	 * @描述 执行远程代码
	 * @param beanName 对应SpringBean名称
	 * @param remoteMethod 远程方法信息
	 * @param remoteIpPort 远程地址端口信息
	 * @param args 远程方法参数
	 * @return 执行返回值
	 */
	public Object invoke(String beanName, RemoteMethodDto remoteMethod, RemoteIpPortDto remoteIpPort, Object... args) throws Throwable;
}
