package org.city.common.support.aop;

import java.lang.reflect.Field;
import java.util.List;

import javax.annotation.PostConstruct;

import org.city.common.api.annotation.log.LogCallBackImpl;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.constant.group.Default;
import org.city.common.api.dto.LogDto;
import org.city.common.api.spi.LogCallBackProvider;
import org.city.common.api.util.PlugUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2022-06-26 12:10:07
 * @版本 1.0
 * @描述 自定义异常拦截（装饰原ConsoleAppender类）
 */
@Slf4j
@Component
@DependsOn(CommonConstant.PLUG_UTIL_NAME)
public class AppenderAop extends ConsoleAppender<ILoggingEvent> implements BeanPostProcessor{
	/*原打印名称 - 改为现打印名称*/
	private final String OLD_CONSOLE_NAME = "CONSOLE";
	@Value("${logging.system:true}")
	private boolean isSystem;
	@Value("${spring.application.name}")
	private String appName;
	
	/*初始化配置 - 使用顶级全局打印捕获*/
	@PostConstruct
	private void init() {
		try {
			Field field = log.getClass().getDeclaredField("parent");
			field.setAccessible(true);
			/*logback日志对象*/
			Logger parent = null, upLogger = (Logger) log;
			
			/*循环取root节点*/
			while((parent = (Logger) field.get(upLogger)) != null){
				/*对每级追加新控制台*/
				setNewConsoleAppender(upLogger);
				/*获取上一级*/
				field = parent.getClass().getDeclaredField("parent");
				field.setAccessible(true);
				upLogger = parent;
			}
			
			/*最后根目录*/
			setNewConsoleAppender(upLogger);
			/*打印提示*/
			System.out.println(String.format("已实现自定义日志打印》》》 [%s]:系统打印开关[%b]", this.getClass().getName(), isSystem));
		} catch (Exception e) {
			log.error("自定义日志打印失败！", e);
		}
	}
	/*设置新的控制台追加信息*/
	private void setNewConsoleAppender(Logger logger) {
		/*获取原字符追加者*/
		Appender<ILoggingEvent> oldConsoleAppender = logger.getAppender(OLD_CONSOLE_NAME);
		if (oldConsoleAppender != null) {
			/*去除原追加信息打印*/
			logger.detachAppender(oldConsoleAppender);
			
			/*使用原打印流初始化*/
			this.setEncoder(((ConsoleAppender<ILoggingEvent>) oldConsoleAppender).getEncoder());
			this.start();
			/*注册新装饰追加打印*/
			logger.addAppender(this);
			/*新装饰使用原名称*/
			this.setName(OLD_CONSOLE_NAME);
		}
	}
	
	/*不要再此调用log打印，不然会死递归栈溢出*/
	@Override
	protected void append(ILoggingEvent eventObject) {
		/*获取所有实现注解值*/
		List<LogCallBackImpl> allValue = PlugUtil.getAllValue(LogCallBackProvider.class, LogCallBackImpl.class);
		for (LogCallBackImpl value : allValue) {
			/* 按分组类型匹配 */
			for (Class<?> group : value.groups()) {
				try {
					String throwMsg = null;
					Class<?> throwCls = null, logger = Class.forName(eventObject.getLoggerName());
					/* 如果有错误类 */
					if (eventObject.getThrowableProxy() != null) {
						Throwable throwable = ((ThrowableProxy) eventObject.getThrowableProxy()).getThrowable();
						throwCls = throwable.getClass(); throwMsg = throwable.getMessage();
					}
					
					/* 如果类一致 */
					if (group == Default.class || group.isAssignableFrom(logger) || (throwCls != null && group.isAssignableFrom(throwCls))) {
						LogCallBackProvider logCallBackProvider = PlugUtil.getBean(value.id(), LogCallBackProvider.class);
						/* 调用提供实现者 */
						logCallBackProvider.message(new LogDto(eventObject.getMessage(), throwMsg, new String(this.encoder.encode(eventObject)),
								eventObject.getLevel().levelInt, logger.getName(), throwCls == null ? null : throwCls.getName(), appName));
						/* 如果调用了则不在继续调用 */
						break;
					}
				/* 不处理类找不到异常 */
				} catch (ClassNotFoundException e) {} 
				catch (Throwable e) {e.printStackTrace();}
			}
		}
		/*调用原打印方法 - 打印在控制台*/
		if(isSystem) {super.append(eventObject);}
	}
}
