package org.city.common.core.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.city.common.api.dto.Response;
import org.city.common.api.dto.remote.RemoteDto;
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
@RequestMapping("/remote/path")
public class RemoteInvoke {
	@Autowired
	private RemoteInvokeService remoteInvokeService;
	
	@PostMapping("/invoke")
	public Response invoke(@Validated @RequestBody RemoteDto remoteDto, HttpServletRequest request, HttpServletResponse response) throws Throwable {
		return remoteInvokeService.$invoke(remoteDto, request, response);
	}
}
