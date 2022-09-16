package org.city.common.api.dto;

import org.city.common.api.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2023年5月9日
 * @版本 1.0
 * @描述 Redis参数
 */
@Getter
@Setter
@Accessors(chain = true)
public class RedisDto extends BaseEntity {
	/* Redis键 */
	private String key;
	/* 范围条件 */
	private String pattern;
	
	/**
	 * @描述 生成带key范围和分页的参数
	 * @param key 键（除keys方法都要）
	 * @param pattern 范围条件（List与ZSet不需要）
	 * @param pageNum 页码
	 * @param pageSize 页大小
	 * @return Redis参数
	 */
	public static RedisDto of(String key, String pattern, long pageNum, long pageSize) {
		return (RedisDto) new RedisDto().setKey(key).setPattern(pattern).setPageNum(pageNum).setPageSize(pageSize);
	}
}
