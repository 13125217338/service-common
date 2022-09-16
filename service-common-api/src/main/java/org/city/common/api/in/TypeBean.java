package org.city.common.api.in;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.util.CollectionUtils;

/**
 * @作者 ChengShi
 * @日期 2022-08-20 09:16:00
 * @版本 1.0
 * @描述 类型Bean对象
 */
public interface TypeBean {
	/**
	 * @描述 按实际实现类取Bean对象
	 * @param <T> 父类
	 * @param beans 原多个实现类
	 * @param bean 实际实现类
	 * @return 实际实现类对象
	 */
	default <T> T getBean(List<T> beans, Class<?> bean) {
		/* 只有一个取一个 */
		if (beans != null && beans.size() == 1) {return beans.get(0);}
		
		/* 如果没有则抛出异常 */
		if (CollectionUtils.isEmpty(beans)) {
			throw new NullPointerException(String.format("服务类[%s]未交给Spring管理！", bean.getName()));
		}
		/* 有多个取第一个 */
		return beans.stream().filter(v -> AopProxyUtils.ultimateTargetClass(v) == bean).collect(Collectors.toList()).get(0);
	}
}
