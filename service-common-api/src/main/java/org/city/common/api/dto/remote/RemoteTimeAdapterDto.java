package org.city.common.api.dto.remote;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-09-27 09:25:23
 * @版本 1.0
 * @描述 最短时间选择适配器参数
 */
@Data
@Accessors(chain = true)
public class RemoteTimeAdapterDto {
	/* 记录时间 */
	private Map<String, Long> recordTime = new HashMap<String, Long>();
	/* 统计数量 */
	private AtomicLong countSum = new AtomicLong();
	/* 达到该值后远程请求重新计时 */
	private int clearSum;
}
