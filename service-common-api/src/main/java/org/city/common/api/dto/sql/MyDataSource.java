package org.city.common.api.dto.sql;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.alibaba.druid.pool.DruidDataSource;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * @作者 ChengShi
 * @日期 2022-07-04 10:06:37
 * @版本 1.0
 * @描述 自定义数据源
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.datasource")
public class MyDataSource {
	/* 其他数据源 */
	private List<DataSource> others;
	
	/**
	 * @作者 ChengShi
	 * @日期 2023-02-23 10:19:39
	 * @版本 1.0
	 * @parentClass MyDataSource
	 * @描述 数据源
	 */
	@Getter
	@Setter
	public static class DataSource extends DruidDataSource {
		private static final long serialVersionUID = 1L;
		/* 数据源ID */
		private String dsId;
	}
}
