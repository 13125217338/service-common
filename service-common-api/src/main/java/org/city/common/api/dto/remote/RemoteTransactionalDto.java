package org.city.common.api.dto.remote;

import org.city.common.api.util.SpringUtil;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2023年4月20日
 * @版本 1.0
 * @描述 远程事务参数
 */
@Data
@Accessors(chain = true)
public class RemoteTransactionalDto {
	/* true=成功，false=失败，NULL=初始化 */
	private Boolean state;
	/* 应用名称 */
	private String appName;
	/* 当前状态为false时的错误信息 */
	private String errorMsg;
	
	/**
	 * @描述 初始状态参数
	 * @return 远程事务参数
	 */
	public static RemoteTransactionalDto init() {
		return new RemoteTransactionalDto().setAppName(SpringUtil.getAppName());
	}
}
