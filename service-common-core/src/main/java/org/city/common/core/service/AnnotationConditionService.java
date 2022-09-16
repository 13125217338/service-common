package org.city.common.core.service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.city.common.api.annotation.sql.Conditions;
import org.city.common.api.annotation.sql.Fields;
import org.city.common.api.constant.Operation;
import org.city.common.api.constant.group.Default;
import org.city.common.api.dto.BaseDto;
import org.city.common.api.dto.Condition;
import org.city.common.api.dto.Condition.Join;
import org.city.common.api.dto.Condition.JoinTable;
import org.city.common.api.dto.Condition.Param;
import org.city.common.api.dto.Condition.Join.Cur;
import org.city.common.api.dto.Condition.Join.ON;
import org.city.common.api.in.sql.AnnotationCondition;
import org.city.common.api.in.sql.Crud;
import org.city.common.api.in.sql.MathSqlValue;
import org.city.common.api.util.MyUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @作者 ChengShi
 * @日期 2022年8月7日
 * @版本 1.0
 * @描述 注解条件服务
 */
class AnnotationConditionService<D extends BaseDto> implements AnnotationCondition<D>{
	private final Map<String, Field> DTO_FIELD;
	private final Condition condition;
	private final Crud<D> curService;
	private final ApplicationContext applicationContext;
	private AnnotationConditionService(Map<String, Field> DTO_FIELD, Condition condition, Crud<D> curService, ApplicationContext applicationContext) {
		this.DTO_FIELD = DTO_FIELD; this.condition = condition; this.curService = curService; this.applicationContext = applicationContext;
	}
	
	/* 生成注解条件服务 */
	static <D extends BaseDto> AnnotationCondition<D> getJoin(Crud<D> curService, Class<?> DTO_CLASS, Map<String, Field> DTO_FIELD, ApplicationContext applicationContext, int... groups) {
		if (groups == null) {throw new NullPointerException("分组条件可以不传，但是不能为空！");}
		
		/* 生成条件 */
		Condition condition = new Condition();
		AnnotationConditionService<D> conditionService = new AnnotationConditionService<D>(DTO_FIELD, condition, curService, applicationContext);
		
		/* 只取有连接的类 */
		List<DataCondition<Object, org.city.common.api.annotation.sql.Join>> fcs = new ArrayList<>();
		Annotation annotation = DTO_CLASS.getDeclaredAnnotation(org.city.common.api.annotation.sql.JoinTable.class);
		if (annotation != null) {conditionService.parseGroups(fcs, annotation, null, groups);}
		/* 没有条件则退出 */
		if (fcs.size() == 0) {return conditionService;}
		
		/* 顺序处理条件 */
		for (DataCondition<Object, org.city.common.api.annotation.sql.Join> fc : fcs) {
			org.city.common.api.annotation.sql.Join jn = fc.annotation;
			if (jn.join() != Crud.class) {
				Crud<?> crud = conditionService.getCrud(jn.join());
				if (crud != null) {
					ON[] ons = new ON[jn.ons().length];
					for (int i = 0, j = ons.length; i < j; i++) {ons[i] = ON.of(jn.ons()[i]);}
					
					Cur cur = new Cur();
					if (jn.cur().service() != Crud.class) {
						cur.setCurService(conditionService.getCrud(jn.cur().service()));
						cur.setAlias(jn.cur().alias());
					}
					/* 添加连接条件 */
					condition.addJoin(cur, crud, jn.alias(), jn.joinType(), ons);
				}
			}
		}
		return conditionService;
	}

	@Override
	public AnnotationCondition<D> getNotEmpty(BaseDto d, int... groups) {
		if (d == null) {throw new NullPointerException("传入待获取条件对象不能为空！");}
		if (groups == null) {throw new NullPointerException("分组条件可以不传，但是不能为空！");}
		
		/* 只取有条件的字段 */
		List<DataCondition<Field, org.city.common.api.annotation.sql.Condition>> fcs = new ArrayList<>();
		for (Field field : DTO_FIELD.values()) {
			Annotation annotation = field.getDeclaredAnnotation(Conditions.class);
			if (annotation != null) {parseGroups(fcs, annotation, field, groups);}
		}
		/* 没有条件则使用全条件 */
		if (fcs.size() == 0) {return this;}
		
		/* 通过条件升序排 */
		fcs.sort(new Comparator<DataCondition<Field, org.city.common.api.annotation.sql.Condition>>() {
			@Override
			public int compare(DataCondition<Field, org.city.common.api.annotation.sql.Condition> o1, DataCondition<Field, org.city.common.api.annotation.sql.Condition> o2) {
				return o1.annotation.order() - o2.annotation.order();
			}
		});
		
		try {
			/* 顺序处理条件 */
			for (DataCondition<Field, org.city.common.api.annotation.sql.Condition> fc : fcs) {
				Field field = fc.data; org.city.common.api.annotation.sql.Condition cd = (org.city.common.api.annotation.sql.Condition) fc.annotation;
				/* 取出对象字段值 */
				Object val = null;
				if (StringUtils.hasText(cd.fixVal())) {
					try {val = NumberFormat.getInstance().parse(cd.fixVal());}
					catch (ParseException e) {val = cd.fixVal();}
				} else {
					if (cd.alias().length() == 0) {val = field.get(d);}
					else {
						Field aliasField = DTO_FIELD.get(cd.alias());
						val = aliasField == null ? d.getParams().get(cd.alias()) : aliasField.get(d);
					}
				}
				/* 特殊条件处理 - 否则没有值直接跳过 */
				if (Operation.Is_Null != cd.make() && Operation.Is_Not_Null != cd.make() && !MyUtil.isNotBlank(val)) {continue;}
				
				/* 条件 */
				Param param = new Param().setAnd(cd.isAnd()).setOperation(cd.make()).setLeft(cd.isLeft()).setRight(cd.isRight()).setName(field.getName()).setValue(val);
				if (cd.joinTable().join() != Crud.class) {
					Crud<?> crud = getCrud(cd.joinTable().join());
					if (crud != null) {
						/* 如果有连接实体类的字段名则使用它 */
						if (StringUtils.hasText(cd.joinTable().joinFieldName())) {param.setName(cd.joinTable().joinFieldName());}
						param.setJoinTable(new JoinTable(crud, cd.joinTable().alias()));
					}
				}
				/* 添加条件 */
				condition.addParam(param);
			}
			return this;
		} catch (Exception e) {throw new RuntimeException(e);}
	}

	@Override
	public AnnotationCondition<D> getField(int... groups) {
		if (groups == null) {throw new NullPointerException("分组条件可以不传，但是不能为空！");}
		
		/* 只取有字段的类 */
		List<DataCondition<Field, org.city.common.api.annotation.sql.Field>> fcs = new ArrayList<>();
		for (Field field : DTO_FIELD.values()) {
			Annotation annotation = field.getDeclaredAnnotation(Fields.class);
			if (annotation != null) {parseGroups(fcs, annotation, field, groups);}
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
			if (fc.annotation.joinTable().join() == Crud.class) {
				condition.addField(fc.annotation.receiveName(), fc.annotation.mathSql(), fc.data.getName(), getVals(fc.annotation, curService, null));
			} else {
				Crud<?> crud = getCrud(fc.annotation.joinTable().join());
				if (crud != null) {
					JoinTable joinTable = new JoinTable(crud, fc.annotation.joinTable().alias());
					join.setJoinTable(joinTable).addField(fc.annotation.mathSql(), fc.data.getName(),
							fc.annotation.joinTable().joinFieldName(), getVals(fc.annotation, curService, crud));
				}
			}
		}
		return this;
	}
	/* 获取函数值 */
	private String[] getVals(org.city.common.api.annotation.sql.Field field, Crud<?> cur, Crud<?> join) {
		/* 优先字符数组 */
		if (field.vals().length > 0) {return field.vals();}
		/* 没有使用该值 */
		if (field.val() != MathSqlValue.class) {
			/* 获取实现类 */
			MathSqlValue bean = applicationContext.getBean(field.val());
			if (bean == null) {throw new NullPointerException(String.format("实现类[%s]未交给Spring管理！", field.val().getName()));}
			
			try {return bean.setVals(cur, join);}
			catch (Exception e) {throw new RuntimeException("自定义获取函数值错误！", e);}
		}
		/* 都没有返回空数组 */
		return new String[0];
	}

	@Override
	public Condition cd() {return condition;}
	
	/* 解析分组 */
	@SuppressWarnings("unchecked")
	private <R, A> void parseGroups(List<DataCondition<R, A>> fcs, Annotation anno, R r, int...groups) {
		Map<String, Object> annotationVal = MyUtil.getAnnotationVal((Annotation) anno);
		for (Annotation an : (Annotation[]) annotationVal.get("value")) {
			int[] cdGroups = (int[]) MyUtil.getAnnotationVal(an).get("groups");
			
			/* 如果有指定条件 */
			if (groups.length > 0) {
				for (int i : groups) {
					for (int j : cdGroups) {
						/* 只取相同条件ID */
						if (i == j) {fcs.add(new DataCondition<R, A>(r, (A) an));}
					}
				}
			} else {
				for (int i : cdGroups) {
					/* 只取默认条件ID */
					if (i == Default.VALUE) {fcs.add(new DataCondition<R, A>(r, (A) an));}
				}
			}
		}
	}
	/* 获取数据库操作对象 */
	private Crud<?> getCrud(Class<?> join) {
		Map<String, ?> beansOfType = applicationContext.getBeansOfType(join);
		for (Entry<String, ?> entry : beansOfType.entrySet()) {
			if (entry.getValue() instanceof Crud) {
				return (Crud<?>) entry.getValue();
			}
		}
		return null;
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
