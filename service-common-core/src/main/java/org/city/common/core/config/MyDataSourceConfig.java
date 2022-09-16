package org.city.common.core.config;

import java.lang.reflect.Constructor;
import java.util.Deque;
import java.util.HashMap;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.sql.MyDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.CollectionUtils;

import com.alibaba.druid.pool.DruidDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2023-09-26 20:13:45
 * @版本 1.0
 * @描述 自定义数据源配置
 */
@Slf4j
@Configuration
public class MyDataSourceConfig {
	
	@Bean(initMethod = "init")
    public DataSource masterDataSource() throws Throwable {
        log.info("Init DruidDataSource");
        Class<?> druidDataSource = Class.forName("com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceWrapper");
        Constructor<?> constructor = druidDataSource.getDeclaredConstructor();
        constructor.setAccessible(true);
        return (DataSource) constructor.newInstance();
    }
	
	@Bean
	@Primary
	public DataSource dataSource(DruidDataSource master, MyDataSource myDataSource) {
		/* 动态数据源 */
		AbstractRoutingDataSource dataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				Deque<String> deque = CommonConstant.OTHER_DATA_SOURCE.get();
				return deque == null ? null : deque.peekFirst();
			}
		};
		
		/* 配置其他数据源 */
		if (CollectionUtils.isEmpty(myDataSource.getOthers())) {dataSource.setTargetDataSources(new HashMap<>(0));}
		else {dataSource.setTargetDataSources(myDataSource.getOthers().stream().collect(Collectors.toMap(k -> k.getDsId(), v -> v)));}
		
		/* 默认主数据源 */
		dataSource.setDefaultTargetDataSource(master);
		return dataSource;
	}
}
