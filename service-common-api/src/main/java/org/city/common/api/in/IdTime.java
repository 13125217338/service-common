package org.city.common.api.in;

/**
 * @作者 ChengShi
 * @日期 2022-09-16 17:12:34
 * @版本 1.0
 * @描述 记录时间与ID
 */
public interface IdTime {
	/**
	 * @描述 获取记录时间
	 * @return 记录时间
	 */
	public long getRecordTime();
	/**
	 * @描述 获取唯一ID
	 * @return 唯一ID
	 */
	public String getOnlyId();
}
