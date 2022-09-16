package org.city.common.api.in.remote;

import org.city.common.api.dto.remote.RemoteIpPortDto;

/**
 * @作者 ChengShi
 * @日期 2023-04-06 14:12:08
 * @版本 1.0
 * @描述 远程服务上线接口
 */
public interface RemoteOnline {
	/**
	 * @描述 服务上线通知
	 * @param remoteIpPortDto 上线的服务
	 */
	public void online(RemoteIpPortDto remoteIpPortDto);
}
