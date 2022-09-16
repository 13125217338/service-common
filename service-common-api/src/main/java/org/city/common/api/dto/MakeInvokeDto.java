package org.city.common.api.dto;

import org.city.common.api.annotation.make.MakeInvoke;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @作者 ChengShi
 * @日期 2023年1月14日
 * @版本 1.0
 * @描述 执行参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MakeInvokeDto {
	/* 执行注解 */
	private MakeInvoke makeInvoke;
	/* 注解中已被替换的自定义参数 */
	private String[] values;
}
