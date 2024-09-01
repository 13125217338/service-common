package org.city.common.api.adapter.impl;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
	private final AtomicInteger atomicInteger = new AtomicInteger();
	
	@Override
	public RemoteInfo select(List<RemoteInfo> invokes, Object param) {
		if (invokes.size() == 0) {return null;}
		int index = atomicInteger.incrementAndGet() %  invokes.size();
		return invokes.get(index);
	}
}
