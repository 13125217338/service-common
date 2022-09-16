package org.city.common.api.dto.remote;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * @作者 ChengShi
 * @日期 2022年10月15日
 * @版本 1.0
 * @描述 远程配置参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "remote")
public class RemoteConfigDto {
	/* 是否执行远程 */
	private boolean invoke = true;
	/* 验证盐 */
	private String verify = "RemoteInvokeService-Verify";
	/* 连接时间 */
	private int connectTimeout = 12000;
	/* 读取时间 */
	private int readTimeout = 120000;
	/* 任务线程最大数量 */
	private int taskThread = 100;
}
