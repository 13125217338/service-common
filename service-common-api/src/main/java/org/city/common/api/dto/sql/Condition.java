package org.city.common.api.dto.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.city.common.api.annotation.sql.Conditions;
import org.city.common.api.constant.JoinType;
import org.city.common.api.constant.MathSql;
import org.city.common.api.constant.Operation;
import org.city.common.api.constant.group.Default;
import org.city.common.api.dto.sql.Condition.Join.ON;
import org.city.common.api.entity.BaseEntity;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.sql.Crud;
import org.city.common.api.util.FieldUtil;
import org.city.common.api.util.SpringUtil;
import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-07-04 10:06:37
 * @版本 1.0
 * @描述 条件参数
 */
@Getter
public class Condition implements JSONParser {
	/** 子查询字段分隔符 */
	public final static String SPLIT = "$";
	/** 非基本类型字段 - 对象字段 */
	private final static Map<BaseEntity, Map<String, java.lang.reflect.Field>> SUB_FIELDS = new ConcurrentHashMap<>();
	
	/** 初始条件 - 自定义条件查询 */
	public Condition(BaseEntity baseEntity, String name, Operation operation, Object value) {
		if (baseEntity == null) {throw new NullPointerException("公共实体类不能为空！");}
		this.baseEntity = baseEntity;
		params.add(new Param(name, operation, value));
	}
	/** 初始条件 - 默认等于查询 */
	public Condition(BaseEntity baseEntity, String name, Object value) {
		if (baseEntity == null) {throw new NullPointerException("公共实体类不能为空！");}
		this.baseEntity = baseEntity;
		params.add(new Param(name, Operation.Equals, value));
	}
	/** 初始条件 - 默认查所有 */
	public Condition(BaseEntity baseEntity) {
		if (baseEntity == null) {throw new NullPointerException("公共实体类不能为空！");}
		this.baseEntity = baseEntity;
	}
	
	/* 公共实体类 */
	private final BaseEntity baseEntity;
	/* 自定义字段 */
	private List<Field> fields = new ArrayList<>();
	/* 所有条件 */
	private List<Param> params = new ArrayList<>();
	/* 连接表 */
	private Map<String, Join> joins = new LinkedHashMap<>();
	/* 分组条件 */
	private List<GroupBy> groupBys = new ArrayList<>();
	/* 排序条件 */
	private List<OrderBy> orderBys = new ArrayList<>();
	/* 分页 */
	private Page page = null;
	/* 忽略索引名 */
	private String ignore = null;
	/* 子查询表 - 优先级最高 */
	private SubCondition subTable = null;
	
	/**
	 * @return 获取条件实体类字段
	 */
	public Map<String, java.lang.reflect.Field> getEntityFields() {
		return FieldUtil.getAllDeclaredField(baseEntity.getClass(), true, BaseEntity.class);
	}
	/**
	 * @return 获取条件实体类非基本类型字段
	 */
	public Map<String, java.lang.reflect.Field> getSubEntityFields() {
		return SUB_FIELDS.computeIfAbsent(baseEntity, k -> {
			Map<String, java.lang.reflect.Field> SUB_FIELD = new HashMap<>();
			java.lang.reflect.Field[] declaredFields = baseEntity.getClass().getDeclaredFields();
			/* 只处理当前类字段 */
			for (java.lang.reflect.Field field : declaredFields) {
				if (!isBaseType(field.getType())) {
					field.setAccessible(true);
					SUB_FIELD.put(field.getName(), field); //非基本类型记录
				}
			}
			return SUB_FIELD;
		});
	}
	
	/**
	 * @描述 设置子查询表
	 * @param subTable 子查询表
	 * @return 当前条件
	 */
	public Condition setSubTable(SubCondition subTable) {this.subTable = subTable; return this;}
	
	/**
	 * @描述 忽略索引名称
	 * @param ignoreName 忽略索引名称
	 * @return 当前条件
	 */
	public Condition setIgnore(String ignoreName) {this.ignore = ignoreName; return this;}
	
	/**
	 * @描述 获取连接对象（只对查询有效）
	 * @param joinAlias 连接的表别名
	 * @return 连接对象
	 */
	public Join getJoin(String joinAlias) {return joins.get(joinAlias);}
	/**
	 * @描述 获取连接对象（只对查询有效）
	 * @param joinService 连接的表服务
	 * @return 连接对象
	 */
	public Join getJoin(Crud<? extends BaseEntity> joinService) {return joins.get(joinService.getTable().alias());}
	
	/**
	 * @描述 自定义添加字段
	 * @param field 字段对象
	 * @return 当前条件
	 */
	public Condition addField(Field field) {
		if (field == null) {throw new IllegalArgumentException("字段参数不能为空！");}
		fields.add(field);
		return this;
	}
	/**
	 * @描述 自定义添加多个字段
	 * @param fields 字段对象集合
	 * @return 当前条件
	 */
	public Condition addFields(Collection<Field> fields) {
		if (fields == null) {throw new IllegalArgumentException("字段参数集合不能为空！");}
		this.fields.addAll(fields);
		return this;
	}
	/**
	 * @描述 自定义添加连接（只对查询有效）
	 * @param join 连接对象
	 * @return 当前条件
	 */
	public Condition addJoin(Join join) {
		if (join == null) {throw new IllegalArgumentException("连接表参数不能为空！");}
		joins.put(join.joinTable.joinAlias, join.setCondition(this));
		return this;
	}
	/**
	 * @描述 自定义添加参数
	 * @param param 参数
	 * @return 当前条件
	 */
	public Condition addParam(Param param) {
		if (param == null) {throw new IllegalArgumentException("条件参数不能为空！");}
		params.add(param);
		return this;
	}
	/**
	 * @描述 自定义添加排序
	 * @param orderBy 排序对象
	 * @return 当前条件
	 */
	public Condition addOrderBy(OrderBy orderBy) {
		if (orderBy == null) {throw new IllegalArgumentException("排序参数不能为空！");}
		orderBys.add(orderBy);
		return this;
	}
	/**
	 * @描述 自定义添加分页对象
	 * @param page 分页对象
	 * @return 当前条件
	 */
	public Condition addPage(Page page) {
		if (page == null) {throw new IllegalArgumentException("分页参数不能为空！");}
		this.page = page;
		return this;
	}
	
	/**
	 * @描述 自定义字段（当一个都不添加时查所有，只对查询有效）
	 * @param fieldName 字段名称（必须连接表的Dto类型）
	 * @param joinTable 字段对应连接表 - 输出该表JSON对象
	 * @return 当前条件
	 */
	public Condition addField(String fieldName, JoinTable joinTable) {
		fields.add(new Field().setFieldName(fieldName).setJoinTable(joinTable));
		return this;
	}
	/**
	 * @描述 自定义字段（当一个都不添加时查所有，只对查询有效）
	 * @param fieldName 字段名称（必须连接表的Dto类型）
	 * @param joinTable 字段对应连接表 - 输出该表JSON对象
	 * @param groupField 分组对应连接表 - 输出其他表JSON对象
	 * @return 当前条件
	 */
	public Condition addField(String fieldName, JoinTable joinTable, Map<String, JoinTable> groupField) {
		if (groupField == null) {throw new IllegalArgumentException("分组字段不能为空！");}
		fields.add(new Field().setFieldName(fieldName).setJoinTable(joinTable).setGroupField(groupField));
		return this;
	}
	/**
	 * @描述 自定义字段（当一个都不添加时查所有，只对查询有效）
	 * @param fieldNames 多个字段名称
	 * @return 当前条件
	 */
	public Condition addField(String...fieldNames) {
		if (fieldNames == null) {throw new IllegalArgumentException("字段名称不能为空！");}
		return addField(Arrays.asList(fieldNames));
	}
	/**
	 * @描述 自定义字段（当一个都不添加时查所有，只对查询有效）
	 * @param fieldNames 字段名称集合
	 * @return 当前条件
	 */
	public Condition addField(Collection<String> fieldNames) {
		if (fieldNames == null) {throw new IllegalArgumentException("字段名称不能为空！");}
		for (String fieldName : fieldNames) {fields.add(new Field().setFieldName(fieldName));}
		return this;
	}
	/**
	 * @描述 自定义字段（当一个都不添加时查所有，只对查询有效）
	 * @param mathSql 函数类型（所有字段使用该函数）
	 * @param fieldNames 字段名称集合
	 * @return 当前条件
	 */
	public Condition addField(MathSql mathSql, Collection<String> fieldNames) {
		if (fieldNames == null) {throw new IllegalArgumentException("字段名称不能为空！");}
		for (String fieldName : fieldNames) {fields.add(new Field().setMathSql(mathSql).setFieldName(fieldName));}
		return this;
	}
	/**
	 * @描述 自定义字段（当一个都不添加时查所有，只对查询有效）
	 * @param fieldName 字段名称
	 * @param subField 子查询字段
	 * @return 当前条件
	 */
	public Condition addField(String fieldName, SubCondition subField) {
		fields.add(new Field().setFieldName(fieldName).setSubField(subField));
		return this;
	}
	/**
	 * @描述 自定义字段（当一个都不添加时查所有，只对查询有效）
	 * @param receiveFieldName 自定义接收值字段名
	 * @param fieldName 字段名称
	 * @param subField 子查询字段
	 * @return 当前条件
	 */
	public Condition addField(String receiveFieldName, String fieldName, SubCondition subField) {
		fields.add(new Field().setFieldName(fieldName).setReceiveFieldName(receiveFieldName).setSubField(subField));
		return this;
	}
	/**
	 * @描述 自定义字段（当一个都不添加时查所有，只对查询有效）
	 * @param mathSql 函数类型
	 * @param fieldName 字段名称
	 * @param vals 函数需要时的值
	 * @return 当前条件
	 */
	public Condition addField(MathSql mathSql, String fieldName, String...vals) {
		fields.add(new Field().setFieldName(fieldName).setMathSql(mathSql).setVals(vals));
		return this;
	}
	/**
	 * @描述 自定义字段（当一个都不添加时查所有，只对查询有效）
	 * @param receiveFieldName 自定义接收值字段名
	 * @param mathSql 函数类型
	 * @param fieldName 字段名称
	 * @param vals 函数需要时的值
	 * @return 当前条件
	 */
	public Condition addField(String receiveFieldName, MathSql mathSql, String fieldName, String...vals) {
		fields.add(new Field().setFieldName(fieldName).setReceiveFieldName(receiveFieldName).setMathSql(mathSql).setVals(vals));
		return this;
	}
	
	/**
	 * @描述 自定义连接（只对查询有效）
	 * @param join 待连接的表
	 * @param joinAlias 连接的表别名（为空使用join表别名）
	 * @param joinType 连接类型
	 * @param ons 连接条件
	 * @return 连接对象
	 */
	public Join addJoin(Crud<? extends BaseEntity> join, String joinAlias, JoinType joinType, ON...ons) {
		Join jn = new Join().setJoinTable(new JoinTable(join, joinAlias)).setJoinType(joinType).setOns(ons).setCondition(this);
		joins.put(jn.joinTable.joinAlias, jn);
		return jn;
	}
	
	/**
	 * @描述 与添加参数（默认等于查询）
	 * @param name 参数名
	 * @param value 参数值
	 * @return 当前条件
	 */
	public Condition and(String name, Object value) {return and(name, Operation.Equals, value);}
	/**
	 * @描述 与添加参数
	 * @param name 参数名
	 * @param operation 参数条件（如果为Like默认左右模糊查询）
	 * @param value 参数值
	 * @return 当前条件
	 */
	public Condition and(String name, Operation operation, Object value) {
		params.add(new Param(name, operation, value));
		return this;
	}
	
	/**
	 * @描述 或添加参数（默认等于查询）
	 * @param name 参数名
	 * @param value 参数值
	 * @return 当前条件
	 */
	public Condition or(String name, Object value) {return or(name, Operation.Equals, value);}
	/**
	 * @描述 或添加参数
	 * @param name 参数名
	 * @param operation 参数条件（如果为Like默认左右模糊查询）
	 * @param value 参数值
	 * @return 当前条件
	 */
	public Condition or(String name, Operation operation, Object value) {
		params.add(new Param(name, operation, value).setAnd(false));
		return this;
	}
	
	/**
	 * @描述 与是NULL参数
	 * @param name 参数名
	 * @return 当前条件
	 */
	public Condition andIsNull(String name) {
		params.add(new Param(name, Operation.Is_Null, null));
		return this;
	}
	/**
	 * @描述 与不是NULL参数
	 * @param name 参数名
	 * @return 当前条件
	 */
	public Condition andIsNotNull(String name) {
		params.add(new Param(name, Operation.Is_Not_Null, null));
		return this;
	}
	
	/**
	 * @描述 或是NULL参数
	 * @param name 参数名
	 * @return 当前条件
	 */
	public Condition orIsNull(String name) {
		params.add(new Param(name, Operation.Is_Null, null).setAnd(false));
		return this;
	}
	/**
	 * @描述 或不是NULL参数
	 * @param name 参数名
	 * @return 当前条件
	 */
	public Condition orIsNotNull(String name) {
		params.add(new Param(name, Operation.Is_Not_Null, null).setAnd(false));
		return this;
	}
	
	/**
	 * @描述 与模糊添加参数（自定义左右是否模糊查询）
	 * @param name 参数名
	 * @param value 参数值
	 * @param isLeft 是否左模糊匹配
	 * @param isRight 是否右模糊匹配
	 * @return 当前条件
	 */
	public Condition andLike(String name, Object value, boolean isLeft, boolean isRight) {
		params.add(new Param(name, Operation.Like, value).setLeft(isLeft).setRight(isRight));
		return this;
	}
	/**
	 * @描述 与反向模糊添加参数（自定义左右是否模糊查询）
	 * @param name 参数名
	 * @param value 参数值
	 * @param isLeft 是否左模糊匹配
	 * @param isRight 是否右模糊匹配
	 * @return 当前条件
	 */
	public Condition andNotLike(String name, Object value, boolean isLeft, boolean isRight) {
		params.add(new Param(name, Operation.Not_Like, value).setLeft(isLeft).setRight(isRight));
		return this;
	}
	
	/**
	 * @描述 或模糊添加参数（自定义左右是否模糊查询）
	 * @param name 参数名
	 * @param value 参数值
	 * @param isLeft 是否左模糊匹配
	 * @param isRight 是否右模糊匹配
	 * @return 当前条件
	 */
	public Condition orLike(String name, Object value, boolean isLeft, boolean isRight) {
		params.add(new Param(name, Operation.Like, value).setAnd(false).setLeft(isLeft).setRight(isRight));
		return this;
	}
	/**
	 * @描述 或反向模糊添加参数（自定义左右是否模糊查询）
	 * @param name 参数名
	 * @param value 参数值
	 * @param isLeft 是否左模糊匹配
	 * @param isRight 是否右模糊匹配
	 * @return 当前条件
	 */
	public Condition orNotLike(String name, Object value, boolean isLeft, boolean isRight) {
		params.add(new Param(name, Operation.Not_Like, value).setAnd(false).setLeft(isLeft).setRight(isRight));
		return this;
	}
	
	/**
	 * @描述 分组参数
	 * @param joinTable 分组表，NULL=原样输出name
	 * @param name 分组名称
	 * @return 当前条件
	 */
	public Condition groupBy(JoinTable joinTable, String name) {
		if (name == null) {throw new IllegalArgumentException("分组名称不能为空！");}
		groupBys.add(new GroupBy(joinTable, name));
		return this;
	}
	/**
	 * @描述 分组参数
	 * @param names 多个分组名称
	 * @return 当前条件
	 */
	public Condition groupBy(String...names) {
		if (names == null) {throw new IllegalArgumentException("分组名称不能为空！");}
		for (String name : names) {groupBys.add(new GroupBy(null, name));}
		return this;
	}
	
	/**
	 * @描述 排序参数
	 * @param names 多个排序名称（默认升序）
	 * @return 当前条件
	 */
	public Condition orderBy(String...names) {
		if (names == null) {throw new IllegalArgumentException("排序名称不能为空！");}
		for (String name : names) {orderBys.add(new OrderBy(name, true, null, null));}
		return this;
	}
	/**
	 * @描述 排序参数
	 * @param name 排序名称
	 * @param asc 排序类型 - true为升序
	 * @param joinTable 连接表 - NULL为当前表
	 * @return 当前条件
	 */
	public Condition orderBy(String name, boolean asc, JoinTable joinTable) {
		orderBys.add(new OrderBy(name, asc, null, joinTable));
		return this;
	}
	/**
	 * @描述 排序参数
	 * @param baseEntity 公共实体类
	 * @return 当前条件
	 */
	public Condition orderBy(BaseEntity baseEntity) {
		return orderBy(baseEntity, Default.VALUE);
	}
	/**
	 * @描述 排序参数
	 * @param baseEntity 公共实体类
	 * @param group 条件分组（取Conditions注解groups字段第一个匹配）
	 * @return 当前条件
	 */
	public Condition orderBy(BaseEntity baseEntity, int group) {
		if (baseEntity.getOrderBys() != null) {
			Map<String, java.lang.reflect.Field> fields = FieldUtil.getAllDeclaredField(baseEntity.getClass(), true); //所有参数字段
			for (OrderBy orderBy : baseEntity.getOrderBys()) { //处理自定义排序
				String[] ods = orderBy.getName().split(String.format("[%s]", SPLIT));
				if (ods.length == 2) { //长度为2代表使用连接表排序
					java.lang.reflect.Field field = fields.get(ods[0]);
					if (field != null) {
						Conditions conditions = field.getDeclaredAnnotation(Conditions.class);
						if (conditions != null) {setOrderBy(group, orderBy, ods, conditions);}
					}
				}
			}
			/* 将重新处理的排序填入条件中 */
			orderBys.addAll(baseEntity.getOrderBys());
		}
		return this;
	}
	/* 重新设置排序数据 */
	private void setOrderBy(int group, OrderBy orderBy, String[] ods, Conditions conditions) {
		for (org.city.common.api.annotation.sql.Condition condition : conditions.value()) {
			if (condition.joinTable().join() != Crud.class) { //必须得有Join的表
				for (int groupVal : condition.groups()) {
					if (groupVal == group) { //同一个分组
						Crud<?> crud = getCrud(condition.joinTable().join()); //连接表排序
						if (crud != null) { //添加连接条件
							orderBy.setName(ods[1]);
							orderBy.setJoinTable(new JoinTable(crud, condition.joinTable().alias()));
							return; //只设置第一个
						}
					}
				}
			}
		}
	}
	
	/**
	 * @描述 分页参数（通过偏移量查询）
	 * @param offset 分页偏移量（下标0为第一个数据）
	 * @param pageSize 分页大小
	 * @return 当前条件
	 */
	public Condition limitOffset(long offset, long pageSize) {
		offset = offset < 0 ? 0 : offset;
		pageSize = pageSize < 1 ? 20 : pageSize;
		this.page = new Page(offset, pageSize);
		return this;
	}
	/**
	 * @描述 分页参数
	 * @param baseEntity 公共实体类
	 * @return 当前条件
	 */
	public Condition limit(BaseEntity baseEntity) {
		long offset = (baseEntity.getPageNum() - 1) * baseEntity.getPageSize();
		return limitOffset(offset, baseEntity.getPageSize());
	}
	
	/**
	 * @描述 获取数据库操作对象
	 * @param cls 实现类或接口
	 * @return 数据库操作对象
	 */
	public Crud<?> getCrud(Class<?> cls) {
		Map<String, ?> beansOfType = SpringUtil.getApplicationContext().getBeansOfType(cls);
		for (Entry<String, ?> entry : beansOfType.entrySet()) {
			if (entry.getValue() instanceof Crud) {
				return (Crud<?>) entry.getValue();
			}
		}
		return null;
	}
	
	@Data
	@Accessors(chain = true)
	public static class Param {
		public Param(String name, Operation operation, Object value) {
			this.name = name; this.operation = operation; this.value = value;
		}
		/* 连接表 */
		private JoinTable joinTable;
		/* 字段名 */
		private String name;
		/* 带判断的字段名 */
		public String getName() {
			if (!StringUtils.hasText(this.name)) {throw new NullPointerException("Sql条件字段名参数为空，请检查条件参数信息！");}
			return this.name;
		}
		/* 操作符 */
		private Operation operation;
		/* 值 */
		private Object value;
		/* 是否与操作 */
		private boolean isAnd = true;
		/* 模糊参数是否左模糊（只对Like操作符有用） */
		private boolean isLeft = true;
		/* 模糊参数是否右模糊（只对Like操作符有用） */
		private boolean isRight = true;
	}
	
	@Data
	@Accessors(chain = true)
	public static class Field {
		/* 字段名 */
		private String fieldName;
		/* 自定义接收值字段名 */
		private String receiveFieldName;
		/* 函数需要时的值 */
		private String[] vals;
		/* 函数 */
		private MathSql mathSql = MathSql.Normal;
		
		/* 子查询字段 - 优先级最高 */
		private SubCondition subField;
		/* 接收字段为对象时，子对象对应表连接对象 */
		private JoinTable joinTable;
		/* 接收字段为对象时，子对象字段对应表连接对象 */
		private Map<String, JoinTable> groupField = new HashMap<>();
		
		/**
		 * @描述 自定义Sql格式化函数值
		 * @return 格式化后的函数值
		 */
		public String sqlFormat() {return mathSql.format(vals[0]);}
		/**
		 * @描述 格式化函数值
		 * @param tableField 原表字段名
		 * @return 格式化后的函数值
		 */
		public String format(String tableField) {return mathSql.format(tableField, vals);}
	}
	
	@Data
	@Accessors(chain = true)
	public static class Join {
		/* 原条件 */
		private Condition condition;
		/* 连接表 */
		private JoinTable joinTable;
		/* 连接类型 */
		private JoinType joinType;
		/* 连接条件 */
		private ON[] ons;
		/* 忽略索引名 */
		private String ignore = null;
		/* 子查询连表 - 优先级最高 */
		private SubCondition subJoin = null;
		
		/**
		 * @描述 设置子查询（joinTable中的join替换成虚拟表）
		 * @param subJoin 子查询
		 */
		public void setSubJoin(SubCondition subJoin) {
			this.subJoin = subJoin;
			this.subJoin.setCurAlias(this.joinTable.joinAlias);
		}
		
		/**
		 * @描述 忽略索引名称
		 * @param ignoreName 忽略索引名称
		 * @return 连接对象
		 */
		public Join setIgnore(String ignoreName) {this.ignore = ignoreName; return this;}
		
		/**
		 * @描述 自定义连接字段（当一个都不添加时查所有，只对查询有效）
		 * @param curFieldName 当前Dto对象接收的字段名（必须该连接表的Dto类型）
		 * @return 连接对象
		 */
		public Join addField(String curFieldName) {
			condition.addField(curFieldName, joinTable);
			return this;
		}
		/**
		 * @描述 自定义连接字段（当一个都不添加时查所有，只对查询有效）
		 * @param curFieldName 当前Dto对象接收的字段名（必须该连接表的Dto类型）
		 * @param groupField 分组条件时连接表对象 - 设置其他字段的连接表对象数据
		 * @return 连接对象
		 */
		public Join addField(String curFieldName, Map<String, JoinTable> groupField) {
			condition.addField(curFieldName, joinTable, groupField);
			return this;
		}
		/**
		 * @描述 自定义连接字段（当一个都不添加时查所有，只对查询有效）
		 * @param curFieldName 当前Dto对象接收的字段名
		 * @param joinFieldName 连接实体类字段名
		 * @return 连接对象
		 */
		public Join addField(String curFieldName, String joinFieldName) {
			addField(MathSql.Normal, curFieldName, joinFieldName);
			return this;
		}
		/**
		 * @描述 自定义连接字段（当一个都不添加时查所有，只对查询有效）
		 * @param curFieldNames 多个当前Dto对象接收的字段名
		 * @param joinFieldNames 多个连接实体类字段名
		 * @return 连接对象
		 */
		public Join addField(String[] curFieldNames, String[] joinFieldNames) {
			if (joinFieldNames == null || curFieldNames == null) {throw new IllegalArgumentException("字段名称不能为空！");}
			if (joinFieldNames.length != curFieldNames.length) {throw new IllegalArgumentException("二个字段参数长度不一致！");}
			
			/* 处理多个字段 */
			for (int i = 0, j = joinFieldNames.length; i < j; i++) {addField(MathSql.Normal, curFieldNames[i], joinFieldNames[i]);}
			return this;
		}
		/**
		 * @描述 自定义连接字段（当一个都不添加时查所有，只对查询有效）
		 * @param curFieldNames 当前Dto对象接收的字段名集合
		 * @param joinFieldNames 连接实体类字段名集合
		 * @return 连接对象
		 */
		public Join addField(Collection<String> curFieldNames, Collection<String> joinFieldNames) {
			if (joinFieldNames == null || curFieldNames == null) {throw new IllegalArgumentException("字段名称不能为空！");}
			if (joinFieldNames.size() != curFieldNames.size()) {throw new IllegalArgumentException("二个字段参数长度不一致！");}
			
			/* 调用数组的方法 */
			addField(curFieldNames.toArray(new String[curFieldNames.size()]), joinFieldNames.toArray(new String[joinFieldNames.size()]));
			return this;
		}
		/**
		 * @描述 自定义连接字段（当一个都不添加时查所有，只对查询有效）
		 * @param mathSql 函数类型
		 * @param curFieldName 当前Dto对象接收的字段名
		 * @param joinFieldName 连接实体类字段名（如果mathSql为自定义Sql类型时该值无效）
		 * @param vals 函数需要时的值
		 * @return 连接对象
		 */
		public Join addField(MathSql mathSql, String curFieldName, String joinFieldName, String...vals) {
			if (MathSql.Sql == mathSql) {throw new IllegalArgumentException("Join字段不支持自定义函数类型！");}
			
			/* 如果没有joinFieldName则使用curFieldName */
			joinFieldName = StringUtils.hasText(joinFieldName) ? joinFieldName : curFieldName;
			/* 获取表字段参数 */
			String tableField = joinTable.join.getTableField(joinFieldName);
			condition.addField(MathSql.Sql, curFieldName, mathSql.format(joinTable.joinAlias + "." + tableField, vals));
			return this;
		}
		
		/**
		 * @描述 连接添加参数
		 * @param param 参数
		 * @return 连接对象
		 */
		public Join addParam(Param param) {
			condition.addParam(param.setJoinTable(joinTable));
			return this;
		}
		
		/**
		 * @描述 与连接添加参数（默认等于查询）
		 * @param name 连接表实体参数名
		 * @param value 参数值
		 * @return 连接对象
		 */
		public Join and(String name, Object value) {return and(name, Operation.Equals, value);}
		/**
		 * @描述 与连接添加参数
		 * @param name 连接表实体参数名
		 * @param operation 参数条件（如果为Like默认左右模糊查询）
		 * @param value 参数值
		 * @return 连接对象
		 */
		public Join and(String name, Operation operation, Object value) {
			condition.addParam(new Param(name, operation, value).setJoinTable(joinTable));
			return this;
		}
		
		/**
		 * @描述 或连接添加参数（默认等于查询）
		 * @param name 连接表实体参数名
		 * @param value 参数值
		 * @return 连接对象
		 */
		public Join or(String name, Object value) {return or(name, Operation.Equals, value);}
		/**
		 * @描述 或连接添加参数
		 * @param name 连接表实体参数名
		 * @param operation 参数条件（如果为Like默认左右模糊查询）
		 * @param value 参数值
		 * @return 连接对象
		 */
		public Join or(String name, Operation operation, Object value) {
			condition.addParam(new Param(name, operation, value).setJoinTable(joinTable).setAnd(false));
			return this;
		}
		
		/**
		 * @描述 与连接是NULL参数
		 * @param name 连接表实体参数名
		 * @return 连接对象
		 */
		public Join andIsNull(String name) {
			condition.addParam(new Param(name, Operation.Is_Null, null).setJoinTable(joinTable));
			return this;
		}
		/**
		 * @描述 与连接不是NULL参数
		 * @param name 连接表实体参数名
		 * @return 连接对象
		 */
		public Join andIsNotNull(String name) {
			condition.addParam(new Param(name, Operation.Is_Not_Null, null).setJoinTable(joinTable));
			return this;
		}
		
		/**
		 * @描述 或连接是NULL参数
		 * @param name 连接表实体参数名
		 * @return 连接对象
		 */
		public Join orIsNull(String name) {
			condition.addParam(new Param(name, Operation.Is_Null, null).setJoinTable(joinTable).setAnd(false));
			return this;
		}
		/**
		 * @描述 或连接不是NULL参数
		 * @param name 连接表实体参数名
		 * @return 连接对象
		 */
		public Join orIsNotNull(String name) {
			condition.addParam(new Param(name, Operation.Is_Not_Null, null).setJoinTable(joinTable).setAnd(false));
			return this;
		}
		
		/**
		 * @描述 与连接模糊添加参数（自定义左右是否模糊查询）
		 * @param name 连接表实体参数名
		 * @param value 参数值
		 * @param isLeft 是否左模糊匹配
		 * @param isRight 是否右模糊匹配
		 * @return 连接对象
		 */
		public Join andLike(String name, Object value, boolean isLeft, boolean isRight) {
			condition.addParam(new Param(name, Operation.Like, value).setJoinTable(joinTable).setLeft(isLeft).setRight(isRight));
			return this;
		}
		/**
		 * @描述 与连接反向模糊添加参数（自定义左右是否模糊查询）
		 * @param name 连接表实体参数名
		 * @param value 参数值
		 * @param isLeft 是否左模糊匹配
		 * @param isRight 是否右模糊匹配
		 * @return 连接对象
		 */
		public Join andNotLike(String name, Object value, boolean isLeft, boolean isRight) {
			condition.addParam(new Param(name, Operation.Not_Like, value).setJoinTable(joinTable).setLeft(isLeft).setRight(isRight));
			return this;
		}
		
		/**
		 * @描述 或连接模糊添加参数（自定义左右是否模糊查询）
		 * @param name 连接表实体参数名
		 * @param value 参数值
		 * @param isLeft 是否左模糊匹配
		 * @param isRight 是否右模糊匹配
		 * @return 连接对象
		 */
		public Join orLike(String name, Object value, boolean isLeft, boolean isRight) {
			condition.addParam(new Param(name, Operation.Like, value).setJoinTable(joinTable).setAnd(false).setLeft(isLeft).setRight(isRight));
			return this;
		}
		/**
		 * @描述 或连接反向模糊添加参数（自定义左右是否模糊查询）
		 * @param name 连接表实体参数名
		 * @param value 参数值
		 * @param isLeft 是否左模糊匹配
		 * @param isRight 是否右模糊匹配
		 * @return 连接对象
		 */
		public Join orNotLike(String name, Object value, boolean isLeft, boolean isRight) {
			condition.addParam(new Param(name, Operation.Not_Like, value).setJoinTable(joinTable).setAnd(false).setLeft(isLeft).setRight(isRight));
			return this;
		}
		
		/**
		 * @描述 分组连接参数
		 * @param name 分组名称
		 * @return 连接对象
		 */
		public Join groupBy(String name) {
			condition.groupBy(joinTable, name);
			return this;
		}
		/**
		 * @描述 分组连接参数
		 * @param names 多个分组名称
		 * @return 连接对象
		 */
		public Join groupBy(String...names) {
			for (String name : names) {condition.groupBy(joinTable, name);}
			return this;
		}
		
		/**
		 * @描述 排序连接参数
		 * @param name 排序名称
		 * @param asc 排序类型 - true为升序
		 * @return 连接对象
		 */
		public Join orderBy(String name, boolean asc) {
			condition.orderBy(name, asc, joinTable);
			return this;
		}
		/**
		 * @描述 排序连接参数
		 * @param names 多个排序名称（默认升序）
		 * @return 连接对象
		 */
		public Join orderBy(String...names) {
			for (String name : names) {condition.orderBy(name, true, joinTable);}
			return this;
		}
		
		/**
		 * @描述 转成Sql语句
		 * @param ignoreName 忽略索引名称
		 * @param curService 当前服务
		 * @param dtoValues 待追加设置的参数
		 * @return 连接后的sql
		 */
		public String toSql(String ignoreName, Crud<? extends BaseEntity> curService, List<Object> dtoValues) {
			if (subJoin == null) { //非子查询连表
				ignoreName = StringUtils.hasText(ignoreName) ? " IGNORE INDEX(" + ignoreName + ")" : "";
				String tableName = joinTable.join.getTableName(condition.getBaseEntity());
				return String.format("%s`%s` %s%s%s", joinType.getVal(), tableName, joinTable.joinAlias, ignoreName, onSql(curService, dtoValues));
			} else { //子查询连表
				dtoValues.addAll(subJoin.getParams());
				return String.format("%s(%s) %s%s", joinType.getVal(), subJoin.getSql(), joinTable.joinAlias, onSql(curService, dtoValues));
			}
		}
		/* 条件返回 */
		private String onSql(Crud<? extends BaseEntity> curService, List<Object> dtoValues) {
			if (ons == null || ons.length == 0) {return "";}
			
			/* 连接表所有字段 */
			Map<String, String> joinFields = joinTable.join.getTableFields();
			StringBuilder sb = new StringBuilder();
			
			/* 迭代处理所有连接条件 - 针对or条件追加括号 */
			boolean isKH = false;
			for (int i = 0, j = ons.length; i < j; i++) {
				/* 当前条件 - 针对条件和下一个条件特殊处理 */
				ON on = ons[i];
				if (on.isAnd) {
					/* 如果有下一个参数且是or条件 - 追加起始括号 */
					if ((i + 1) < j && !ons[i + 1].isAnd()) {
						sb.append(" and ("); isKH = true;
					} else {
						/* 如果已经追加起始括号 - 则追加结束括号 */
						if (isKH) {sb.append(") and "); isKH = false;}
						/* 无追加起始括号则默认参数 */
						else {sb.append(" and ");}
					}
				} else {sb.append(" or ");}
				
				/* 当前表 */
				Cur cur = on.cur == null ? new Cur().setCurService(curService) : on.cur;
				Map<String, String> curFields = cur.curService.getTableFields(); //当前表所有字段
				/* 当前表别名 */
				String curAlias = StringUtils.hasText(cur.alias) ? cur.alias : cur.curService.getTable().alias();
				String curFieldName = curFields.get(on.curField), joinFieldName = joinFields.get(on.joinField);
				
				/* 空条件特殊处理 */
				if (on.make == Operation.Is_Null || on.make == Operation.Is_Not_Null) {
					if (curFieldName != null) {sb.append(String.format("%s.%s %s", curAlias, curFieldName, on.make.getOptVal()));}
					else if (joinFieldName != null) {sb.append(String.format("%s.%s %s", joinTable.joinAlias, joinFieldName, on.make.getOptVal()));}
					else {
						String curServiceName = cur.curService.getClass().getSimpleName(), joinServiceName = joinTable.join.getClass().getSimpleName();
						throw new RuntimeException(String.format("当前表服务[%s]与连接表服务[%s]对应[join on]条件字段未找到，请检查对应条件字段名称！", curServiceName, joinServiceName));
					}
				} else {
					if (curFieldName == null) {dtoValues.add(on.curField); curFieldName = "?";} else {curFieldName = curAlias + "." + curFieldName;}
					if (joinFieldName == null) {dtoValues.add(on.joinField); joinFieldName = "?";} else {joinFieldName = joinTable.joinAlias + "." + joinFieldName;}
					sb.append(String.format("%s %s %s", curFieldName, on.make.getOptVal(), joinFieldName));
				}
			}
			
			/* 最后处理条件 */
			sb.delete(0, 4).insert(0, " on (").append(")");
			/* 最后如果是or要追加结束括号 */
			if (isKH) {sb.append(")");}
			return sb.toString();
		}
		
		@Data
		@Accessors(chain = true)
		public static class ON {
			/* 当前表实体字段名 */
			private String curField;
			/* 连接表实体字段名 */
			private String joinField;
			/* 是否And条件 */
			private boolean isAnd = true;
			/* 当前表信息 */
			private Cur cur;
			/* 操作符 */
			private Operation make = Operation.Equals;
			
			/**
			 * @描述 生成join条件
			 * @param curField 当前表实体字段名（找不到会直接用该值代替，意思是可以用固定值）
			 * @param joinField 连接表实体字段名（找不到会直接用该值代替，意思是可以用固定值）
			 * @return 连接条件
			 */
			public static ON of(String curField, String joinField) {
				return new ON().setCurField(curField).setJoinField(joinField);
			}
			/**
			 * @描述 生成join条件
			 * @param curField 当前表实体字段名（找不到会直接用该值代替，意思是可以用固定值）
			 * @param make 操作符
			 * @param joinField 连接表实体字段名（找不到会直接用该值代替，意思是可以用固定值）
			 * @return 连接条件
			 */
			public static ON of(String curField, Operation make, String joinField) {
				return new ON().setCurField(curField).setJoinField(joinField).setMake(make);
			}
			/**
			 * @描述 生成join条件
			 * @param cur 当前表信息
			 * @param curField 当前表实体字段名（找不到会直接用该值代替，意思是可以用固定值）
			 * @param joinField 连接表实体字段名（找不到会直接用该值代替，意思是可以用固定值）
			 * @return 连接条件
			 */
			public static ON of(Cur cur, String curField, String joinField) {
				return new ON().setCurField(curField).setJoinField(joinField).setCur(cur);
			}
			/**
			 * @描述 生成join条件
			 * @param cur 当前表信息
			 * @param curField 当前表实体字段名（找不到会直接用该值代替，意思是可以用固定值）
			 * @param make 操作符
			 * @param joinField 连接表实体字段名（找不到会直接用该值代替，意思是可以用固定值）
			 * @return 连接条件
			 */
			public static ON of(Cur cur, String curField, Operation make, String joinField) {
				return new ON().setCurField(curField).setJoinField(joinField).setCur(cur).setMake(make);
			}
			/**
			 * @描述 生成join条件
			 * @param on 注解连接条件
			 * @return 连接条件
			 */
			public static ON of(org.city.common.api.annotation.sql.ON on) {
				ON ON = new ON().setCurField(on.curField()).setJoinField(on.joinField()).setAnd(on.isAnd()).setMake(on.make());
				if (on.cur().service() != Crud.class) {ON.setCur(new Cur().setCurService(getCrud(on.cur().service())).setAlias(on.cur().alias()));}
				return ON; //连接条件
			}
			/* 获取数据库操作对象 */
			private static Crud<?> getCrud(Class<?> join) {
				Map<String, ?> beansOfType = SpringUtil.getApplicationContext().getBeansOfType(join);
				for (Entry<String, ?> entry : beansOfType.entrySet()) {
					if (entry.getValue() instanceof Crud) {
						return (Crud<?>) entry.getValue();
					}
				}
				return null;
			}
		}
		
		@Data
		@Accessors(chain = true)
		public static class Cur {
			/* 当前表服务 */
			private Crud<? extends BaseEntity> curService;
			/* 表别名 */
			private String alias;
		}
	}
	
	@Data
	@AllArgsConstructor
	public static class OrderBy {
		/* 排序名 */
		private String name;
		/* 排序类型 - true为升序 */
		private boolean asc;
		/* 分组排序 - NULL=内外都排，true=只排序内部，false=只排序外部 */
		private Boolean group;
		/* 排序连接表 */
		private JoinTable joinTable;
	}
	
	@Data
	@AllArgsConstructor
	public static class GroupBy {
		/* 使用哪个表做分组 - NULL=自己 */
		private JoinTable joinTable;
		/* 分组名 */
		private String name;
	}
	
	@Data
	@AllArgsConstructor
	public static class Page {
		/* 分页偏移量 */
		private long offset;
		/* 分页大小 */
		private long pageSize;
	}
	
	@Data
	@Accessors(chain = true)
	public static class JoinTable {
		/* 连接的表 */
		private Crud<? extends BaseEntity> join;
		/* 连接的表别名 */
		private String joinAlias;
		/* 忽略字段 - 只有查询有用 */
		private List<String> ignoreFields = new ArrayList<>();
		/* 分组分页数量 */
		private int limit = Integer.MAX_VALUE;
		
		/**
		 * @param join 连接的表
		 * @param joinAlias 连接的表别名（为空使用join表别名）
		 */
		public JoinTable(Crud<? extends BaseEntity> join, String joinAlias) {
			this.join = join;
			this.joinAlias = StringUtils.hasText(joinAlias) ? joinAlias : join.getTable().alias();
		}
	}
}
