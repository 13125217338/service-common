package org.city.common.api.dto.remote;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @作者 ChengShi
 * @日期 2022-09-27 09:24:04
 * @版本 1.0
 * @描述 远程地址端口参数（默认本地）
 */
@Setter
@Getter
@Component
@NoArgsConstructor
public class RemoteIpPortDto {
	/* 地址 */
	private String ip;
	/* 端口 */
	@Value("${server.port}")
	private Integer port;
	@PostConstruct
	private void init() throws UnknownHostException {ip = Inet4Address.getLocalHost().getHostAddress();}
	
	@Override
	public String toString() {
		Assert.hasText(ip, "远程ip地址不能为空！");
		Assert.notNull(port, "远程port端口不能为空！");
		return ip + ":" + port;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {return false;}
		return this.toString().equals(obj.toString());
	}
	
	/**
	 * @param ipPort 分号隔开的信息
	 */
	public RemoteIpPortDto(String ipPort) {
		String[] ipPorts = ipPort.split(":");
		this.ip = ipPorts[0];
		this.port = Integer.parseInt(ipPorts[1]);
	}
}
