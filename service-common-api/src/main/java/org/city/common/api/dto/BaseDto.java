package org.city.common.api.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.city.common.api.in.sql.Limit;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 11:34:25
 * @版本 1.0
 * @描述 公共的Dto
 */
@Data
@Accessors(chain = true)
public class BaseDto implements Serializable{
	private static final long serialVersionUID = 1L;
	/* 页码 */
	private Integer pageNum;
	/* 页大小 */
	private Integer pageSize;
	/* 附加参数 */
	private Map<String, Object> params = new HashMap<>();
	
	/**
	 * @描述 分页处理
	 * @param max 设定最大分页值（对pageSize再分页）
	 * @param limit 分页回调
	 */
	public void limitHandler(int max, Limit limit) {
		if (this.pageSize == null) {limit.handler(this); return;}
		
		LimitPage limitPage = new LimitPage(max, this);
		BaseDto baseDto = null;
		while((baseDto = limitPage.getCurBase()) != null) {
			limit.handler(baseDto);
		}
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2022-08-28 09:45:25
	 * @版本 1.0
	 * @parentClass BaseDto
	 * @描述 分页限制
	 */
	private class LimitPage {
		private final int max;
		private final int lastSize;
		private final BaseDto baseDto;
		private int cur = 1;
		private LimitPage(int max, BaseDto baseDto) {
			this.max = baseDto.getPageSize() / max;
			this.lastSize = baseDto.getPageSize() % max;
			this.baseDto = baseDto;
		}
		
		/**
		 * @描述 获取分页信息
		 * @return 分页信息
		 */
		private BaseDto getCurBase() {
			if (this.cur == -1) {return null;}
			if (this.cur > max) {
				if (lastSize > 0) {
					this.baseDto.setPageNum(cur);
					this.baseDto.setPageSize(lastSize);
					this.cur = -1;
				} else {return null;}
			} else {this.baseDto.setPageNum(cur); this.cur++;}
			return this.baseDto;
		}
	}
}
