package org.city.common.api.in.redis;

import org.city.common.api.in.parse.JSONParser;

/**
 * @作者 ChengShi
 * @日期 2023-07-07 15:36:13
 * @版本 1.0
 * @描述 令牌移除事件
 */
public interface RedisTokenRemove extends JSONParser {
	/**
	 * @描述 回调移除事件
	 * @param tkId 令牌ID
	 * @param value 数据
	 */
	public void remove(String tkId, Object value);
}
