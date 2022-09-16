package org.city.common.core.service;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.city.common.api.annotation.sql.Column;
import org.city.common.api.annotation.sql.Table;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.constant.MathSql;
import org.city.common.api.constant.Operation;
import org.city.common.api.dto.BaseDto;
import org.city.common.api.dto.Condition;
import org.city.common.api.dto.Condition.Join;
import org.city.common.api.dto.Condition.OrderBy;
import org.city.common.api.dto.Condition.Param;
import org.city.common.api.dto.Condition.Join.Cur;
import org.city.common.api.in.sql.AnnotationCondition;
import org.city.common.api.in.sql.Crud;
import org.city.common.api.util.FieldUtil;
import org.city.common.api.util.MyUtil;
import org.city.common.core.entity.BaseEntity;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * @作者 ChengShi
 * @日期 2022-06-21 11:16:37
 * @版本 1.0
 * @描述 公共服务方法
 */
public abstract class AbstractService<D extends BaseDto, E extends BaseEntity> implements Crud<D>{
	/*存储所有继承该类的表信息*/
	private final static Map<String, LinkedHashMap<String, String>> TABLES = new HashMap<>(8);
	/* 用于记录是否存在过当前实体类 */
	private final static Set<Class<?>> RECORD = new HashSet<>();
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private ApplicationContext applicationContext;
	/*dto类型*/
	private final Class<?> DTO_CLASS = getGenericsClass(0);
	/* dto字段名对应字段 */
	private final Map<String, Field> DTO_FIELD = FieldUtil.getAllDeclaredField(DTO_CLASS).stream().collect(Collectors.toMap(Field::getName, f -> f, (v1, v2) -> v1));
	/*该实体类注解*/
	private final Table TABLE = setTable();
	/* 空字符 */
	private final String NULLSTR = "";
	/* 英文逗号 */
	private final String DH = ",";
	
	@Override
	public Table getTable() {return this.TABLE;}
	@Override
	public String getTableField(String fieldName) {return getTableFAndV(TABLES.get(TABLE.name()).get(fieldName), fieldName)[0];}
	@Override
	public String getNowTime() {return jdbcTemplate.queryForObject("select now()", String.class);}
	
	@Override
	public Map<String, String> getTableFields() {
		Map<String, String> tableFields = new LinkedHashMap<>();
		for (Entry<String, String> entry : TABLES.get(TABLE.name()).entrySet()) {
			tableFields.put(entry.getKey(), splitValue(entry.getValue())[0]);
		}
		return tableFields;
	}
	
	@Override
	public AnnotationCondition<D> getJoin(int... groups) {
		return AnnotationConditionService.getJoin(this, DTO_CLASS, DTO_FIELD, applicationContext, groups);
	}
	
	@Override
	public int count(Condition condition) {
		try {
			return countSql(condition);
		} catch (Exception e) {
			throw new RuntimeException(e);
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
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean add(D d) {
		return addBatch(Arrays.asList(d)) > 0;
	}
	@Override
	public int addBatch(List<D> ds) {
		try {
			return insertSql(ds);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public long getLastAddId() {
		return jdbcTemplate.queryForObject("select last_insert_id()", long.class);
	}
	
	@Override
	public boolean delete(Condition condition) {
		try {
			/*字段映射*/
			Map<String, String> fields = TABLES.get(TABLE.name());
			
			StringBuilder sb = new StringBuilder();
			sb.append("delete " + TABLE.alias() + " from `" + TABLE.name() + "` " + TABLE.alias());
			/*追加条件*/
			List<Object> dtoValues = whereCondition(condition, fields, sb);
			
			/*执行删除*/
			return jdbcTemplate.update(sb.toString(), dtoValues.toArray()) > 0;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean update(Condition condition, D dto, boolean isUpdateNull) {
		try {
			if (dto == null) {throw new NullPointerException("更新不能传入空对象！");}
			
			/*字段映射*/
			Map<String, String> fields = TABLES.get(TABLE.name());
			
			StringBuilder sb = new StringBuilder();
			sb.append("update `" + TABLE.name() + "` " + TABLE.alias() + " set ");
			/*追加参数*/
			List<Object> dtoValues = updatePamera(fields, dto, sb, isUpdateNull);
			/*如果没有值代表不用更新*/
			if (dtoValues == null) {return false;}
			/*追加条件*/
			dtoValues.addAll(whereCondition(condition, fields, sb));
			
			/*执行更新*/
			return jdbcTemplate.update(sb.toString(), dtoValues.toArray()) > 0;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public int updateBatch(Condition condition, List<D> dtos, boolean isUpdateNull) {
		try {
			if (condition.getParams().size() == 0) {throw new IllegalArgumentException("批量更新不允许无条件执行！");}
			/*字段映射*/
			Map<String, String> fields = TABLES.get(TABLE.name());
			
			StringBuilder sb = new StringBuilder();
			sb.append("update `" + TABLE.name() + "` " + TABLE.alias() + " set ");
			/*参数配置*/
			List<Field> paramFields = new ArrayList<>();
			
			/*拼接Sql返回参数*/
			String paramSql = getParamSql(condition, fields, paramFields);
			List<Object> dtoValues = updatePameras(fields, paramSql, dtos, isUpdateNull, paramFields, sb);
			
			/*如果没有值代表不用更新*/
			if (dtoValues == null) {return 0;}
			dtoValues.addAll(whereCondition(condition, dtos, sb, fields));
			
			/*执行更新*/
			return jdbcTemplate.update(sb.toString(), dtoValues.toArray());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/* 获取批量更新条件 */
	private List<Object> whereCondition(Condition condition, List<D> ds, StringBuilder sb, Map<String, String> fields) throws Exception {
		List<Object> dtoValues = new ArrayList<>();
		sb.append(" where 1 = 1 ");
		
		/*参数条件保存*/
		Map<Integer, Map<Boolean, Map<String, String>>> cds = new LinkedHashMap<>();
		for (int i = 0, curIndex = 0, j = condition.getParams().size(); i < j; i++) {
			Param param = condition.getParams().get(i);
			/*获取表字段参数*/
			String[] tbFV = getTableFAndV(fields.get(param.getName()), param.getName());
			curIndex = putCds(cds, cds.get(curIndex), param, tbFV[0], curIndex);
		}
		
		/*参数条件追加*/
		boolean isFrist = true;
		for (Map<Boolean, Map<String, String>> values : cds.values()) {
			for (Entry<Boolean, Map<String, String>> entry : values.entrySet()) {
				if (isFrist) {sb.append(" and "); isFrist = false;}
				else {sb.append(entry.getKey() ? " and " : " or ");}
				sb.append("(" + String.join(",", entry.getValue().values()) + ") in (");
				
				/* 追加值 */
				for (D d : ds) {
					sb.append("(");
					for (String name : entry.getValue().keySet()) {
						sb.append("?,");
						dtoValues.add(DTO_FIELD.get(name).get(d));
					}
					appendFkh(sb); sb.append(",");
				}
				appendFkh(sb);
			}
		}
		
		/* 最后追加条件 */
		sb.append(getSqlWhere(condition.getBaseDto()));
		return dtoValues;
	}
	/* 添加参数条件 */
	private int putCds(Map<Integer, Map<Boolean, Map<String, String>>> cds, Map<Boolean, Map<String, String>> map,
			Param param, String tableField, int curIndex) {
		/* 如果为空则添加 */
		if (map == null) {map = new LinkedHashMap<>(); cds.put(curIndex, map);}
		
		/* 记录参数 */
		Map<String, String> names = new LinkedHashMap<>();
		if (map.isEmpty()) {map.put(param.isAnd(), names);}
		else {
			names = map.get(param.isAnd());
			/* 如果没有对应条件名称 - 继续这一轮 */
			if (names == null) {return putCds(cds, null, param, tableField, ++curIndex);}
		}
		
		names.put(param.getName(), TABLE.alias() + "." + tableField);
		return curIndex;
	}
	
	/*更新参数*/
	private List<Object> updatePameras(Map<String, String> fields, String paramSql, List<D> ds, boolean isUpdateNull, List<Field> paramFields, StringBuilder sb) throws Exception {
		/*dto字段值*/
		List<Object> dtoValues = new ArrayList<>();
		/*追加条件*/
		for (Entry<String, String> entry : fields.entrySet()) {
			Field dtoField = DTO_FIELD.get(entry.getKey());
			String[] tbKeys = splitValue(entry.getValue());
			
			/* 批量追加更新 */
			sb.append(getFieldWhenSql(TABLE.alias() + "." + tbKeys[0], ds, dtoField, isUpdateNull, paramSql, paramFields, dtoValues));
		}
		/*如果没有设置的列则不更新*/
		if (dtoValues.size() == 0) {return null;}
		/*sql尾部操作*/
		sb.delete(sb.length() - 1, sb.length());
		return dtoValues;
	}
	
	/* 获取字段条件sql */
	private String getFieldWhenSql(String tableField, List<D> ds, Field dtoField, boolean isUpdateNull, String paramSql, List<Field> paramFields, List<Object> dtoValues) throws Exception {
		boolean isAdd = false;
		StringBuilder sb = new StringBuilder();
		sb.append(tableField + " = case ");
		for (D d : ds) {
			if (d == null) {throw new NullPointerException("更新不能传入空对象！");}
			
			Object data = dtoField.get(d);
			/*不更新参数为空的值*/
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
			
			/*获取表字段参数*/
			String[] tbFV = getTableFAndV(fields.get(param.getName()), param.getName());
			paramSqlSb.append(TABLE.alias() + "." + tbFV[0] + " = ?");
			paramFields.add(DTO_FIELD.get(param.getName()));
		}
		return paramSqlSb.toString();
	}
	
	/* 统计 */
	private int countSql(Condition condition) throws SQLException {
		/*字段映射*/
		Map<String, String> fields = TABLES.get(TABLE.name());
		/* 查询sql追加 */
		StringBuilder sb = querySqlAppend(fields, true, condition);
		/*追加条件*/
		List<Object> dtoValues = whereCondition(condition, fields, sb);
		
		/*分组*/
		if (!CollectionUtils.isEmpty(condition.getGroupBys())) {
			sb.append(" group by ");
			for (String groupBy : condition.getGroupBys()) {
				sb.append("`" + groupBy + "`,");
			}
			sb.delete(sb.length() - 1, sb.length());
		}
		
		/* 针对分组特殊处理 */
		String sql = sb.toString();
		/* 如果有分组 - 对原来的sql外部包一层 */
		if (!CollectionUtils.isEmpty(condition.getGroupBys())) {
			sql = "select count(1) from (" + sql + ") t";
		}
		
		/*执行统计*/
		return jdbcTemplate.queryForObject(sql, int.class, dtoValues.toArray());
	}
	
	/* 查询sql追加 */
	private StringBuilder querySqlAppend(Map<String, String> fields, boolean isCount, Condition condition) {
		StringBuilder sb = new StringBuilder();
		/* 如果是统计只需要简单追加 */
		if (isCount && CollectionUtils.isEmpty(condition.getGroupBys())) {sb.append("select count(1)");}
		else {
			sb.append("select ");
			/* 提前追加字段 */
			sb.append(getSqlField(condition.getBaseDto()));
			
			/*追加字段*/
			if (condition.getFields().size() > 0) {
				for (org.city.common.api.dto.Condition.Field field : condition.getFields()) {
					String receiveName = StringUtils.hasText(field.getReceiveName()) ? field.getReceiveName() : field.getFieldName();
					/* 自定义Sql */
					if (MathSql.Sql == field.getMathSql()) {
						if (field.getVals() == null || field.getVals().length == 0) {
							throw new IllegalArgumentException("自定义Sql参数值至少得有一个！");
						}
						sb.append(field.format(field.getVals()[0]) + " " + receiveName + ",");
					} else {
						/*获取表字段参数*/
						String[] tbFV = getTableFAndV(fields.get(field.getFieldName()), field.getFieldName());
						sb.append(field.format(TABLE.alias() + "." + tbFV[0]) + " " + receiveName + ",");
					}
				}
			} else {
				for (Entry<String, String> entry : fields.entrySet()) {
					String[] tbKeys = splitValue(entry.getValue());
					sb.append(TABLE.alias() + "." + tbKeys[0] + " " + entry.getKey() + ",");
				}
			}
			sb.deleteCharAt(sb.length() - 1);
		}
		
		sb.append(" from `" + TABLE.name() + "` " + TABLE.alias());
		for (Join join : condition.getJoins().values()) {
			Cur cur = join.getCur();
			if (cur == null || cur.getCurService() == null) {join.setCur(new Cur().setCurService(this));}
			sb.append(join.toSql());
		}
		
		/* 最后追加链表 */
		sb.append(getSqlJoin(condition.getBaseDto()));
		return sb;
	}
	
	/*公共查询实现代码*/
	private List<D> querySql(Condition condition) throws SQLException {
		/*字段映射*/
		Map<String, String> fields = TABLES.get(TABLE.name());
		/* 查询sql追加 */
		StringBuilder sb = querySqlAppend(fields, false, condition);
		/*追加条件*/
		List<Object> dtoValues = whereCondition(condition, fields, sb);
		
		/*分组*/
		if (!CollectionUtils.isEmpty(condition.getGroupBys())) {
			sb.append(" group by ");
			for (String groupBy : condition.getGroupBys()) {
				sb.append("`" + groupBy + "`,");
			}
			sb.delete(sb.length() - 1, sb.length());
		}
		
		/*排序*/
		if (!CollectionUtils.isEmpty(condition.getOrderBys())) {
			sb.append(" order by ");
			for (OrderBy orderBy : condition.getOrderBys()) {
				sb.append("`" + orderBy.getName() + (orderBy.isAsc() ? "` asc," : "` desc,"));
			}
			sb.delete(sb.length() - 1, sb.length());
		}
		
		/*分页*/
		if (condition.getPage() != null) {
			sb.append(" limit " + condition.getPage().getOffset() + "," + condition.getPage().getPageSize());
		}
		
		/*执行查询*/
		return queryResult(jdbcTemplate.queryForList(sb.toString(), dtoValues.toArray()));
	}
	/*查询返回结果*/
	@SuppressWarnings("unchecked")
	private List<D> queryResult(List<Map<String, Object>> resultMaps) throws SQLException {
		List<D> result = new ArrayList<>();
		for(Map<String, Object> data : resultMaps) {
			/*时间格式自定义序列化方式*/
			String dataJsonStr = StringUtils.hasText(TABLE.dateFormat()) ? 
						JSONObject.toJSONStringWithDateFormat(data, TABLE.dateFormat(), SerializerFeature.WriteEnumUsingName) :
						JSONObject.toJSONString(data, SerializerFeature.WriteEnumUsingName);
			/*将结果map转json后在转对应类对象*/
			result.add((D) JSONObject.parseObject(dataJsonStr, DTO_CLASS));
		}
		return result;
	}
	
	/*条件语法*/
	private List<Object> whereCondition(Condition condition, Map<String, String> fields, StringBuilder sb) {
		/*dto字段值*/
		List<Object> dtoValues = new ArrayList<>();
		List<Param> params = condition.getParams();
		/*开始条件*/
		sb.append(" where 1 = 1 ");
		/*参数条件追加*/
		boolean isFrist = true;
		for (Param param : params) {
			if (isFrist) {sb.append(" and "); isFrist = false;}
			else {sb.append(param.isAnd() ? " and " : " or ");}
			
			/* 多个字段条件 */
			String[] names = param.getName().split(DH);
			if (param.getJoinTable() == null) {
				/*获取表字段参数*/
				for (int i = 0, j = names.length; i < j; i++) {
					String[] tbTV = getTableFAndV(fields.get(names[i]), names[i]);
					names[i] = TABLE.alias() + "." + tbTV[0];
				}
			} else {
				/*获取连接表字段参数*/
				for (int i = 0, j = names.length; i < j; i++) {
					String joinField = param.getJoinTable().getJoin().getTableField(names[i]);
					names[i] = param.getJoinTable().getJoinAlias() + "." + joinField;
				}
			}
			appendConditionSql(sb, param, names, param.getValue(), dtoValues);
		}
		
		/* 最后追加条件 */
		sb.append(getSqlWhere(condition.getBaseDto()));
		return dtoValues;
	}
	
	/*追加条件语法*/
	private void appendConditionSql(StringBuilder sb, Param param, String[] names, Object val, List<Object> dtoValues) {
		/* 特殊条件处理 */
		if (Operation.Is_Null == param.getOperation() || Operation.Is_Not_Null == param.getOperation()) {
			sb.append(names[0] + param.getOperation().getOptVal());
			return;
		}
		/* 如果值为空则条件直接不成立 - 空字符，空集合，空数组或NULL都算空 */
		if (!MyUtil.isNotBlank(val)) {sb.append("1 = 0");return;}
		
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
				/*集合做循环处理*/
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
			Map<String, Field> fields = FieldUtil.getAllDeclaredField(val.getClass()).stream().collect(Collectors.toMap(k -> k.getName(), v -> v, (v1, v2) -> v1));
			try {
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
		sb.delete(sb.length() - 1, sb.length());
		sb.append(")");
	}
	
	/*按英文冒号分割表字段名*/
	private String[] splitValue(String tbKey) {
		if (tbKey == null) {return null;}
		if (tbKey.contains(":")) {return tbKey.split(":");}
		else {return new String[]{tbKey};}
	}
	
	/*批量添加sql*/
	@SuppressWarnings("unchecked")
	private int insertSql(List<D> ds) throws Exception {
		StringBuilder key = new StringBuilder();
		StringBuilder value = new StringBuilder();
		
		/*字段映射*/
		Map<String, String> fields = TABLES.get(TABLE.name());
		/*用于判断是否有默认值*/
		Map<String, List<List<Object>>> parmera = new HashMap<>(fields.size());
		
		for (D d : ds) {
			if (d == null) {throw new NullPointerException("添加不能传入空对象！");}
			
			List<List<Object>> datas = new ArrayList<>();
			key.append(" (");value.append(" (");
			
			/*dto字段值*/
			List<Object> dtoValues = new ArrayList<>();
			/*追加字段*/
			for (Entry<String, String> entry : fields.entrySet()) {
				Field dtoField = DTO_FIELD.get(entry.getKey());
				String[] tbKeys = splitValue(entry.getValue());
				key.append(tbKeys[0] + ",");
				/*dto对应字段数据*/
				Object data = dtoField.get(d);
				
				if (data == null && tbKeys.length > 1) {
					value.append(tbKeys[1] + ",");
				} else {
					value.append("?,");
					dtoValues.add(data);
				}
			}
			datas.add(dtoValues);
			/*sql尾部操作*/
			key.deleteCharAt(key.length() - 1);
			value.deleteCharAt(value.length() - 1);
			key.append(")");value.append(")");
			/*生成的sql*/
			String sql = "insert into `" + TABLE.name() + "`" + key.toString() + " values " + value.toString();
			
			/*用于后续多个添加*/
			if (parmera.containsKey(sql)) {parmera.get(sql).addAll(datas);}
			else {parmera.put(sql, datas);}
			
			/*置空用于继续追加*/
			key.setLength(0);value.setLength(0);
		}
		/* 批量插入 - 用代理模式调用触发事务 */
		return ((AbstractService<BaseDto, BaseEntity>) AopContext.currentProxy()).insertBatch(parmera);
	}
	/* 批量插入并返回插入成功数 - 使用事务会特别快 */
	@Transactional
	public int insertBatch(Map<String, List<List<Object>>> parmera) throws SQLException {
		/*判断是否全部成功*/
		int successSum = 0;
		for (Entry<String, List<List<Object>>> pms : parmera.entrySet()) {
			List<Object[]> dtoValues = pms.getValue().stream().map(v -> v.toArray()).collect(Collectors.toList());
			
			/*执行添加*/
			int[] batchUpdate = jdbcTemplate.batchUpdate(pms.getKey(), dtoValues);
			/*追加成功数量*/
			for (int rs : batchUpdate) {successSum = rs > 0 ? successSum + 1 : successSum;}
		}
		/* 返回是否与成功数一致 */
		return successSum;
	}
	
	/*更新参数*/
	private List<Object> updatePamera(Map<String, String> fields, D d, StringBuilder sb, boolean isUpdateNull) throws IllegalArgumentException, IllegalAccessException {
		/*dto字段值*/
		List<Object> dtoValues = new ArrayList<>();
		/*追加条件*/
		for (Entry<String, String> entry : fields.entrySet()) {
			Field dtoField = DTO_FIELD.get(entry.getKey());
			String[] tbKeys = splitValue(entry.getValue());
			Object data = dtoField.get(d);
			/*不更新参数为空的值*/
			if (data == null && !isUpdateNull) {continue;}
			
			sb.append(TABLE.alias() + "." + tbKeys[0] + " = ?,");
			dtoValues.add(data);
		}
		/*如果没有设置的列则不更新*/
		if (dtoValues.size() == 0) {return null;}
		/*sql尾部操作*/
		sb.delete(sb.length() - 1, sb.length());
		return dtoValues;
	}
	
	/**
	 * @描述 按约定分割存放表字段名称，驼峰标志作为分割点追加“_”符号，有默认值则用“:”分割追加默认值，默认值只对添加有效
	 * @param fields 实体类对应所有字段
	 * @param result key为类字段名，value为表字段名（有默认值则追加“:默认值”，默认值只对添加有效）
	 */
	protected void setFieldTable(List<Field> fields, LinkedHashMap<String, String> result) {
		StringBuilder sb = new StringBuilder();
		for (Field field : fields) {
			String name = field.getName();
			Column column = field.getAnnotation(Column.class);
			
			/*如果没有注解或者注解名称为空内容*/
			if (column == null || !StringUtils.hasText(column.name())) {
				int start = 0;String tableField = "";
				/*迭代每个字符判断*/
				char[] charArray = name.toCharArray();
				for (int i = 0, j = charArray.length; i < j; i++) {
					if (charArray[i] >= 65 && charArray[i] <= 90) {
						tableField += (name.substring(start, i) + "_");
						start = i;
					}
				}
				
				/* 追加表字段名 */
				sb.append(tableField + name.substring(start));
				/*追加默认值*/
				if (column != null && StringUtils.hasText(column.value())) {
					sb.append(":" + column.value());
				}
			} else {
				sb.append(column.name());
				/*追加默认值*/
				if (StringUtils.hasText(column.value())) {
					sb.append(":" + column.value());
				}
			}
			
			/*存放按约定分割的字符*/
			result.put(name, sb.toString().toLowerCase());
			sb.setLength(0);
		}
	}
	
	/*初始化设置该实体类信息*/
	private Table setTable() {
		/*该类型必须是带泛型类*/
		Class<?> entityClass = getGenericsClass(1);
		
		/*扫描配置参数*/
		Table table = entityClass.getAnnotation(Table.class);
		if (table == null) {throw new NullPointerException(String.format("继承AbstractService后泛型对应的Entity[%s]必须申明@Table注解！", entityClass.getName()));}
		if(TABLES.containsKey(table.name())) {
			/* 如果存在类则不初始化 - 否则代表不存在类但是表名相同 */
			if (RECORD.contains(entityClass)) {return table;}
			throw new IllegalArgumentException(String.format("存在该表名[%s]，请修改[%s]类下的表名！", table.name(), entityClass.getName()));
		}
		/* 记录已存在实体类 */
		RECORD.add(entityClass);
		TABLES.put(table.name(), new LinkedHashMap<>());
		
		/* 重设别名 */
		if (!StringUtils.hasText(table.alias())) {
			try {MyUtil.setAnnotationValue(table, "alias", "t_" + table.name());}
			catch (Exception e) {
				throw new IllegalArgumentException(String.format("重设注解值[%s]失败，msg》》》 ", table.annotationType().getName(), e.toString()));
			}
		}
		
		/*获取所有字段*/
		List<Field> allFields = FieldUtil.getAllDeclaredField(entityClass);
		
		for (Field field : allFields) {
			if (!DTO_FIELD.containsKey(field.getName())) {
				throw new NullPointerException(String.format("Dto[%s]中找不到Entity[%s]对应的字段名[%s]！", DTO_CLASS.getName(), entityClass.getName(), field.getName()));
			}
		}
		/*设置对应表信息*/
		setFieldTable(allFields, TABLES.get(table.name()));
		
		return table;
	}
	
	/*获取当前类对应泛型类型*/
	private Class<?> getGenericsClass(int i){
		Class<?> superclass = this.getClass().getSuperclass();
		if (superclass == AbstractService.class) {
			return (Class<?>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[i];
		} else {
			return (Class<?>) ((ParameterizedType) superclass.getGenericSuperclass()).getActualTypeArguments()[i];
		}
	}
	
	/* 获取自定义字段Sql */
	private String getSqlField(BaseDto baseDto) {
		Object extField = baseDto.getParams().get(CommonConstant.SQL_FIELD);
		if (extField instanceof String) {return extField + ",";}
		else {return NULLSTR;}
	}
	/* 获取自定义链接Sql */
	private String getSqlJoin(BaseDto baseDto) {
		Object extJoin = baseDto.getParams().get(CommonConstant.SQL_JOIN);
		if (extJoin instanceof String) {return " " + extJoin;}
		else {return NULLSTR;}
	}
	/* 获取自定义条件Sql */
	private String getSqlWhere(BaseDto baseDto) {
		Object extCondition = baseDto.getParams().get(CommonConstant.SQL_WHERE);
		if (extCondition instanceof String) {return " " + extCondition;}
		else {return NULLSTR;}
	}
	
	/* 获取表字段参数 */
	private String[] getTableFAndV(String tableValue, String fieldName) {
		String[] tbKeys = splitValue(tableValue);
		if (tbKeys == null) {throw new IllegalArgumentException(String.format("请检查条件参数[%s]的名称是否在实体类[%s]中存在！", fieldName, TABLE.name()));}
		return tbKeys;
	}
}
