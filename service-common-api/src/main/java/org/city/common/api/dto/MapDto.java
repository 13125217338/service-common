package org.city.common.api.dto;

import java.util.List;

import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @作者 ChengShi
 * @日期 2024年1月26日
 * @版本 1.0
 * @描述 Map参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapDto {
	/* 键 */
	private String key;
	/* 值 */
	private Object value;
	/* 备注 */
	private String remake;
	
	/**
	 * @描述 将集合Map参数转为JSON对象
	 * @param mapDtos 集合Map参数
	 * @return JSON对象
	 */
	public static JSONObject toJSON(List<MapDto> mapDtos) {
		JSONObject jObj = new JSONObject();
		for (MapDto mapDto : mapDtos) {
			jObj.put(mapDto.key, mapDto.value);
		}
		return jObj;
	}
}
