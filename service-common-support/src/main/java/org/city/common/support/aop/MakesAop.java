package org.city.common.support.aop;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.city.common.api.annotation.make.MakeInvoke;
import org.city.common.api.annotation.make.Makes;
import org.city.common.api.dto.MakeInvokeDto;
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
@Order(Short.MAX_VALUE)
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
		Map<org.city.common.api.in.MakeInvoke, MakeInvokeDto> invokes = new LinkedHashMap<>();
		for (MakeInvoke makeInvoke : makes.value()) {
			if (makeInvoke.invoke() != org.city.common.api.in.MakeInvoke.class) {
				/* 替换自定义参数 */
				String[] values = makeInvoke.values();
				replaceVals(environment, values, jp.getTarget(), jp.getArgs(), names);
				/* 添加解析操作 */
				invokes.put(SpringUtil.getBean(makeInvoke.invoke()), new MakeInvokeDto(makeInvoke, values));
			}
		}
		/* 如果没有操作 */
		if (invokes.size() == 0) {return jp.proceed();}
		
		/* 原方法结束时回调 */
		final List<org.city.common.api.in.Runnable> afters = new ArrayList<>(invokes.size());
		final Process process = new Process() { //执行器
			/* 自定义参数 */
			private final Map<String, Object> PARAM = new HashMap<>();
			/* 执行链路 */
			private final LinkedHashMap<org.city.common.api.in.MakeInvoke, MakeInvokeDto> Makes = new LinkedHashMap<>();
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
				if (this.isError) {return null;}
				else {
					this.isExcute = true;
					return args == null ? jp.proceed() : jp.proceed(args);
				}
			}
			@Override
			public void setReturn(Object returnVal) {
				this.returnVal = returnVal;
				this.isSetReturn = true;
			}
			@Override
			public void addAfter(org.city.common.api.in.Runnable after) {afters.add(after);}
			@Override
			public boolean isSetReturn() {return this.isSetReturn;}
			@Override
			public boolean isExcute() {return this.isExcute;}
			@Override
			public void end() {this.isEnd = true;}
			@Override
			public Object getTarget() {return jp.getTarget();}
			@Override
			public Object getReturn() {return this.returnVal;}
			@Override
			public Method getMethod() {return method;}
			@Override
			public LinkedHashMap<org.city.common.api.in.MakeInvoke, MakeInvokeDto> getMakes() {return this.Makes;}
			@Override
			public Map<org.city.common.api.in.MakeInvoke, MakeInvokeDto> getAllMakes() {return invokes;}
			@Override
			public Object[] getArgs() {return jp.getArgs();}
			@Override
			public void setParam(String key, Object value) {this.PARAM.put(key, value);}
			@Override
			public <T> T getParam(String key, Type type) {return parse(this.PARAM.get(key), type);}
			
			/* 初始执行 */
			private void init() throws Throwable {
				try {
					for (Entry<org.city.common.api.in.MakeInvoke, MakeInvokeDto> entry : invokes.entrySet()) {
						if (this.isEnd) {break;} //结束链路执行
						MakeInvokeDto invokeDto = entry.getValue(); //执行参数
						/* 开始执行方法 */
						entry.getKey().invoke(this, invokeDto.getMakeInvoke().value(), invokeDto.getValues());
						this.Makes.put(entry.getKey(), entry.getValue());
					}
				} catch (Throwable e) {
					this.isError = true;
					for (Entry<org.city.common.api.in.MakeInvoke, MakeInvokeDto> entry : invokes.entrySet()) {
						MakeInvokeDto invokeDto = entry.getValue(); //执行参数
						/* 开始执行异常 */
						entry.getKey().throwable(this, e, invokeDto.getMakeInvoke().value(), invokeDto.getValues());
					}
					throw e; //抛出当前异常错误
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
