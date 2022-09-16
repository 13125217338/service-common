package org.city.common.core.scanner;

import java.io.IOException;
import java.util.Map;

import org.city.common.api.annotation.plug.Remote;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.classreading.MetadataReader;

/**
 * @作者 ChengShi
 * @日期 2022-10-07 10:27:52
 * @版本 1.0
 * @描述 自定义远程注解扫描
 */
public class RemoteScanner extends ClassPathBeanDefinitionScanner{
	private final Map<Class<?>, Remote> scanClass;
	public RemoteScanner(BeanDefinitionRegistry registry, Map<Class<?>, Remote> scanClass) {
		super(registry);
		this.scanClass = scanClass;
	}
	
	@Override
	protected boolean isCandidateComponent(MetadataReader metadataReader) throws IOException {
		/* 只扫描接口且注解有远程标志 */
		if (metadataReader.getClassMetadata().isInterface() && metadataReader.getAnnotationMetadata().isAnnotated(Remote.class.getName())) {
			try {
				Class<?> forName = Class.forName(metadataReader.getClassMetadata().getClassName());
				scanClass.put(forName, forName.getDeclaredAnnotation(Remote.class));
			} catch (ClassNotFoundException e) {e.printStackTrace();}
		}
		return false;
	}
}
