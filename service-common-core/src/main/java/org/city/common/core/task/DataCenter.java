package org.city.common.core.task;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @作者 ChengShi
 * @日期 2020-04-18 11:47:05
 * @版本 1.0
 * @描述 数据中心
 */
class DataCenter {
	DataCenter(long maxTimeout) {this.jvmCloseHandler = new JvmCloseHandler(this, maxTimeout);}
	/** 任务数据 */
	final Queue<Runnable> TASKS = new Queue<>();
	
	/** 所有运行线程 */
	final List<Run> RUNS = new LinkedList<>();
	
	/** 所有定时任务线程 */
	final Map<String, Schedula> SCALUDES = new TreeMap<>();
	
	/** 是否jvm关闭 */
	boolean isClose = false;
	/** jvm关闭执行 */
	final JvmCloseHandler jvmCloseHandler;
	
	/** 最大线程数 */
	int MAX = 1;
	
	/** 核心线程数 */
	int CORE = 1;
			
	/** 最大任务数量 */
	int TASKMAX = Short.MAX_VALUE;
	
}
