package org.city.common.support.aop;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.annotation.sql.GlobalTransactional;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.exception.RemoteTransactionalException;
import org.city.common.api.in.sql.RemoteTransactional;
import org.city.common.api.in.sql.TransactionalThrowable;
import org.city.common.api.util.HeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2023年6月20日
 * @版本 1.0
 * @描述 子事务拦截
 */
@Slf4j
@Aspect
@Order(3)
@Component
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
		Method method = ((MethodSignature) jp.getSignature()).getMethod();
		if (notGlobalTransactional(method)) {return jp.proceed();} //非远程事务则直接执行原方法
		if (IS_IN.get() != null) {return jp.proceed();} //进入过事务则直接执行原方法
		
		/* 获取事务ID */
		String tranId = RemoteAdapter.REMOTE_TRANSACTIONAL.get();
		/* 子事务处理 */
		try {IS_IN.set(true); return handler(tranId, jp);}
		catch (Throwable e) {handlerThrowable(tranId, method, jp, e); throw e;} finally {IS_IN.remove();}
	}
	
	/* 子事务处理 */
	private Object handler(String tranId, ProceedingJoinPoint jp) throws Throwable {
		if (HeaderUtil.getHeaderVal(CommonConstant.REMOTE_TRANSACTIONAL_HEADER_NAME) == null) {remoteTransactional.add(tranId);}
		else {remoteTransactional.append(tranId);} //发起者添加事务 - 参与者追加事务
		long recordTime = System.currentTimeMillis(); //执行方法前记录时间
		
		Object jpReturnVal = jp.proceed(); //执行原方法并返回值
		transactionalAop.TRAN_DATA.put(tranId, jpReturnVal == null ? TransactionalAop.class : jpReturnVal);
		synchronized (tranId) {tranId.notifyAll();} //通知主线程运行返回值
		
		remoteTransactional.setState(tranId, true, null); //设置执行完成
		/* 循环判断直到返回成功 - 超时会抛出异常 */
		while (!remoteTransactional.verifyResult(tranId)) {
			if (isTimeout(tranId, recordTime)) {break;}
		}
		return null; //子线程返回什么都可以，对应返回值已经在TRAN_DATA里面
	}
	
	/* 判断方法和类有无分布式事务注解 - true=无，false=有 */
	private boolean notGlobalTransactional(Method method) {
		return !method.isAnnotationPresent(GlobalTransactional.class) &&
			   !method.getDeclaringClass().isAnnotationPresent(GlobalTransactional.class);
	}
	/* 判断是否超时 - true=虽然超时但是所有事务执行成功，false=未超时，超时且等待状态则抛出异常 */
	private boolean isTimeout(String tranId, long recordTime) throws Throwable {
		if ((System.currentTimeMillis() - recordTime) > remoteConfigDto.getReadTimeout()) {
			remoteTransactional.setState(tranId, false, new RemoteTransactionalException("事务执行等待超时！"));
			return true;
		}
		return false;
	}
	/* 处理异常 */
	private void handlerThrowable(String tranId, Method method, ProceedingJoinPoint jp, Throwable e) throws Throwable {
		try {
			transactionalAop.TRAN_DATA.put(tranId, e);
			remoteTransactional.setState(tranId, false, e);
		} finally {
			try {
				if (jp.getTarget() instanceof TransactionalThrowable) { //自定义处理异常逻辑
					((TransactionalThrowable) jp.getTarget()).throwable(tranId, method, jp.getArgs(), e);
				}
			} catch (Throwable e2) {
				log.error("分布式事务-自定义处理异常逻辑失败》》》 " + method.getDeclaringClass().getName(), e2);
			}
		}
	}
}
