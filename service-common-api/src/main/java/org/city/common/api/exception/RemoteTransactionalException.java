package org.city.common.api.exception;

import org.city.common.api.dto.remote.RemoteTransactionalDto;

/**
 * @作者 ChengShi
 * @日期 2023年6月19日
 * @版本 1.0
 * @描述 远程事务异常
 */
public class RemoteTransactionalException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private final RemoteTransactionalDto tran;
	
	public RemoteTransactionalException(RemoteTransactionalDto tran) {
		super(tran.getErrorMsg());
		this.tran = tran;
	}
	public RemoteTransactionalException(String msg) {
		super(msg);
		this.tran = RemoteTransactionalDto.init().setErrorMsg(this.toString());
	}
	public RemoteTransactionalException(Throwable e) {
		super(e);
		this.tran = RemoteTransactionalDto.init().setErrorMsg(e.toString());
	}
	
	/**
	 * @描述 解析成特定消息
	 * @param tranId 事务ID
	 * @return 特定消息
	 */
	public String parseMsg(String tranId) {
		return String.format("%s》》》 [%s][%s] \r\n\tat %s", "分布式事务-执行异常", tran.getAppName(), tranId, tran.getErrorMsg());
	}
}
