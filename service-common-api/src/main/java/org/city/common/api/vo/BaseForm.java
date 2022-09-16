package org.city.common.api.vo;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.city.common.api.annotation.plug.AliasFrom;
import org.city.common.api.annotation.plug.AliasTo;
import org.city.common.api.constant.group.Default;
import org.city.common.api.dto.DataList;
import org.city.common.api.entity.BaseEntity;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.util.FieldUtil;
import org.city.common.api.util.JsonUtil;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;

/**
 * @作者 ChengShi
 * @日期 2022-07-24 19:32:46
 * @版本 1.0
 * @描述 公共表单
 */
public class BaseForm<D extends BaseEntity> extends BaseEntity {
	private final static Map<Class<?>, Map<Field, AliasTo>> AT_CACHE = new ConcurrentHashMap<>();
	private final static Map<Class<?>, Map<Field, String>> AF_CACHE = new ConcurrentHashMap<>();
	
	/**
	 * @描述 转换成Dto对象
	 * @param groups 与注解@AliasTo下的groups对应，不传获取所有默认参数
	 * @return Dto对象
	 */
	public D to(int...groups) {
		if (groups == null) {throw new NullPointerException("分组条件可以不传，但是不能为空！");}
		try {
			/* 获取所有缓存字段解析 */
			Map<Field, AliasTo> fields = AT_CACHE.computeIfAbsent(this.getClass(), vcls -> {
				Map<Field, AliasTo> cache = new HashMap<>();
				/* 获取所有字段 */
				for (Field field : FieldUtil.getAllDeclaredField(this.getClass(), true).values()) {
					AliasTo aliasTo = field.getDeclaredAnnotation(AliasTo.class);
					if (aliasTo != null) {cache.put(field, aliasTo);}
				}
				return cache;
			});
			
			/* 待转换Dto */
			JSONObject dto = (JSONObject) JSON.toJSON(this.clone());
			for (Entry<Field, AliasTo> entry : fields.entrySet()) {
				Object val = entry.getKey().get(this);
				if (val == null) {continue;} //无数据跳过
				
				groups = groups.length == 0 ? new int[] {Default.VALUE} : groups;
				if (notGroup(groups, entry.getValue().groups())) {continue;} //无分组跳过
				val = parseDto(val, groups); //解析嵌套Dto
				
				/* 地址转换 */
				for (String to : entry.getValue().value()) {
					String path = StringUtils.hasText(to) ? to : entry.getKey().getName();
					JsonUtil.setValue(dto, path, val);
				}
			}
			
			/* 参数转换 */
			return dto.toJavaObject(dtoClass());
		} catch (Exception e) {throw new RuntimeException(e);}
	}
	/* 获取当前Dto类 */
	@SuppressWarnings("unchecked")
	private Class<D> dtoClass() {
		Class<?> curCls = this.getClass();
		while (curCls != Object.class) {
			if (curCls.getSuperclass() == BaseForm.class) {
				Type type = ((ParameterizedType) curCls.getGenericSuperclass()).getActualTypeArguments()[0];
				return type instanceof Class ? (Class<D>) type : (Class<D>) ((ParameterizedType) type).getRawType(); 
			} else {curCls = curCls.getSuperclass();}
		}
		throw new NullPointerException(String.format("当前条件类[%s]未继承[BaseForm]公共条件！", this.getClass().getName()));
	}
	/* 验证分组 */
	private boolean notGroup(int[] groups, int[] atGroups) {
		for (int i : groups) {
			for (int j : atGroups) {
				if (i == j) { //只取相同条件分组
					return false;
				}
			}
		}
		return true;
	}
	/* 解析嵌套Dto */
	@SuppressWarnings("unchecked")
	private Object parseDto(Object vo, int...groups) throws Exception {
		if (vo instanceof BaseForm) {return ((BaseForm<?>) vo).to(groups);}
		else if (vo instanceof Collection) {
			JSONArray dtos = new JSONArray();
			for (Object v : (Collection<Object>) vo) {
				if (v != null && v instanceof BaseForm) {dtos.add(((BaseForm<?>) v).to(groups));}
				else {dtos.add(v);} //添加原值
			}
			return dtos;
		} else if (vo.getClass().isArray()) {
			int len = Array.getLength(vo);
			Object dtos = Array.newInstance(BaseEntity.class, len);
			for (int i = 0; i < len; i++) {
				Object v = Array.get(vo, i);
				if (v != null && v instanceof BaseForm) {Array.set(dtos, i, ((BaseForm<?>) v).to(groups));}
				else {Array.set(dtos, i, v);} //重设原值
			}
			return dtos;
		} else {return vo;}
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2024-05-28 21:46:06
	 * @版本 1.0
	 * @parentClass BaseForm
	 * @描述 公共结果
	 */
	public static class BaseVo<D extends BaseEntity> implements JSONParser {
		
		/**
		 * @描述 转换成Vo对象
		 * @param dto 待转换入参（支持数据集与集合与数组）
		 * @return Vo对象
		 */
		@SuppressWarnings("unchecked")
		public <R> R from(Object dto) {
			try {
				/* 先解析参数类型 */
				if (dto == null) {return null;}
				Object result = typeParse(dto);
				if (result != null) {return (R) result;}
				
				/* 获取所有缓存字段解析 */
				Map<Field, String> fields = AF_CACHE.computeIfAbsent(this.getClass(), vcls -> {
					Map<Field, String> cache = new HashMap<>();
					/* 获取所有非常量字段 */
					for (Field field : FieldUtil.getAllDeclaredField(this.getClass(), false).values()) {
						AliasFrom aliasFrom = field.getDeclaredAnnotation(AliasFrom.class);
						if (aliasFrom != null) {cache.put(field, StringUtils.hasText(aliasFrom.value()) ? aliasFrom.value() : field.getName());}
					}
					return cache;
				});
				
				/* 待转换Dto - 调用toJSONString是为了触发注解 */
				dto = JSON.parse(JSON.toJSONString(dto));
				for (Entry<Field, String> entry : fields.entrySet()) {
					Object val = JSONPath.eval(dto, entry.getValue());
					if (val == null) {continue;} //无数据跳过
					
					val = parseVo(entry.getKey(), val); //解析嵌套Vo
					entry.getKey().set(this, parse(val, entry.getKey().getGenericType()));
				}
				
				/* 返回原值 */
				return (R) this;
			} catch (Exception e) {throw new RuntimeException(e);}
		}
		/* 类型解析 */
		@SuppressWarnings("unchecked")
		private Object typeParse(Object dto) throws Exception {
			/* 数据集解析 */
			if (dto instanceof DataList) {
				DataList<Object> dl = (DataList<Object>) dto;
				dl.setRows((List<Object>) from(dl.getRows()));
				return dl;
			}
			/* 集合解析 */
			if (dto instanceof Collection) {
				JSONArray vos = new JSONArray();
				for (Object d : (Collection<?>) dto) {vos.add(this.getClass().getConstructor().newInstance().from(d));}
				return vos;
			}
			/* 数组解析 */
			if (dto.getClass().isArray()) {
				int len = Array.getLength(dto);
				Object vos = Array.newInstance(this.getClass(), len);
				for (int i = 0; i < len; i++) {Array.set(vos, i, this.getClass().getConstructor().newInstance().from(Array.get(dto, i)));}
				return vos;
			}
			return null;
		}
		/* 解析嵌套Vo */
		@SuppressWarnings("unchecked")
		private Object parseVo(Field field, Object dto) throws Exception {
			if (BaseVo.class.isAssignableFrom(field.getType())) {
				return ((BaseVo<D>) field.getType().getConstructor().newInstance()).from(dto);
			} else if (Collection.class.isAssignableFrom(field.getType())) {
				Class<?> fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
				if (BaseVo.class.isAssignableFrom(fieldType)) {return ((BaseVo<D>) fieldType.getConstructor().newInstance()).from(dto);}
			} else if (field.getType().isArray()) {
				Class<?> fieldType = field.getType().getComponentType();
				if (BaseVo.class.isAssignableFrom(fieldType)) {return ((BaseVo<D>) fieldType.getConstructor().newInstance()).from(dto);}
			}
			return dto;
		}
	}
}
