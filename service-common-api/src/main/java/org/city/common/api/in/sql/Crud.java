package org.city.common.api.in.sql;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.city.common.api.annotation.sql.Table;
import org.city.common.api.dto.DataList;
import org.city.common.api.dto.sql.BaseDto;
import org.city.common.api.dto.sql.Condition;
import org.city.common.api.dto.sql.SubCondition;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 14:54:19
 * @版本 1.0
 * @描述 简单增删改查
 */
public interface Crud<D extends BaseDto> {
	/**
	 * @描述 获取自定义表名
	 * @param baseDto 基本参数
	 * @return 自定义表名
	 */
	default String getTableName(BaseDto baseDto) {
		if (baseDto.getUserSqlDto() == null || baseDto.getUserSqlDto().getTable() == null) {return getTable().name();}
		else {return baseDto.getUserSqlDto().getTable();}
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
	 * @描述 获取所有字段名
	 * @return 所有字段名（key=实体类字段名，value=表字段名）
	 */
	public Map<String, String> getTableFields();
	
	/**
	 * @描述 获取Dto所有字段名
	 * @return 所有字段名（key=参数字段名，value=参数字段）
	 */
	public Map<String, Field> getDtoFields();
	
	/**
	 * @描述 获取数据库当前时间
	 * @return 时间戳
	 */
	public Timestamp getNowTime();
	
	/**
	 * @描述 获取当前Dto连接条件（只获取有@JoinTable注解的参数）
	 * @param groups 与注解@JoinTable下的groups对应，不传获取所有默认连接
	 * @return 注解操作对象
	 */
	public AnnotationCondition<D> getJoin(int...groups);
	
	/**
	 * @描述 通过条件生成子条件（只对查询有效）
	 * @param condition 条件
	 * @return 子条件
	 */
	public SubCondition findBySubCondition(Condition condition);
	
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
	public D findOne(Condition condition);
	
	/**
	 * @描述 通过条件获取所有数据
	 * @param condition 条件
	 * @return 所有数据
	 */
	public List<D> findAll(Condition condition);
	
	/**
	 * @描述 判断条目后通过条件获取分页数据
	 * @param condition 条件
	 * @return 分页数据
	 */
	public DataList<D> findAllByCount(Condition condition);
	
	/**
	 * @描述 添加一个数据
	 * @param d 数据
	 * @return 添加结果
	 */
	public boolean add(D d);
	
	/**
	 * @描述 获取最后添加的自增主键值（必须在事务中执行该方法）
	 * @return 最后添加的自增主键值
	 */
	public long getLastAddId();
	
	/**
	 * @描述 批量添加数据（不使用事务会特别慢）
	 * @param ds 批量数据
	 * @return 添加成功数
	 */
	public int addBatch(Collection<D> ds);
	
	/**
	 * @描述 通过条件删除数据
	 * @param condition 条件
	 * @return 删除数量
	 */
	public int delete(Condition condition);
	
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
	 * @param condition 必须and条件（只有名称有效，操作类型与值可随意）
	 * @param dtos 被批量更新的数据（条件值从这里取）
	 * @param isUpdateNull 是否更新值为NULL的数据（false = 为空的数据不更新）
	 * @return 更新成功数
	 */
	public int updateBatch(Condition condition, Collection<D> dtos, boolean isUpdateNull);
	
	/**
	 * @描述 只更新Sql语法 - 只会本地执行
	 * @param sql 待更新语法
	 * @return 更新返回值
	 */
	public int update(String sql);
	/**
	 * @描述 只执行Sql语法 - 只会本地执行
	 * @param sql 待执行语法
	 */
	public void execute(String sql);
	/**
	 * @描述 只查询一行数据Sql语法 - 只会本地执行
	 * @param <T> 转换类型
	 * @param sql 待查询语法
	 * @param type 转换类型
	 * @return 查询结果（如果为空数据或者大于一行数据则会抛出异常）
	 */
	public <T> T queryOne(String sql, Class<T> type);
	
	/**
	 * @描述 清除用户自定义Sql参数
	 * @param args 参数
	 */
	public static void clearSql(Object...args) {
		if (args == null) {return;} //不处理空
		for (Object dto : args) {
			if (dto instanceof BaseDto) {remove((BaseDto) dto);} //如果是基本参数直接移除
			else if(dto instanceof Collection) { //如果是集合需判断类型
				for (Object obj : (Collection<?>) dto) { //如果集合中是基本参数则移除
					if (obj instanceof BaseDto) {remove((BaseDto) obj);}
				}
			}
		}
	}
	/* 清除用户自定义Sql参数 */
	private static void remove(BaseDto baseDto) {
		baseDto.setUserSqlDto(null);
	}
}
