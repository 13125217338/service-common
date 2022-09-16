package org.city.common.support.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.dto.Response;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.sql.Crud;
import org.city.common.api.util.FormatUtil;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2023年4月19日
 * @版本 1.0
 * @描述 拦截所有控制层并清除自定义Sql与自定义事务
 */
@Aspect
@Order(-1)
@Component
public class ControllerAop implements JSONParser {
	@Around("@within(org.springframework.web.bind.annotation.RestController) || @within(org.springframework.stereotype.Controller)")
	public Object authBefore(ProceedingJoinPoint jp) throws Throwable {
		Crud.clearSql(jp.getArgs()); //清除自定义Sql
		RemoteAdapter.REMOTE_TRANSACTIONAL.remove(); //清除自定义事务
		Object result = jp.proceed(); //执行原方法
		if (result instanceof Response) {FormatUtil.format(((Response) result).getData());} //格式化返回值
		return result; //返回格式化后的结果
	}
}
