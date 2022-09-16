package org.city.common.mysql.repository;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Instant;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.city.common.api.annotation.sql.Column;
import org.city.common.api.annotation.sql.Table;
import org.city.common.api.constant.MathSql;
import org.city.common.api.constant.Operation;
import org.city.common.api.constant.SqlType;
import org.city.common.api.dto.DataList;
import org.city.common.api.dto.sql.Condition;
import org.city.common.api.dto.sql.Condition.GroupBy;
import org.city.common.api.dto.sql.Condition.Join;
import org.city.common.api.dto.sql.Condition.JoinTable;
import org.city.common.api.dto.sql.Condition.OrderBy;
import org.city.common.api.dto.sql.Condition.Param;
import org.city.common.api.dto.sql.SubCondition;
import org.city.common.api.entity.BaseEntity;
import org.city.common.api.entity.TimeEntity;
import org.city.common.api.in.interceptor.SqlInterceptor;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.sql.AnnotationCondition;
import org.city.common.api.in.sql.Crud;
import org.city.common.api.in.sql.MyDataSource;
import org.city.common.api.util.FieldUtil;
import org.city.common.api.util.MyUtil;
import org.city.common.api.util.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 11:16:37
 * @版本 1.0
 * @描述 公共仓储
 */
@Slf4j
public abstract class AbstractRepository<E extends BaseEntity> implements Crud<E>,JSONParser {
	/* 存储所有继承该类的表信息 */
	private final static Map<String, LinkedHashMap<String, String>> TABLES = new HashMap<>(8);
	/* 存储所有数据源对应模板 */
	private final static Map<DataSource, JdbcTemplate> JDBC_TEMPLATES = new ConcurrentHashMap<>();
	/* 默认数据源对应模板 */
	private final static JdbcTemplate DEFAULT_JDBC_TEMPLATE = SpringUtil.getBean(JdbcTemplate.class);
	/* 自定义拦截器 */
	@Autowired(required = false)
	private List<SqlInterceptor> sqlInterceptors;
	/* 父类所有泛型信息 */
	private final Map<String, Class<?>> GENERICS_CLASS = getGenericSuperClass(this.getClass());
	/* 实体类型 */
	@SuppressWarnings("unchecked")
	private final Class<E> ENTITY_CLASS = (Class<E>) GENERICS_CLASS.get("E");
	/* 该实体类注解 */
	private final Table TABLE = setTable();
	/* 我的数据源 */
	private final MyDataSource MY_DATA_SOURCE = getMyDataSource();
	/* 空字符 */
	private final String NULLSTR = "";
	/* 英文逗号 */
	private final String DH = ",";
	/* 自定义拦截 */
	private void interceptor(SqlType sqlType, String sql, Object param, long execTime) {
		if (sqlInterceptors != null) {
			StackTraceElement execStack = null; //执行类的当前栈
			final String name = this.getClass().getName();
			final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			/* 查找指定栈 */
			for (int i = 0, j = stackTrace.length; i < j; i++) {
				if (stackTrace[i].getClassName().equals(name)) {execStack = stackTrace[i]; break;}
				else if (stackTrace[i].getClassName().contains(name)) {execStack = stackTrace[i + 1]; break;}
			}
			for (SqlInterceptor sqlInterceptor : sqlInterceptors) {
				sqlInterceptor.exec(sqlType, sql, param, execTime, execStack);
			}
		}
	}
	/* 执行Sql */
	private <R> R execSql(SqlType sqlType, String sql, Object param, Supplier<R> exec) {
		if (log.isDebugEnabled()) {log.debug("执行的Sql信息[{}]，Sql参数信息》》》 \r\n{}", sql, JSONObject.toJSONString(param == null ? Collections.EMPTY_LIST : param, true));}
		long recordTime = System.currentTimeMillis(); //记录执行前时间 - 用于计算实际执行时间
		try {return exec.get();} finally {interceptor(sqlType, sql, param, (System.currentTimeMillis() - recordTime));}
	}
	/* 获取Sql模板 */
	private JdbcTemplate getJdbcTemplate(boolean write) {
		return MY_DATA_SOURCE == null ? DEFAULT_JDBC_TEMPLATE : JDBC_TEMPLATES.computeIfAbsent(MY_DATA_SOURCE.getDataSource(write), dataSource -> {
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			jdbcTemplate.execute("set global group_concat_max_len=" + Integer.MAX_VALUE);
			jdbcTemplate.execute("set session group_concat_max_len=" + Integer.MAX_VALUE);
			return jdbcTemplate;
		});
	}
	/* 获取我的数据源 */
	private MyDataSource getMyDataSource() {
		List<? extends MyDataSource> myDataSources = SpringUtil.getBeans(TABLE.dataSource());
		Assert.isTrue(myDataSources.size() < 2, String.format("实体类[%s]对应数据源有多个实现！", TABLE.name()));
		if (myDataSources.isEmpty()) { //默认数据源
			DEFAULT_JDBC_TEMPLATE.execute("set global group_concat_max_len=" + Integer.MAX_VALUE);
			DEFAULT_JDBC_TEMPLATE.execute("set session group_concat_max_len=" + Integer.MAX_VALUE);
			return null;
		} else {return myDataSources.iterator().next();} //自定义数据源
	}
	
	@Override
	public Table getTable() {return this.TABLE;}
	@Override
	public String getTableField(String fieldName) {return getTableFAndV(TABLES.get(TABLE.name()).get(fieldName), fieldName)[0];}
	
	@Override
	public Map<String, String> getTableFields() {
		Map<String, String> tableFields = new LinkedHashMap<>();
		for (Entry<String, String> entry : TABLES.get(TABLE.name()).entrySet()) {
			tableFields.put(entry.getKey(), splitValue(entry.getValue())[0]);
		}
		return tableFields;
	}
	
	@Override
	public AnnotationCondition getJoin(E entity, int...groups) {
		return AnnotationConditionRepository.getJoin(entity, this, groups);
	}
	
	@Override
	public SubCondition findBySubCondition(Condition condition, boolean useTableField) {
		/* 是否有接收字段 */
		boolean hasField = condition.getFields().size() > 0;
		/* 是否分组分页 */
		boolean hasGroup = condition.getGroupBys().size() > 0;
		boolean hasOrder = condition.getOrderBys().size() > 0;
		/* 处理分组排序 */
		Map<GroupBy, String> groups = handlerGroupBy(condition);
		Map<OrderBy, String> orders = handlerOrderBy(condition);
		/* 分组分页处理 */
		Map<String, List<OrderBy>> groupOrder = getGroupOrders(condition, hasGroup, hasOrder, orders, true);
		
		/* 字段映射 */
		Map<String, String> fields = TABLES.get(TABLE.name());
		/* 条件参数 */
		List<Object> entityValues = new ArrayList<>();
		/* 查询所有接收字段名 */
		Set<String> subFieldName = new HashSet<>();
		/* 查询Sql追加 */
		StringBuilder sb = querySqlAppend(fields, false, hasField, condition, entityValues, subFieldName, useTableField, groupOrder);
		/* 追加条件 */
		whereCondition(condition, fields, sb, entityValues);
		
		/* 先分组 */
		if (hasGroup) {groupBy(condition, sb, groups, entityValues);}
		/* 在排序 */
		if (hasOrder) {orderBy(condition, sb, orders);}
		
		/* 最后分页 */
		if (condition.getPage() != null) {
			sb.append(" limit " + condition.getPage().getOffset() + "," + condition.getPage().getPageSize());
		}
		return new SubCondition(sb.toString(), entityValues, subFieldName, TABLE.alias(), false);
	}
	/* 获取分组分页 */
	private Map<String, List<OrderBy>> getGroupOrders(Condition condition, boolean hasGroup, boolean hasOrder, Map<OrderBy, String> orders, boolean notCount) {
		Map<String, List<OrderBy>> groupOrder = hasGroup ? new HashMap<>() : null;
		if (notCount && hasGroup && hasOrder) {
			for (Entry<OrderBy, String> entry : orders.entrySet()) {
				OrderBy orderBy = entry.getKey();
				if (Boolean.FALSE.equals(orderBy.getGroup())) {continue;} //只排序外部则不处理
				
				/* 只处理有关联的排序 */
				JoinTable joinTable = orderBy.getJoinTable();
				if (joinTable != null) {
					OrderBy go = new OrderBy(entry.getValue(), orderBy.isAsc(), orderBy.getGroup(), joinTable);
					groupOrder.computeIfAbsent(joinTable.getJoinAlias(), k -> new ArrayList<>()).add(go);
					
					/* 排序字段接收名称 */
					entry.setValue(entry.getValue().replace(".", "$"));
					/* 添加排序字段 */
					Join join = condition.getJoin(joinTable.getJoinAlias());
					if (join == null) {condition.addField(entry.getValue(), orderBy.isAsc() ? MathSql.Min : MathSql.Max, orderBy.getName());}
					else {join.addField(orderBy.isAsc() ? MathSql.Min : MathSql.Max, entry.getValue(), orderBy.getName());}
				}
			}
		}
		return groupOrder;
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
	public <R> R findOne(Condition condition) {
		List<R> querySql = findAll(condition.limitOffset(0, 1));
		return querySql.size() > 0 ? querySql.get(0) : null;
	}
	@Override
	public <R> List<R> findAll(Condition condition) {
		try {
			return querySql(condition);
		} catch (Throwable e) {
			throw new DataAccessResourceFailureException("查询Sql执行失败！", e);
		}
	}
	
	@Override
	public <R> DataList<R> findAllByCount(Condition condition) {
		long count = count(condition), curCount = (condition.getBaseEntity().getPageNum() - 1) * condition.getBaseEntity().getPageSize();
		return (count - curCount) > 0 ? new DataList<>(findAll(condition), count) : new DataList<>(Collections.emptyList(), count);
	}
	
	@Override
	public boolean add(E e) {
		return addBatch(Arrays.asList(e)) > 0;
	}
	@Override
	public int addBatch(Collection<E> es) {
		try {
			return insertSql(es);
		} catch (Throwable e) {
			throw new DataAccessResourceFailureException("添加Sql执行失败！", e);
		}
	}
	
	@Override
	public long getLastAddId() {
		try {
			/* 必须在事务中执行该方法 */
			if (!TransactionSynchronizationManager.isActualTransactionActive()) {throw new NoTransactionException("请在事务中执行该方法！");}
			/* 获取自增主键 */
			final String sql = "select last_insert_id()";
			Long result = execSql(SqlType.SELECT, sql, null, () -> getJdbcTemplate(false).queryForObject(sql, Long.class));
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
			sb.append("delete " + TABLE.alias() + " from `" + getTableName(condition.getBaseEntity()) + "` " + TABLE.alias() + " ");
			
			/* 追加参数 */
			List<Object> entityValues = new ArrayList<>();
			/* 链表 */
			for (Join join : condition.getJoins().values()) {
				sb.append(join.toSql(join.getIgnore(), this, entityValues));
			}
			/* 最后追加链表 */
			sb.append(getSqlJoin(condition.getBaseEntity(), entityValues));
			
			/* 追加条件 */
			whereCondition(condition, fields, sb, entityValues);
			
			/* 执行删除 */
			final String sql = sb.toString();
			return execSql(SqlType.DELETE, sql, entityValues, () -> getJdbcTemplate(true).update(sql, entityValues.toArray()));
		} catch (Throwable e) {
			throw new DataAccessResourceFailureException("删除Sql执行失败！", e);
		}
	}
	
	@Override
	public boolean update(Condition condition, boolean isUpdateNull) {
		try {
			setUpdateTime(condition.getBaseEntity()); //设置更新时间
			
			/* 字段映射 */
			Map<String, String> fields = TABLES.get(TABLE.name());
			StringBuilder sb = new StringBuilder();
			sb.append("update `" + getTableName(condition.getBaseEntity()) + "` " + TABLE.alias() + " ");
			
			/* 追加参数 */
			List<Object> entityValues = new ArrayList<>();
			/* 链表 */
			for (Join join : condition.getJoins().values()) {
				sb.append(join.toSql(join.getIgnore(), this, entityValues));
			}
			/* 最后追加链表 */
			sb.append(getSqlJoin(condition.getBaseEntity(), entityValues) + " set ");
			
			/* 如果没有设置的列则不更新 */
			if (updatePamera(condition.getEntityFields(), fields, condition.getBaseEntity(), sb, isUpdateNull, entityValues)) {return false;}
			/* 追加条件 */
			whereCondition(condition, fields, sb, entityValues);
			
			/* 执行更新 */
			final String sql = sb.toString();
			return execSql(SqlType.UPDATE, sql, entityValues, () -> getJdbcTemplate(true).update(sql, entityValues.toArray()) > 0);
		} catch (Throwable e) {
			throw new DataAccessResourceFailureException("更新Sql执行失败！", e);
		}
	}
	
	/* 设置更新时间 */
	private void setUpdateTime(BaseEntity e) {
		if (e instanceof TimeEntity) {((TimeEntity) e).setUpdateTime(Timestamp.from(Instant.now()));}
	}
	/* 批量设置更新时间 */
	private void setUpdateTimes(Collection<E> entitys) {
		Timestamp nowTime = Timestamp.from(Instant.now());
		for (E e : entitys) {if (e instanceof TimeEntity) {((TimeEntity) e).setUpdateTime(nowTime);}}
	}
	/* 批量设置创建与更新时间 */
	private void setCreateUpdateTimes(Collection<E> entitys) {
		Timestamp nowTime = Timestamp.from(Instant.now());
		for (E e : entitys) {if (e instanceof TimeEntity) {((TimeEntity) e).setCreateTime(nowTime).setUpdateTime(nowTime);}}
	}
	
	@Override
	public int updateBatch(List<Param> params, Collection<E> entitys, boolean isUpdateNull) {
		try {
			if (CollectionUtils.isEmpty(params)) {throw new IllegalArgumentException("批量更新不允许无条件执行！");}
			if (CollectionUtils.isEmpty(entitys)) {throw new NullPointerException("批量更新不能传入空对象！");}
			setUpdateTimes(entitys); //批量设置更新时间
			
			/* 字段映射 */
			Map<String, String> fields = TABLES.get(TABLE.name());
			StringBuilder sb = new StringBuilder();
			sb.append("update `" + getTableName(entitys.iterator().next()) + "` " + TABLE.alias() + " set ");
			
			/* 追加参数 */
			List<Object> entityValues = new ArrayList<>();
			/* 参数字段配置 */
			List<Field> paramFields = new ArrayList<>();
			/* 获取条件实体类字段 */
			Map<String, Field> entityFields = FieldUtil.getAllDeclaredField(entitys.iterator().next().getClass(), true);
			/*获取参数条件Sql*/
			String paramSql = getParamSql(entityFields, params, fields, paramFields);
			
			/* 如果没有设置的列则不更新 */
			if (updatePameras(entityFields, fields, paramSql, entitys, isUpdateNull, paramFields, sb, entityValues)) {return 0;}
			/* 追加条件 */
			whereCondition(entityFields, params, entitys, sb, fields, entityValues);
			
			/* 执行更新 */
			final String sql = sb.toString();
			return execSql(SqlType.UPDATE, sql, entityValues, () -> getJdbcTemplate(true).update(sql, entityValues.toArray()));
		} catch (Throwable e) {
			throw new DataAccessResourceFailureException("批量更新Sql执行失败！", e);
		}
	}
	
	@Override
	public int update(String sql) {
		try {return execSql(SqlType.UPDATE, sql, null, () -> getJdbcTemplate(true).update(sql));}
		catch (Throwable e) {throw new DataAccessResourceFailureException("手动更新Sql执行失败！", e);}
	}
	@Override
	public void execute(String sql) {
		try {execSql(SqlType.EXECUTE, sql, null, () -> {getJdbcTemplate(true).execute(sql); return null;});}
		catch (Throwable e) {throw new DataAccessResourceFailureException("手动执行Sql失败！", e);}
	}
	@Override
	public <T> T queryOne(String sql, Class<T> type) {
		try {return execSql(SqlType.SELECT, sql, null, () -> getJdbcTemplate(false).queryForObject(sql, type));}
		catch (Throwable e) {throw new DataAccessResourceFailureException("手动查询Sql执行失败！", e);}
	}
	@Override
	public Map<String, Object> queryMap(String sql) {
		try {return execSql(SqlType.SELECT, sql, null, () -> getJdbcTemplate(false).queryForMap(sql));}
		catch (Throwable e) {throw new DataAccessResourceFailureException("手动查询Sql执行失败！", e);}
	}
	
	/* 获取批量更新条件 */
	private void whereCondition(Map<String, Field> entityFields, List<Param> params, Collection<E> es, StringBuilder sb, Map<String, String> fields, List<Object> entityValues) throws Exception {
		/* 开始条件 - 批量更新一定有条件 */
		sb.append(" where ");
		
		/* 参数条件保存 */
		Map<String, String> names = new LinkedHashMap<>();
		for (Param param : params) {
			Assert.isTrue(param.isAnd(), String.format("批量更新必须And条件，当前字段[%s]非And条件！", param.getName()));
			/* 获取表字段参数 */
			String[] tbFV = getTableFAndV(fields.get(param.getName()), param.getName());
			names.put(param.getName(), TABLE.alias() + ".`" + tbFV[0] + "`");
		}
		
		/* 参数条件追加 */
		sb.append("(" + String.join(",", names.values()) + ") in (");
		/* 追加值 */
		for (E e : es) {
			sb.append("(");
			for (String name : names.keySet()) {
				sb.append("?,");
				entityValues.add(entityFields.get(name).get(e));
			}
			appendFkh(sb); sb.append(",");
		}
		appendFkh(sb);
	}
	
	/* 更新参数 */
	private boolean updatePameras(Map<String, Field> entityFields, Map<String, String> fields, String paramSql, Collection<E> es, boolean isUpdateNull, List<Field> paramFields, StringBuilder sb, List<Object> entityValues) throws Exception {
		/* 追加条件 */
		for (Entry<String, String> entry : fields.entrySet()) {
			Field entityField = entityFields.get(entry.getKey());
			String[] tbKeys = splitValue(entry.getValue());
			/* 批量追加更新 */
			sb.append(getFieldWhenSql(TABLE.alias() + ".`" + tbKeys[0] + "`", es, entityField, isUpdateNull, paramSql, paramFields, entityValues));
		}
		
		/* 如果没有设置的列则不更新 */
		if (entityValues.size() == 0) {return true;}
		/* Sql尾部操作 */
		sb.deleteCharAt(sb.length() - 1);
		return false;
	}
	
	/* 获取字段条件Sql */
	private String getFieldWhenSql(String tableField, Collection<E> es, Field entityField, boolean isUpdateNull, String paramSql, List<Field> paramFields, List<Object> entityValues) throws Exception {
		/* 是否添加标识 */
		boolean isAdd = false;
		StringBuilder sb = new StringBuilder(tableField + " = case ");
		
		/* 拼接所有条件Sql */
		for (E e : es) {
			if (e == null) {throw new NullPointerException("更新不能传入空对象！");}
			
			/* 不更新参数为空的值 */
			Object data = entityField.get(e);
			if (data == null && !isUpdateNull) {continue;}
			
			/* 追加更新参数 */
			sb.append("when " + paramSql + " then ? ");
			for (Field paramField : paramFields) {entityValues.add(paramField.get(e));}
			entityValues.add(data);
			isAdd = true;
		}
		sb.append("else " + tableField + " end,");
		return isAdd ? sb.toString() : "";
	}
	/* 获取参数条件Sql */
	private String getParamSql(Map<String, Field> entityFields, List<Param> params, Map<String, String> fields, List<Field> paramFields) {
		StringBuilder paramSqlSb = new StringBuilder();
		boolean isAppend = false;
		for (Param param : params) {
			Assert.isTrue(param.isAnd(), String.format("批量更新必须And条件，当前字段[%s]非And条件！", param.getName()));
			if (isAppend) {paramSqlSb.append(" and ");} else {isAppend = true;}
			
			/* 获取表字段参数 */
			String[] tbFV = getTableFAndV(fields.get(param.getName()), param.getName());
			paramSqlSb.append(TABLE.alias() + ".`" + tbFV[0] + "` = ?");
			paramFields.add(entityFields.get(param.getName()));
		}
		return paramSqlSb.toString();
	}
	
	/* 统计数量 */
	private long countSql(Condition condition) throws Throwable {
		/* 是否有接收字段 */
		boolean hasField = condition.getFields().size() > 0;
		/* 是否分组排序 */
		boolean hasGroup = condition.getGroupBys().size() > 0;
		boolean hasOrder = condition.getOrderBys().size() > 0;
		/* 处理分组排序 */
		Map<GroupBy, String> groups = handlerGroupBy(condition);
		Map<OrderBy, String> orders = handlerOrderBy(condition);
		/* 分组分页处理 */
		Map<String, List<OrderBy>> groupOrder = getGroupOrders(condition, hasGroup, hasOrder, orders, false);
		
		/* 字段映射 */
		Map<String, String> fields = TABLES.get(TABLE.name());
		/* 条件参数 */
		List<Object> entityValues = new ArrayList<>();
		/* 查询Sql追加 */
		StringBuilder sb = querySqlAppend(fields, true, hasField, condition, entityValues, new HashSet<>(), false, groupOrder);
		/* 追加条件 */
		whereCondition(condition, fields, sb, entityValues);
		
		/* 分组 */
		if (hasGroup) {
			groupBy(condition, sb, groups, entityValues);
			/* 对原来的Sql外部包一层 */
			sb = new StringBuilder("select count(1) from (" + sb.toString() + ") t");
		}
		
		/* 执行统计 */
		final String sql = sb.toString();
		return execSql(SqlType.SELECT, sql, entityValues, () -> getJdbcTemplate(false).queryForObject(sql, long.class, entityValues.toArray()));
	}
	
	/* 分组 */
	private void groupBy(Condition condition, StringBuilder sb, Map<GroupBy, String> groups, List<Object> entityValues) {
		sb.append(" group by ");
		/* 处理所有分组 */
		for (Entry<GroupBy, String> entry : groups.entrySet()) {
			sb.append(entry.getValue() + ",");
		}
		/* 删除最后的逗号 */
		sb.deleteCharAt(sb.length() - 1);
		/* 最后追加分组条件 */
		sb.append(getSqlHaving(condition.getBaseEntity(), entityValues));
	}
	/* 处理分组 */
	private Map<GroupBy, String> handlerGroupBy(Condition condition) {
		Map<GroupBy, String> groups = new LinkedHashMap<>(condition.getGroupBys().size());
		Map<String, String> tableFields = getTableFields(); //当前表映射字段
		
		/* 迭代处理 */
		for (GroupBy groupBy : condition.getGroupBys()) {
			String groupName = null; //分组名
			if (groupBy.getJoinTable() == null) { //当前表分组
				if (tableFields.containsKey(groupBy.getName())) {
					groupName = TABLE.alias() + "." + tableFields.get(groupBy.getName());
					groupBy.setJoinTable(new JoinTable(this, TABLE.alias())); //当前表
				} else {groupName = groupBy.getName();}
			} else {
				JoinTable jt = groupBy.getJoinTable(); //连接表分组
				groupName = jt.getJoinAlias() + "." + jt.getJoin().getTableField(groupBy.getName());
			}
			groups.put(groupBy, groupName);
		}
		return groups;
	}
	/* 排序 */
	private void orderBy(Condition condition, StringBuilder orgin, Map<OrderBy, String> orders) {
		StringBuilder sb = new StringBuilder(" order by ");
		boolean hasOrder = false;
		/* 处理所有排序 */
		for (Entry<OrderBy, String> entry : orders.entrySet()) {
			if (Boolean.TRUE.equals(entry.getKey().getGroup())) {continue;} //只排序内部则不处理
			sb.append(entry.getValue() + (entry.getKey().isAsc() ? " asc," : " desc,"));
			hasOrder = true;
		}
		/* 如果有排序 */
		if (hasOrder) {
			/* 删除最后的逗号 */
			sb.deleteCharAt(sb.length() - 1);
			/* 最后追加排序条件 */
			orgin.append(sb.toString());
		}
	}
	/* 处理分页 */
	private Map<OrderBy, String> handlerOrderBy(Condition condition) {
		Map<OrderBy, String> orders = new LinkedHashMap<>(condition.getOrderBys().size());
		Map<String, String> tableFields = getTableFields(); //当前表映射字段
		
		/* 迭代处理 */
		for (OrderBy orderBy : condition.getOrderBys()) {
			String orderName = null; //排序名
			if (orderBy.getJoinTable() == null) { //当前表排序
				if (tableFields.containsKey(orderBy.getName())) {
					orderName = TABLE.alias() + "." + tableFields.get(orderBy.getName());
					orderBy.setJoinTable(new JoinTable(this, TABLE.alias())); //当前表
				} else {orderName = orderBy.getName();}
			} else {
				JoinTable jt = orderBy.getJoinTable(); //连接表排序
				orderName = jt.getJoinAlias() + "." + jt.getJoin().getTableField(orderBy.getName());
			}
			orders.put(orderBy, orderName);
		}
		return orders;
	}
	
	/* 查询Sql追加 */
	private StringBuilder querySqlAppend(Map<String, String> fields, boolean isCount, boolean hasField, Condition condition,
			List<Object> entityValues, Set<String> subFieldName, boolean useTableField, Map<String, List<OrderBy>> groupOrder) {
		StringBuilder sb = new StringBuilder();
		/* 如果是统计只需要简单追加 */
		if (isCount && condition.getGroupBys().isEmpty()) {sb.append("select count(1)");}
		else {
			sb.append("select ");
			/* 提前追加字段 */
			sb.append(getSqlField(condition.getBaseEntity()));
			
			/* 追加字段 */
			Map<String, String> tableFields = getTableFields();
			if (condition.getFields().size() > 0) {
				if (!hasField) {setReceiveField(fields, subFieldName, useTableField, sb, tableFields);} //设置当前表接收所有字段
				
				/* 其他字段处理 */
				for (org.city.common.api.dto.sql.Condition.Field field : condition.getFields()) {
					String receiveName = StringUtils.hasText(field.getReceiveFieldName()) ? field.getReceiveFieldName() : field.getFieldName();
					if (useTableField) { //使用表字段名
						String tableName = tableFields.get(receiveName);
						receiveName = tableName == null ? receiveName : tableName;
					}
					
					if (field.getSubField() == null) { //非子查询字段
						if (field.getJoinTable() == null) { //非子对象查询
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
						} else { //针对子对象追加查询字段
							String receiveField = null; //接收字段数据
							List<OrderBy> orders = groupOrder == null ? null : groupOrder.get(field.getJoinTable().getJoinAlias()); //分组排序
							int groupIndex = field.getJoinTable().getLimit() - 1; //分组结束下标
							
							/* Json化处理 */
							if (isArray(condition.getSubEntityFields(), condition.getBaseEntity().getClass(), receiveName)) { //数组类型处理
								if (field.getGroupField().size() > 0) {receiveField = appendGroupFieldJsonArray(field.getGroupField(), orders, groupIndex);}
								else {receiveField = String.format("json_extract(concat('[',(group_concat(DISTINCT %s%s)),']'),'$[0 to %d]')", appendJsonObject(field.getJoinTable()), getGroupOrderBy(orders), groupIndex);}
							} else {
								if (groupOrder == null) {receiveField = appendJsonObject(field.getJoinTable());} //无分组且非数组类型
								else {receiveField = String.format("json_extract(concat('[',(group_concat(DISTINCT %s%s)),']'),'$[0]')", appendJsonObject(field.getJoinTable()), getGroupOrderBy(orders));}
							}
							/* 设置接收字段 */
							sb.append(receiveField + " `" + receiveName + Condition.SPLIT + "`,");
						}
					} else { //子查询字段
						if (condition.getSubEntityFields().containsKey(receiveName)) {receiveName = receiveName + Condition.SPLIT;} //标记对象解析
						entityValues.addAll(field.getSubField().getParams());
						sb.append("(" + field.getSubField().getSql() + ") `" + receiveName + "`,");
					}
					subFieldName.add(receiveName); //查询所有接收字段名
				}
			} else {
				setReceiveField(fields, subFieldName, useTableField, sb, tableFields); //设置当前表接收所有字段
			}
			sb.deleteCharAt(sb.length() - 1);
			
			/* 如果有分组 - 替换为any_value函数 - 否则替换为空字符 */
			if (condition.getGroupBys().size() > 0) {sb = new StringBuilder(sb.toString().replace(MathSql.NORMAL, "any_value"));}
			else {sb = new StringBuilder(sb.toString().replace(MathSql.NORMAL, NULLSTR));}
		}
		
		if (condition.getSubTable() == null) { //非子查询表
			/* 忽略索引 */
			String index = StringUtils.hasText(condition.getIgnore()) ? " IGNORE INDEX(" + condition.getIgnore() + ")" : "";
			sb.append(" from `" + getTableName(condition.getBaseEntity()) + "` " + TABLE.alias() + index);
		} else { //子查询表
			condition.addFields(condition.getSubTable().parseField().values());
			entityValues.addAll(condition.getSubTable().getParams());
			sb.append(" from (" + condition.getSubTable().getSql() + ") " + TABLE.alias());
		}
		
		/* 链表 */
		for (Join join : condition.getJoins().values()) {
			sb.append(join.toSql(join.getIgnore(), this, entityValues));
		}
		/* 最后追加链表 */
		sb.append(getSqlJoin(condition.getBaseEntity(), entityValues));
		return sb;
	}
	/* 设置表接收字段 */
	private void setReceiveField(Map<String, String> fields, Set<String> subFieldName, boolean useTableField, StringBuilder sb, Map<String, String> tableFields) {
		for (Entry<String, String> entry : fields.entrySet()) {
			String receiveName = entry.getKey();
			if (useTableField) { //使用表字段名
				String tableName = tableFields.get(receiveName);
				receiveName = tableName == null ? receiveName : tableName;
			}
			
			String[] tbKeys = splitValue(entry.getValue());
			sb.append(MathSql.Normal.format(TABLE.alias() + ".`" + tbKeys[0] + "`") + " `" + receiveName + "`,");
			subFieldName.add(entry.getKey()); //查询所有接收字段名
		}
	}
	/* 获取分组排序 */
	private String getGroupOrderBy(List<OrderBy> orderBys) {
		if (CollectionUtils.isEmpty(orderBys)) {return NULLSTR;}
		StringBuilder sb = new StringBuilder(" order by ");
		for (OrderBy orderBy : orderBys) {
			sb.append(String.format("%s %s,", orderBy.getName(), orderBy.isAsc() ? "asc" : "desc"));
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	/* 验证接收字段是否数组 */
	private boolean isArray(Map<String, Field> subFields, Class<?> entityClass, String receiveName) {
		Assert.isTrue(subFields.containsKey(receiveName), String.format("实体类[%s]字段名或分组注解字段名[%s]必须非基本类型！", entityClass.getName(), receiveName));
		Class<?> fieldType = subFields.get(receiveName).getType();
		return fieldType.isArray() || Collection.class.isAssignableFrom(fieldType);
	}
	/* 追加分组字段Json数组对象参数 */
	private String appendGroupFieldJsonArray(Map<String, JoinTable> groupField, List<OrderBy> orders, int groupIndex) {
		StringBuilder jsonArr = new StringBuilder("json_extract(concat('[',group_concat(DISTINCT json_object(");
		for (Entry<String, JoinTable> entry : groupField.entrySet()) {
			String receiveField = appendJsonObject(entry.getValue()); //接收字段数据
			jsonArr.append(String.format("'%s',%s,", entry.getKey(), receiveField)); //条件拼接
		}
		/* 结尾处理 */
		return jsonArr.deleteCharAt(jsonArr.length() - 1).append(String.format(")%s),']'),'$[0 to %d]')", getGroupOrderBy(orders), groupIndex)).toString();
	}
	/* 追加Json对象参数 */
	private String appendJsonObject(JoinTable joinTable) {
		/* 追加连接表对象字段 */
		StringBuilder json = new StringBuilder("json_object(");
		for (Entry<String, String> entry : joinTable.getJoin().getTableFields().entrySet()) {
			if (joinTable.getIgnoreFields().contains(entry.getKey())) {continue;} //忽略字段
			String fieldName = MathSql.Normal.format(joinTable.getJoinAlias() + ".`" + entry.getValue() + "`");
			json.append(String.format("'%s',%s,", entry.getKey(), fieldName)); //条件拼接
		}
		/* 结尾处理 */
		return json.deleteCharAt(json.length() - 1).append(")").toString();
	}
	
	/* 公共查询实现代码 */
	private <R> List<R> querySql(Condition condition) throws Throwable {
		SubCondition subCondition = findBySubCondition(condition, false);
		/* 执行查询 */
		final String sql = subCondition.getSql();
		return execSql(SqlType.SELECT, sql, subCondition.getParams(), () -> queryResult(condition, getJdbcTemplate(false).queryForList(sql, subCondition.getParams().toArray())));
	}
	/* 查询返回结果 */
	private <R> List<R> queryResult(Condition condition, List<Map<String, Object>> resultMaps) {
		List<R> result = new ArrayList<>();
		for(Map<String, Object> data : resultMaps) {
			result.add(parseSub(condition.getSubEntityFields().values(), data, condition.getBaseEntity().getClass(), Condition.SPLIT));
		}
		return result;
	}
	/* 解析子对象 */
	private <R> R parseSub(Collection<Field> subFields, Object data, Class<?> parseType, String split) {
		R parse = parse(data, parseType); //最顶级对象数据解析
		if (CollectionUtils.isEmpty(subFields)) {return parse;}
		
		JSONObject oldDate = (JSONObject) parse(data, JSONObject.class);
		for (Field subField : subFields) { //开始处理关联子对象
			try {
				Object value = oldDate.get(subField.getName() + split); //如果字段名一致且有数据 - 则直接转换对象
				if (value != null) {subField.set(parse, parse(getValue(value), subField.getGenericType()));}
			} catch (Exception e) {
				throw new RuntimeException(String.format("Sql查询解析子对象失败，类[%s]当前字段[%s]！", parseType.getName(), subField.getName()), e);
			}
		}
		return parse;
	}
	/* 获取字段值 */
	private Object getValue(Object value) {
		Object jsonVal = JSON.parse(String.valueOf(value));
		if (jsonVal instanceof JSONArray && ((JSONArray) jsonVal).size() == 1) { //处理单个数组 - 多个数组表示关联查询有数据
			Object jVal = ((JSONArray) jsonVal).iterator().next();
			if (jVal instanceof Map) { //对象才做处理
				boolean empty = ((Map<?, ?>) jVal).values().stream().allMatch(v -> v == null);
				value = empty ? JSON.toJSONString(Collections.emptyList()) : value; //空对象或者值全是空 - 设置空数组
			}
		}
		return value;
	}
	
	/* 条件语法 */
	private void whereCondition(Condition condition, Map<String, String> fields, StringBuilder orgin, List<Object> entityValues) {
		/* 条件参数 */
		List<Param> params = condition.getParams();
		StringBuilder sb = new StringBuilder();
		
		/* 如果有参数条件则处理条件 */
		if (params.size() > 0) {
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
				appendConditionSql(sb, param, names, param.getValue(), entityValues);
			}
			
			/* 最后处理条件 */
			sb.delete(0, 4).insert(0, " where ");
			/* 最后如果是or要追加结束括号 */
			if (isKH) {sb.append(")");}
			/* 最后追加自定义条件 */
			sb.append(getSqlWhere(condition.getBaseEntity(), entityValues));
		} else {
			/* 无参数条件且有自定义条件 - 手动Where */
			String sqlWhere = getSqlWhere(condition.getBaseEntity(), entityValues).trim();
			if (sqlWhere.startsWith("and")) {sb.append(" where " + sqlWhere.substring(sqlWhere.indexOf("and") + 3));}
			else if (sqlWhere.startsWith("or")) {sb.append(" where " + sqlWhere.substring(sqlWhere.indexOf("or") + 2));}
		}
		
		/* 追加Where条件 */
		orgin.append(sb.toString());
	}
	
	/* 追加条件语法 */
	private void appendConditionSql(StringBuilder sb, Param param, String[] names, Object val, List<Object> entityValues) {
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
						setIn(sb, names, entityValues, vl);
					}
				} else if(val.getClass().isArray()) {
					for (int i = 0, j = Array.getLength(val); i < j; i++) {
						setIn(sb, names, entityValues, Array.get(val, i));
					}
				} else {setIn(sb, names, entityValues, val);}
				appendFkh(sb);
				return;
			} else {
				sb.append(names[0] + param.getOperation().getOptVal());
				if (Operation.Like == param.getOperation() || Operation.Not_Like == param.getOperation()) {
					sb.append("CONCAT(" + (param.isLeft() ? "'%',?" : "?") + (param.isRight() ? ",'%')" : ")"));
				} else {sb.append("?");}
			}
		}
		entityValues.add(val);
	}
	/* 设置In操作条件 */
	private void setIn(StringBuilder sb, String[] names, List<Object> entityValues, Object val) {
		if (names.length == 1) {sb.append("?,"); entityValues.add(val);}
		else {
			if (!(val instanceof BaseEntity)) {throw new IllegalArgumentException(String.format("In操作使用了多个条件，但是值类型[%s]没有继承至BaseEntity！", val.getClass().getName()));}
			try {
				Map<String, Field> fields = FieldUtil.getAllDeclaredField(val.getClass(), true);
				sb.append("(");
				for (String name : names) {sb.append("?,"); entityValues.add(fields.get(name).get(val));}
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
	
	/* 批量添加Sql */
	private int insertSql(Collection<E> es) throws Throwable {
		StringBuilder key = new StringBuilder();
		StringBuilder value = new StringBuilder();
		setCreateUpdateTimes(es); //批量设置创建与更新时间
		
		/* 字段映射 */
		Map<String, String> fields = TABLES.get(TABLE.name());
		/* 用于判断是否有默认值 */
		Map<String, List<List<Object>>> parmera = new HashMap<>(fields.size());
		/* 获取条件实体类字段 */
		Map<String, Field> entityFields = FieldUtil.getAllDeclaredField(es.iterator().next().getClass(), true);
		
		/* 拼接所有添加Sql */
		for (E e : es) {
			if (e == null) {throw new NullPointerException("添加不能传入空对象！");}
			List<List<Object>> datas = new ArrayList<>();
			key.append(" ("); value.append(" (");
			
			/* 实体类字段值 - 追加字段与值 */
			List<Object> entityValues = new ArrayList<>();
			for (Entry<String, String> entry : fields.entrySet()) {
				Field entityField = entityFields.get(entry.getKey());
				String[] tbKeys = splitValue(entry.getValue());
				Object data = entityField.get(e); //Entity对应字段数据
				
				if (data == null) {
					if (tbKeys.length > 1) { //如果没有数据且实体类有默认值
						key.append("`" + tbKeys[0] + "`,");
						value.append(tbKeys[1] + ",");
					}
				} else { //有数据则直接追加
					key.append("`" + tbKeys[0] + "`,");
					value.append("?,");
					entityValues.add(data);
				}
			}
			
			/* 添加字段值 */
			datas.add(entityValues);
			/* Sql尾部操作 */
			if (key.length() > 2) {
				key.deleteCharAt(key.length() - 1);
				value.deleteCharAt(value.length() - 1);
			}
			key.append(")"); value.append(")");
			
			/* 生成的Sql */
			String sql = "insert into `" + getTableName(e) + "`" + key.toString() + " values " + value.toString();
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
			List<Object[]> entityValues = pms.getValue().stream().map(v -> v.toArray()).collect(Collectors.toList());
			/* 执行添加 */
			final String sql = pms.getKey();
			int[] batchUpdate = execSql(SqlType.INSERT, sql, entityValues, () -> getJdbcTemplate(true).batchUpdate(sql, entityValues));
			/* 追加成功数量 */
			for (int rs : batchUpdate) {successSum = rs > 0 ? successSum + 1 : successSum;}
		}
		/* 返回是否与成功数一致 */
		return successSum;
	}
	
	/* 更新参数 */
	private boolean updatePamera(Map<String, Field> entityFields, Map<String, String> fields, BaseEntity e, StringBuilder sb, boolean isUpdateNull, List<Object> entityValues) throws Throwable {
		/* 追加条件 */
		for (Entry<String, String> entry : fields.entrySet()) {
			Field entityField = entityFields.get(entry.getKey());
			String[] tbKeys = splitValue(entry.getValue());
			
			/* 不更新参数为空的值 */
			Object data = entityField.get(e);
			if (data == null && !isUpdateNull) {continue;}
			
			/* 追加更新参数 */
			sb.append(TABLE.alias() + ".`" + tbKeys[0] + "` = ?,");
			entityValues.add(data);
		}
		
		/* 如果没有设置的列则不更新 */
		if (entityValues.size() == 0) {return true;}
		/* Sql尾部操作 */
		sb.deleteCharAt(sb.length() - 1);
		return false;
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
		/* 扫描配置参数 */
		Table table = ENTITY_CLASS.getDeclaredAnnotation(Table.class);
		if (table == null) {throw new NullPointerException(String.format("继承AbstractService后泛型对应的Entity[%s]必须申明@Table注解！", ENTITY_CLASS.getName()));}
		/* 表名不能重复 */
		if (TABLES.containsKey(table.name())) {throw new IllegalArgumentException(String.format("实体类[%s]对应表名[%s]已存在！", ENTITY_CLASS.getName(), table.name()));}
		
		/* 重设别名 */
		if (!StringUtils.hasText(table.alias())) {
			try {MyUtil.setAnnotationValue(table, "alias", "t_" + table.name());}
			catch (Exception e) {
				throw new IllegalArgumentException(String.format("重设注解值[%s]失败，错误信息》》》 %s", table.annotationType().getName(), e.toString()));
			}
		}
		
		/* 设置对应表信息 */
		TABLES.put(table.name(), setFieldTable(FieldUtil.getAllDeclaredField(ENTITY_CLASS, true, BaseEntity.class)));
		return table;
	}
	
	/* 获取自定义字段Sql */
	private String getSqlField(BaseEntity baseEntity) {
		if (baseEntity.getUserSql() == null || baseEntity.getUserSql().getFields() == null) {return NULLSTR;}
		else {return baseEntity.getUserSql().getFields();}
	}
	/* 获取自定义连接Sql */
	private String getSqlJoin(BaseEntity baseEntity, List<Object> entityValues) {
		if (baseEntity.getUserSql() == null || baseEntity.getUserSql().getJoin() == null) {return NULLSTR;}
		else {
			entityValues.addAll(baseEntity.getUserSql().getJoinParam());
			return " " + baseEntity.getUserSql().getJoin();
		}
	}
	/* 获取自定义条件Sql */
	private String getSqlWhere(BaseEntity baseEntity, List<Object> entityValues) {
		if (baseEntity.getUserSql() == null || baseEntity.getUserSql().getWhere() == null) {return NULLSTR;}
		else {
			entityValues.addAll(baseEntity.getUserSql().getWhereParam());
			return " " + baseEntity.getUserSql().getWhere();
		}
	}
	/* 获取自定义分组条件Sql */
	private String getSqlHaving(BaseEntity baseEntity, List<Object> entityValues) {
		if (baseEntity.getUserSql() == null || baseEntity.getUserSql().getHaving() == null) {return NULLSTR;}
		else {
			entityValues.addAll(baseEntity.getUserSql().getHavingParam());
			return " having " + baseEntity.getUserSql().getHaving();
		}
	}
	
	/* 获取表字段参数 */
	private String[] getTableFAndV(String tableValue, String fieldName) {
		String[] tbKeys = splitValue(tableValue);
		if (tbKeys == null) {throw new IllegalArgumentException(String.format("请检查条件参数[%s]的名称是否在实体类[%s]中存在！", fieldName, TABLE.name()));}
		return tbKeys;
	}
}
