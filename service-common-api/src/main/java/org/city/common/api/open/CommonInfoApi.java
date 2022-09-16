package org.city.common.api.open;

import java.util.Set;

import org.city.common.api.annotation.plug.Remote;
import org.city.common.api.dto.AuthResultDto.AuthMethod;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.dto.remote.RemoteServerInfoDto;

/**
 * @作者 ChengShi
 * @日期 2023年6月26日
 * @版本 1.0
 * @描述 公共信息Api
 */
@Remote
public interface CommonInfoApi {
	/**
	 * @描述 获取当前服务IP端口
	 * @return 当前服务IP端口
	 */
	public RemoteIpPortDto getIpPort();
	
	/**
	 * @描述 服务上线通知
	 * @param remoteIpPortDto 上线的服务
	 */
	public void online(RemoteIpPortDto remoteIpPortDto);
	
	/**
	 * @描述 获取当前服务所有验证方法
	 * @return 当前服务所有验证方法
	 */
	public Set<AuthMethod> getAllAuthMethod();
	
	/**
	 * @描述 获取当前服务器信息
	 * @return 当前服务器信息
	 */
	public RemoteServerInfoDto getServerInfo();
}
