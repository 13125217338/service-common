package org.city.common.core.handler;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.city.common.api.adapter.RemoteAdapter;
import org.city.common.api.adapter.impl.IpPortAdapter;
import org.city.common.api.constant.RemoteSqlType;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.dto.remote.RemoteIpPortDto;
import org.city.common.api.dto.remote.RemoteTransactionalDto;
import org.city.common.api.in.Runnable;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.open.RemoteTransactionalApi;
import org.city.common.api.open.Task;
import org.city.common.api.util.PlugUtil;
import org.city.common.core.service.RemoteTransactionalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * @作者 ChengShi
 * @日期 2022-12-01 10:39:52
 * @版本 1.0
 * @描述 远程事务处理
 */
@Component
public class RemoteTransactionalHandler implements JSONParser {
	@Autowired
	private Task task;
	@Autowired
	private RemoteConfigDto remoteConfigDto;
	@Autowired
	private RemoteTransactionalService remoteTransactionalService;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private IpPortAdapter ipPortAdapter;
	@Autowired
	private RemoteIpPortDto localIpPort;
	/** 获取事务参数 */
	public RemoteTransactionalDto getRemoteTransactionalDto(String tId) {return remoteTransactionalService.get(tId);}
	
	/**
	 * @描述 远程查询执行
	 * @param sql 语句
	 * @param args 参数
	 * @param dateFormat 时间格式
	 * @param type 返回类型
	 * @return 执行结果
	 * @throws Throwable
	 */
	public <D> D remoteExecQ(String sql, List<Object> args, String dateFormat, Class<D> type) throws Throwable {
		Object result = getResult(sql, new RemoteTransactionalDto().setType(RemoteSqlType.query).setArgs(args).setDateFormat(dateFormat));
		if (result == null) {return null;} else {return parse(result, type);}
	}
	
	/**
	 * @描述 远程多个结果查询执行
	 * @param sql 语句
	 * @param args 参数
	 * @param dateFormat 时间格式
	 * @param type 返回类型
	 * @return 执行结果
	 * @throws Throwable
	 */
	public <D> List<D> remoteExecQS(String sql, List<Object> args, String dateFormat, Class<D> type) throws Throwable {
		Object result = getResult(sql, new RemoteTransactionalDto().setType(RemoteSqlType.querys).setArgs(args).setDateFormat(dateFormat));
		if (result == null) {return null;} else {return parse(result, new ParameterizedType() {
			@Override
			public Type getRawType() {return List.class;}
			@Override
			public Type getOwnerType() {return null;}
			@Override
			public Type[] getActualTypeArguments() {return new Type[] {type};}
		});}
	}
	
	/**
	 * @描述 远程更新执行
	 * @param sql 语句
	 * @param args 参数
	 * @return 执行结果
	 * @throws Throwable
	 */
	public Integer remoteExecU(String sql, List<Object> args) throws Throwable {
		Object result = getResult(sql, new RemoteTransactionalDto().setType(RemoteSqlType.update).setArgs(args));
		if (result == null) {return null;} else {return parse(result, Integer.class);}
	}
	
	/**
	 * @描述 批量远程更新执行
	 * @param sql 语句
	 * @param bArgs 参数
	 * @return 执行结果
	 * @throws Throwable
	 */
	public int[] remoteExecBU(String sql, List<Object[]> bArgs) throws Throwable {
		Object result = getResult(sql, new RemoteTransactionalDto().setType(RemoteSqlType.bUpdate).setBArgs(bArgs));
		if (result == null) {return null;} else {return parse(result, int[].class);}
	}
	
	/* 公共获取结果 */
	private Object getResult(String sql, RemoteTransactionalDto dto) throws Throwable {
		String tId = RemoteAdapter.REMOTE_TRANSACTIONAL.get();
		if (tId == null) {return null;}
		String remoteIpPort = tId.split("[$]")[1];
		if (localIpPort.toString().equals(remoteIpPort)) {return null;} //本地就不远程调用了
		
		/* 远程执行参数 */
		return PlugUtil.getBean(new RemoteIpPortDto(remoteIpPort), ipPortAdapter, RemoteTransactionalApi.class).exec(tId, dto.setSql(sql));
	}
	
	/**
	 * @描述 处理远程事务
	 * @param dto 事务参数
	 * @param runnable 执行回调
	 * @return 执行结果
	 * @throws Throwable
	 */
	public Object handler(RemoteTransactionalDto dto, Runnable runnable) throws Throwable {
		task.putTask(runnable);
		
		/* 没结束一直循环 - 直到超时 */
		long recordTime = System.currentTimeMillis(); Object returnVal = null;
		while(true) {
			synchronized (dto) {
				if (dto.isEnd()) {returnVal = dto.resetReturn(); break;}
				dto.wait(remoteConfigDto.getReadTimeout());
				/* 处理sql */
				try {handlerSql(dto);} catch (Throwable e) {dto.setSqlReturn(e);}
			}
			/* 超时 */
			if ((System.currentTimeMillis() - recordTime) > remoteConfigDto.getReadTimeout()) {
				throw new TimeoutException("远程子任务执行超时！");
			}
		}
		
		/* 返回值 */
		if (returnVal instanceof Throwable) {throw (Throwable) returnVal;} else {return returnVal;}
	}
	
	/* 处理Sql */
	private void handlerSql(RemoteTransactionalDto dto) throws SQLException {
		switch (dto.getType()) {
			case task: break; //不处理默认任务类型
			case query: dto.$setSqlReturn(jdbcTemplate.queryForObject(dto.getSql(), Object.class, dto.getArgs().toArray())); break;
			case querys: dto.$setSqlReturn(queryResult(jdbcTemplate.queryForList(dto.getSql(), dto.getArgs().toArray()), dto.getDateFormat())); break;
			case update: dto.$setSqlReturn(jdbcTemplate.update(dto.getSql(), dto.getArgs().toArray())); break;
			case bUpdate: dto.$setSqlReturn(jdbcTemplate.batchUpdate(dto.getSql(), dto.getBArgs())); break;
			default: throw new NullPointerException(String.format("远程执行Sql类型[%s]不存在！", dto.getType().name()));
		}
	}
	/* 查询结果 */
	private List<JSONObject> queryResult(List<Map<String, Object>> resultMaps, String dateFormat) throws SQLException {
		List<JSONObject> result = new ArrayList<>();
		for(Map<String, Object> data : resultMaps) {
			/*时间格式自定义序列化方式*/
			String dataJsonStr = StringUtils.hasText(dateFormat) ? 
						JSONObject.toJSONStringWithDateFormat(data, dateFormat, SerializerFeature.WriteEnumUsingName) :
						JSONObject.toJSONString(data, SerializerFeature.WriteEnumUsingName);
			/*将结果map转json后在转对应类对象*/
			result.add(JSONObject.parseObject(dataJsonStr));
		}
		return result;
	}
}
