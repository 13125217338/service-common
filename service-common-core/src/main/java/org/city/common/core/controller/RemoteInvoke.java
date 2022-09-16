package org.city.common.core.controller;

import org.city.common.api.dto.RemoteDto;
import org.city.common.core.service.RemoteInvokeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @作者 ChengShi
 * @日期 2022-07-27 15:08:22
 * @版本 1.0
 * @描述 远程调用接口
 */
@RestController
@RequestMapping("${remote.path:/remote/path}")
public class RemoteInvoke {
	@Autowired
	private RemoteInvokeService remoteInvokeService;
	
	@PostMapping("/invoke")
	public Object invoke(@Validated @RequestBody RemoteDto remoteDto) throws Throwable {
		return remoteInvokeService.$invoke(remoteDto);
	}
}
