package org.city.common.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022年8月20日
 * @版本 1.0
 * @描述 类型参数
 */
@Data
@Accessors(chain = true)
public class TypeDto {
	/* 类型名称 */
	private String typeName;
	/* 原类型 */
	private TypeDto rawType;
	/* 所有者类型 */
	private TypeDto ownerType;
	/* 泛型所有类型 */
	private TypeDto[] actualType;
	/* 数组泛型 */
	private TypeDto genericArray;
}
