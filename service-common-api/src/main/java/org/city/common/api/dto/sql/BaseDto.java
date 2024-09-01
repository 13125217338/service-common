package org.city.common.api.dto.sql;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.city.common.api.dto.sql.Condition.OrderBy;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.sql.Limit;
import org.springframework.util.Assert;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 11:34:25
 * @版本 1.0
 * @描述 公共的Dto
 */
@Getter
@Setter
@Accessors(chain = true)
public class BaseDto implements JSONParser {
	@Hidden
	private Long pageNum; //页码
	@Hidden
	private Long pageSize; //页大小
	@Hidden
	private List<OrderBy> orderBys; //排序 - 支持<obj$field>排序
	@Hidden
	private UserSqlDto userSqlDto; //用户自定义Sql参数（防注入安全参数）
	@Hidden
	private Object param; //自定义参数
	@Hidden
	private final Map<String, Object> params = new HashMap<>(); //附加参数
	
	/**
	 * @描述 获取用户自定义Sql参数
	 * @return 用户自定义Sql参数
	 */
	public synchronized UserSqlDto userSql() {
		if (userSqlDto == null) {userSqlDto = new UserSqlDto();}
		return userSqlDto;
	}
	
	/**
	 * @描述 获取参数值
	 * @param <T> 参数值对象
	 * @param key 参数键
	 * @param type 参数类型
	 * @return 参数值
	 */
	public <T> T $getParam(String key, Type type) {
		return parse(this.params.get(key), type);
	}
	
	/**
	 * @描述 设置参数值
	 * @param key 参数键
	 * @param data 参数值
	 * @return 公共参数
	 */
	public BaseDto $setParam(String key, Object data) {
		this.params.put(key, data);
		return this;
	}
	
	/**
	 * @描述 拷贝参数值
	 * @param data 被拷贝数据
	 * @param keys 被拷贝key
	 * @return 公共参数
	 */
	public BaseDto copyParamValue(BaseDto data, String...keys) {
		Assert.notNull(data, "被拷贝数据不能为空！");
		Assert.notNull(keys, "被拷贝key不能为空！");
		/* 所有key值拷贝 */
		for (String key : keys) {this.params.put(key, data.getParams().get(key));}
		return this;
	}
	
	/**
	 * @描述 拷贝所有参数值
	 * @param data 被拷贝数据
	 * @return 公共参数
	 */
	public BaseDto copyParamAll(BaseDto data) {
		Assert.notNull(data, "被拷贝数据不能为空！");
		this.params.putAll(data.getParams());
		return this;
	}
	
	/**
	 * @描述 拷贝分页数据
	 * @param data 被拷贝数据
	 * @return 公共参数
	 */
	public BaseDto copyPage(BaseDto data) {
		this.pageNum = data.pageNum;
		this.pageSize = data.pageSize;
		return this;
	}
	
	/**
	 * @描述 克隆公共的Dto
	 * @return 公共的Dto
	 */
	public BaseDto clone() {
		BaseDto baseDto = new BaseDto();
		baseDto.pageNum = this.pageNum;
		baseDto.pageSize = this.pageSize;
		baseDto.orderBys = this.orderBys;
		baseDto.userSqlDto = this.userSqlDto;
		baseDto.param = this.param;
		baseDto.params.putAll(this.params);
		return baseDto;
	}
	
	/**
	 * @描述 分页处理
	 * @param total 分页总数
	 * @param limit 分页回调
	 */
	public void limitHandler(long total, Limit limit) {
		if (this.pageSize == null) {limit.handler(this); return;}
		
		LimitPage limitPage = new LimitPage(total, this);
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
		private final long maxPage;
		private final long lastSize;
		private final BaseDto baseDto;
		private long cur = 1;
		private LimitPage(long total, BaseDto baseDto) {
			this.maxPage = total / baseDto.getPageSize();
			this.lastSize = total % baseDto.getPageSize();
			this.baseDto = baseDto;
		}
		
		/**
		 * @描述 获取分页信息
		 * @return 分页信息
		 */
		private BaseDto getCurBase() {
			if (this.cur == -1) {return null;}
			if (this.cur > maxPage) {
				if (lastSize > 0) {
					this.baseDto.setPageNum(cur);
					this.baseDto.setPageSize(lastSize);
					this.cur = -1;
				} else {return null;}
			} else {
				this.baseDto.setPageNum(cur);
				this.cur++;
			}
			return this.baseDto;
		}
	}
}
