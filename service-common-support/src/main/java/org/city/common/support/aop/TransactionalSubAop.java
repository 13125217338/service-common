package org.city.common.support.aop;

import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.TimeoutException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.annotation.sql.GlobalTransactional;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.in.sql.RemoteTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2023年6月20日
 * @版本 1.0
 * @描述 子事务拦截
 */
@Aspect
@Component
@Order(Integer.MAX_VALUE)
public class TransactionalSubAop {
	final ThreadLocal<Boolean> IS_IN = new ThreadLocal<>();
	@Autowired
	private RemoteTransactional remoteTransactional;
	@Autowired
	private RemoteConfigDto remoteConfigDto;
	@Autowired
	private TransactionalAop transactionalAop;
	
	@Pointcut("@annotation(org.springframework.transaction.annotation.Transactional) || @within(org.springframework.transaction.annotation.Transactional)")
	private void transactionalCut() {}
	
	@Around("transactionalCut()")
	public Object transactionalAround(ProceedingJoinPoint jp) throws Throwable {
		String tranId = RemoteAdapter.REMOTE_TRANSACTIONAL.get(); //获取事务ID
		Method method = ((MethodSignature) jp.getSignature()).getMethod();
		if (notGlobalTransactional(method)) {return jp.proceed();} //非远程事务则直接执行原方法
		if (IS_IN.get() != null) {return jp.proceed();} //进入过事务则直接执行原方法
		
		/* 子事务处理 */
		try {IS_IN.set(true); return handler(tranId, method, jp);}
		catch (Throwable e) {throw handlerThrowable(tranId, e);}
		finally {IS_IN.remove();}
	}
	
	/* 子事务处理 */
	private Object handler(String tranId, Method method, ProceedingJoinPoint jp) throws Throwable {
		if (transactionalAop.TRAN_DATA.get(tranId) == null) {remoteTransactional.add(tranId);}
		else {remoteTransactional.append(tranId);} //发起者添加事务 - 参与者追加事务
		long recordTime = System.currentTimeMillis(); //执行方法前记录时间
		
		Object jpReturnVal = jp.proceed(); //执行原方法并返回值
		transactionalAop.TRAN_DATA.put(tranId, jpReturnVal == null ? TransactionalAop.class : jpReturnVal);
		synchronized (tranId) {tranId.notifyAll();} //通知主线程运行返回值
		
		remoteTransactional.setState(tranId, true, null); //设置执行完成
		/* 循环判断直到返回成功 - 超时会抛出异常 */
		while (!remoteTransactional.verifyResult(tranId)) {verifyTimeout(tranId, recordTime);}
		/* 子线程返回什么都可以，对应返回值已经在TRAN_DATA里面 */
		return null;
	}
	/* 处理异常 */
	private Throwable handlerThrowable(String tranId, Throwable e) throws Throwable {
		transactionalAop.TRAN_DATA.put(tranId, e);
		remoteTransactional.setState(tranId, false, e);
		return new UndeclaredThrowableException(e);
	}
	
	/* 判断方法和类有无全局分布式事务注解 - true=无，false=有 */
	private boolean notGlobalTransactional(Method method) {
		return !method.isAnnotationPresent(GlobalTransactional.class) &&
			   !method.getDeclaringClass().isAnnotationPresent(GlobalTransactional.class);
	}
	/* 判断是否超时 - 超时且等待状态则设置抛出异常 */
	private void verifyTimeout(String tranId, long recordTime) throws Throwable {
		if ((System.currentTimeMillis() - recordTime) > remoteConfigDto.getReadTimeout()) {
			remoteTransactional.setState(tranId, false, new TimeoutException("事务执行等待超时！"));
		} else {try {Thread.sleep((long) (Math.random() * 80 + 20));} catch (Throwable e) {}} //等待20-100毫秒防止过快
	}
}
