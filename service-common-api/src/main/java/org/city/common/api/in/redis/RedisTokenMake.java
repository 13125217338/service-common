package org.city.common.api.in.redis;

import java.lang.reflect.Type;

/**
 * @作者 ChengShi
 * @日期 2023年6月18日
 * @版本 1.0
 * @描述 缓存令牌操作
 */
public interface RedisTokenMake {
	/**
	 * @描述 获取令牌
	 * @param onlyId 唯一ID
	 * @param data 令牌数据
	 * @return 令牌
	 */
	public String getToken(String onlyId, Object data);
	
	/**
	 * @描述 获取令牌与唯一ID来自令牌
	 * @param token 令牌
	 * @return 令牌与唯一ID
	 */
	public String getTkIdByToken(String token);
	
	/**
	 * @描述 获取令牌与唯一ID来自唯一ID
	 * @param onlyId 唯一ID
	 * @return 令牌与唯一ID
	 */
	public String getTkIdByOnlyId(String onlyId);
	
	/**
	 * @描述 获取数据来自令牌
	 * @param <T> 数据类型
	 * @param token 令牌
	 * @param type 数据类型
	 * @return 数据
	 */
	public <T> T getByToken(String token, Type type);
	/**
	 * @描述 获取数据来自唯一ID
	 * @param <T> 数据类型
	 * @param onlyId 唯一ID
	 * @param type 数据类型
	 * @return 数据
	 */
	public <T> T getByOnlyId(String onlyId, Type type);
	
	/**
	 * @描述 重新设置数据来自令牌
	 * @param token 令牌
	 * @param data 数据
	 */
	public void setByToken(String token, Object data);
	/**
	 * @描述 重新设置数据来自唯一ID
	 * @param onlyId 唯一ID
	 * @param data 数据
	 */
	public void setByOnlyId(String onlyId, Object data);
	
	/**
	 * @描述 获取运行时间来自令牌
	 * @param token 令牌
	 * @return 运行时间（毫秒）
	 */
	public long getRunTimeByToken(String token);
	/**
	 * @描述 获取运行时间来自唯一ID
	 * @param onlyId 唯一ID
	 * @return 运行时间（毫秒）
	 */
	public long getRunTimeByOnlyId(String onlyId);
	
	/**
	 * @描述 刷新过期时间来自令牌
	 * @param token 令牌
	 */
	public void flushByToken(String token);
	/**
	 * @描述 刷新过期时间来自唯一ID
	 * @param onlyId 唯一ID
	 */
	public void flushByOnlyId(String onlyId);
	
	/**
	 * @描述 删除数据来自令牌
	 * @param token 令牌
	 */
	public void deleteByToken(String token);
	/**
	 * @描述 删除数据来自唯一ID
	 * @param onlyId 唯一ID
	 */
	public void deleteByOnlyId(String onlyId);
}
