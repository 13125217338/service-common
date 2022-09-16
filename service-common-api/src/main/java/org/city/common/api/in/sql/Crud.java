package org.city.common.api.in.sql;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.city.common.api.annotation.sql.Table;
import org.city.common.api.dto.DataList;
import org.city.common.api.dto.sql.Condition;
import org.city.common.api.dto.sql.Condition.Param;
import org.city.common.api.dto.sql.SubCondition;
import org.city.common.api.entity.BaseEntity;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 14:54:19
 * @版本 1.0
 * @描述 简单增删改查
 */
public interface Crud<E extends BaseEntity> {
	/**
	 * @描述 获取自定义表名
	 * @param baseEntity 基本参数
	 * @return 自定义表名
	 */
	default String getTableName(BaseEntity baseEntity) {
		if (baseEntity.getUserSql() == null || baseEntity.getUserSql().getTable() == null) {return getTable().name();}
		else {return baseEntity.getUserSql().getTable();}
	}
	
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
	 * @描述 获取实体类所有字段名
	 * @return 所有字段名（key=实体类字段名，value=表字段名）
	 */
	public Map<String, String> getTableFields();
	
	/**
	 * @描述 获取实体类连接条件（只获取有@Joins注解的参数）
	 * @param entity 实体类
	 * @param groups 与注解@Joins下的groups对应，不传获取所有默认连接
	 * @return 注解操作对象
	 */
	public AnnotationCondition getJoin(E entity, int...groups);
	
	/**
	 * @描述 通过条件生成子条件（只对查询有效）
	 * @param condition 条件
	 * @param useTableField 使用表字段名接收数据
	 * @return 子条件
	 */
	public SubCondition findBySubCondition(Condition condition, boolean useTableField);
	
	/**
	 * @描述 统计条目
	 * @param condition 条件
	 * @return 统计数
	 */
	public long count(Condition condition);
	
	/**
	 * @描述 通过条件获取一个数据
	 * @param condition 条件
	 * @return 一个数据
	 */
	public <R> R findOne(Condition condition);
	
	/**
	 * @描述 通过条件获取所有数据
	 * @param condition 条件
	 * @return 所有数据
	 */
	public <R> List<R> findAll(Condition condition);
	
	/**
	 * @描述 判断条目后通过条件获取分页数据
	 * @param condition 条件
	 * @return 分页数据
	 */
	public <R> DataList<R> findAllByCount(Condition condition);
	
	/**
	 * @描述 添加一个数据
	 * @param entity 数据
	 * @return 添加结果
	 */
	public boolean add(E entity);
	
	/**
	 * @描述 获取最后添加的自增主键值（必须在事务中执行该方法）
	 * @return 最后添加的自增主键值
	 */
	public long getLastAddId();
	
	/**
	 * @描述 批量添加数据（不使用事务会特别慢）
	 * @param entitys 批量数据
	 * @return 添加成功数
	 */
	public int addBatch(Collection<E> entitys);
	
	/**
	 * @描述 通过条件删除数据
	 * @param condition 条件
	 * @return 删除数量
	 */
	public int delete(Condition condition);
	
	/**
	 * @描述 通过条件更新数据
	 * @param condition 条件
	 * @param isUpdateNull 是否更新值为NULL的数据（false = 为空的数据不更新）
	 * @return 更新结果
	 */
	public boolean update(Condition condition, boolean isUpdateNull);
	
	/**
	 * @描述 通过条件批量更新数据
	 * @param params 只能And条件（Join无效）
	 * @param entitys 被批量更新的数据（条件值从这里取）
	 * @param isUpdateNull 是否更新值为NULL的数据（false = 为空的数据不更新）
	 * @return 更新成功数
	 */
	public int updateBatch(List<Param> params, Collection<E> entitys, boolean isUpdateNull);
	
	/**
	 * @描述 只更新Sql语法
	 * @param sql 待更新语法
	 * @return 更新返回值
	 */
	public int update(String sql);
	/**
	 * @描述 只执行Sql语法
	 * @param sql 待执行语法
	 */
	public void execute(String sql);
	/**
	 * @描述 只查询一行一列数据Sql语法
	 * @param <R> 转换类型
	 * @param sql 待查询语法
	 * @param type 转换类型
	 * @return 查询结果（如果为空数据或者大于一行一列数据则会抛出异常）
	 */
	public <R> R queryOne(String sql, Class<R> type);
	/**
	 * @描述 只查询Map数据Sql语法
	 * @param sql 待查询语法
	 * @return 查询结果（如果为空数据或者大于一行数据则会抛出异常）
	 */
	public Map<String, Object> queryMap(String sql);
	
	/**
	 * @描述 清除用户自定义Sql参数
	 * @param args 参数
	 */
	public static void clearSql(Object...args) {
		if (args == null) {return;} //不处理空
		for (Object entity : args) {
			if (entity instanceof BaseEntity) {remove((BaseEntity) entity);} //如果是基本参数直接移除
			else if(entity instanceof Collection) { //如果是集合需判断类型
				for (Object obj : (Collection<?>) entity) { //如果集合中是基本参数则移除
					if (obj instanceof BaseEntity) {remove((BaseEntity) obj);}
				}
			}
		}
	}
	/* 清除用户自定义Sql参数 */
	private static void remove(BaseEntity baseEntity) {
		baseEntity.setUserSql(null);
	}
}
