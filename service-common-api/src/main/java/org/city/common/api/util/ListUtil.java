package org.city.common.api.util;

import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.city.common.api.dto.Condition.Param;
import org.springframework.util.CollectionUtils;

/**
 * @作者 ChengShi
 * @日期 2020-08-03 16:02:33
 * @版本 1.0
 * @描述 操作内存中的集合对象
 */
public final class ListUtil {
	private ListUtil() {}
	
	/**
	* @用途 手动提取集合第几页范围数据，实现分页
	* @param lstData 需要分页的集合对象
	* @param pageNo 页码，从1开始
	* @param pageSize 分页大小
	* @return 分页后的数据集合
	*/
	public static <T> List<T> getPageListData(List<T> lstData, Integer pageNo, Integer pageSize){
		if (!MyUtil.isNotBlank(lstData) || pageNo == null || pageSize == null) {return lstData;}
		int curPage = pageNo - 1;
		int total = lstData.size();
		/*计算开始下标*/
		int startIndex = curPage * pageSize;
		if (startIndex > total) {startIndex = total;}
		/*计算总迭代数*/
		int endIndex = startIndex + pageSize;
		if (endIndex > total) {endIndex = total;}
		/*迭代添加*/
		return new ArrayList<T>(lstData.subList(startIndex, endIndex));
	}
	
	/**
	 * @描述 内存查询
	 * @param <T> 与入参类型一致
	 * @param datas 返回查询的数据
	 * @param param 查询条件
	 * @return 查询结果
	 */
	public static <T> List<T> getQueryData(List<T> datas, Param param) {
		if (CollectionUtils.isEmpty(datas)) {return datas;}
		
		/* 所有字段 */
		List<Field> collect = FieldUtil.getAllDeclaredField(datas.get(0).getClass()).stream()
				.filter(v -> v.getName().equals(param.getName())).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(collect)) {throw new NullPointerException(String.format("请检查条件[%s]字段名是否在类中存在！", param.getName()));}
		List<T> result = new ArrayList<>();
		
		/* 按条件执行 */
		switch (param.getOperation()) {
			case Equals: getEquals(result, datas, collect.get(0), param.getValue(), true); break;
			case Not_Equals: getEquals(result, datas, collect.get(0), param.getValue(), false); break;
			case Like: getLike(result, datas, collect.get(0), param.getValue(), true); break; //必须是字符串类型才判断
			case Not_Like: getLike(result, datas, collect.get(0), param.getValue(), false); break; //必须是字符串类型才判断
			case Greater: getGLE(result, datas, collect.get(0), param.getValue(), false, true); break;
			case Greater_Equals: getGLE(result, datas, collect.get(0), param.getValue(), true, true); break;
			case Less: getGLE(result, datas, collect.get(0), param.getValue(), false, false); break;
			case Less_Equals: getGLE(result, datas, collect.get(0), param.getValue(), true, false); break;
			case In: getIn(result, datas, collect.get(0), param.getValue(), true); break; //必须是集合类型才判断
			case Not_In: getIn(result, datas, collect.get(0), param.getValue(), false); break; //必须是集合类型才判断
			case Is_Null: getNull(result, datas, collect.get(0), true); break; 
			case Is_Not_Null: getNull(result, datas, collect.get(0), false); break; 
			default: throw new NullPointerException("未匹配到任何操作条件！");
		}
		return result;
	}
	/* 空判断 */
	private static <T> void getNull(List<T> result, List<T> datas, Field field, boolean isNull) {
		for (T t : datas) {
			if (t != null) {
				boolean isAdd = false;
				/* 判断结果 */
				if (getVal(field, t) == null) {isAdd = isNull;} else {isAdd = !isNull;}
				if (isAdd) {result.add(t);}
			}
		}
	}
	/* 包含判断 */
	private static <T> void getIn(List<T> result, List<T> datas, Field field, Object value, boolean isIn) {
		for (T t : datas) {
			if (t != null) {
				boolean isAdd = false;
				Object val = getVal(field, t);
				if (value instanceof Collection) {
					/* 判断结果 */
					if (((Collection<?>) value).contains(val)) {isAdd = isIn;} else {isAdd = !isIn;}
					if (isAdd) {result.add(t);}
				}
			}
		}
	}
	/* 大于小于等于判断 */
	private static <T> void getGLE(List<T> result, List<T> datas, Field field, Object value, boolean isEqual, boolean isGreater) {
		for (T t : datas) {
			if (t != null) {
				boolean isAdd = false;
				Object val = getVal(field, t);
				int compare = compare(val, value);
				/* 判断结果 */
				if (compare == 0) {isAdd = isEqual;}
				else if(compare > 0) {isAdd = isGreater;} 
				else {isAdd = !isGreater;}
				if (isAdd) {result.add(t);}
			}
		}
	}
	/* 模糊判断 */
	private static <T> void getLike(List<T> result, List<T> datas, Field field, Object value, boolean isLike) {
		for (T t : datas) {
			if (t != null) {
				boolean isAdd = false;
				Object val = getVal(field, t);
				if (val instanceof String && value instanceof String) {
					/* 判断结果 */
					if (val == value) {isAdd = isLike;}
					else if(val != null && ((String) val).contains((String) value)) {isAdd = isLike;}
					else {isAdd = !isLike;}
					if (isAdd) {result.add(t);}
				}
			}
		}
	}
	/* 相同判断 */
	private static <T> void getEquals(List<T> result, List<T> datas, Field field, Object value, boolean isEqual) {
		for (T t : datas) {
			if (t != null) {
				boolean isAdd = false;
				Object val = getVal(field, t);
				/* 判断结果 */
				if (val == value) {isAdd = isEqual;}
				else if(val != null && val.equals(value)) {isAdd = isEqual;}
				else {isAdd = !isEqual;}
				if (isAdd) {result.add(t);}
			}
		}
	}
	
	/*对比值*/
	private static int compare(Object o1, Object o2){
		NumberFormat format = NumberFormat.getInstance();
		try {o1 = format.format(o1);} catch (Exception e) {}
		try {o2 = format.format(o2);} catch (Exception e) {}
		/* 数值对比 */
		if (o1 instanceof Number && o2 instanceof Number) {
			if (o1.getClass() == Long.class || o1.getClass() == long.class) {
				return ((Long)((Number)o1).longValue()).compareTo(((Number)o2).longValue());
			}
			return ((Double)((Number)o1).doubleValue()).compareTo(((Number)o2).doubleValue());
		}
		/*对象对比*/
		if (o1 == null) {return o2 == null ? 0 : -1;}
		else{
			if (o2 == null) {return 1;}
			else {return o1.toString().compareTo(o2.toString());}
		}
	}
	/* 反射获取值 */
	private static Object getVal(Field field, Object data) {
		try {return field.get(data);}
		catch (Exception e) {throw new RuntimeException(e);}
	}
}
