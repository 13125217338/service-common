package org.city.common.api.in.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.city.common.api.dto.MapDto;

/**
 * @作者 ChengShi
 * @日期 2024年1月26日
 * @版本 1.0
 * @描述 Map枚举
 */
public interface MapEnum {
	/**
	 * @描述 转成Map参数
	 * @return Map参数
	 */
	public MapDto to();
	
	/**
	 * @描述 转成Map参数集合
	 * @param mapEnums Map枚举数组
	 * @return Map参数集合
	 */
	public static List<MapDto> toList(MapEnum[] mapEnums) {
		return Arrays.asList(mapEnums).stream().map(v -> v.to()).collect(Collectors.toList());
	}
}
