package org.city.common.api.open;

import org.city.common.api.annotation.plug.Remote;

/**
 * @作者 ChengShi
 * @日期 2023年6月26日
 * @版本 1.0
 * @描述 缓存刷新Api
 */
@Remote
public interface RedisCacheFlushApi {
	/**
	 * @描述 刷新缓存
	 * @param key 缓存键
	 */
	public void flush(String key);
}
