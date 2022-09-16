package org.city.common.api.exception;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 19:00:32
 * @版本 1.0
 * @描述 远程调用限速异常
 */
public class RemoteSpeedLimitException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public RemoteSpeedLimitException(String msg) {super(msg);}
	public RemoteSpeedLimitException(Throwable e) {super(e);}
	public RemoteSpeedLimitException(String msg, Throwable e) {super(msg, e);}
}
