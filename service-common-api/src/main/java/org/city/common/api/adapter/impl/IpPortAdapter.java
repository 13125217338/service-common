package org.city.common.api.adapter.impl;

import java.util.List;
import java.util.Optional;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.adapter.RemoteSaveAdapter.RemoteInfo;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2022-10-08 10:02:28
 * @版本 1.0
 * @描述 地址端口选择
 */
@Component
public class IpPortAdapter implements RemoteAdapter {
	@Override
	public RemoteInfo select(List<RemoteInfo> invokes, Object param) {
		Optional<RemoteInfo> findFirst = invokes.stream().filter(v -> v.getRemoteIpPortDto().equals(param)).findFirst();
		return findFirst.orElse(null);
	}
}
