package org.city.common.api.adapter.impl;

import java.util.List;
import java.util.Optional;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.in.remote.RemoteSave.RemoteInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2022-10-08 10:02:28
 * @版本 1.0
 * @描述 地址端口选择适配器
 */
@Component
public class IpPortAdapter implements RemoteAdapter {
	@Autowired
	private RemoteIpPortDto localIpPort;
	
	@Override
	public RemoteInfo select(List<RemoteInfo> invokes, Object param) {
		if (invokes.size() == 0) {return null;}
		Optional<RemoteInfo> findFirst = invokes.stream().filter(v -> v.getRemoteIpPortDto().equals(param == null ? localIpPort : param)).findFirst();
		return findFirst.orElse(null);
	}
}
