package org.city.common.api.in;

/**
 * @作者 ChengShi
 * @日期 2022-08-21 12:25:39
 * @版本 1.0
 * @描述 代理类型
 */
public interface ProxyType {
	/** CgLib标识 */
	public final static String CGLIB_FLAG = "$$EnhancerBySpringCGLIB$$";
	
	/**
	 * @描述 获取原类（只兼容CgLib类型）
	 * @param candidate 待获取类
	 * @return 原类
	 */
	default Class<?> getType(Class<?> candidate) {
		if (isCgLib(candidate)) {
			try {return Class.forName(candidate.getName().substring(0, candidate.getName().indexOf(CGLIB_FLAG)));}
			catch (Exception e) {return null;}
		} else {return candidate;}
	}
	
	/**
	 * @描述 是否是CgLib代理类
	 * @param candidate 判断类型
	 * @return true=是CgLib代理类
	 */
	default boolean isCgLib(Class<?> candidate) {
		return candidate == null ? false : candidate.getName().contains(CGLIB_FLAG);
	}
}
