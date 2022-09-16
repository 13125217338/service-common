package org.city.common.api.in;

import java.lang.reflect.Method;
import java.util.LinkedList;

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
	 * @param values 自定义参数
	 */
	public void invoke(Process process, String[] values) throws Throwable;
	/**
	 * @描述 返回执行的异常（方法所有操作实现者都会被调用）
	 * @param process 执行器
	 * @param throwClass 异常类
	 * @param values 自定义参数
	 */
	public void throwable(Process process, Throwable throwClass, String[] values);
	
	/**
	 * @描述 判断执行原方法并设置返回值
	 * @param process 执行器（执行条件 - 当没有执行过原方法，则先执行原方法并后设置返回值）
	 * @throws Throwable
	 */
	default void NotProcess(Process process) throws Throwable {
		if (!process.isExcute()) {
			process.setReturn(process.process());
		}
	}
	
	/**
	 * @描述 判断执行原方法并设置返回值
	 * @param process 执行器（执行条件 - 当没有设置过返回值，则先执行原方法并后设置返回值）
	 * @throws Throwable
	 */
	default void NotReturn(Process process) throws Throwable {
		if (!process.isSetReturn()) {
			process.setReturn(process.process());
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
		 * @描述 执行原方法（异常方法内执行时只会返回NULL）
		 * @return 方法返回值
		 * @throws Throwable
		 */
		public Object process() throws Throwable;
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
		 * @描述 手动设置替换返回值
		 * @param returnVal 待替换的返回值
		 */
		public void setReturn(Object returnVal);
		/**
		 * @描述 判断是否设置过返回值
		 * @return true设置过
		 */
		public boolean isSetReturn();
		/**
		 * @描述 获取执行过process方法的类
		 * @return 执行过process方法的类
		 */
		public LinkedList<Class<? extends MakeInvoke>> getMakes();
		/**
		 * @描述 判断是否执行过原方法
		 * @return true执行过
		 */
		default boolean isExcute() {return getMakes().size() > 0;}
		/**
		 * @描述 判断是否包含执行过的类
		 * @param invokes 待判断的类
		 * @return true代表包含执行过的类
		 */
		default boolean contains(Class<?>...invokes) {
			LinkedList<?> makes = getMakes();
			for (Class<?> makeInvoke : invokes) {
				if (makes.contains(makeInvoke)) {return true;}
			}
			return false;
		}
	}
}
