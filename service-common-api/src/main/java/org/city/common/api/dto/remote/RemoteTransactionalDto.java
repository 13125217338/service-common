package org.city.common.api.dto.remote;

import java.util.List;

import org.city.common.api.constant.RemoteSqlType;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022年10月15日
 * @版本 1.0
 * @描述 远程事务参数
 */
@Data
@Accessors(chain = true)
public class RemoteTransactionalDto {
	/* 执行类型 */
	private RemoteSqlType type = RemoteSqlType.task;
	/* 待执行sql */
	private String sql;
	/* 待执行参数 */
	private List<Object> args;
	/* 批量执行参数 */
	private List<Object[]> bArgs;
	/* 时间格式化 */
	private String dateFormat;
	/* sql返回值 */
	private Object sqlReturn;
	
	/* 执行返回值 */
	private Object returnVal;
	/* 是否主任务结束 */
	private boolean isEnd;
	
	/**
	 * @描述 设置Sql执行返回值并唤醒线程
	 * @param sqlReturn 执行返回值
	 */
	public synchronized void $setSqlReturn(Object sqlReturn) {
		this.sqlReturn = sqlReturn;
		this.notifyAll();
	}
	
	
	/**
	 * @描述 重置并返回任务执行结果
	 * @return 任务执行结果
	 */
	public synchronized Object resetReturn() {
		Object rVal = returnVal;
		sqlReturn = null; returnVal = null; isEnd = false;
		return rVal;
	}
}
