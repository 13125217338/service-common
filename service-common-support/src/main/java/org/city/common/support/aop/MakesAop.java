package org.city.common.support.aop;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.city.common.api.annotation.make.Make;
import org.city.common.api.annotation.make.Makes;
import org.city.common.api.dto.MakeDto;
import org.city.common.api.exception.SkipException;
import org.city.common.api.in.MakeInvoke.Process;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.util.Replace;
import org.city.common.api.util.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @作者 ChengShi
 * @日期 2022-07-01 18:12:16
 * @版本 1.0
 * @描述 操作拦截
 */
@Aspect
@Component
@Order(Byte.MAX_VALUE)
public class MakesAop implements Replace,JSONParser {
	@Autowired
	private Environment environment;
	
	@Around("@annotation(org.city.common.api.annotation.make.Makes)")
	public Object makesAround(ProceedingJoinPoint jp) throws Throwable {
		Method method = ((MethodSignature) jp.getSignature()).getMethod();
		/* 执行操作 */
		return makeInvoke(jp, method.getDeclaredAnnotation(Makes.class), method);
	}
	
	/* 操作执行 */
	private Object makeInvoke(ProceedingJoinPoint jp, Makes makes, Method method) throws Throwable {
		/* 获取参数真实名称 */
		LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
		String[] names = discoverer.getParameterNames(method);
		
		/* 所有可以执行的操作 */
		Map<MakeDto, org.city.common.api.in.MakeInvoke> invokes = new LinkedHashMap<>();
		for (Make make : makes.value()) {
			if (make.invoke() != org.city.common.api.in.MakeInvoke.class) {
				/* 替换自定义参数 */
				String[] values = make.values();
				replaceVals(environment, values, jp.getTarget(), jp.getArgs(), names);
				/* 添加解析操作 */
				invokes.put(new MakeDto(make, values), SpringUtil.getBean(make.invoke()));
			}
		}
		/* 如果没有操作 */
		if (invokes.size() == 0) {return jp.proceed();}
		
		/* 原方法结束时回调 */
		final List<org.city.common.api.in.Runnable> afters = new ArrayList<>(invokes.size());
		final Process process = new Process() { //执行器
			/* 所有可以执行的操作 */
			private final Iterator<Entry<MakeDto, org.city.common.api.in.MakeInvoke>> INVOKES = invokes.entrySet().iterator();
			/* 自定义参数 */
			private final Map<String, Object> PARAM = new HashMap<>();
			/* 执行链路 */
			private final LinkedHashMap<MakeDto, org.city.common.api.in.MakeInvoke> MAKES = new LinkedHashMap<>();
			/* 是否错误执行 */
			private boolean isError = false;
			/* 是否执行过原方法 */
			private boolean isExcute = false;
			/* 是否设置过返回值 */
			private boolean isSetReturn = false;
			/* 是否结束链路执行 */
			private boolean isEnd = false;
			/* 返回值 */
			private Object returnVal = null;
			/* 初始执行 */
			{init();}
			
			@Override
			public Object process() throws Throwable {
				return process(null);
			}
			@Override
			public Object process(Object[] args) throws Throwable {
				if (isError) {return null;}
				else {
					isExcute = true;
					return args == null ? jp.proceed() : jp.proceed(args);
				}
			}
			@Override
			public boolean nextInvoke() throws Throwable {
				if (isEnd) {return false;} //提前结束链路执行
				boolean hasNext = INVOKES.hasNext();
				if (!hasNext) {return false;} //已无操作
				
				/* 使用链路顺序执行 */
				Entry<MakeDto, org.city.common.api.in.MakeInvoke> next = INVOKES.next();
				next.getValue().invoke(this, next.getKey().getMake().value(), next.getKey().getValues());
				MAKES.put(next.getKey(), next.getValue()); //添加执行后的操作
				
				/* 返回下一个结果 */
				return INVOKES.hasNext();
			}
			@Override
			public void setReturn(Object returnVal) {
				this.returnVal = returnVal;
				isSetReturn = true;
			}
			@Override
			public void addAfter(org.city.common.api.in.Runnable after) {afters.add(after);}
			@Override
			public boolean isSetReturn() {return isSetReturn;}
			@Override
			public boolean isExcute() {return isExcute;}
			@Override
			public void end() {isEnd = true;}
			@Override
			public Object getTarget() {return jp.getTarget();}
			@Override
			public Object getReturn() {return returnVal;}
			@Override
			public Method getMethod() {return method;}
			@Override
			public LinkedHashMap<MakeDto, org.city.common.api.in.MakeInvoke> getMakes() {return MAKES;}
			@Override
			public Map<MakeDto, org.city.common.api.in.MakeInvoke> getAllMakes() {return invokes;}
			@Override
			public Object[] getArgs() {return jp.getArgs();}
			@Override
			public void setParam(String key, Object value) {PARAM.put(key, value);}
			@Override
			public <T> T getParam(String key, Type type) {return parse(PARAM.get(key), type);}
			
			/* 初始执行 */
			private void init() throws Throwable {
				try {while (nextInvoke());} //顺序执行操作
				catch (Throwable e) {
					if (e instanceof SkipException) {throw e;} //跳过异常则不处理该异常
					isError = true; //标记为异常
					invokes.forEach((k, v) -> {try {v.throwable(this, e, k.getMake().value(), k.getValues());} catch (Throwable e2) {}}); //顺序执行异常
					throw e; //抛出当前异常
				}
			}
		};
		
		/* 最后的返回值 - 如果没有设置过返回值 - 则会自动执行原方法 */
		Object returnVal = process.isSetReturn() ? parse(process.getReturn(), method.getGenericReturnType()) : process.process();
		/* 原方法结束时回调 - 顺序执行所有添加过回调钩子的方法逻辑 */
		for (org.city.common.api.in.Runnable after : afters) {after.run();}
		return returnVal; //回调结束后返回结果
	}
}
