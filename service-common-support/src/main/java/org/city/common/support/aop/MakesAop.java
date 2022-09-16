package org.city.common.support.aop;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.city.common.api.annotation.make.MakeInvoke;
import org.city.common.api.annotation.make.Makes;
import org.city.common.api.constant.CommonConstant;
import org.city.common.api.in.Replace;
import org.city.common.api.in.MakeInvoke.Process;
import org.city.common.api.in.parse.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
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
@DependsOn(CommonConstant.PLUG_UTIL_NAME)
public class MakesAop implements Replace,JSONParser{
	@Autowired
	private Environment environment;
	@Autowired
	private ApplicationContext applicationContext;
	
	@Around("@annotation(makes)")
	public Object makesAround(ProceedingJoinPoint jp, Makes makes) throws Throwable {
		Class<?> target = jp.getTarget().getClass();
		Method method = null;
		/* 获取实现类的方法 */
		for (Method mth : target.getDeclaredMethods()) {if (mth.getName().equals(jp.getSignature().getName())) {method = mth; break;}}
		if (method == null) {throw new NullPointerException(String.format("未找到被拦截的方法[%s]，类名[%s]", jp.getSignature().getName(), target.getName()));}
		
		/* 执行操作 */
		return makeInvoke(jp, makes, method);
	}

	/* 操作执行 */
	private Object makeInvoke(ProceedingJoinPoint jp, Makes makes, Method method) throws Throwable {
		/*获取参数真实名称*/
		LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
		String[] names = discoverer.getParameterNames(method);
		
		/* 所有可以执行的操作 */
		Map<org.city.common.api.in.MakeInvoke, String[]> invokes = new LinkedHashMap<>();
		for (MakeInvoke makeInvoke : makes.value()) {
			if (makeInvoke.invoke() != org.city.common.api.in.MakeInvoke.class) {
				/* 替换自定义参数 */
				String[] values = makeInvoke.values();
				replaceVals(environment, values, jp.getArgs(), names);
				/* 添加解析操作 */
				invokes.put(applicationContext.getBean(makeInvoke.invoke()), values);
			}
		}
		/* 如果没有操作 */
		if (invokes.size() == 0) {return jp.proceed(jp.getArgs());}
		
		/* 执行器 */
		Process process = new org.city.common.api.in.MakeInvoke.Process() {
			/*执行链路*/
			private LinkedList<Class<? extends org.city.common.api.in.MakeInvoke>> Makes = new LinkedList<>();
			/* 是否错误执行 */
			private boolean isError = false;
			/* 是否执行过 */
			private boolean isExcute = false;
			/* 是否设置过返回值 */
			private boolean isSetReturn = false;
			/* 返回值 */
			private Object returnVal = null;
			/* 初始执行 */
			{init();}
			
			@Override
			public Object process() throws Throwable {
				if (this.isError) {return null;}
				else {
					this.isExcute = true;
					return jp.proceed(jp.getArgs());
				}
			}
			@Override
			public void setReturn(Object returnVal) {
				/* 判断是否进行转换 */
				if(this.isSetReturn && returnVal != null && (this.returnVal == null || this.returnVal.getClass() != returnVal.getClass())) {
					this.returnVal = parse(returnVal, method.getGenericReturnType());
				} else {this.returnVal = returnVal;}
				this.isSetReturn = true;
			}
			@Override
			public boolean isSetReturn() {return this.isSetReturn;}
			@Override
			public Object getTarget() {return jp.getTarget();}
			@Override
			public Object getReturn() {return this.returnVal;}
			@Override
			public Method getMethod() {return method;}
			@Override
			public LinkedList<Class<? extends org.city.common.api.in.MakeInvoke>> getMakes() {return Makes;}
			@Override
			public Object[] getArgs() {return jp.getArgs();}
			
			/* 初始执行 */
			private void init() throws Throwable {
				/* 开始执行 */
				try {
					for (Entry<org.city.common.api.in.MakeInvoke, String[]> entry : invokes.entrySet()) {
						/* 执行方法 */
						entry.getKey().invoke(this, entry.getValue());
						if (this.isExcute) {Makes.add(entry.getKey().getClass());}
						this.isExcute = false;
					}
				} catch (Throwable e) {
					this.isError = true;
					/* 执行异常 */
					for (Entry<org.city.common.api.in.MakeInvoke, String[]> entry : invokes.entrySet()) {
						entry.getKey().throwable(this, e, entry.getValue());
					}
					/* 抛出当前异常错误 */
					throw e;
				}
			}
		};
		/* 最后的返回值 */
		return process.isSetReturn() ? process.getReturn() : process.process();
	}
}
