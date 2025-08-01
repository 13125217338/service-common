package org.city.common.support.aop;

import java.lang.reflect.Method;
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
import org.city.common.api.in.util.ThrowableMessage;
import org.city.common.api.util.HeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

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
	
	@Pointcut("@annotation(org.city.common.api.annotation.sql.GlobalTransactional) || @within(org.city.common.api.annotation.sql.GlobalTransactional)")
	private void transactionalCut() {}
	
	@Around("transactionalCut()")
	public Object transactionalAround(ProceedingJoinPoint jp) throws Throwable {
		Method method = ((MethodSignature) jp.getSignature()).getMethod();
		if (notTransactional(method)) {return jp.proceed();} //非本地事务则直接执行原方法
		if (transactionalSubAop.IS_IN.get() != null) {return jp.proceed();} //进入过事务则直接执行原方法
		
		/* 获取事务ID */
		String tranId = RemoteAdapter.REMOTE_TRANSACTIONAL.get();
		if (tranId == null) {tranId = CommonConstant.START_TIME + "-" + SUM.incrementAndGet() + "$" + localIpPort.toString();}
		else {TRAN_DATA.put(tranId, tranId);}
		/* 主事务处理 */
		try {return hander(tranId, jp);} finally {TRAN_DATA.remove(tranId);}
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
	
	/* 判断方法和类有无事务注解 - true=无，false=有 */
	private boolean notTransactional(Method method) {
		return !method.isAnnotationPresent(Transactional.class) &&
			   !method.getDeclaringClass().isAnnotationPresent(Transactional.class);
	}
}
