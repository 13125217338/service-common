package org.city.common.mysql.service;

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
import org.city.common.api.dto.sql.BaseDto;
import org.city.common.api.dto.sql.Condition;
import org.city.common.api.dto.sql.Condition.GroupBy;
import org.city.common.api.dto.sql.Condition.Join;
import org.city.common.api.dto.sql.Condition.JoinTable;
import org.city.common.api.dto.sql.Condition.OrderBy;
import org.city.common.api.dto.sql.Condition.Param;
import org.city.common.api.dto.sql.SubCondition;
import org.city.common.api.dto.sql.TimeDto;
import org.city.common.api.in.interceptor.SqlInterceptor;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.sql.AnnotationCondition;
import org.city.common.api.in.sql.Crud;
import org.city.common.api.in.sql.MyDataSource;
import org.city.common.api.util.FieldUtil;
import org.city.common.api.util.MyUtil;
import org.city.common.api.util.SpringUtil;
import org.city.common.core.entity.BaseEntity;
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
 * @描述 公共服务方法
 */
@Slf4j
public abstract class AbstractService<D extends BaseDto, E extends BaseEntity> implements Crud<D>,JSONParser {
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
	/* dto类型 */
	@SuppressWarnings("unchecked")
	private final Class<D> DTO_CLASS = (Class<D>) GENERICS_CLASS.get("D");
	/* dto字段名对应字段 */
	private final Map<String, Field> DTO_FIELD = FieldUtil.getAllDeclaredField(DTO_CLASS, true);
	/* 非基本类型字段 - 对象字段 */
	private final Map<String, Field> SUB_FIELD = new HashMap<>();
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
	private <T> T execSql(SqlType sqlType, String sql, Object param, Supplier<T> exec) {
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
	public Class<D> getDtoClass() {return DTO_CLASS;}
	@Override
	public Map<String, Field> getDtoFields() {return DTO_FIELD;}
	@Override
	public String getTableField(String fieldName) {return getTableFAndV(TABLES.get(TABLE.name()).get(fieldName), fieldName)[0];}
	@Override
	public Map<String, Field> getSubFields() {return SUB_FIELD;}
	
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
	public SubCondition findBySubCondition(Condition condition, boolean useTableField) {
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
		List<Object> dtoValues = new ArrayList<>();
		/* 查询所有接收字段名 */
		Set<String> subFieldName = new HashSet<>();
		/* 查询sql追加 */
		StringBuilder sb = querySqlAppend(fields, false, condition, dtoValues, subFieldName, useTableField, groupOrder);
		/* 追加条件 */
		whereCondition(condition, fields, sb, dtoValues);
		
		/* 先分组 */
		if (hasGroup) {groupBy(condition, sb, groups, dtoValues);}
		/* 在排序 */
		if (hasOrder) {orderBy(condition, sb, orders);}
		
		/* 最后分页 */
		if (condition.getPage() != null) {
			sb.append(" limit " + condition.getPage().getOffset() + "," + condition.getPage().getPageSize());
		}
		return new SubCondition(sb.toString(), dtoValues, subFieldName, TABLE.alias(), false);
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
			sb.append("delete " + TABLE.alias() + " from `" + getTableName(condition.getBaseDto()) + "` " + TABLE.alias() + " ");
			
			/* 追加参数 */
			List<Object> dtoValues = new ArrayList<>();
			/* 链表 */
			for (Join join : condition.getJoins().values()) {
				sb.append(join.toSql(join.getIgnore(), this, dtoValues));
			}
			/* 最后追加链表 */
			sb.append(getSqlJoin(condition.getBaseDto(), dtoValues));
			
			/* 追加条件 */
			whereCondition(condition, fields, sb, dtoValues);
			
			/* 执行删除 */
			final String sql = sb.toString();
			return execSql(SqlType.DELETE, sql, dtoValues, () -> getJdbcTemplate(true).update(sql, dtoValues.toArray()));
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
			sb.append("update `" + getTableName(dto) + "` " + TABLE.alias() + " ");
			
			/* 追加参数 */
			List<Object> dtoValues = new ArrayList<>();
			/* 链表 */
			for (Join join : condition.getJoins().values()) {
				sb.append(join.toSql(join.getIgnore(), this, dtoValues));
			}
			/* 最后追加链表 */
			sb.append(getSqlJoin(condition.getBaseDto(), dtoValues) + " set ");
			
			/* 如果没有设置的列则不更新 */
			if (updatePamera(fields, dto, sb, isUpdateNull, dtoValues)) {return false;}
			/* 追加条件 */
			whereCondition(condition, fields, sb, dtoValues);
			
			/* 执行更新 */
			final String sql = sb.toString();
			return execSql(SqlType.UPDATE, sql, dtoValues, () -> getJdbcTemplate(true).update(sql, dtoValues.toArray()) > 0);
		} catch (Throwable e) {
			throw new DataAccessResourceFailureException("更新Sql执行失败！", e);
		}
	}
	
	/* 设置更新时间 */
	private void setUpdateTime(D d) {
		if (d instanceof TimeDto) {((TimeDto) d).setUpdateTime(Timestamp.from(Instant.now()));}
	}
	/* 批量设置更新时间 */
	private void setUpdateTimes(Collection<D> dtos) {
		Timestamp nowTime = Timestamp.from(Instant.now());
		for (D d : dtos) {if (d instanceof TimeDto) {((TimeDto) d).setUpdateTime(nowTime);}}
	}
	/* 批量设置创建与更新时间 */
	private void setCreateUpdateTimes(Collection<D> dtos) {
		Timestamp nowTime = Timestamp.from(Instant.now());
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
			
			/* 追加参数 */
			List<Object> dtoValues = new ArrayList<>();
			/* 参数字段配置 */
			List<Field> paramFields = new ArrayList<>();
			/* 拼接Sql返回参数 */
			String paramSql = getParamSql(condition, fields, paramFields);
			
			/* 如果没有设置的列则不更新 */
			if (updatePameras(fields, paramSql, dtos, isUpdateNull, paramFields, sb, dtoValues)) {return 0;}
			/* 追加条件 */
			whereCondition(condition, dtos, sb, fields, dtoValues);
			
			/* 执行更新 */
			final String sql = sb.toString();
			return execSql(SqlType.UPDATE, sql, dtoValues, () -> getJdbcTemplate(true).update(sql, dtoValues.toArray()));
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
	
	/* 获取批量更新条件 */
	private void whereCondition(Condition condition, Collection<D> ds, StringBuilder sb, Map<String, String> fields, List<Object> dtoValues) throws Exception {
		/* 更新参数 */
		List<Param> params = condition.getParams();
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
		sb.append(getSqlWhere(condition.getBaseDto(), dtoValues));
	}
	
	/* 更新参数 */
	private boolean updatePameras(Map<String, String> fields, String paramSql, Collection<D> ds, boolean isUpdateNull, List<Field> paramFields, StringBuilder sb, List<Object> dtoValues) throws Exception {
		/* 追加条件 */
		for (Entry<String, String> entry : fields.entrySet()) {
			Field dtoField = DTO_FIELD.get(entry.getKey());
			String[] tbKeys = splitValue(entry.getValue());
			/* 批量追加更新 */
			sb.append(getFieldWhenSql(TABLE.alias() + ".`" + tbKeys[0] + "`", ds, dtoField, isUpdateNull, paramSql, paramFields, dtoValues));
		}
		
		/* 如果没有设置的列则不更新 */
		if (dtoValues.size() == 0) {return true;}
		/* sql尾部操作 */
		sb.deleteCharAt(sb.length() - 1);
		return false;
	}
	
	/* 获取字段条件sql */
	private String getFieldWhenSql(String tableField, Collection<D> ds, Field dtoField, boolean isUpdateNull, String paramSql, List<Field> paramFields, List<Object> dtoValues) throws Exception {
		boolean isAdd = false;
		StringBuilder sb = new StringBuilder();
		sb.append(tableField + " = case ");
		for (D d : ds) {
			if (d == null) {throw new NullPointerException("更新不能传入空对象！");}
			
			/* 不更新参数为空的值 */
			Object data = dtoField.get(d);
			if (data == null && !isUpdateNull) {continue;}
			
			/* 追加更新参数 */
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
			Assert.isTrue(param.isAnd(), String.format("批量更新必须And条件，当前字段[%s]非And条件！", param.getName()));
			if (isAppend) {paramSqlSb.append(" and ");} else {isAppend = true;}
			
			/* 获取表字段参数 */
			String[] tbFV = getTableFAndV(fields.get(param.getName()), param.getName());
			paramSqlSb.append(TABLE.alias() + ".`" + tbFV[0] + "` = ?");
			paramFields.add(DTO_FIELD.get(param.getName()));
		}
		return paramSqlSb.toString();
	}
	
	/* 统计数量 */
	private long countSql(Condition condition) throws Throwable {
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
		List<Object> dtoValues = new ArrayList<>();
		/* 查询sql追加 */
		StringBuilder sb = querySqlAppend(fields, true, condition, dtoValues, new HashSet<>(), false, groupOrder);
		/* 追加条件 */
		whereCondition(condition, fields, sb, dtoValues);
		
		/* 分组 */
		if (hasGroup) {
			groupBy(condition, sb, groups, dtoValues);
			/* 对原来的sql外部包一层 */
			sb = new StringBuilder("select count(1) from (" + sb.toString() + ") t");
		}
		
		/* 执行统计 */
		final String sql = sb.toString();
		return execSql(SqlType.SELECT, sql, dtoValues, () -> getJdbcTemplate(false).queryForObject(sql, long.class, dtoValues.toArray()));
	}
	
	/* 分组 */
	private void groupBy(Condition condition, StringBuilder sb, Map<GroupBy, String> groups, List<Object> dtoValues) {
		sb.append(" group by ");
		/* 处理所有分组 */
		for (Entry<GroupBy, String> entry : groups.entrySet()) {
			sb.append(entry.getValue() + ",");
		}
		/* 删除最后的逗号 */
		sb.deleteCharAt(sb.length() - 1);
		/* 最后追加分组条件 */
		sb.append(getSqlHaving(condition.getBaseDto(), dtoValues));
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
	
	/* 查询sql追加 */
	private StringBuilder querySqlAppend(Map<String, String> fields, boolean isCount, Condition condition,
			List<Object> dtoValues, Set<String> subFieldName, boolean useTableField, Map<String, List<OrderBy>> groupOrder) {
		StringBuilder sb = new StringBuilder();
		/* 如果是统计只需要简单追加 */
		if (isCount && condition.getGroupBys().isEmpty()) {sb.append("select count(1)");}
		else {
			sb.append("select ");
			/* 提前追加字段 */
			sb.append(getSqlField(condition.getBaseDto()));
			
			/* 追加字段 */
			Map<String, String> tableFields = getTableFields();
			if (condition.getFields().size() > 0) {
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
							
							/* json化处理 */
							if (isArray(SUB_FIELD, DTO_CLASS, receiveName, true)) { //数组类型处理
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
						if (SUB_FIELD.containsKey(receiveName)) {receiveName = receiveName + Condition.SPLIT;} //标记对象解析
						dtoValues.addAll(field.getSubField().getParams());
						sb.append("(" + field.getSubField().getSql() + ") `" + receiveName + "`,");
					}
					subFieldName.add(receiveName); //查询所有接收字段名
				}
			} else {
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
		sb.append(getSqlJoin(condition.getBaseDto(), dtoValues));
		return sb;
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
	private boolean isArray(Map<String, Field> subFields, Class<?> dtoClass, String receiveName, boolean arrayPass) {
		Assert.isTrue(subFields.containsKey(receiveName), String.format("Dto[%s]字段名或分组注解字段名[%s]必须非基本类型！", dtoClass.getName(), receiveName));
		Class<?> fieldType = subFields.get(receiveName).getType();
		boolean isArray = fieldType.isArray() || Collection.class.isAssignableFrom(fieldType);
		Assert.isTrue(!isArray || arrayPass, String.format("Dto[%s]分组注解字段名[%s]必须非数组类型！", dtoClass.getName(), receiveName));
		return isArray;
	}
	/* 追加分组字段Json数组对象参数 */
	private String appendGroupFieldJsonArray(Map<String, JoinTable> groupField, List<OrderBy> orders, int groupIndex) {
		StringBuilder jsonArr = new StringBuilder("json_extract(concat('[',group_concat(DISTINCT json_object(");
		for (Entry<String, JoinTable> entry : groupField.entrySet()) {
			Crud<? extends BaseDto> fieldService = entry.getValue().getJoin(); //分组字段服务
			isArray(fieldService.getSubFields(), fieldService.getDtoClass(), entry.getKey(), false); //数组则不通过
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
	private List<D> querySql(Condition condition) throws Throwable {
		SubCondition subCondition = findBySubCondition(condition, false);
		/* 执行查询 */
		final String sql = subCondition.getSql();
		return execSql(SqlType.SELECT, sql, subCondition.getParams(), () -> queryResult(getJdbcTemplate(false).queryForList(sql, subCondition.getParams().toArray())));
	}
	/* 查询返回结果 */
	private List<D> queryResult(List<Map<String, Object>> resultMaps) {
		List<D> result = new ArrayList<>();
		for(Map<String, Object> data : resultMaps) {
			result.add(parseSub(SUB_FIELD.values(), data, DTO_CLASS, Condition.SPLIT));
		}
		return result;
	}
	/* 解析子对象 */
	private D parseSub(Collection<Field> subFields, Object data, Class<?> parseType, String split) {
		D parse = parse(data, parseType); //最顶级对象数据解析
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
	private void whereCondition(Condition condition, Map<String, String> fields, StringBuilder orgin, List<Object> dtoValues) {
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
				appendConditionSql(sb, param, names, param.getValue(), dtoValues);
			}
			
			/* 最后处理条件 */
			sb.delete(0, 4).insert(0, " where ");
			/* 最后如果是or要追加结束括号 */
			if (isKH) {sb.append(")");}
			/* 最后追加自定义条件 */
			sb.append(getSqlWhere(condition.getBaseDto(), dtoValues));
		} else {
			/* 无参数条件且有自定义条件 - 手动where */
			String sqlWhere = getSqlWhere(condition.getBaseDto(), dtoValues).trim();
			if (sqlWhere.startsWith("and")) {sb.append(" where " + sqlWhere.substring(sqlWhere.indexOf("and") + 3));}
			else if (sqlWhere.startsWith("or")) {sb.append(" where " + sqlWhere.substring(sqlWhere.indexOf("or") + 2));}
		}
		
		/* 追加where条件 */
		orgin.append(sb.toString());
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
			/* 执行添加 */
			final String sql = pms.getKey();
			int[] batchUpdate = execSql(SqlType.INSERT, sql, dtoValues, () -> getJdbcTemplate(true).batchUpdate(sql, dtoValues));
			/* 追加成功数量 */
			for (int rs : batchUpdate) {successSum = rs > 0 ? successSum + 1 : successSum;}
		}
		/* 返回是否与成功数一致 */
		return successSum;
	}
	
	/* 更新参数 */
	private boolean updatePamera(Map<String, String> fields, D d, StringBuilder sb, boolean isUpdateNull, List<Object> dtoValues) throws Throwable {
		/* 追加条件 */
		for (Entry<String, String> entry : fields.entrySet()) {
			Field dtoField = DTO_FIELD.get(entry.getKey());
			String[] tbKeys = splitValue(entry.getValue());
			
			/* 不更新参数为空的值 */
			Object data = dtoField.get(d);
			if (data == null && !isUpdateNull) {continue;}
			
			/* 追加更新参数 */
			sb.append(TABLE.alias() + ".`" + tbKeys[0] + "` = ?,");
			dtoValues.add(data);
		}
		
		/* 如果没有设置的列则不更新 */
		if (dtoValues.size() == 0) {return true;}
		/* sql尾部操作 */
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
		/* 该类型必须是带泛型类 */
		@SuppressWarnings("unchecked")
		Class<E> entityClass = (Class<E>) GENERICS_CLASS.get("E");
		/* 扫描配置参数 */
		Table table = entityClass.getDeclaredAnnotation(Table.class);
		if (table == null) {throw new NullPointerException(String.format("继承AbstractService后泛型对应的Entity[%s]必须申明@Table注解！", entityClass.getName()));}
		/* 表名重复 */
		if(TABLES.containsKey(table.name())) {
			log.warn("该表名[{}]已存在，新实体类字段[{}]即将覆盖旧实体类字段！", table.name(), entityClass.getName());
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
				SUB_FIELD.put(field.getName(), field); //非基本类型记录
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
	
	/* 获取自定义字段Sql */
	private String getSqlField(BaseDto baseDto) {
		if (baseDto.getUserSqlDto() == null || baseDto.getUserSqlDto().getFields() == null) {return NULLSTR;}
		else {return baseDto.getUserSqlDto().getFields();}
	}
	/* 获取自定义连接Sql */
	private String getSqlJoin(BaseDto baseDto, List<Object> dtoValues) {
		if (baseDto.getUserSqlDto() == null || baseDto.getUserSqlDto().getJoin() == null) {return NULLSTR;}
		else {
			dtoValues.addAll(baseDto.getUserSqlDto().getJoinParam());
			return " " + baseDto.getUserSqlDto().getJoin();
		}
	}
	/* 获取自定义条件Sql */
	private String getSqlWhere(BaseDto baseDto, List<Object> dtoValues) {
		if (baseDto.getUserSqlDto() == null || baseDto.getUserSqlDto().getWhere() == null) {return NULLSTR;}
		else {
			dtoValues.addAll(baseDto.getUserSqlDto().getWhereParam());
			return " " + baseDto.getUserSqlDto().getWhere();
		}
	}
	/* 获取自定义分组条件Sql */
	private String getSqlHaving(BaseDto baseDto, List<Object> dtoValues) {
		if (baseDto.getUserSqlDto() == null || baseDto.getUserSqlDto().getHaving() == null) {return NULLSTR;}
		else {
			dtoValues.addAll(baseDto.getUserSqlDto().getHavingParam());
			return " having " + baseDto.getUserSqlDto().getHaving();
		}
	}
	
	/* 获取表字段参数 */
	private String[] getTableFAndV(String tableValue, String fieldName) {
		String[] tbKeys = splitValue(tableValue);
		if (tbKeys == null) {throw new IllegalArgumentException(String.format("请检查条件参数[%s]的名称是否在实体类[%s]中存在！", fieldName, TABLE.name()));}
		return tbKeys;
	}
}
