package org.city.common.api.dto.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2023年5月2日
 * @版本 1.0
 * @描述 用户自定义Sql参数（防注入安全参数）
 */
@Data
@Accessors(chain = true)
public class UserSql {
	@Hidden
	private String fields; //自定义sql追加字段 - 如：sys.create_time as createTime,xxx,等 - 只有查询有用（逗号结尾）
	@Hidden
	private String join; //自定义sql追加连接表 - 如：left join system sys on c.id = sys.id - 查询、删除、更新有用
	@Hidden
	private final List<Object> joinParam = new ArrayList<>(); //自定义sql追加连接表参数 - 如：left join system sys on c.id = ? - 查询、删除、更新有用
	@Hidden
	private String where; //自定义sql条件 - 如：and sys.name = '插件' - 查询、删除、更新有用
	@Hidden
	private final List<Object> whereParam = new ArrayList<>(); //自定义sql条件参数 - 如：and sys.name = ? - 查询、删除、更新有用
	@Hidden
	private String table; //自定义sql表名 - 如：system - 全部有用
	@Hidden
	private String having; //自定义sql分组条件 - 如：total > 10 - 分组后有用
	@Hidden
	private final List<Object> havingParam = new ArrayList<>(); //自定义sql分组条件参数 - 如：total > ? - 分组后有用
	
	/**
	 * @描述 设置自定义sql追加连接表和参数
	 * @param join 自定义sql追加连接表
	 * @param params 参数
	 * @return 用户自定义Sql参数
	 */
	public UserSql setJoin(String join, Object...params) {
		this.join = join;
		this.joinParam.addAll(Arrays.asList(params));
		return this;
	}
	/**
	 * @描述 设置自定义sql追加连接表
	 * @param join 自定义sql追加连接表
	 * @return 用户自定义Sql参数
	 */
	public UserSql setJoin(String join) {
		this.join = join;
		return this;
	}
	
	/**
	 * @描述 设置自定义sql条件和参数
	 * @param where 自定义sql条件
	 * @param params 参数
	 * @return 用户自定义Sql参数
	 */
	public UserSql setWhere(String where, Object...params) {
		this.where = where;
		this.whereParam.addAll(Arrays.asList(params));
		return this;
	}
	/**
	 * @描述 设置自定义sql条件
	 * @param where 自定义sql条件
	 * @return 用户自定义Sql参数
	 */
	public UserSql setWhere(String where) {
		this.where = where;
		return this;
	}
	
	/**
	 * @描述 设置自定义sql分组条件和参数
	 * @param having 自定义sql分组条件
	 * @param params 参数
	 * @return 用户自定义Sql参数
	 */
	public UserSql setHaving(String having, Object...params) {
		this.having = having;
		this.havingParam.addAll(Arrays.asList(params));
		return this;
	}
	/**
	 * @描述 设置自定义sql分组条件
	 * @param having 自定义sql分组条件
	 * @return 用户自定义Sql参数
	 */
	public UserSql setHaving(String having) {
		this.having = having;
		return this;
	}
}
