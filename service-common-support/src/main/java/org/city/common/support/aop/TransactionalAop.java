package org.city.common.support.aop;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.annotation.sql.GlobalTransactional;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.in.sql.RemoteTransactional;
import org.city.common.api.util.HeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @作者 ChengShi
 * @日期 2022年10月15日
 * @版本 1.0
 * @描述 事务
 */
@Aspect
@Order(1)
@Component
@DependsOn(CommonConstant.PLUG_UTIL_NAME)
@EnableTransactionManagement(order = 2)
public class TransactionalAop {
	@Autowired
	private RemoteIpPortDto localIpPort;
	@Autowired
	private RemoteTransactional remoteTransactional;
	/* 唯一事务ID计数 */
	private final AtomicLong SUM = new AtomicLong();
	
	@Pointcut("@annotation(org.springframework.transaction.annotation.Transactional) || @within(org.springframework.transaction.annotation.Transactional)")
	private void transactionalCut() {}
	
	@Around("transactionalCut()")
	public Object transactionalAround(ProceedingJoinPoint jp) throws Throwable {
		if (RemoteAdapter.REMOTE_TRANSACTIONAL.get() != null) {return jp.proceed();}
		
		/* 验证是否有分布式注解 */
		Method method = ((MethodSignature)jp.getSignature()).getMethod();
		if (method.isAnnotationPresent(GlobalTransactional.class)) {
			String transactionalId = HeaderUtil.getHeaderVal(CommonConstant.REMOTE_TRANSACTIONAL_HEADER_NAME);
			/* 如果没有则代表当前是主事务，生成事务 */
			if (transactionalId == null) {
				transactionalId = String.valueOf(System.currentTimeMillis()) + SUM.incrementAndGet() + "$" + localIpPort.toString();
			}
			/* 处理事务 */
			return handler(transactionalId, jp);
		} else {return jp.proceed();}
	}
	
	/* 处理事务 */
	private Object handler(String transactionalId, ProceedingJoinPoint jp) throws Throwable {
		/* 当前线程添加自定义事务头 */
		try {remoteTransactional.add(transactionalId); RemoteAdapter.REMOTE_TRANSACTIONAL.set(transactionalId); return jp.proceed();}
		finally {RemoteAdapter.REMOTE_TRANSACTIONAL.remove(); remoteTransactional.remove(transactionalId);}
	}
}
