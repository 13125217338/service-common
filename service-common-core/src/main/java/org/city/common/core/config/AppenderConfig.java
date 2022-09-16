package org.city.common.core.config;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.stream.Collectors;

import org.city.common.api.annotation.log.LogFilter;
import org.city.common.api.constant.group.Default;
import org.city.common.api.dto.LogDto;
import org.city.common.api.in.remote.RemoteSave.RemoteInfo;
import org.city.common.api.in.util.ThrowableMessage;
import org.city.common.api.spi.LogCallBackProvider;
import org.city.common.api.util.PlugUtil;
import org.city.common.api.util.SpringUtil;
import org.springframework.beans.factory.annotation.Value;

import ch.qos.logback.classic.Level;
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
 * @描述 自定义日志拦截（装饰原ConsoleAppender类）
 */
@Slf4j
public class AppenderConfig extends ConsoleAppender<ILoggingEvent> implements ThrowableMessage {
	/* 原打印名称 - 改为现打印名称 */
	private final String OLD_CONSOLE_NAME = "CONSOLE";
	@Value("${logging.system:true}")
	private boolean isSystem = true;
	
	/**
	 * @描述 初始化配置 - 使用顶级全局打印捕获
	 */
	protected void init() {
		try {
			Field field = log.getClass().getDeclaredField("parent");
			field.setAccessible(true);
			/* logback日志对象 */
			Logger parent = null, upLogger = (Logger) log;
			
			/* 循环取root节点 */
			while((parent = (Logger) field.get(upLogger)) != null){
				/* 对每级追加新控制台 */
				setNewConsoleAppender(upLogger);
				/* 获取上一级 */
				field = parent.getClass().getDeclaredField("parent");
				field.setAccessible(true);
				upLogger = parent;
			}
			
			/* 最后根目录 */
			setNewConsoleAppender(upLogger);
			/* 打印提示 */
			System.out.println(String.format("<------------ 已实现自定义日志打印》》》 [%s]:系统打印开关[%b] ------------>", this.getClass().getName(), isSystem));
		} catch (Exception e) {
			log.error("自定义日志打印失败！", e);
		}
	}
	/* 设置新的控制台追加信息 */
	private void setNewConsoleAppender(Logger logger) {
		/* 获取原字符追加者 */
		Appender<ILoggingEvent> oldConsoleAppender = logger.getAppender(OLD_CONSOLE_NAME);
		if (oldConsoleAppender != null) {
			/* 去除原追加信息打印 */
			logger.detachAppender(oldConsoleAppender);
			
			/* 使用原打印流初始化 */
			this.setEncoder(((ConsoleAppender<ILoggingEvent>) oldConsoleAppender).getEncoder());
			this.start();
			/* 注册新装饰追加打印 */
			logger.addAppender(this);
			/* 新装饰使用原名称 */
			this.setName(OLD_CONSOLE_NAME);
		}
	}
	
	/* 不要再此调用log打印，不然会死递归栈溢出 */
	@Override
	protected void append(ILoggingEvent eventObject) {
		try {
			/* 实现的类不能一致 */
			Collection<RemoteInfo> remoteInfos = PlugUtil.getRemotes(LogCallBackProvider.class).stream().collect(
					Collectors.toMap(k -> k.getRemoteClassDto().getName(), v -> v, (k1, k2) -> k1)).values();
			/* 获取所有实现注解值 */
			for (RemoteInfo remote : remoteInfos) {
				/* 必须是日志回调实例 */
				if (!(remote.getBean() instanceof LogCallBackProvider)) {continue;}
				LogCallBackProvider provider = (LogCallBackProvider) remote.getBean();
				if (provider == null) {System.err.println(String.format("日志回调实现类[%s]未有实例对象！", remote.getRemoteClassDto().getName())); continue;}
				LogFilter logFilter = remote.getRemoteClassDto().getAnnotation(LogFilter.class);
				if (logFilter == null) {System.err.println(String.format("日志回调实现类[%s]未有@LogFilter注解！", remote.getRemoteClassDto().getName())); continue;}
				
				/* 日志类与错误信息 */
				String throwMsg = null; Class<?> throwCls = null, logger = null;
				/* 不处理类找不到异常 */
				try {logger = Class.forName(eventObject.getLoggerName());} catch (ClassNotFoundException e) {continue;} 
				/* 如果有错误类 */
				if (eventObject.getThrowableProxy() != null) {
					Throwable throwable = getRealExcept(((ThrowableProxy) eventObject.getThrowableProxy()).getThrowable());
					throwCls = throwable.getClass(); throwMsg = throwable.getMessage();
				}
				
				/* 日志等级 */
				int logLevel = eventObject.getLevel().levelInt;
				try {
					/* 等级判断 */
					if (logFilter.fixVal() == Level.ALL_INT && logFilter.value() == Level.ALL_INT) {
						/* 默认执行 */
						invoke(eventObject, logFilter, provider, throwMsg, throwCls, logger, logLevel);
					} else if (logFilter.fixVal() == logLevel || logFilter.value() >= logLevel) {
						/* 调用提供实现者 */
						invoke(eventObject, logFilter, provider, throwMsg, throwCls, logger, logLevel);
					}
				} catch (Throwable e) {/* 不处理 */}
			}
		} catch (Throwable e) {e.printStackTrace();}
		/* 调用原打印方法 - 打印在控制台 */
		if(isSystem) {super.append(eventObject);}
	}
	
	/* 执行远程调用 */
	private void invoke(ILoggingEvent eventObject, LogFilter logFilter, LogCallBackProvider provider, String throwMsg, Class<?> throwCls, Class<?> logger, int logLevel) {
		/* 按分组类型匹配 */
		for (Class<?> group : logFilter.groups()) {
			/* 如果类一致 */
			if (group == Default.class || group.isAssignableFrom(logger) || (throwCls != null && group.isAssignableFrom(throwCls))) {
				/* 调用提供实现者 */
				provider.message(new LogDto(eventObject.getMessage(), throwMsg, new String(this.encoder.encode(eventObject)),
						logLevel, logger.getName(), throwCls == null ? null : throwCls.getName(), SpringUtil.getAppName()));
				/* 如果调用了则不在继续调用 */
				break;
			}
		}
	}
}
