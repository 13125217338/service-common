package org.city.common.api.in.remote;

import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.dto.remote.RemoteMethodDto;

/**
 * @作者 ChengShi
 * @日期 2022-07-27 15:22:27
 * @版本 1.0
 * @描述 远程方法执行超时
 */
public interface RemoteMethodTimeout {
	/**
	 * @描述 是否熔断处理
	 * @param remoteMethod 远程方法信息
	 * @param remoteIpPort 远程地址端口参数
	 * @param defaultFail 默认是否熔断
	 * @return true=熔断，false=不熔断
	 */
	public boolean isFail(RemoteMethodDto remoteMethod, RemoteIpPortDto remoteIpPort, boolean defaultFail);
}
