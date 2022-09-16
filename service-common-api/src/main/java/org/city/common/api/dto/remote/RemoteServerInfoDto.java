package org.city.common.api.dto.remote;

import org.city.common.api.util.SpringUtil;

import lombok.Data;

/**
 * @作者 ChengShi
 * @日期 2023年6月26日
 * @版本 1.0
 * @描述 远程服务信息参数
 */
@Data
public class RemoteServerInfoDto {
	/* 服务名称 */
	private String appName = SpringUtil.getAppName();
	/* java总内存占用 */
	private String javaTotalMemory;
	/* java剩余内存 */
	private String javaFreeMemory;
	/* java最大挖取内存 */
	private String javaMaxMemory;
	/* 物理总内存 */
	private String physicalTotalMemory;
	/* 物理剩余内存 */
	private String physicalFreeMemory;
	/* 物理总交换空间 */
	private String physicalTotalSwapMemory;
	/* 物理剩余交换空间 */
	private String physicalFreeSwapMemory;
	/* 提交虚拟内存 */
	private String submitVirtualMemory;
	/* 系统cpu总数 */
	private Integer physicalCpuTotal;
	/* 系统cpu使用率 */
	private String physicalCpuUse;
	/* java程序cpu使用率 */
	private String javaCpuUse;
	/* java程序运行时间 */
	private String javaRuntime;
}
