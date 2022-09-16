package org.city.common.api.adapter.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.in.remote.RemoteSave.RemoteInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2023年5月7日
 * @版本 1.0
 * @描述 最短时间选择适配器
 */
@Component
public class TimeAdapter implements RemoteAdapter {
	private final Map<String, Long> timeSum = new ConcurrentHashMap<>();
	private final AtomicInteger atomicInteger = new AtomicInteger();
	@Autowired
	private RemoteConfigDto remoteConfigDto;
	@Autowired
	private LoopAdapter loopAdapter;
	
	@Override
	public RemoteInfo select(List<RemoteInfo> invokes, Object param) {
		/* 到达最大清除数时清除计时缓存 */
		if(atomicInteger.incrementAndGet() % remoteConfigDto.getClearSum() == 0) {timeSum.clear();}
		
		/* 取执行时间最小的远程信息 */
		RemoteInfo curSelect = null; long minTime = Long.MAX_VALUE;
		for (RemoteInfo remoteInfo : invokes) {
			Long tm = timeSum.get(remoteInfo.getRemoteIpPortDto().toString());
			if (tm == null) {
				if (curSelect == null) {curSelect = loopAdapter.select(invokes, param);}
				continue;
			
			/* 通过比较时间来取最小 */
			} else if(tm < minTime) {
				curSelect = remoteInfo;
				minTime = tm;
			}
		}
		return curSelect;
	}
	
	@Override
	public void invokedInfo(int invokedTime, RemoteInfo remoteInfo) {
		Long tm = timeSum.get(remoteInfo.getRemoteIpPortDto().toString());
		tm = tm == null ? invokedTime : tm + invokedTime;
		timeSum.put(remoteInfo.getRemoteIpPortDto().toString(), tm);
	}
}
