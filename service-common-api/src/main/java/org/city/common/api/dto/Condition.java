package org.city.common.api.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.city.common.api.constant.JoinType;
import org.city.common.api.constant.MathSql;
import org.city.common.api.constant.Operation;
import org.city.common.api.dto.Condition.Join.Cur;
import org.city.common.api.dto.Condition.Join.ON;
import org.city.common.api.in.sql.Crud;
import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-07-04 10:06:37
 * @版本 1.0
 * @描述 条件对象
 */
@Data
@NoArgsConstructor
public class Condition {
	/** 初始条件 - 无条件查所有 */
	public Condition(String name, Operation operation, Object value) {
		params.add(new Param(name, operation, value));
	}
	/** 初始条件 - 默认等于查询 */
	public Condition(String name, Object value) {
		params.add(new Param(name, Operation.Equals, value));
	}
	/* 自定义字段 */
	private List<Field> fields = new ArrayList<>();
	/*所有条件*/
	private List<Param> params = new ArrayList<>();
	/* 连接表 */
	private Map<String, Join> joins = new LinkedHashMap<>();
	/*分组条件*/
	private Set<String> groupBys = new HashSet<>();
	/* 排序条件 */
	private List<OrderBy> orderBys = new ArrayList<>();
	/*分页*/
	private Page page = null;
	/* 自定义条件 */
	private BaseDto baseDto = new BaseDto();
	
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
	public Join getJoin(Crud<? extends BaseDto> joinService) {return joins.get(joinService.getTable().alias());}
	
	/**
	 * @描述 自定义添加Sql条件
	 * @param baseDto Sql条件对象
	 * @return 当前条件
	 */
	public Condition addBase(BaseDto baseDto) {
		if (baseDto == null) {throw new IllegalArgumentException("Sql条件参数不能为空！");}
		this.baseDto = baseDto;
		return this;
	}
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
	 * @param fieldNames 多个字段名称
	 * @return 当前条件
	 */
	public Condition addField(String...fieldNames) {
		if (fieldNames == null) {throw new IllegalArgumentException("字段名称不能为空！");}
		for (String fieldName : fieldNames) {addField(MathSql.Nomal, fieldName);}
		return this;
	}
	/**
	 * @描述 自定义字段（当一个都不添加时查所有，只对查询有效）
	 * @param fieldNames 字段名称集合
	 * @return 当前条件
	 */
	public Condition addField(Collection<String> fieldNames) {
		if (fieldNames == null) {throw new IllegalArgumentException("字段名称不能为空！");}
		for (String fieldName : fieldNames) {addField(MathSql.Nomal, fieldName);}
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
	 * @param receiveName 接收值字段名
	 * @param mathSql 函数类型
	 * @param fieldName 字段名称
	 * @param vals 函数需要时的值
	 * @return 当前条件
	 */
	public Condition addField(String receiveName, MathSql mathSql, String fieldName, String...vals) {
		fields.add(new Field().setFieldName(fieldName).setReceiveName(receiveName).setMathSql(mathSql).setVals(vals));
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
	public Join addJoin(Crud<? extends BaseDto> join, String joinAlias, JoinType joinType, ON...ons) {
		Join jn = new Join().setJoinTable(new JoinTable(join, joinAlias)).setJoinType(joinType).setOns(ons).setCondition(this);
		joins.put(jn.joinTable.joinAlias, jn);
		return jn;
	}
	/**
	 * @描述 自定义连接（只对查询有效）
	 * @param cur 当前表
	 * @param join 待连接的表
	 * @param joinAlias 连接的表别名（为空使用join表别名）
	 * @param joinType 连接类型
	 * @param ons 连接条件
	 * @return 连接对象
	 */
	public Join addJoin(Cur cur, Crud<? extends BaseDto> join, String joinAlias, JoinType joinType, ON...ons) {
		Join jn = new Join().setCur(cur).setJoinTable(new JoinTable(join, joinAlias)).setJoinType(joinType).setOns(ons).setCondition(this);
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
	 * @param names 多个分组名称
	 * @return 当前条件
	 */
	public Condition groupBy(String...names) {
		if (names == null) {throw new IllegalArgumentException("分组名称不能为空！");}
		for (String name : names) {groupBys.add(name);}
		return this;
	}
	
	/**
	 * @描述 排序参数
	 * @param name 多个排序名称（默认升序）
	 * @return 当前条件
	 */
	public Condition orderBy(String...names) {
		if (names == null) {throw new IllegalArgumentException("排序名称不能为空！");}
		for (String name : names) {orderBys.add(new OrderBy(name, true));}
		return this;
	}
	/**
	 * @描述 排序参数
	 * @param name 排序名称
	 * @param isAsc 排序类型 - true为升序
	 * @return 当前条件
	 */
	public Condition orderBy(String name, boolean isAsc) {
		orderBys.add(new OrderBy(name, isAsc));
		return this;
	}
	
	/**
	 * @描述 分页参数（通过偏移量查询）
	 * @param offset 分页偏移量（下标0为第一个数据）
	 * @param pageSize 分页大小
	 * @return 当前条件
	 */
	public Condition limitOffset(int offset, int pageSize) {
		this.page = new Page(offset, pageSize);
		return this;
	}
	
	/**
	 * @描述 分页参数
	 * @param baseDto 基本参数
	 * @return 当前条件
	 */
	public Condition limit(BaseDto baseDto) {
		if (baseDto.getPageNum() == null || baseDto.getPageSize() == null) {return this;}
		int offset = (baseDto.getPageNum() - 1) * baseDto.getPageSize();
		return limitOffset(offset, baseDto.getPageSize());
	}
	
	@Data
	@NoArgsConstructor
	@Accessors(chain = true)
	public static class Param {
		public Param(String name, Operation operation, Object value) {
			this.name = name; this.operation = operation; this.value = value;
		}
		/* 连接表 */
		private JoinTable joinTable;
		/*字段名*/
		private String name;
		/* 带判断的字段名 */
		public String getName() {
			if (!StringUtils.hasText(this.name)) {throw new NullPointerException("Sql条件字段名参数为空，请检查条件参数信息！");}
			return this.name;
		}
		/*操作符*/
		private Operation operation;
		/*值*/
		private Object value;
		/*是否与操作*/
		private boolean isAnd = true;
		/*模糊参数是否左模糊（只对Like操作符有用）*/
		private boolean isLeft = true;
		/*模糊参数是否右模糊（只对Like操作符有用）*/
		private boolean isRight = true;
	}
	
	@Data
	@NoArgsConstructor
	@Accessors(chain = true)
	public static class Field {
		/* 字段名 */
		private String fieldName;
		/* 接收值字段名 */
		private String receiveName;
		/* 函数需要时的值 */
		private String[] vals;
		/* 函数 */
		private MathSql mathSql;
		
		/**
		 * @描述 格式化表字段名
		 * @param tableField 表字段名
		 * @return 格式化后的表字段名
		 */
		public String format(String tableField) {return mathSql.format(tableField, vals);}
	}
	
	@Data
	@NoArgsConstructor
	@Accessors(chain = true)
	public static class Join {
		/* 原条件 */
		private Condition condition;
		/* 连接表 */
		private JoinTable joinTable;
		/* 连接类型 */
		private JoinType joinType;
		/* 当前表 */
		private Cur cur;
		/* 连接条件 */
		private ON[] ons;
		
		/**
		 * @描述 自定义连接字段（当一个都不添加时查所有，只对查询有效）
		 * @param curFieldNames 多个当前Dto对象接收的字段名
		 * @param joinFieldNames 多个连接实体类字段名
		 * @return 连接对象
		 */
		public Join addField(String[] curFieldNames, String...joinFieldNames) {
			if (joinFieldNames == null || curFieldNames == null) {throw new IllegalArgumentException("字段名称不能为空！");}
			if (joinFieldNames.length != curFieldNames.length) {throw new IllegalArgumentException("二个字段参数长度不一致！");}
			
			for (int i = 0, j = joinFieldNames.length; i < j; i++) {addField(MathSql.Nomal, curFieldNames[i], joinFieldNames[i]);}
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
			if (MathSql.Sql == mathSql) {condition.addField(mathSql, curFieldName, vals); return this;}
			
			/* 如果没有joinFieldName则使用curFieldName */
			joinFieldName = StringUtils.hasText(joinFieldName) ? joinFieldName : curFieldName;
			/*获取表字段参数*/
			String tableField = joinTable.join.getTableField(joinFieldName);
			condition.addField(MathSql.Sql, curFieldName, mathSql.format(joinTable.joinAlias + "." + tableField));
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
		 * @描述 转成Sql语句
		 * @return 连接后的sql
		 */
		public String toSql() {
			return String.format("%s`%s` %s on (%s)", joinType.val,
					joinTable.join.getTable().name(), joinTable.joinAlias, onSql(joinTable.joinAlias));
		}
		/* 条件返回 */
		private String onSql(String joinAlias) {
			if (ons == null || ons.length == 0) {return "1 = 1";}
			
			/* 二个表的信息 */
			Map<String, String> curFields = cur.curService.getTableFields();
			Map<String, String> joinFields = joinTable.join.getTableFields();
			/* 当前表别名 */
			String curAlias = StringUtils.hasText(cur.alias) ? cur.alias : cur.curService.getTable().alias();
			
			StringBuilder sb = new StringBuilder();
			for (ON on : ons) {
				sb.append(String.format("%s = %s and ", getName(curFields, curAlias, on.curField),
						getName(joinFields, joinAlias, on.joinField)));
			}
			/* 去除最后的逗号 */
			return sb.substring(0, sb.length() - 4);
		}
		/* 取字段名称 */
		private String getName(Map<String, String> fields, String alias, String field) {
			String name = fields.get(field);
			return name == null ? field : alias + "." + name;
		}
		
		@Data
		@NoArgsConstructor
		@Accessors(chain = true)
		public static class ON {
			/* 当前表实体字段名 */
			private String curField;
			/* 连接表实体字段名 */
			private String joinField;
			
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
			 * @param on 注解连接条件
			 * @return 连接条件
			 */
			public static ON of(org.city.common.api.annotation.sql.ON on) {
				return new ON().setCurField(on.curField()).setJoinField(on.joinField());
			}
		}
		
		@Data
		@NoArgsConstructor
		@Accessors(chain = true)
		public static class Cur {
			/* 当前表服务 */
			private Crud<? extends BaseDto> curService;
			/* 表别名 */
			private String alias;
		}
	}
	
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class OrderBy {
		/* 排序名 */
		private String name;
		/* 排序类型 - true为升序 */
		private boolean isAsc;
	}
	
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Page {
		/*分页偏移量*/
		private int offset;
		/*分页大小*/
		private int pageSize;
	}
	
	@Data
	@NoArgsConstructor
	public static class JoinTable {
		/* 连接的表 */
		private Crud<? extends BaseDto> join;
		/* 连接的表别名 */
		private String joinAlias;
		
		/**
		 * @param join 连接的表
		 * @param joinAlias 连接的表别名（为空使用join表别名）
		 */
		public JoinTable(Crud<? extends BaseDto> join, String joinAlias) {
			this.join = join;
			this.joinAlias = StringUtils.hasText(joinAlias) ? joinAlias : join.getTable().alias();
		}
	}
}
