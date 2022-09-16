package org.city.common.mysql.repository;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.city.common.api.annotation.sql.Conditions;
import org.city.common.api.annotation.sql.Fields;
import org.city.common.api.annotation.sql.GroupField;
import org.city.common.api.constant.Operation;
import org.city.common.api.constant.group.Default;
import org.city.common.api.dto.sql.Condition;
import org.city.common.api.dto.sql.Condition.Join;
import org.city.common.api.dto.sql.Condition.Join.ON;
import org.city.common.api.entity.BaseEntity;
import org.city.common.api.dto.sql.Condition.JoinTable;
import org.city.common.api.dto.sql.Condition.Param;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.sql.AnnotationCondition;
import org.city.common.api.in.sql.Crud;
import org.city.common.api.in.sql.MathSqlValue;
import org.city.common.api.util.FieldUtil;
import org.city.common.api.util.MyUtil;
import org.city.common.api.util.SpringUtil;
import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @作者 ChengShi
 * @日期 2022年8月7日
 * @版本 1.0
 * @描述 注解条件仓储
 */
class AnnotationConditionRepository<E extends BaseEntity> implements AnnotationCondition,JSONParser {
	private final Condition condition;
	private final Crud<? extends BaseEntity> curRepository;
	private AnnotationConditionRepository(Condition condition, Crud<? extends BaseEntity> curRepository) {
		this.condition = condition; this.curRepository = curRepository;
	}
	
	/* 生成注解条件服务 */
	static <E extends BaseEntity> AnnotationCondition getJoin(E entity, Crud<? extends BaseEntity> curRepository, int...groups) {
		if (entity == null) {throw new NullPointerException("实体类条件对象不能为空！");}
		if (groups == null) {throw new NullPointerException("分组条件可以不传，但是不能为空！");}
		
		/* 生成条件 */
		Condition condition = new Condition(entity);
		AnnotationConditionRepository<E> conditionService = new AnnotationConditionRepository<E>(condition, curRepository);
		
		/* 只取有连接的类 */
		List<DataCondition<Object, org.city.common.api.annotation.sql.Join>> fcs = new ArrayList<>();
		Annotation annotation = entity.getClass().getDeclaredAnnotation(org.city.common.api.annotation.sql.Joins.class);
		if (annotation != null) {conditionService.parseGroups(false, fcs, annotation, null, groups);}
		/* 没有条件则退出 */
		if (fcs.size() == 0) {return conditionService;}
		
		/* 顺序处理条件 */
		for (DataCondition<Object, org.city.common.api.annotation.sql.Join> fc : fcs) {
			org.city.common.api.annotation.sql.Join jn = fc.annotation;
			if (jn.join() != Crud.class) {
				Crud<?> crud = condition.getCrud(jn.join());
				if (crud != null) { //添加连接条件
					ON[] ons = new ON[jn.ons().length];
					for (int i = 0, j = ons.length; i < j; i++) {ons[i] = ON.of(jn.ons()[i]);}
					condition.addJoin(crud, jn.alias(), jn.joinType(), ons).setIgnore(jn.ignore());
				}
			}
		}
		return conditionService;
	}
	
	@Override
	public Condition cd() {return condition;}
	
	@Override
	public AnnotationCondition getNotEmpty(int...groups) {
		if (groups == null) {throw new NullPointerException("分组条件可以不传，但是不能为空！");}
		List<DataCondition<Field, org.city.common.api.annotation.sql.Condition>> fcs = getCondition(condition.getBaseEntity(), groups); //获取所有条件
		if (fcs == null) {return this;} //无条件直接返回
		
		try {
			/* 顺序处理条件 */
			for (DataCondition<Field, org.city.common.api.annotation.sql.Condition> fc : fcs) {
				Field field = fc.data; org.city.common.api.annotation.sql.Condition cd = (org.city.common.api.annotation.sql.Condition) fc.annotation;
				Object val = getFieldValue(condition.getBaseEntity(), field, cd); //取出对象字段值
				
				/* 基本类型或时间类型处理 - 当前对象 */
				if (isBaseType(field.getType()) || Date.class.isAssignableFrom(field.getType())) {
					setBaseCondition(field, cd, val, null);
				} else {
					if (val != null && cd.joinTable().join() != Crud.class) { //其他类型 - 连接表对象
						Crud<?> crud = condition.getCrud(cd.joinTable().join());
						if (val.getClass().isArray() && Array.getLength(val) > 0) {val = Array.get(val, 0);} //如果字段值是数组则取第一个值
						else if (val instanceof Collection && ((Collection<?>) val).size() > 0) {val = ((Collection<?>) val).iterator().next();} //如果字段值是集合则取第一个值
						if (val instanceof BaseEntity && crud != null) { //添加连接条件
							List<DataCondition<Field, org.city.common.api.annotation.sql.Condition>> fcsJoin = getCondition((BaseEntity) val, cd.joinTable().groups());
							if (fcsJoin == null) {continue;} //无条件跳过
							
							/* 顺序处理条件 */
							for (DataCondition<Field, org.city.common.api.annotation.sql.Condition> fcJoin : fcsJoin) {
								Field fieldJoin = fcJoin.data; org.city.common.api.annotation.sql.Condition cdJoin = (org.city.common.api.annotation.sql.Condition) fcJoin.annotation;
								Object valJoin = getFieldValue((BaseEntity) val, fieldJoin, cdJoin); //取出连接对象字段值
								if (isBaseType(fieldJoin.getType()) || Date.class.isAssignableFrom(fieldJoin.getType())) { //基本类型或时间类型处理 - 连接对象
									JoinTable joinTable = new JoinTable(crud, cd.joinTable().alias());
									setBaseCondition(fieldJoin, cdJoin, valJoin, joinTable);
								}
							}
						}
					}
				}
			}
			return this;
		} catch (Exception e) {throw new RuntimeException(e);}
	}
	/* 获取所有条件 */
	private List<DataCondition<Field, org.city.common.api.annotation.sql.Condition>> getCondition(BaseEntity baseEntity, int...groups) {
		/* 只取有条件的字段 */
		List<DataCondition<Field, org.city.common.api.annotation.sql.Condition>> fcs = new ArrayList<>();
		for (Field field : FieldUtil.getAllDeclaredField(baseEntity.getClass(), true, BaseEntity.class).values()) {
			Annotation annotation = field.getDeclaredAnnotation(Conditions.class);
			if (annotation != null) {parseGroups(false, fcs, annotation, field, groups);}
		}
		/* 没有条件则使用全条件 */
		if (fcs.size() == 0) {return null;}
		
		/* 通过条件升序排 */
		fcs.sort(new Comparator<DataCondition<Field, org.city.common.api.annotation.sql.Condition>>() {
			@Override
			public int compare(DataCondition<Field, org.city.common.api.annotation.sql.Condition> o1, DataCondition<Field, org.city.common.api.annotation.sql.Condition> o2) {
				return o1.annotation.order() - o2.annotation.order();
			}
		});
		return fcs;
	}
	/* 取出对象字段值 */
	@SuppressWarnings("unchecked")
	private Object getFieldValue(BaseEntity entity, Field field, org.city.common.api.annotation.sql.Condition cd) throws IllegalAccessException {
		Object val = cd.fixVal();
		if (StringUtils.hasText((String) val)) {
			if (cd.fixValEnum() != Enum.class) { //对枚举值取toString
				val = Enum.valueOf(cd.fixValEnum(), (String) val).toString();
			}
			/* 尝试格式化字符串为int类型 */
			try {val = Integer.parseInt((String) val);} catch (NumberFormatException e) {}
		} else {
			if (cd.alias().length() == 0) {val = field.get(entity);}
			else {
				Field aliasField = FieldUtil.getAllDeclaredField(entity.getClass(), true).get(cd.alias());
				val = aliasField == null ? entity.getParams().get(cd.alias()) : aliasField.get(entity);
			}
		}
		return val;
	}
	/* 设置基本类型 */
	private void setBaseCondition(Field field, org.city.common.api.annotation.sql.Condition cd, Object val, JoinTable joinTable) {
		boolean valEmpty = val == null || !StringUtils.hasText(val.toString()); //是否空值
		if (Operation.Is_Null == cd.make() || Operation.Is_Not_Null == cd.make()) {if (!valEmpty) {return;}} //如果是空条件 - 但值不为空则跳过
		else if (valEmpty) {return;} //如果是非空条件 - 但值为空则跳过
		
		/* 条件 */
		Param param = new Param(field.getName(), cd.make(), val).setAnd(cd.isAnd()).setLeft(cd.isLeft()).setRight(cd.isRight()).setJoinTable(joinTable);
		if (cd.joinTable().join() != Crud.class) {
			Crud<?> crud = condition.getCrud(cd.joinTable().join());
			if (joinTable == null && crud != null) {
				/* 如果有连接实体类的字段名则使用它 */
				if (StringUtils.hasText(cd.joinTable().joinFieldName())) {param.setName(cd.joinTable().joinFieldName());}
				param.setJoinTable(new JoinTable(crud, cd.joinTable().alias()));
			} else {return;} //非基本类型或没有连接对象 - 不添加条件
		}
		/* 添加条件 */
		condition.addParam(param);
	}
	
	@Override
	public AnnotationCondition getField(int...groups) {
		if (groups == null) {throw new NullPointerException("分组条件可以不传，但是不能为空！");}
		
		/* 只取有字段的类 */
		List<DataCondition<Field, org.city.common.api.annotation.sql.Field>> fcs = new ArrayList<>();
		for (Field field : condition.getEntityFields().values()) {
			Annotation annotation = field.getDeclaredAnnotation(Fields.class);
			if (annotation != null) {parseGroups(true, fcs, annotation, field, groups);}
		}
		/* 没有字段则使用全条件 */
		if (fcs.size() == 0) {return this;}
		
		/* 通过字段升序排 */
		fcs.sort(new Comparator<DataCondition<Field, org.city.common.api.annotation.sql.Field>>() {
			@Override
			public int compare(DataCondition<Field, org.city.common.api.annotation.sql.Field> o1, DataCondition<Field, org.city.common.api.annotation.sql.Field> o2) {
				return o1.annotation.order() - o2.annotation.order();
			}
		});
		
		/* 连接条件 */
		Join join = new Join().setCondition(condition);
		/* 顺序处理字段 */
		for (DataCondition<Field, org.city.common.api.annotation.sql.Field> fc : fcs) {
			org.city.common.api.annotation.sql.Join joinTb = fc.annotation.joinTable(); //连接表
			if (joinTb.join() == Crud.class) {
				if (StringUtils.hasText(fc.annotation.receiveFieldName())) {
					condition.addField(fc.annotation.receiveFieldName(), fc.annotation.mathSql(), fc.data.getName(), getVals(fc.annotation, null));
				} else {
					condition.addField(fc.annotation.mathSql(), fc.data.getName(), getVals(fc.annotation, null));
				}
			} else {
				Crud<?> joinService = condition.getCrud(joinTb.join());
				if (joinService != null) {
					/* 设置字段参数 */
					JoinTable joinTable = new JoinTable(joinService, joinTb.alias()).setLimit(joinTb.limit());
					if (isBaseType(fc.data.getType())) { //基本类型直接用字段名装填
						join.setJoinTable(joinTable).addField(fc.annotation.mathSql(), fc.data.getName(), joinTb.joinFieldName(), getVals(fc.annotation, joinService));
					} else { //非基本类型使用子对象模式装填
						Map<String, JoinTable> groupFields = new HashMap<>();
						/* 处理分组字段 */
						for (GroupField groupField : joinTb.groupFields()) {
							Crud<?> fieldService = condition.getCrud(groupField.field());
							if (fieldService != null) {groupFields.put(groupField.fieldName(), new JoinTable(fieldService, groupField.alias()));}
						}
						/* 添加忽略字段与分组字段 */
						joinTable.getIgnoreFields().addAll(Arrays.asList(joinTb.ignoreFieldNames()));
						join.setJoinTable(joinTable).addField(fc.data.getName(), groupFields);
					}
				}
			}
		}
		return this;
	}
	
	/* 获取自定义函数值 */
	private String[] getVals(org.city.common.api.annotation.sql.Field field, Crud<? extends BaseEntity> join) {
		if (field.mathSqlVal() == MathSqlValue.class) {return field.vals();}
		else {return SpringUtil.getBean(field.mathSqlVal()).getVals(curRepository, join, field.vals());}
	}
	
	/* 解析分组 */
	@SuppressWarnings("unchecked")
	private <R, A> void parseGroups(boolean first, List<DataCondition<R, A>> fcs, Annotation anno, R r, int...groups) {
		Map<String, Object> annotationVal = MyUtil.getAnnotationVal((Annotation) anno);
		for (Annotation an : (Annotation[]) annotationVal.get("value")) {
			int[] cdGroups = (int[]) MyUtil.getAnnotationVal(an).get("groups");
			groups = groups.length == 0 ? new int[] {Default.VALUE} : groups;
			
			/* 验证分组添加 */
			for (int i : groups) {
				for (int j : cdGroups) {
					if (i == j) { //只取相同条件分组
						fcs.add(new DataCondition<R, A>(r, (A) an));
						if (first) {return;}
					}
				}
			}
		}
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2022-08-06 11:04:56
	 * @版本 1.0
	 * @描述 数据条件类
	 */
	@Data
	@AllArgsConstructor
	private static class DataCondition<R, A> {
		/* 数据 */
		private R data;
		/* 条件 */
		private A annotation;
	}
}
