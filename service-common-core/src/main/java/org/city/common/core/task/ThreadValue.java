package org.city.common.core.task;

import java.util.HashMap;
import java.util.Map;

/**
 * @作者 ChengShi
 * @日期 2021-10-15 16:31:56
 * @版本 1.0
 * @描述 线程共享值
 */
public final class ThreadValue {
	private final Map<Thread, Map<Class<?>, Object>> value = new HashMap<>();
	/**
	 * @描述 设置一个值
	 * @param val 值
	 */
	public void set(Object val){
		if (val == null) {return;}
		Map<Class<?>, Object> map = value.get(Thread.currentThread());
		if (map == null) {map = new HashMap<>();}
		map.put(val.getClass(), val);
		value.put(Thread.currentThread(), map);
	}
	
	/**
	 * @描述 移除对应类型值
	 * @param val
	 */
	public <T> void remove(Class<T> type){
		Map<Class<?>, Object> map = value.get(Thread.currentThread());
		if (map != null) {map.remove(type);}
	}
	
	/**
	 * @描述 通过添加的类型获取添加的值
	 * @param type 想要获取对应类型的值
	 * @return 对应类型的值
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> type){
		T val = null;
		Map<Class<?>, Object> map = value.get(Thread.currentThread());
		if (map != null) {val = (T) map.get(type);}
		return val;
	}
	
	/**
	 * @描述 将目标线程值拷贝至指定线程
	 * @param val 待拷贝值
	 * @param to 拷贝至当前线程
	 */
	public void copy(Map<Class<?>, Object> val, Thread to){
		if (val != null) {
			Map<Class<?>, Object> tv = value.get(to);
			if (tv == null) {value.put(to, val);}
			else{tv.putAll(val);}
		}
	}
	
	/**
	 * @描述 获取当前线程所有值
	 * @return 当前线程所有值
	 */
	public Map<Class<?>, Object> getAll(){
		return value.get(Thread.currentThread());
	}
	
	/**
	 * @描述 获取其他线程所有值
	 * @param source 待获取线程
	 * @return 其他线程所有值
	 */
	public Map<Class<?>, Object> getAll(Thread source){
		return value.get(source);
	}
	
	/**
	 * @描述 移除其他线程所有值
	 * @param source 待移除线程
	 */
	public void removeAll(Thread source){
		value.remove(source);
	}
}
