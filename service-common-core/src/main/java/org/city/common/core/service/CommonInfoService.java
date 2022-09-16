package org.city.common.core.service;

import java.lang.management.ManagementFactory;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.AuthResultDto.AuthMethod;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.dto.remote.RemoteServerInfoDto;
import org.city.common.api.in.redis.RedisMake;
import org.city.common.api.in.remote.RemoteOnline;
import org.city.common.api.in.remote.RemoteSave;
import org.city.common.api.open.CommonInfoApi;
import org.city.common.core.scanner.AuthScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sun.management.OperatingSystemMXBean;

import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2023年6月26日
 * @版本 1.0
 * @描述 公共信息服务
 */
@Slf4j
@Service
public class CommonInfoService implements CommonInfoApi {
	@Autowired
	private RemoteIpPortDto localIpPort;
	@Autowired
	private RemoteSave remoteSave;
	@Autowired
	private AuthScanner authScanner;
	@Autowired
	private RedisMake redisMake;
	@Autowired(required = false)
	private List<RemoteOnline> remoteOnlines;
	/* 远程验证信息 */
	private final Map<String, String> REMOTE_AUTH = new ConcurrentHashMap<>();
	/* 内存映射 */
	private final Map<String, Integer> MEMORY = getMemory();
	/* 时间映射 */
	private final Map<String, Integer> TIME = getTime();
	/* 获取内存映射 */
	private LinkedHashMap<String, Integer> getMemory() {
		LinkedHashMap<String, Integer> memory = new LinkedHashMap<String, Integer>();
		memory.put("B", 1024); //字节
		memory.put("KB", 1024); //千字节
		memory.put("MB", 1024); //兆字节
		memory.put("GB", 1024); //吉字节
		memory.put("TB", 1024); //太字节
		return memory;
	}
	/* 获取时间映射 */
	private LinkedHashMap<String, Integer> getTime() {
		LinkedHashMap<String, Integer> time = new LinkedHashMap<String, Integer>();
		time.put("ms", 1000); //毫秒
		time.put("s", 60); //秒
		time.put("m", 60); //分钟
		time.put("h", 24); //小时
		time.put("d", 30); //月
		time.put("M", 12); //年
		time.put("y", 100); //世纪 - 没有
		return time;
	}
	
	@Override
	public RemoteIpPortDto getIpPort() {return localIpPort;}
	
	@Override
	public synchronized void online(RemoteIpPortDto remoteIpPortDto) {
		if (!localIpPort.equals(remoteIpPortDto)) { //自己服务不用上线
			log.info("<------------ 当前服务[{}]已上线 ------------>", remoteIpPortDto);
			REMOTE_AUTH.remove(remoteIpPortDto.toString()); //移除远程验证信息重新拉取
			remoteSave.flush(); //刷新缓存操作
		}
		
		/* 回调自定义上线通知接口 */
		if (!CollectionUtils.isEmpty(remoteOnlines)) {
			for (RemoteOnline remoteOnline : remoteOnlines) {
				remoteOnline.online(remoteIpPortDto);
			}
		}
	}
	
	@Override
	public Set<AuthMethod> getAllAuthMethod() {return authScanner.getAuthMethods();}
	
	@Override
	@SuppressWarnings("all")
	public RemoteServerInfoDto getServerInfo() {
		RemoteServerInfoDto remoteServerInfoDto = new RemoteServerInfoDto();
		NumberFormat instance = NumberFormat.getInstance();
		instance.setMaximumFractionDigits(2);
		OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		
		/* 系统参数 */
		remoteServerInfoDto.setPhysicalCpuTotal(operatingSystemMXBean.getAvailableProcessors());
		remoteServerInfoDto.setPhysicalCpuUse(instance.format(operatingSystemMXBean.getSystemCpuLoad() * 100) + "%");
		remoteServerInfoDto.setPhysicalTotalMemory(parse(operatingSystemMXBean.getTotalPhysicalMemorySize(), MEMORY));
		remoteServerInfoDto.setPhysicalFreeMemory(parse(operatingSystemMXBean.getFreePhysicalMemorySize(), MEMORY));
		remoteServerInfoDto.setPhysicalTotalSwapMemory(parse(operatingSystemMXBean.getTotalSwapSpaceSize(), MEMORY));
		remoteServerInfoDto.setPhysicalFreeSwapMemory(parse(operatingSystemMXBean.getFreeSwapSpaceSize(), MEMORY));
		remoteServerInfoDto.setSubmitVirtualMemory(parse(operatingSystemMXBean.getCommittedVirtualMemorySize(), MEMORY));
		
		/* java参数 */
		remoteServerInfoDto.setJavaCpuUse(instance.format(operatingSystemMXBean.getProcessCpuLoad() * 100) + "%");
		remoteServerInfoDto.setJavaRuntime(parse(System.currentTimeMillis() - CommonConstant.START_TIME, TIME));
		Runtime runtime = Runtime.getRuntime();
		remoteServerInfoDto.setJavaMaxMemory(parse(runtime.maxMemory(), MEMORY));
		remoteServerInfoDto.setJavaFreeMemory(parse(runtime.freeMemory(), MEMORY));
		remoteServerInfoDto.setJavaTotalMemory(parse(runtime.totalMemory(), MEMORY));
		return remoteServerInfoDto;
	}
	
	/**
	 * @描述 获取远程验证唯一值
	 * @param remoteIpPort 远程服务IP端口
	 * @return 远程验证唯一值
	 */
	public String getRemoteAuthValue(String remoteIpPort) {
		return REMOTE_AUTH.computeIfAbsent(remoteIpPort, (k) -> {
			return redisMake.getHValue(CommonConstant.REDIS_REMOTE_AUTH_HKEY, k, String.class);
		});
	}
	
	/* 转换展示 */
	private String parse(long dt, Map<String, Integer> datas) {
		StringBuilder sb = new StringBuilder();
		long lastData = dt; String lastKey = null;
		for(Entry<String, Integer> entry : datas.entrySet()) {
			lastData = dt; lastKey = entry.getKey();
			/* 计算 */
			long data = dt / entry.getValue();
			if (data == 0) {break;}
			else {
				dt = data;
				long d = lastData % entry.getValue();
				if (d != 0) {sb.insert(0, "-" + d + lastKey);}
			}
		}
		return sb.insert(0, lastData + lastKey).toString();
	}
}
