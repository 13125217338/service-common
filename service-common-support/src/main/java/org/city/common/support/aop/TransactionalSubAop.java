package org.city.common.support.aop;

import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
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
import org.city.common.api.in.sql.TransactionalSuccess;
import org.city.common.api.in.sql.TransactionalThrowable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2023年6月20日
 * @版本 1.0
 * @描述 子事务拦截
 */
@Slf4j
@Aspect
@Component
@Order(Integer.MAX_VALUE)
public class TransactionalSubAop {
	final ThreadLocal<Boolean> IS_IN = new ThreadLocal<>();
	private final ThreadLocal<List<TransactionalObj>> TOS = new ThreadLocal<>();
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
		try {
			Method method = ((MethodSignature) jp.getSignature()).getMethod();
			if (notGlobalTransactional(method)) {return jp.proceed();} //非远程事务则直接执行原方法
			appendTOS(jp, method); //有远程事务 - 追加事务对象链路
			if (IS_IN.get() != null) {return jp.proceed();} //进入过事务则直接执行原方法
			
			/* 获取事务ID */
			String tranId = RemoteAdapter.REMOTE_TRANSACTIONAL.get();
			/* 子事务处理 */
			try {IS_IN.set(true); TOS.set(new ArrayList<>()); appendTOS(jp, method); return handler(tranId, method, jp);}
			catch (Throwable e) {handlerThrowable(tranId, e); throw e;} finally {TOS.remove(); IS_IN.remove();}
		} catch (Throwable e) {throw new UndeclaredThrowableException(e);}
	}
	/* 追加事务对象链路 */
	private void appendTOS(ProceedingJoinPoint jp, Method method) {
		List<TransactionalObj> tos = TOS.get(); //事务对象链路
		if (tos != null) {tos.add(new TransactionalObj(jp.getTarget(), method, jp.getArgs()));} //追加事务对象
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
		return handlerSuccess(tranId);
	}
	/* 处理成功 */
	private Object handlerSuccess(String tranId) {
		for (TransactionalObj to : TOS.get()) {
			try {
				if (to.getTarget() instanceof TransactionalSuccess) { //自定义处理成功逻辑
					((TransactionalSuccess) to.getTarget()).success(tranId, to.getMethod(), to.getArgs());
				}
			} catch (Throwable e2) {
				log.error("分布式事务-自定义处理成功逻辑失败》》》 " + to.getMethod().toGenericString(), e2);
			}
		}
		return null; //子线程返回什么都可以，对应返回值已经在TRAN_DATA里面
	}
	
	/* 判断方法和类有无分布式事务注解 - true=无，false=有 */
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
	/* 处理异常 */
	private void handlerThrowable(String tranId, Throwable e) throws Throwable {
		try {
			transactionalAop.TRAN_DATA.put(tranId, e);
			remoteTransactional.setState(tranId, false, e);
		} finally {
			for (TransactionalObj to : TOS.get()) {
				try {
					if (to.getTarget() instanceof TransactionalThrowable) { //自定义处理异常逻辑
						((TransactionalThrowable) to.getTarget()).throwable(tranId, to.getMethod(), to.getArgs(), e);
					}
				} catch (Throwable e2) {
					log.error("分布式事务-自定义处理异常逻辑失败》》》 " + to.getMethod().toGenericString(), e2);
				}
			}
		}
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2024-08-14 11:38:16
	 * @版本 1.0
	 * @parentClass TransactionalSubAop
	 * @描述 事务对象
	 */
	@Data
	private class TransactionalObj {
		/* 原对象 */
		private final Object target;
		/* 方法 */
		private final Method method;
		/* 参数 */
		private final Object[] args;
	}
}
