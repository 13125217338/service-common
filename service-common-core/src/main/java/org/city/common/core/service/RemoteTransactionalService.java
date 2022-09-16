package org.city.common.core.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.city.common.api.constant.RemoteSqlType;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.dto.remote.RemoteTransactionalDto;
import org.city.common.api.in.sql.RemoteTransactional;
import org.city.common.api.open.RemoteTransactionalApi;
import org.city.common.api.util.SpringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSONObject;

/**
 * @作者 ChengShi
 * @日期 2022年10月15日
 * @版本 1.0
 * @描述 远程分布式事务服务
 */
@Service
public class RemoteTransactionalService implements RemoteTransactional,RemoteTransactionalApi{
	@Autowired
	private RemoteConfigDto remoteConfigDto;
	/* 记录待执行Sql */
	private final Map<String, RemoteTransactionalDto> SQL = new ConcurrentHashMap<>();
	
	@Override
	public Object exec(String transactionalId, RemoteTransactionalDto dto) throws Throwable {
		RemoteTransactionalDto old = SQL.get(transactionalId);
		Assert.notNull(old, String.format("主事务[%s]未设置远程分布式事务参数！", transactionalId));
		
		/* 同步操作事务参数 */
		synchronized (old) {
			BeanUtils.copyProperties(dto, old); old.notifyAll();
			old.wait(remoteConfigDto.getReadTimeout());
			old.setType(RemoteSqlType.task); //重新置为任务类型
		}
		
		/* 返回值判断 */
		if (old.getSqlReturn() instanceof Throwable) {throw (Throwable) old.getSqlReturn();}
		else if (old.getSqlReturn() == null) {
			String msg = String.format("当前服务[%s]，Sql执行语句[%s]、执行参数[%s]未返回任何值！", SpringUtil.getAppName(), old.getSql(), JSONObject.toJSONString(old.getArgs()));
			throw new IllegalArgumentException(msg);
		} else {return old.getSqlReturn();}
	}
	
	@Override
	public void add(String transactionalId) {SQL.put(transactionalId, new RemoteTransactionalDto());}
	
	@Override
	public RemoteTransactionalDto get(String transactionalId) {return SQL.get(transactionalId);}
	
	@Override
	public void remove(String transactionalId) {SQL.remove(transactionalId);}
}
