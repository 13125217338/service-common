package org.city.common.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @作者 ChengShi
 * @日期 2022-07-15 14:08:55
 * @版本 1.0
 * @描述 分页数据列表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页数据列表")
public class DataList<D> {
	
	@Schema(description = "数据列表")
	private List<D> rows;
	
	@Schema(description = "总数据量")
	private long total;
}
