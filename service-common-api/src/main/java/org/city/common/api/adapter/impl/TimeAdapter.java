package org.city.common.api.adapter.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.dto.remote.RemoteTimeAdapterDto;
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
	private final Map<String, RemoteTimeAdapterDto> TIME_SUM = new ConcurrentHashMap<>();
	@Autowired
	private RemoteConfigDto remoteConfigDto;
	@Autowired
	private LoopAdapter loopAdapter;
	
	@Override
	public RemoteInfo select(List<RemoteInfo> remoteInfos, Object param) {
		RemoteInfo curSelect = null; long minTime = Long.MAX_VALUE;
		for (RemoteInfo remoteInfo : remoteInfos) { //取执行时间最小的远程信息
			RemoteTimeAdapterDto timeSum = TIME_SUM.computeIfAbsent(remoteInfo.getRemoteClassDto().getInterfaceName(), k -> {
				int clearSum = remoteConfigDto.getClearSum() < (remoteInfos.size() << 1) ? (remoteInfos.size() << 1) : remoteConfigDto.getClearSum();
				return new RemoteTimeAdapterDto().setClearSum(clearSum); //设置重新计时阈值
			});
			final Long recordTime = timeSum.getRecordTime().get(remoteInfo.getRemoteIpPortDto().toString());
			if (recordTime == null) {return loopAdapter.select(remoteInfos, remoteInfo.getRemoteClassDto().getInterfaceName());} //无时间默认就是最小 - 多线程下需轮询适配
			else if(recordTime.longValue() < minTime) {curSelect = remoteInfo; minTime = recordTime.longValue();} //通过比较时间来取最小
		}
		return curSelect;
	}
	
	@Override
	public void invokedInfo(int invokedTime, RemoteInfo remoteInfo) {
		RemoteTimeAdapterDto timeSum = TIME_SUM.get(remoteInfo.getRemoteClassDto().getInterfaceName());
		if (timeSum != null) {
			Long recordTime = timeSum.getRecordTime().get(remoteInfo.getRemoteIpPortDto().toString());
			recordTime = recordTime == null ? invokedTime : (recordTime.longValue() + invokedTime);
			timeSum.getRecordTime().put(remoteInfo.getRemoteIpPortDto().toString(), recordTime); //记录执行时间
		}
		
		if ((timeSum.getCountSum().incrementAndGet() % timeSum.getClearSum()) == 0) {
			TIME_SUM.remove(remoteInfo.getRemoteClassDto().getInterfaceName()); //重新计时
		}
	}
}
