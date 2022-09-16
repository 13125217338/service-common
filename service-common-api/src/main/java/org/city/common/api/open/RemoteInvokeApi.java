package org.city.common.api.open;

import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.dto.remote.RemoteMethodDto;
import org.springframework.http.HttpHeaders;

/**
 * @作者 ChengShi
 * @日期 2022-07-27 15:08:22
 * @版本 1.0
 * @描述 远程执行代码
 */
public interface RemoteInvokeApi {
	/**
	 * @描述 执行远程代码
	 * @param data 远程参数
	 * @param headers 头信息
	 * @param remoteIpPortDto 远程地址端口信息
	 * @param args 远程执行参数
	 * @return 执行返回值
	 */
	public Object invoke(RemoteMethodDto data, HttpHeaders headers, RemoteIpPortDto remoteIpPortDto, Object... args);
}
