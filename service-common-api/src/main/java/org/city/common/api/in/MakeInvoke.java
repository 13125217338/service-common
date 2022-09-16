package org.city.common.api.in;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.city.common.api.dto.MakeDto;

/**
 * @作者 ChengShi
 * @日期 2022-07-28 13:45:27
 * @版本 1.0
 * @描述 在当前服务器操作
 */
public interface MakeInvoke {
	/**
	 * @描述 自定义操作执行
	 * @param process 执行器
	 * @param value 自定义参数（通常用作标记）
	 * @param values 已被替换的自定义参数
	 */
	public void invoke(Process process, int value, String[] values) throws Throwable;
	/**
	 * @描述 执行后的异常处理（该方法所有操作实现者一定会被调用）
	 * @param process 执行器
	 * @param throwable 异常信息
	 * @param value 自定义参数（通常用作标记）
	 * @param values 已被替换的自定义参数
	 */
	default void throwable(Process process, Throwable throwable, int value, String[] values) throws Throwable {}
	
	/**
	 * @描述 判断执行原方法并设置返回值
	 * @param process 执行器（执行条件 - 当没有执行过原方法，则先执行原方法并后设置返回值）
	 * @throws Throwable
	 */
	default void NotProcess(Process process) throws Throwable {
		NotProcess(process, null);
	}
	/**
	 * @描述 判断执行原方法并设置返回值
	 * @param process 执行器（执行条件 - 当没有执行过原方法，则先执行原方法并后设置返回值）
	 * @param args 原方法入参
	 * @throws Throwable
	 */
	default void NotProcess(Process process, Object[] args) throws Throwable {
		if (!process.isExcute()) {
			process.setReturn(process.process(args));
		}
	}
	
	/**
	 * @描述 判断执行原方法并设置返回值
	 * @param process 执行器（执行条件 - 当没有设置过返回值，则先执行原方法并后设置返回值）
	 * @throws Throwable
	 */
	default void NotReturn(Process process) throws Throwable {
		NotReturn(process, null);
	}
	/**
	 * @描述 判断执行原方法并设置返回值
	 * @param process 执行器（执行条件 - 当没有设置过返回值，则先执行原方法并后设置返回值）
	 * @param args 原方法入参
	 * @throws Throwable
	 */
	default void NotReturn(Process process, Object[] args) throws Throwable {
		if (!process.isSetReturn()) {
			process.setReturn(process.process(args));
		}
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2022年7月29日
	 * @版本 1.0
	 * @描述 执行器
	 */
	public interface Process {
		/**
		 * @描述 执行原方法（在异常方法内执行时只会返回NULL，不会执行原方法）
		 * @return 方法返回值
		 * @throws Throwable
		 */
		public Object process() throws Throwable;
		/**
		 * @描述 执行原方法（在异常方法内执行时只会返回NULL，不会执行原方法）
		 * @param args 原方法入参
		 * @return 方法返回值
		 * @throws Throwable
		 */
		public Object process(Object[] args) throws Throwable;
		/**
		 * @描述 手动执行下一个操作
		 * @return true=可以继续执行下一个操作
		 * @throws Throwable
		 */
		public boolean nextInvoke() throws Throwable;
		/**
		 * @描述 添加原方法结束时回调
		 * @param after 原方法结束时回调实现的逻辑
		 */
		public void addAfter(Runnable after);
		/**
		 * @描述 获取当前对象
		 * @return 当前对象
		 */
		public Object getTarget();
		/**
		 * @描述 获取当前方法
		 * @return 当前方法
		 */
		public Method getMethod();
		/**
		 * @描述 获取当前方法参数
		 * @return 当前方法参数
		 */
		public Object[] getArgs();
		/**
		 * @描述 获取方法返回值
		 * @return 方法返回值
		 */
		public Object getReturn();
		/**
		 * @描述 手动设置返回值
		 * @param returnVal 待设置的返回值
		 */
		public void setReturn(Object returnVal);
		/**
		 * @描述 判断是否设置过返回值
		 * @return true=设置过
		 */
		public boolean isSetReturn();
		/**
		 * @描述 判断是否执行过原方法
		 * @return true=执行过
		 */
		public boolean isExcute();
		/**
		 * @描述 提前结束链路执行
		 */
		public void end();
		/**
		 * @描述 获取执行过的对象
		 * @return key=执行参数，value=执行对象
		 */
		public LinkedHashMap<MakeDto, org.city.common.api.in.MakeInvoke> getMakes();
		/**
		 * @描述 获取所有执行的对象（不管执没执行过）
		 * @return key=执行参数，value=执行对象
		 */
		public Map<MakeDto, org.city.common.api.in.MakeInvoke> getAllMakes();
		/**
		 * @描述 设置自定义参数
		 * @param key 键
		 * @param value 值
		 */
		public void setParam(String key, Object value);
		/**
		 * @描述 获取自定义参数
		 * @param <T> 值类型
		 * @param key 键
		 * @param type 待转换类型
		 * @return 值对象
		 */
		public <T> T getParam(String key, Type type);
		/**
		 * @描述 判断是否包含执行类
		 * @param isExec true=查询执行过的类，false=查询所有执行的类（不管执没执行过）
		 * @param invokes 待判断的类
		 * @return 与invokes顺序一致的结果，true=执行过
		 */
		default boolean[] contains(boolean isExec, Class<?>...invokes) {
			Set<?> makes = (isExec ? getMakes() : getAllMakes()).keySet().stream().map(v -> v.getClass()).collect(Collectors.toSet());
			boolean[] isExecs = new boolean[invokes.length];
			for (int i = 0; i < invokes.length; i++) {
				isExecs[i] = makes.contains(invokes[i]);
			}
			return isExecs;
		}
	}
}
