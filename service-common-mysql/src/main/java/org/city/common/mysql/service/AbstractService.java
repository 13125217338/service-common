package org.city.common.mysql.service;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.city.common.api.annotation.sql.Column;
import org.city.common.api.annotation.sql.Table;
import org.city.common.api.constant.MathSql;
import org.city.common.api.constant.Operation;
import org.city.common.api.dto.DataList;
import org.city.common.api.dto.sql.BaseDto;
import org.city.common.api.dto.sql.Condition;
import org.city.common.api.dto.sql.Condition.GroupBy;
import org.city.common.api.dto.sql.Condition.Join;
import org.city.common.api.dto.sql.Condition.JoinTable;
import org.city.common.api.dto.sql.Condition.OrderBy;
import org.city.common.api.dto.sql.Condition.Param;
import org.city.common.api.dto.sql.SubCondition;
import org.city.common.api.dto.sql.TimeDto;
import org.city.common.api.in.parse.SqlQuerySubObjectParse;
import org.city.common.api.in.sql.AnnotationCondition;
import org.city.common.api.in.sql.Crud;
import org.city.common.api.util.FieldUtil;
import org.city.common.api.util.MyUtil;
import org.city.common.api.util.SpringUtil;
import org.city.common.core.entity.BaseEntity;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 11:16:37
 * @版本 1.0
 * @描述 公共服务方法
 */
@Slf4j
public abstract class AbstractService<D extends BaseDto, E extends BaseEntity> implements Crud<D>,SqlQuerySubObjectParse {
	/* 存储所有继承该类的表信息 */
	private final static Map<String, LinkedHashMap<String, String>> TABLES = new HashMap<>(8);
	/* 用于记录是否存在过当前实体类 */
	private final static Set<Class<?>> RECORD = new HashSet<>();
	/* 根据数据源自动选择模板 */
	private JdbcTemplate jdbcTemplate;
	/* dto类型 */
	@SuppressWarnings("unchecked")
	private final Class<D> DTO_CLASS = (Class<D>) getGenericsClass(0);
	/* 非基本类型字段 - 对象字段 */
	private final Map<String, Field> subFields = new HashMap<>();
	/* dto字段名对应字段 */
	private final Map<String, Field> DTO_FIELD = FieldUtil.getAllDeclaredField(DTO_CLASS, true);
	/* 该实体类注解 */
	private final Table TABLE = setTable();
	/* 空字符 */
	private final String NULLSTR = "";
	/* 英文逗号 */
	private final String DH = ",";
	/* 调试打印Sql */
	private void debugSql(String sql, Object data) {
		if (log.isDebugEnabled()) {
			log.debug("执行的Sql信息[{}]，Sql参数信息》》》 \r\n{}", sql, JSONObject.toJSONString(data == null ? Collections.EMPTY_LIST : data, true));
		}
	}
	/* 获取Sql模板 */
	private JdbcTemplate getJdbcTemplate() {
		if (jdbcTemplate == null) {
			synchronized (this) {
				if (jdbcTemplate == null) {
					if (TABLE.dataSource() == DataSource.class) {jdbcTemplate = SpringUtil.getBean(JdbcTemplate.class);}
					else {
						DataSource dataSource = SpringUtil.getBean(TABLE.dataSource());
						jdbcTemplate = new JdbcTemplate(dataSource);
					}
				}
			}
			/* 设置分组字符长度 */
			jdbcTemplate.execute("SET GLOBAL group_concat_max_len = " + Integer.MAX_VALUE);
			jdbcTemplate.execute("SET SESSION group_concat_max_len = " + Integer.MAX_VALUE);
		}
		return jdbcTemplate;
	}
	
	@Override
	public Table getTable() {return this.TABLE;}
	@Override
	public Map<String, Field> getDtoFields() {return DTO_FIELD;}
	@Override
	public String getTableField(String fieldName) {return getTableFAndV(TABLES.get(TABLE.name()).get(fieldName), fieldName)[0];}
	@Override
	public Timestamp getNowTime() {return queryOne("select now()", Timestamp.class);}
	
	@Override
	public Map<String, String> getTableFields() {
		Map<String, String> tableFields = new LinkedHashMap<>();
		for (Entry<String, String> entry : TABLES.get(TABLE.name()).entrySet()) {
			tableFields.put(entry.getKey(), splitValue(entry.getValue())[0]);
		}
		return tableFields;
	}
	
	@Override
	public AnnotationCondition<D> getJoin(int...groups) {
		return AnnotationConditionService.getJoin(DTO_CLASS, this, groups);
	}
	
	@Override
	public SubCondition findBySubCondition(Condition condition) {
		/* 字段映射 */
		Map<String, String> fields = TABLES.get(TABLE.name());
		/* 条件参数 */
		List<Object> dtoValues = new ArrayList<>();
		/* 查询所有接收字段名 */
		Set<String> subFieldName = new HashSet<>();
		/* 查询sql追加 */
		StringBuilder sb = querySqlAppend(fields, false, condition, dtoValues, subFieldName);
		/* 追加条件 */
		dtoValues.addAll(whereCondition(condition, fields, sb));
		
		/* 先分组 */
		if (condition.getGroupBys().size() > 0) {
			groupBy(condition, sb);
		}
		/* 在排序 */
		if (condition.getOrderBys().size() > 0) {
			orderBy(condition, sb, condition.getGroupBys().size() > 0);
		}
		
		/* 分页 */
		if (condition.getPage() != null) {
			sb.append(" limit " + condition.getPage().getOffset() + "," + condition.getPage().getPageSize());
		}
		return new SubCondition(sb.toString(), dtoValues, subFieldName, TABLE.alias(), false);
	}
	
	@Override
	public long count(Condition condition) {
		try {
			return countSql(condition);
		} catch (Throwable e) {
			throw new DataAccessResourceFailureException("统计Sql执行失败！", e);
		}
	}
	
	@Override
	public D findOne(Condition condition) {
		List<D> querySql = findAll(condition.limitOffset(0, 1));
		return querySql.size() > 0 ? querySql.get(0) : null;
	}
	@Override
	public List<D> findAll(Condition condition) {
		try {
			return querySql(condition);
		} catch (Throwable e) {
			throw new DataAccessResourceFailureException("查询Sql执行失败！", e);
		}
	}
	
	@Override
	public DataList<D> findAllByCount(Condition condition) {
		long count = count(condition);
		return count > 0 ? new DataList<>(findAll(condition), count) : new DataList<>(Collections.emptyList(), count);
	}
	
	@Override
	public boolean add(D d) {
		return addBatch(Arrays.asList(d)) > 0;
	}
	@Override
	public int addBatch(Collection<D> ds) {
		try {
			return insertSql(ds);
		} catch (Throwable e) {
			throw new DataAccessResourceFailureException("添加Sql执行失败！", e);
		}
	}
	
	@Override
	public long getLastAddId() {
		try {
			/* 必须在事务中执行该方法 */
			if (!TransactionSynchronizationManager.isActualTransactionActive()) {throw new NoTransactionException("请在事务中执行该方法！");}
			debugSql("select last_insert_id()", null); //Sql打印
			
			Long result = getJdbcTemplate().queryForObject("select last_insert_id()", Long.class);
			if (result == null) {throw new NullPointerException("无自增主键！");} else {return result.longValue();}
		} catch (Throwable e) {
			throw new DataAccessResourceFailureException("查询自增主键Sql执行失败！", e);
		}
	}
	
	@Override
	public int delete(Condition condition) {
		try {
			/* 字段映射 */
			Map<String, String> fields = TABLES.get(TABLE.name());
			
			StringBuilder sb = new StringBuilder();
			sb.append("delete " + TABLE.alias() + " from `" + getTableName(condition.getBaseDto()) + "` " + TABLE.alias());
			/* 追加条件 */
			List<Object> dtoValues = whereCondition(condition, fields, sb);
			
			/* Sql打印 */
			debugSql(sb.toString(), dtoValues);
			/* 执行删除 */
			return getJdbcTemplate().update(sb.toString(), dtoValues.toArray());
		} catch (Throwable e) {
			throw new DataAccessResourceFailureException("删除Sql执行失败！", e);
		}
	}
	
	@Override
	public boolean update(Condition condition, D dto, boolean isUpdateNull) {
		try {
			if (dto == null) {throw new NullPointerException("更新不能传入空对象！");}
			setUpdateTime(dto); //设置更新时间
			
			/* 字段映射 */
			Map<String, String> fields = TABLES.get(TABLE.name());
			StringBuilder sb = new StringBuilder();
			sb.append("update `" + getTableName(dto) + "` " + TABLE.alias() + " set ");
			
			/* 追加参数 */
			List<Object> dtoValues = updatePamera(fields, dto, sb, isUpdateNull);
			/* 如果没有值代表不用更新 */
			if (dtoValues == null) {return false;}
			/* 追加条件 */
			dtoValues.addAll(whereCondition(condition, fields, sb));
			
			/* Sql打印 */
			debugSql(sb.toString(), dtoValues);
			/* 执行更新 */
			return getJdbcTemplate().update(sb.toString(), dtoValues.toArray()) > 0;
		} catch (Throwable e) {
			throw new DataAccessResourceFailureException("更新Sql执行失败！", e);
		}
	}
	
	/* 设置更新时间 */
	private void setUpdateTime(D d) {
		if (d instanceof TimeDto) {((TimeDto) d).setUpdateTime(getNowTime());}
	}
	/* 批量设置更新时间 */
	private void setUpdateTimes(Collection<D> dtos) {
		Timestamp nowTime = getNowTime();
		for (D d : dtos) {if (d instanceof TimeDto) {((TimeDto) d).setUpdateTime(nowTime);}}
	}
	/* 批量设置创建与更新时间 */
	private void setCreateUpdateTimes(Collection<D> dtos) {
		Timestamp nowTime = getNowTime();
		for (D d : dtos) {if (d instanceof TimeDto) {((TimeDto) d).setCreateTime(nowTime).setUpdateTime(nowTime);}}
	}
	
	@Override
	public int updateBatch(Condition condition, Collection<D> dtos, boolean isUpdateNull) {
		try {
			if (condition.getParams().size() == 0) {throw new IllegalArgumentException("批量更新不允许无条件执行！");}
			if (CollectionUtils.isEmpty(dtos)) {throw new NullPointerException("批量更新不能传入空对象！");}
			setUpdateTimes(dtos); //批量设置更新时间
			
			/* 字段映射 */
			Map<String, String> fields = TABLES.get(TABLE.name());
			StringBuilder sb = new StringBuilder();
			sb.append("update `" + getTableName(dtos.iterator().next()) + "` " + TABLE.alias() + " set ");
			
			/* 参数配置 */
			List<Field> paramFields = new ArrayList<>();
			/* 拼接Sql返回参数 */
			String paramSql = getParamSql(condition, fields, paramFields);
			List<Object> dtoValues = updatePameras(fields, paramSql, dtos, isUpdateNull, paramFields, sb);
			
			/* 如果没有值代表不用更新 */
			if (dtoValues == null) {return 0;}
			dtoValues.addAll(whereCondition(condition, dtos, sb, fields));
			
			/* Sql打印 */
			debugSql(sb.toString(), dtoValues);
			/* 执行更新 */
			return getJdbcTemplate().update(sb.toString(), dtoValues.toArray());
		} catch (Throwable e) {
			throw new DataAccessResourceFailureException("批量更新Sql执行失败！", e);
		}
	}
	
	@Override
	public int update(String sql) {
		try {debugSql(sql, null); return getJdbcTemplate().update(sql);}
		catch (Throwable e) {throw new DataAccessResourceFailureException("手动更新Sql执行失败！", e);}
	}
	@Override
	public void execute(String sql) {
		try {debugSql(sql, null); getJdbcTemplate().execute(sql);}
		catch (Throwable e) {throw new DataAccessResourceFailureException("手动执行Sql失败！", e);}
	}
	@Override
	public <T> T queryOne(String sql, Class<T> type) {
		try {debugSql(sql, null); return getJdbcTemplate().queryForObject(sql, type);}
		catch (Throwable e) {throw new DataAccessResourceFailureException("手动查询Sql执行失败！", e);}
	}
	
	/* 获取批量更新条件 */
	private List<Object> whereCondition(Condition condition, Collection<D> ds, StringBuilder sb, Map<String, String> fields) throws Exception {
		/* dto字段值 */
		List<Object> dtoValues = new ArrayList<>();
		List<Param> params = condition.getParams();
		/* 开始条件 - 批量更新一定有条件 */
		sb.append(" where ");
		
		/* 参数条件保存 */
		Map<String, String> names = new LinkedHashMap<>();
		for (Param param : params) {
			Assert.isTrue(param.isAnd(), String.format("批量更新必须and条件，当前字段[%s]非and条件！", param.getName()));
			/* 获取表字段参数 */
			String[] tbFV = getTableFAndV(fields.get(param.getName()), param.getName());
			names.put(param.getName(), TABLE.alias() + ".`" + tbFV[0] + "`");
		}
		
		/* 参数条件追加 */
		sb.append("(" + String.join(",", names.values()) + ") in (");
		/* 追加值 */
		for (D d : ds) {
			sb.append("(");
			for (String name : names.keySet()) {
				sb.append("?,");
				dtoValues.add(DTO_FIELD.get(name).get(d));
			}
			appendFkh(sb); sb.append(",");
		}
		appendFkh(sb);
		
		/* 最后追加条件 */
		sb.append(getSqlWhere(condition.getBaseDto()));
		return dtoValues;
	}
	
	/* 更新参数 */
	private List<Object> updatePameras(Map<String, String> fields, String paramSql, Collection<D> ds, boolean isUpdateNull, List<Field> paramFields, StringBuilder sb) throws Exception {
		/* dto字段值 */
		List<Object> dtoValues = new ArrayList<>();
		/* 追加条件 */
		for (Entry<String, String> entry : fields.entrySet()) {
			Field dtoField = DTO_FIELD.get(entry.getKey());
			String[] tbKeys = splitValue(entry.getValue());
			
			/* 批量追加更新 */
			sb.append(getFieldWhenSql(TABLE.alias() + ".`" + tbKeys[0] + "`", ds, dtoField, isUpdateNull, paramSql, paramFields, dtoValues));
		}
		/* 如果没有设置的列则不更新 */
		if (dtoValues.size() == 0) {return null;}
		/* sql尾部操作 */
		sb.deleteCharAt(sb.length() - 1);
		return dtoValues;
	}
	
	/* 获取字段条件sql */
	private String getFieldWhenSql(String tableField, Collection<D> ds, Field dtoField, boolean isUpdateNull, String paramSql, List<Field> paramFields, List<Object> dtoValues) throws Exception {
		boolean isAdd = false;
		StringBuilder sb = new StringBuilder();
		sb.append(tableField + " = case ");
		for (D d : ds) {
			if (d == null) {throw new NullPointerException("更新不能传入空对象！");}
			
			Object data = dtoField.get(d);
			/* 不更新参数为空的值 */
			if (data == null && !isUpdateNull) {continue;}
			
			sb.append("when " + paramSql + " then ? ");
			for (Field paramField : paramFields) {dtoValues.add(paramField.get(d));}
			dtoValues.add(data);
			isAdd = true;
		}
		sb.append("else " + tableField + " end,");
		return isAdd ? sb.toString() : "";
	}
	/* 获取参数条件Sql */
	private String getParamSql(Condition condition, Map<String, String> fields, List<Field> paramFields) {
		StringBuilder paramSqlSb = new StringBuilder();
		boolean isAppend = false;
		for (Param param : condition.getParams()) {
			if (isAppend) {paramSqlSb.append(param.isAnd() ? " and " : " or ");} else {isAppend = true;}
			
			/* 获取表字段参数 */
			String[] tbFV = getTableFAndV(fields.get(param.getName()), param.getName());
			paramSqlSb.append(TABLE.alias() + ".`" + tbFV[0] + "` = ?");
			paramFields.add(DTO_FIELD.get(param.getName()));
		}
		return paramSqlSb.toString();
	}
	
	/* 统计 */
	private long countSql(Condition condition) throws Throwable {
		/* 字段映射 */
		Map<String, String> fields = TABLES.get(TABLE.name());
		/* 条件参数 */
		List<Object> dtoValues = new ArrayList<>();
		/* 查询sql追加 */
		StringBuilder sb = querySqlAppend(fields, true, condition, dtoValues, new HashSet<>());
		/* 追加条件 */
		dtoValues.addAll(whereCondition(condition, fields, sb));
		
		/* 分组 */
		if (condition.getGroupBys().size() > 0) {
			groupBy(condition, sb);
			/* 对原来的sql外部包一层 */
			sb = new StringBuilder("select count(1) from (" + sb.toString() + ") t");
		}
		
		/* Sql打印 */
		debugSql(sb.toString(), dtoValues);
		/* 执行统计 */
		return getJdbcTemplate().queryForObject(sb.toString(), long.class, dtoValues.toArray());
	}
	
	/* 分组 */
	private void groupBy(Condition condition, StringBuilder sb) {
		sb.append(" group by ");
		Map<String, String> tableFields = getTableFields(); //当前表映射字段
		
		/* 处理所有分组 */
		for (GroupBy groupBy : condition.getGroupBys()) {
			if (groupBy.getJoinTable() == null) {
				if (tableFields.containsKey(groupBy.getName())) {
					sb.append(TABLE.alias() + "." + tableFields.get(groupBy.getName()) + ",");
				} else {sb.append(groupBy.getName() + ",");}
			} else {
				JoinTable jt = groupBy.getJoinTable();
				sb.append(jt.getJoinAlias() + "." + jt.getJoin().getTableField(groupBy.getName()) + ",");
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		/* 最后追加分组条件 */
		sb.append(getSqlHaving(condition.getBaseDto()));
	}
	/* 排序 */
	private void orderBy(Condition condition, StringBuilder sb, boolean hasGroup) {
		sb.append(" order by ");
		Map<String, String> tableFields = getTableFields(); //当前表映射字段
		
		/* 处理所有排序 */
		for (OrderBy orderBy : condition.getOrderBys()) {
			String orderName = null; //索引排序名称
			if (orderBy.getJoinTable() == null) { //当前表排序
				if (tableFields.containsKey(orderBy.getName())) {
					orderName = TABLE.alias() + "." + tableFields.get(orderBy.getName());
				}
			} else {
				JoinTable jt = orderBy.getJoinTable(); //连接表排序
				orderName = jt.getJoinAlias() + "." + jt.getJoin().getTableField(orderBy.getName());
			}
			
			/* 有分组使用自定义排序名 - 否则使用索引排序 */
			MathSql groupMath = orderBy.isAsc() ? MathSql.Min : MathSql.Max;
			orderName = hasGroup ? (orderName == null ? orderBy.getName() : groupMath.sqlFormat(orderName)) : orderName;
			sb.append(orderName + (orderBy.isAsc() ? " asc," : " desc,"));
		}
		sb.deleteCharAt(sb.length() - 1);
	}
	
	/* 查询sql追加 */
	private StringBuilder querySqlAppend(Map<String, String> fields, boolean isCount, Condition condition, List<Object> dtoValues, Set<String> subFieldName) {
		StringBuilder sb = new StringBuilder();
		/* 如果是统计只需要简单追加 */
		if (isCount && condition.getGroupBys().isEmpty()) {sb.append("select count(1)");}
		else {
			sb.append("select ");
			/* 提前追加字段 */
			sb.append(getSqlField(condition.getBaseDto()));
			
			/* 追加字段 */
			if (condition.getFields().size() > 0) {
				for (org.city.common.api.dto.sql.Condition.Field field : condition.getFields()) {
					String receiveName = StringUtils.hasText(field.getReceiveFieldName()) ? field.getReceiveFieldName() : field.getFieldName();
					
					if (field.getSubField() == null) { //非子查询字段
						if (field.getJoinTable() == null) {
							/* 自定义Sql */
							if (MathSql.Sql == field.getMathSql()) {
								if (field.getVals() == null || field.getVals().length == 0) {
									throw new IllegalArgumentException("自定义Sql参数值至少得有一个！");
								}
								sb.append(field.sqlFormat() + " `" + receiveName + "`,");
							} else {
								/* 获取表字段参数 */
								String[] tbFV = getTableFAndV(fields.get(field.getFieldName()), field.getFieldName());
								sb.append(field.format(TABLE.alias() + ".`" + tbFV[0] + "`") + " `" + receiveName + "`,");
							}
						/* 针对子对象追加查询字段 */
						} else {
							Assert.isTrue(subFields.containsKey(receiveName), String.format("Dto[%s]字段名[%s]必须非基本类型！", DTO_CLASS.getName(), receiveName));
							String alias = field.getJoinTable().getJoinAlias();
							Class<?> fieldType = subFields.get(receiveName).getType();
							boolean isArray = fieldType.isArray() || Collection.class.isAssignableFrom(fieldType);
							receiveName = receiveName + Condition.SPLIT; //标记对象解析
							
							/* json化处理 */
							StringBuilder json = new StringBuilder();
							if (isArray) {json.append("concat('[',group_concat(concat('{',");} else {json.append("concat('{',");}
							for (Entry<String, String> entry : field.getJoinTable().getJoin().getTableFields().entrySet()) {
								String fieldName = alias + ".`" + entry.getValue() + "`";
								fieldName = isArray ? fieldName : MathSql.Normal.format(fieldName);
								
								/* 条件拼接 */
								String notNull = String.format("concat('\"%s\":\"',%s,'\",')", entry.getKey(), fieldName);
								json.append(String.format("if(%s is null,'',%s),", fieldName, notNull));
							}
							
							/* 结尾处理 */
							json.delete(json.length() - 5, json.length()); json.append("')),");
							if (isArray) {json.append("'}') separator ','),']')");} else {json.append("'}')");}
							sb.append(json.toString() + " `" + receiveName + "`,");
						}
					} else { //子查询字段
						if (subFields.containsKey(receiveName)) {receiveName = receiveName + Condition.SPLIT;} //标记对象解析
						dtoValues.addAll(field.getSubField().getParams());
						sb.append("(" + field.getSubField().getSql() + ") `" + receiveName + "`,");
					}
					subFieldName.add(receiveName); //查询所有接收字段名
				}
			} else {
				for (Entry<String, String> entry : fields.entrySet()) {
					String[] tbKeys = splitValue(entry.getValue());
					sb.append(MathSql.Normal.format(TABLE.alias() + ".`" + tbKeys[0] + "`") + " `" + entry.getKey() + "`,");
					subFieldName.add(entry.getKey()); //查询所有接收字段名
				}
			}
			sb.deleteCharAt(sb.length() - 1);
			
			/* 如果有分组 - 替换为any_value函数 - 否则替换为空字符 */
			if (condition.getGroupBys().size() > 0) {sb = new StringBuilder(sb.toString().replace(MathSql.NORMAL, "any_value"));}
			else {sb = new StringBuilder(sb.toString().replace(MathSql.NORMAL, NULLSTR));}
		}
		
		if (condition.getSubTable() == null) { //非子查询表
			/* 忽略索引 */
			String index = StringUtils.hasText(condition.getIgnore()) ? " IGNORE INDEX(" + condition.getIgnore() + ")" : "";
			sb.append(" from `" + getTableName(condition.getBaseDto()) + "` " + TABLE.alias() + index);
		} else { //子查询表
			condition.addFields(condition.getSubTable().parseField().values());
			dtoValues.addAll(condition.getSubTable().getParams());
			sb.append(" from (" + condition.getSubTable().getSql() + ") " + TABLE.alias());
		}
		
		/* 链表 */
		for (Join join : condition.getJoins().values()) {
			sb.append(join.toSql(join.getIgnore(), this, dtoValues));
		}
		/* 最后追加链表 */
		sb.append(getSqlJoin(condition.getBaseDto()));
		return sb;
	}
	
	/* 公共查询实现代码 */
	private List<D> querySql(Condition condition) throws Throwable {
		SubCondition subCondition = findBySubCondition(condition);
		/* Sql打印 */
		debugSql(subCondition.getSql(), subCondition.getParams());
		/* 执行查询 */
		return queryResult(getJdbcTemplate().queryForList(subCondition.getSql(), subCondition.getParams().toArray()));
	}
	/* 查询返回结果 */
	private List<D> queryResult(List<Map<String, Object>> resultMaps) throws SQLException {
		List<D> result = new ArrayList<>();
		for(Map<String, Object> data : resultMaps) {
			result.add(parseSub(subFields.values(), data, DTO_CLASS, Condition.SPLIT));
		}
		return result;
	}
	
	/* 条件语法 */
	private List<Object> whereCondition(Condition condition, Map<String, String> fields, StringBuilder sb) {
		/* dto字段值 */
		List<Object> dtoValues = new ArrayList<>();
		List<Param> params = condition.getParams();
		/* 开始条件 */
		sb.append(" where 1 = 1 ");
		/* 如果有条件第一个必须and条件 */
		if (params.size() > 0) {params.iterator().next().setAnd(true);}
		
		/* 参数条件追加 - 针对or条件追加括号 */
		boolean isKH = false;
		for (int i = 0, j = params.size(); i < j; i++) {
			/* 当前参数 - 针对参数和下一个参数特殊处理 */
			Param param = params.get(i);
			if (param.isAnd()) {
				/* 如果有下一个参数且是or条件 - 追加起始括号 */
				if ((i + 1) < j && !params.get(i + 1).isAnd()) {
					sb.append(" and ("); isKH = true;
				} else {
					/* 如果已经追加起始括号 - 则追加结束括号 */
					if (isKH) {sb.append(") and "); isKH = false;}
					/* 无追加起始括号则默认参数 */
					else {sb.append(" and ");}
				}
			} else {sb.append(" or ");}
			
			/* 多个字段条件 */
			String[] names = param.getName().split(DH);
			if (param.getJoinTable() == null) {
				/* 获取表字段参数 */
				for (int n = 0, m = names.length; n < m; n++) {
					String[] tbTV = getTableFAndV(fields.get(names[n]), names[n]);
					names[n] = TABLE.alias() + ".`" + tbTV[0] + "`";
				}
			} else {
				/* 获取连接表字段参数 */
				for (int n = 0, m = names.length; n < m; n++) {
					String joinField = param.getJoinTable().getJoin().getTableField(names[n]);
					names[n] = param.getJoinTable().getJoinAlias() + ".`" + joinField + "`";
				}
			}
			/* 追加条件Sql */
			appendConditionSql(sb, param, names, param.getValue(), dtoValues);
		}
		
		/* 最后如果是or要追加结束括号 */
		if (isKH) {sb.append(")");}
		/* 最后追加条件 */
		sb.append(getSqlWhere(condition.getBaseDto()));
		return dtoValues;
	}
	
	/* 追加条件语法 */
	private void appendConditionSql(StringBuilder sb, Param param, String[] names, Object val, List<Object> dtoValues) {
		/* 特殊条件处理 */
		if (Operation.Is_Null == param.getOperation() || Operation.Is_Not_Null == param.getOperation()) {
			sb.append(names[0] + param.getOperation().getOptVal());
			return;
		}
		/* 如果值为空则条件直接不成立 - 空字符，空集合，空Map，空数组或NULL都算空 - Not_In相反 */
		if (ObjectUtils.isEmpty(val)) {sb.append(Operation.Not_In == param.getOperation() ? "1 = 1" : "1 = 0"); return;}
		
		/* 其他条件判断 */
		if (Operation.Find_In_Set == param.getOperation()) {
			sb.append(param.getOperation().getOptVal() + "(?," + names[0] + ")");
		} else {
			if (Operation.In == param.getOperation() || Operation.Not_In == param.getOperation()) {
				if (names.length == 1) {sb.append(names[0] + param.getOperation().getOptVal());}
				else {sb.append("(" + String.join(",", names) + ")" + param.getOperation().getOptVal());}
				
				/* 重新取字段名 */
				names = param.getName().split(DH);
				sb.append("(");
				/* 集合做循环处理 */
				if (val instanceof Collection) {
					for (Object vl : (Collection<?>) val) {
						setIn(sb, names, dtoValues, vl);
					}
				} else if(val.getClass().isArray()) {
					for (int i = 0, j = Array.getLength(val); i < j; i++) {
						setIn(sb, names, dtoValues, Array.get(val, i));
					}
				} else {setIn(sb, names, dtoValues, val);}
				appendFkh(sb);
				return;
			} else {
				sb.append(names[0] + param.getOperation().getOptVal());
				if (Operation.Like == param.getOperation() || Operation.Not_Like == param.getOperation()) {
					sb.append("CONCAT(" + (param.isLeft() ? "'%',?" : "?") + (param.isRight() ? ",'%')" : ")"));
				} else {sb.append("?");}
			}
		}
		dtoValues.add(val);
	}
	/* 设置In操作条件 */
	private void setIn(StringBuilder sb, String[] names, List<Object> dtoValues, Object val) {
		if (names.length == 1) {sb.append("?,"); dtoValues.add(val);}
		else {
			if (!(val instanceof BaseDto)) {throw new IllegalArgumentException(String.format("In操作使用了多个条件，但是值类型[%s]没有继承至BaseDto！", val.getClass().getName()));}
			try {
				Map<String, Field> fields = FieldUtil.getAllDeclaredField(val.getClass(), true);
				sb.append("(");
				for (String name : names) {sb.append("?,"); dtoValues.add(fields.get(name).get(val));}
				appendFkh(sb); sb.append(",");
			} catch (Exception e) {
				throw new RuntimeException(String.format("添加In操作条件异常，对应值类型[%s], 对应字段[%s]", val.getClass().getName(), JSONObject.toJSONString(names)));
			}
		}
	}
	
	/* 删除最后一个字符追加反扩号 */
	private void appendFkh(StringBuilder sb) {
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
	}
	
	/* 按英文冒号分割表字段名 */
	private String[] splitValue(String tbKey) {
		if (tbKey == null) {return null;}
		if (tbKey.contains(":")) {return tbKey.split(":");}
		else {return new String[]{tbKey};}
	}
	
	/* 批量添加sql */
	private int insertSql(Collection<D> ds) throws Throwable {
		StringBuilder key = new StringBuilder();
		StringBuilder value = new StringBuilder();
		setCreateUpdateTimes(ds); //批量设置创建与更新时间
		
		/* 字段映射 */
		Map<String, String> fields = TABLES.get(TABLE.name());
		/* 用于判断是否有默认值 */
		Map<String, List<List<Object>>> parmera = new HashMap<>(fields.size());
		
		for (D d : ds) {
			if (d == null) {throw new NullPointerException("添加不能传入空对象！");}
			List<List<Object>> datas = new ArrayList<>();
			key.append(" ("); value.append(" (");
			
			/* dto字段值 - 追加字段与值 */
			List<Object> dtoValues = new ArrayList<>();
			for (Entry<String, String> entry : fields.entrySet()) {
				Field dtoField = DTO_FIELD.get(entry.getKey());
				String[] tbKeys = splitValue(entry.getValue());
				Object data = dtoField.get(d); //dto对应字段数据
				
				if (data == null) {
					if (tbKeys.length > 1) { //如果没有数据且实体类有默认值
						key.append("`" + tbKeys[0] + "`,");
						value.append(tbKeys[1] + ",");
					}
				} else { //有数据则直接追加
					key.append("`" + tbKeys[0] + "`,");
					value.append("?,");
					dtoValues.add(data);
				}
			}
			
			/* 添加字段值 */
			datas.add(dtoValues);
			/* sql尾部操作 */
			if (key.length() > 2) {
				key.deleteCharAt(key.length() - 1);
				value.deleteCharAt(value.length() - 1);
			}
			key.append(")"); value.append(")");
			
			/* 生成的sql */
			String sql = "insert into `" + getTableName(d) + "`" + key.toString() + " values " + value.toString();
			/* 用于后续多个添加 */
			if (parmera.containsKey(sql)) {parmera.get(sql).addAll(datas);} else {parmera.put(sql, datas);}
			/* 置空用于继续追加 */
			key.setLength(0); value.setLength(0);
		}
		/* 批量插入 */
		return insertBatch(parmera);
	}
	/* 批量插入并返回插入成功数 */
	private int insertBatch(Map<String, List<List<Object>>> parmera) throws Throwable {
		/* 判断是否全部成功 */
		int successSum = 0;
		for (Entry<String, List<List<Object>>> pms : parmera.entrySet()) {
			List<Object[]> dtoValues = pms.getValue().stream().map(v -> v.toArray()).collect(Collectors.toList());
			
			/* Sql打印 */
			debugSql(pms.getKey(), dtoValues);
			/* 执行添加 */
			int[] batchUpdate = getJdbcTemplate().batchUpdate(pms.getKey(), dtoValues);
			/* 追加成功数量 */
			for (int rs : batchUpdate) {successSum = rs > 0 ? successSum + 1 : successSum;}
		}
		/* 返回是否与成功数一致 */
		return successSum;
	}
	
	/* 更新参数 */
	private List<Object> updatePamera(Map<String, String> fields, D d, StringBuilder sb, boolean isUpdateNull) throws IllegalArgumentException, IllegalAccessException {
		/* dto字段值 */
		List<Object> dtoValues = new ArrayList<>();
		/* 追加条件 */
		for (Entry<String, String> entry : fields.entrySet()) {
			Field dtoField = DTO_FIELD.get(entry.getKey());
			String[] tbKeys = splitValue(entry.getValue());
			Object data = dtoField.get(d);
			/* 不更新参数为空的值 */
			if (data == null && !isUpdateNull) {continue;}
			
			sb.append(TABLE.alias() + ".`" + tbKeys[0] + "` = ?,");
			dtoValues.add(data);
		}
		/* 如果没有设置的列则不更新 */
		if (dtoValues.size() == 0) {return null;}
		/* sql尾部操作 */
		sb.deleteCharAt(sb.length() - 1);
		return dtoValues;
	}
	
	/**
	 * @描述 按约定分割存放表字段名称，默认驼峰标志作为分割点追加[_]符号，有默认值则用[:]分割追加默认值，默认值只对添加有效
	 * @param fields key=实体类字段名，value=实体类字段
	 * @return key=实体类字段名，value=表字段名（有默认值则追加[:默认值]，默认值只对添加有效）
	 */
	protected LinkedHashMap<String, String> setFieldTable(Map<String, Field> fields) {
		LinkedHashMap<String, String> result = new LinkedHashMap<>(fields.size());
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Field> entry : fields.entrySet()) {
			Column column = entry.getValue().getAnnotation(Column.class);
			
			/* 如果没有注解或者注解名称为空内容 */
			if (column == null || !StringUtils.hasText(column.name())) {
				int start = 0;String tableField = "";
				/* 迭代每个字符判断 */
				char[] charArray = entry.getKey().toCharArray();
				for (int i = 0, j = charArray.length; i < j; i++) {
					if (charArray[i] >= 65 && charArray[i] <= 90) {
						tableField += (entry.getKey().substring(start, i) + "_");
						start = i;
					}
				}
				
				/* 追加表字段名 */
				sb.append(tableField + entry.getKey().substring(start));
				/* 追加默认值 */
				if (column != null && StringUtils.hasText(column.value())) {
					sb.append(":" + column.value());
				}
			} else {
				sb.append(column.name());
				/* 追加默认值 */
				if (StringUtils.hasText(column.value())) {
					sb.append(":" + column.value());
				}
			}
			
			/* 存放按约定分割的字符 */
			result.put(entry.getKey(), sb.toString().toLowerCase());
			sb.setLength(0);
		}
		return result;
	}
	
	/* 初始化设置该实体类信息 */
	private Table setTable() {
		/* 该类型必须是带泛型类 */
		Class<?> entityClass = getGenericsClass(1);
		RECORD.add(entityClass);
		
		/* 扫描配置参数 */
		Table table = entityClass.getDeclaredAnnotation(Table.class);
		if (table == null) {throw new NullPointerException(String.format("继承AbstractService后泛型对应的Entity[%s]必须申明@Table注解！", entityClass.getName()));}
		if(TABLES.containsKey(table.name())) {
			/* 如果存在类则不初始化 - 否则代表不存在类但是表名相同 */
			if (RECORD.contains(entityClass)) {return table;}
			throw new IllegalArgumentException(String.format("存在该表名[%s]，请修改[%s]类下的表名！", table.name(), entityClass.getName()));
		}
		
		/* 重设别名 */
		if (!StringUtils.hasText(table.alias())) {
			try {MyUtil.setAnnotationValue(table, "alias", "t_" + table.name());}
			catch (Exception e) {
				throw new IllegalArgumentException(String.format("重设注解值[%s]失败，msg》》》 ", table.annotationType().getName(), e.toString()));
			}
		}
		
		/* 获取所有参数字段 */
		Field[] declaredFields = DTO_CLASS.getDeclaredFields();
		for (Field field : declaredFields) {
			if (!isBaseType(field.getType())) {
				field.setAccessible(true);
				subFields.put(field.getName(), field); //非基本类型记录
			}
		}
		
		/* 获取所有字段 */
		Map<String, Field> fields = FieldUtil.getAllDeclaredField(entityClass, true);
		for (String fieldName : fields.keySet()) {
			if (!DTO_FIELD.containsKey(fieldName)) {
				throw new NullPointerException(String.format("Dto[%s]中找不到Entity[%s]对应的字段名[%s]！", DTO_CLASS.getName(), entityClass.getName(), fieldName));
			}
		}
		
		/* 设置对应表信息 */
		TABLES.put(table.name(), setFieldTable(fields));
		return table;
	}
	
	/* 获取当前类对应泛型类型 */
	private Class<?> getGenericsClass(int i){
		Class<?> curClass = this.getClass(); //当前类
		while (true) { //父类方法代表当前类肯定会继承
			if (curClass.getSuperclass() != AbstractService.class) {curClass = curClass.getSuperclass(); continue;}
			return (Class<?>) ((ParameterizedType) curClass.getGenericSuperclass()).getActualTypeArguments()[i];
		}
	}
	
	/* 获取自定义字段Sql */
	private String getSqlField(BaseDto baseDto) {
		if (baseDto.getUserSqlDto() == null || baseDto.getUserSqlDto().getFields() == null) {return NULLSTR;}
		else {return baseDto.getUserSqlDto().getFields();}
	}
	/* 获取自定义连接Sql */
	private String getSqlJoin(BaseDto baseDto) {
		if (baseDto.getUserSqlDto() == null || baseDto.getUserSqlDto().getJoin() == null) {return NULLSTR;}
		else {return " " + baseDto.getUserSqlDto().getJoin();}
	}
	/* 获取自定义条件Sql */
	private String getSqlWhere(BaseDto baseDto) {
		if (baseDto.getUserSqlDto() == null || baseDto.getUserSqlDto().getWhere() == null) {return NULLSTR;}
		else {return " " + baseDto.getUserSqlDto().getWhere();}
	}
	/* 获取自定义分组条件Sql */
	private String getSqlHaving(BaseDto baseDto) {
		if (baseDto.getUserSqlDto() == null || baseDto.getUserSqlDto().getHaving() == null) {return NULLSTR;}
		else {return " having " + baseDto.getUserSqlDto().getHaving();}
	}
	
	/* 获取表字段参数 */
	private String[] getTableFAndV(String tableValue, String fieldName) {
		String[] tbKeys = splitValue(tableValue);
		if (tbKeys == null) {throw new IllegalArgumentException(String.format("请检查条件参数[%s]的名称是否在实体类[%s]中存在！", fieldName, TABLE.name()));}
		return tbKeys;
	}
}
