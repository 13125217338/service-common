package org.city.common.api.adapter.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.in.remote.RemoteSave.RemoteInfo;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2022-09-27 09:20:11
 * @版本 1.0
 * @描述 轮询选择适配器
 */
@Component
public class LoopAdapter implements RemoteAdapter {
	private final Map<Object, AtomicLong> SUM = new ConcurrentHashMap<>();
	
	@Override
	public RemoteInfo select(List<RemoteInfo> remoteInfos, Object param) {
		if (remoteInfos.size() == 0) {return null;}
		AtomicLong sum = SUM.computeIfAbsent(param == null ? LoopAdapter.class : param, k -> new AtomicLong());
		return remoteInfos.get((int) (sum.incrementAndGet() % remoteInfos.size()));
	}
}
