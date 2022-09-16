package org.city.common.core.config;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.city.common.core.handler.RemoteTransactionalHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * @作者 ChengShi
 * @日期 2022-12-02 09:25:09
 * @版本 1.0
 * @描述 Mybatis配置（针对@Repository注解进行分布式事务处理）
 */
@Aspect
@Component
@Intercepts({
	@Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
	@Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
})
public class MybatisConfig implements Interceptor{
	@Autowired
	private RemoteTransactionalHandler remoteTransactionalHandler;
	/** 返回值类型记录 */
	private final ThreadLocal<Class<?>> RETURN_TYPE = new ThreadLocal<>();
	
	@Pointcut("@within(org.springframework.stereotype.Repository)")
	private void authCut() {}
	
	@Before("authCut()")
	public void authBefore(JoinPoint jp) {
		Method method = ((MethodSignature) jp.getSignature()).getMethod();
		Class<?> rType = method.getReturnType();
		
		/* 针对特殊泛型处理 */
		if (List.class.isAssignableFrom(rType)) {
			Type type = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
			Assert.isTrue(type instanceof Class, "集合中泛型非Class类型！泛型的类型：" + type);
			rType = (Class<?>) type;
		} else if(Map.class.isAssignableFrom(rType)) {rType = JSONObject.class;}
		/* 记录返回类型 */
		RETURN_TYPE.set(rType);
	}
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		StatementHandler handler = (StatementHandler) invocation.getTarget();
		/* 参数 */
		List<Object> args = getArgs(handler.getBoundSql().getParameterObject());
		String sql = handler.getBoundSql().getSql();
		
		/* 分布式事务执行 */
		Object result = handlerQuery(sql, args, invocation.getMethod());
		if (result == null) {result = handlerUpdate(sql, args);}
		if (result == null) {return invocation.proceed();} else {return result;}
	}
	
	@SuppressWarnings("unchecked")
	private List<Object> getArgs(Object param) {
		if (param == null) {return null;}
		List<Object> datas = new ArrayList<>();
		
		/* 提取其中的参数 */
		Map<String, Object> data =  param instanceof Map ? (Map<String, Object>) param : (Map<String, Object>) JSON.toJSON(param);
		for (int i = 0, name = 1, j = data.size(); i < j; i += 2, name++) {
			datas.add(data.get("param" + name));
		}
		return datas;
	}

	/* 处理查询 */
	private Object handlerQuery(String sql, List<Object> args, Method method) throws Throwable {
		int indexOf = sql.indexOf("select");
		if (indexOf == -1) {return null;}
		
		if (List.class.isAssignableFrom(method.getReturnType())) {
			return remoteTransactionalHandler.remoteExecQS(sql, args, "yyyy-MM-dd HH:mm:ss", RETURN_TYPE.get());
		} else {
			return remoteTransactionalHandler.remoteExecQ(sql, args, "yyyy-MM-dd HH:mm:ss", RETURN_TYPE.get());
		}
	}
	/* 处理更新 */
	private Integer handlerUpdate(String sql, List<Object> args) throws Throwable {
		int indexOf = sql.indexOf("insert");
		if (indexOf == -1) {indexOf = sql.indexOf("update");}
		if (indexOf == -1) {indexOf = sql.indexOf("delete");}
		if (indexOf == -1) {return null;}
		
		return remoteTransactionalHandler.remoteExecU(sql, args);
	}
}
