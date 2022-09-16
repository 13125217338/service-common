package org.city.common.api.in.sql;

import java.util.List;
import java.util.Map;

import org.city.common.api.annotation.sql.Table;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.BaseDto;
import org.city.common.api.dto.Condition;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 14:54:19
 * @版本 1.0
 * @描述 简单增删改查
 */
public interface Crud<D extends BaseDto> {
	/**
	 * @描述 获取表信息
	 * @return 表信息
	 */
	public Table getTable();
	
	/**
	 * @描述 通过实体类字段名获取表字段名
	 * @param fieldName 实体类字段名
	 * @return 表字段名
	 */
	public String getTableField(String fieldName);
	
	/**
	 * @描述 获取所有字段名
	 * @return 所有字段名（key=实体类字段名，value=表字段名）
	 */
	public Map<String, String> getTableFields();
	
	/**
	 * @描述 获取数据库当前时间
	 * @return 完整的年月日时分秒
	 */
	public String getNowTime();
	
	/**
	 * @描述 获取当前Dto连接条件（只获取有@JoinTable注解的参数）
	 * @param groups 与注解@JoinTable下的groups对应，不传获取所有默认条件
	 * @return 注解操作对象
	 */
	public AnnotationCondition<D> getJoin(int...groups);
	
	/**
	 * @描述 统计条目
	 * @param condition 条件
	 * @return 统计数
	 */
	public int count(Condition condition);
	
	/**
	 * @描述 通过条件获取一个数据
	 * @param condition 条件
	 * @return 一个数据
	 */
	public D findOne(Condition condition);
	
	/**
	 * @描述 通过条件获取所有数据
	 * @param condition 条件
	 * @return 所有数据
	 */
	public List<D> findAll(Condition condition);
	
	/**
	 * @描述 添加一个数据
	 * @param d 数据
	 * @return 添加结果
	 */
	public boolean add(D d);
	
	/**
	 * @描述 获取最后添加的自增主键值
	 * @return 最后添加的自增主键值
	 */
	public long getLastAddId();
	
	/**
	 * @描述 批量添加数据
	 * @param ds 批量数据
	 * @return 添加成功数
	 */
	public int addBatch(List<D> ds);
	
	/**
	 * @描述 通过条件删除数据
	 * @param condition 条件
	 * @return 删除结果
	 */
	public boolean delete(Condition condition);
	
	/**
	 * @描述 通过条件更新数据
	 * @param condition 条件
	 * @param dto 数据
	 * @param isUpdateNull 是否更新值为NULL的数据（false = 为空的数据不更新）
	 * @return 更新结果
	 */
	public boolean update(Condition condition, D dto, boolean isUpdateNull);
	
	/**
	 * @描述 通过条件批量更新数据
	 * @param condition 条件（只有名称有效，操作类型与值可随意）
	 * @param dtos 被批量更新的数据（条件值从这里取）
	 * @param isUpdateNull 是否更新值为NULL的数据（false = 为空的数据不更新）
	 * @return 更新成功数
	 */
	public int updateBatch(Condition condition, List<D> dtos, boolean isUpdateNull);
	
	/**
	 * @描述 清除参数的所有Sql
	 * @param args 参数
	 */
	public static void clearSql(Object...args) {
		if (args == null) {return;}
		for (Object d : args) {
			if (d instanceof BaseDto) {
				((BaseDto) d).getParams().remove(CommonConstant.SQL_FIELD);
				((BaseDto) d).getParams().remove(CommonConstant.SQL_JOIN);
				((BaseDto) d).getParams().remove(CommonConstant.SQL_WHERE);
			}
		}
	}
}
