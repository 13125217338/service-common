package org.city.common.support.aop;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.exception.RemoteTransactionalException;
import org.city.common.api.in.Runnable;
import org.city.common.api.in.Task;
import org.city.common.api.in.sql.GlobalTransactionalSuccess;
import org.city.common.api.in.sql.GlobalTransactionalThrowable;
import org.city.common.api.in.util.ThrowableMessage;
import org.city.common.api.util.HeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @作者 ChengShi
 * @日期 2022年10月15日
 * @版本 1.0
 * @描述 主事务拦截
 */
@Slf4j
@Aspect
@Component
@Order(Byte.MIN_VALUE)
@EnableTransactionManagement(order = Short.MAX_VALUE)
public class TransactionalAop implements ThrowableMessage {
	final Map<String, Object> TRAN_DATA = new ConcurrentHashMap<>();
	@Autowired
	private RemoteConfigDto remoteConfigDto;
	@Autowired
	private RemoteIpPortDto localIpPort;
	@Autowired
	private Task task;
	@Autowired
	private TransactionalSubAop transactionalSubAop;
	/* 叠加唯一数 */
	private final AtomicInteger SUM = new AtomicInteger();
	/* 事务对象链路 */
	private final Map<String, List<TransactionalObj>> TOS = new ConcurrentHashMap<>();
	
	@Pointcut("@annotation(org.city.common.api.annotation.sql.GlobalTransactional) || @within(org.city.common.api.annotation.sql.GlobalTransactional)")
	private void transactionalCut() {}
	
	@Around("transactionalCut()")
	public Object transactionalAround(ProceedingJoinPoint jp) throws Throwable {
		String tranId = RemoteAdapter.REMOTE_TRANSACTIONAL.get(); //获取事务ID
		Method method = ((MethodSignature) jp.getSignature()).getMethod();
		if (notTransactional(method)) {return jp.proceed();} //非本地事务则直接执行原方法
		if (transactionalSubAop.IS_IN.get() != null) {return appendTOS(jp, method, jp.proceed(), tranId);} //进入过事务则直接执行原方法
		
		/* 不存在则生成事务ID */
		if (tranId == null) {tranId = CommonConstant.START_TIME + "-" + SUM.incrementAndGet() + "$" + localIpPort.toString();}
		else {TRAN_DATA.put(tranId, tranId);}
		
		/* 主事务处理 */
		try {TOS.put(tranId, new ArrayList<>()); return handlerSuccess(tranId, appendTOS(jp, method, hander(tranId, jp), tranId));}
		catch (Throwable e) {throw handlerThrowable(tranId, e);}
		finally {TOS.remove(tranId); TRAN_DATA.remove(tranId);}
	}
	
	/* 主事务处理 */
	private Object hander(String tranId, ProceedingJoinPoint jp) throws Throwable {
		/* 子线程执行并设置返回值 */
		final Map<String, String> headers = HeaderUtil.get();
		task.putTask(new Runnable() {
			@Override
			public void run() throws Throwable {
				/* 设置头信息与事务ID */
				try {HeaderUtil.set(headers); RemoteAdapter.REMOTE_TRANSACTIONAL.set(tranId); jp.proceed();}
				catch (Throwable e) {
					e = getRealExcept(e); //获取真实异常
					e = e instanceof RemoteTransactionalException ? e : new RemoteTransactionalException(e);
					log.error(((RemoteTransactionalException) e).parseMsg(tranId)); //打印特定消息
				} finally {
					RemoteAdapter.REMOTE_TRANSACTIONAL.remove(); HeaderUtil.remove();
					synchronized (tranId) {tranId.notifyAll();} //通知主线程运行返回值
				}
			}
		});
		
		/* 主线程同步等待返回值 - 增加一点等待时间防止子线程超时没有抛出异常 */
		synchronized (tranId) {tranId.wait(remoteConfigDto.getReadTimeout() + Short.MAX_VALUE);}
		/* 处理子线程设置的返回值 */
		Object returnVal = TRAN_DATA.get(tranId); returnVal = returnVal == TransactionalAop.class ? null : returnVal;
		if (returnVal instanceof Throwable) {throw (Throwable) returnVal;} else {return returnVal;}
	}
	/* 处理成功 */
	private Object handlerSuccess(String tranId, Object returnVal) {
		for (TransactionalObj to : TOS.get(tranId)) {
			try {
				if (to.target instanceof GlobalTransactionalSuccess) { //自定义处理成功逻辑
					((GlobalTransactionalSuccess) to.target).success(tranId, to.method, to.args, to.returnVal);
				}
			} catch (Throwable e2) {
				log.error("分布式事务-自定义处理成功逻辑失败》》》 " + to.method.toGenericString(), e2);
			}
		}
		return returnVal;
	}
	/* 处理异常 */
	private Throwable handlerThrowable(String tranId, Throwable e) throws Throwable {
		for (TransactionalObj to : TOS.get(tranId)) {
			try {
				if (to.target instanceof GlobalTransactionalThrowable) { //自定义处理异常逻辑
					((GlobalTransactionalThrowable) to.target).throwable(tranId, to.method, to.args, e);
				}
			} catch (Throwable e2) {
				log.error("分布式事务-自定义处理异常逻辑失败》》》 " + to.method.toGenericString(), e2);
			}
		}
		return e;
	}
	
	/* 判断方法和类有无事务注解 - true=无，false=有 */
	private boolean notTransactional(Method method) {
		return !method.isAnnotationPresent(Transactional.class) &&
			   !method.getDeclaringClass().isAnnotationPresent(Transactional.class);
	}
	/* 追加事务对象链路 */
	private Object appendTOS(ProceedingJoinPoint jp, Method method, Object returnVal, String tranId) {
		List<TransactionalObj> tranObjs = TOS.getOrDefault(tranId, new ArrayList<>()); //事务对象链路
		tranObjs.add(new TransactionalObj(jp.getTarget(), method, jp.getArgs(), returnVal)); //追加事务对象
		return returnVal;
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2024-08-14 11:38:16
	 * @版本 1.0
	 * @parentClass TransactionalAop
	 * @描述 事务对象
	 */
	@AllArgsConstructor
	private class TransactionalObj {
		/* 原对象 */
		private final Object target;
		/* 方法 */
		private final Method method;
		/* 参数 */
		private final Object[] args;
		/* 返回值 */
		private final Object returnVal;
	}
}
