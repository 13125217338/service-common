package org.city.common.api.entity;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.city.common.api.dto.sql.Condition.OrderBy;
import org.city.common.api.dto.sql.UserSql;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.sql.Limit;
import org.springframework.util.Assert;

import com.baomidou.mybatisplus.annotation.TableField;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 11:34:25
 * @版本 1.0
 * @描述 公共实体类
 */
@Getter
@Setter
@Accessors(chain = true)
public class BaseEntity implements JSONParser {
	@Hidden
	@TableField(exist = false)
	private Long pageNum = 1L; //页码
	@Hidden
	@TableField(exist = false)
	private Long pageSize = 20L; //页大小
	@Hidden
	@TableField(exist = false)
	private List<OrderBy> orderBys; //排序 - 支持<obj$field>排序
	@Hidden
	@TableField(exist = false)
	private UserSql userSql; //用户自定义Sql参数（防注入安全参数）
	@Hidden
	@TableField(exist = false)
	private Object param; //自定义参数
	@Hidden
	@TableField(exist = false)
	private final Map<String, Object> params = new HashMap<>(); //附加参数
	
	/**
	 * @描述 获取用户自定义Sql参数
	 * @return 用户自定义Sql参数
	 */
	public synchronized UserSql userSql() {
		if (userSql == null) {userSql = new UserSql();}
		return userSql;
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
	public BaseEntity $setParam(String key, Object data) {
		this.params.put(key, data);
		return this;
	}
	
	/**
	 * @描述 拷贝参数值
	 * @param data 被拷贝数据
	 * @param keys 被拷贝key
	 * @return 公共参数
	 */
	public BaseEntity copyParamValue(BaseEntity data, String...keys) {
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
	public BaseEntity copyParamAll(BaseEntity data) {
		Assert.notNull(data, "被拷贝数据不能为空！");
		this.params.putAll(data.getParams());
		return this;
	}
	
	/**
	 * @描述 拷贝分页数据
	 * @param data 被拷贝数据
	 * @return 公共参数
	 */
	public BaseEntity copyPage(BaseEntity data) {
		this.pageNum = data.pageNum;
		this.pageSize = data.pageSize;
		return this;
	}
	
	/**
	 * @描述 克隆公共的Dto
	 * @return 公共的Dto
	 */
	public BaseEntity clone() {
		BaseEntity baseEntity = new BaseEntity();
		baseEntity.pageNum = this.pageNum;
		baseEntity.pageSize = this.pageSize;
		baseEntity.orderBys = this.orderBys;
		baseEntity.userSql = this.userSql;
		baseEntity.param = this.param;
		baseEntity.params.putAll(this.params);
		return baseEntity;
	}
	
	/**
	 * @描述 分页处理
	 * @param total 分页总数
	 * @param limit 分页回调
	 */
	public void limitHandler(long total, Limit limit) {
		if (this.pageSize == null) {limit.handler(this); return;}
		
		LimitPage limitPage = new LimitPage(total, this);
		BaseEntity baseEntity = null;
		while((baseEntity = limitPage.getCurBase()) != null) {
			limit.handler(baseEntity);
		}
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2022-08-28 09:45:25
	 * @版本 1.0
	 * @parentClass BaseEntity
	 * @描述 分页限制
	 */
	private class LimitPage {
		private final long maxPage;
		private final long lastSize;
		private final BaseEntity baseEntity;
		private long cur = 1;
		private LimitPage(long total, BaseEntity baseEntity) {
			this.maxPage = total / baseEntity.getPageSize();
			this.lastSize = total % baseEntity.getPageSize();
			this.baseEntity = baseEntity;
		}
		
		/**
		 * @描述 获取分页信息
		 * @return 分页信息
		 */
		private BaseEntity getCurBase() {
			if (this.cur == -1) {return null;}
			if (this.cur > maxPage) {
				if (lastSize > 0) {
					this.baseEntity.setPageNum(cur);
					this.baseEntity.setPageSize(lastSize);
					this.cur = -1;
				} else {return null;}
			} else {
				this.baseEntity.setPageNum(cur);
				this.cur++;
			}
			return this.baseEntity;
		}
	}
}
