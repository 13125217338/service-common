package org.city.common.api.in.sql;

/**
 * @作者 ChengShi
 * @日期 2022-12-01 13:56:31
 * @版本 1.0
 * @描述 远程分布式事务
 */
public interface RemoteTransactional {
	/**
	 * @描述 添加一个事务（由发起者调用）
	 * @param tranId 事务ID
	 */
	public void add(String tranId);
	
	/**
	 * @描述 追加一个事务（由参与者调用）
	 * @param tranId 事务ID
	 */
	public void append(String tranId);
	
	/**
	 * @描述 设置当前事务状态
	 * @param tranId 事务ID
	 * @param state 事务状态，true=成功，false=失败
	 * @param throwable 当事务状态为false时的异常
	 * @throws Throwable
	 */
	public void setState(String tranId, boolean state, Throwable throwable) throws Throwable;
	
	/**
	 * @描述 验证事务结果
	 * @param tranId 事务ID
	 * @return true=全部执行成功，false=请继续验证（验证不通过会自动抛出异常）
	 */
	public boolean verifyResult(String tranId);
}
