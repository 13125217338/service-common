package org.city.common.api.dto.remote;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
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
	private String verify = "3fea7df4a746702baf96495999398c598f61b74827db7d579ea97f36c1b1e5b2";
	/* 连接时间（毫秒） */
	private int connectTimeout = 1000 * 60;
	/* 读取时间（毫秒） */
	private int readTimeout = 1000 * 120;
	/* 任务线程最大数量 */
	private int taskThread = 100;
	/* 最短时间选择适配器 - 达到该值后远程请求重新计时 */
	private int clearSum = 60;
	/* 令牌过期时间（毫秒） */
	private long tokenExpireTime = 1000 * 60 * 60 * 24;
	/* 成功响应消息内容 */
	private String msg = HttpStatus.OK.getReasonPhrase();
	/* 响应内容是否写空值 */
	private boolean writeNull = false;
}
