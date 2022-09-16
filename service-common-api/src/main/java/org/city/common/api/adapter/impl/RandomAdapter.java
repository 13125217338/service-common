package org.city.common.api.adapter.impl;

import java.util.List;
import java.util.Random;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.in.remote.RemoteSave.RemoteInfo;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2023年5月7日
 * @版本 1.0
 * @描述 随机选择适配器
 */
@Component
public class RandomAdapter implements RemoteAdapter {
	private final Random random = new Random();
	
	@Override
	public RemoteInfo select(List<RemoteInfo> invokes, Object param) {
		if (invokes.size() == 0) {return null;}
		int index = random.nextInt(invokes.size());
		return invokes.get(index);
	}
}
