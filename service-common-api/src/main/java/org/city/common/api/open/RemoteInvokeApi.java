package org.city.common.api.open;

import org.city.common.api.dto.MethodCacheDto;
import org.springframework.http.HttpEntity;

/**
 * @作者 ChengShi
 * @日期 2022-07-27 15:08:22
 * @版本 1.0
 * @描述 远程执行代码（从请求头里面取端口地址信息，反向调用）
 */
public interface RemoteInvokeApi {
	/**
	 * @描述 执行远程代码
	 * @param httpEntity 请求参数
	 * @param args 方法参数
	 * @return 执行结果
	 */
	public <T> T invoke(HttpEntity<MethodCacheDto> httpEntity, Object...args);
}
