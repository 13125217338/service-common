package org.city.common.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @作者 ChengShi
 * @日期 2022-07-15 14:08:55
 * @版本 1.0
 * @描述 数据集
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataList<D> {
	/* 数据集 */
	private List<D> rows;
	/* 数据量 */
	private int total;
}
