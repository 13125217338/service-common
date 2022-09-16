package org.city.common.api.in.sql;

import javax.sql.DataSource;

/**
 * @作者 ChengShi
 * @日期 2023-09-23 10:00:21
 * @版本 1.0
 * @描述 我的数据源
 */
public interface MyDataSource {
	/**
	 * @描述 获取数据源
	 * @param write 是否写入操作
	 * @return 数据源
	 */
	public DataSource getDataSource(boolean write);
}
