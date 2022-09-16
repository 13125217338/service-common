package org.city.common.api.in.redis;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.DataList;
import org.city.common.api.dto.RedisDto;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.entity.BaseEntity;
import org.city.common.api.in.Runnable;
import org.city.common.api.in.Task;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.sql.Limit;
import org.city.common.api.util.MyUtil;
import org.city.common.api.util.SpringUtil;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;

/**
 * @作者 ChengShi
 * @日期 2023年6月18日
 * @版本 1.0
 * @描述 缓存令牌操作
 */
public final class RedisTokenMake implements JSONParser {
	private final String SPLIT = "_"; //令牌与唯一ID分割线
	private final String SPLIT_DATA = "-"; //数据唯一分割线
	private final RedisMake redisMake;
	private final Task task;
	private final RemoteConfigDto remoteConfigDto;
	private final List<RedisTokenRemove> redisTokenRemoves;
	private final String ONLY_ID;
	/**
	 * @param onlyId 识别验证数据唯一ID
	 */
	public RedisTokenMake(String onlyId) {
		ONLY_ID = onlyId;
		redisMake = SpringUtil.getBean(RedisMake.class);
		task = SpringUtil.getBean(Task.class);
		remoteConfigDto = SpringUtil.getBean(RemoteConfigDto.class);
		redisTokenRemoves = SpringUtil.getBeans(RedisTokenRemove.class);
		init(); //初始化
	}
	/* 初始化 */
	private void init() {
		task.schedula(CommonConstant.REDIS_TOKEN_TIME_EXPIRE_ID + SPLIT + ONLY_ID, new Runnable() {
			@Override
			public void run() throws Throwable {
				Long size = redisMake.getRedisTemplate().opsForHash().size(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID);
				new BaseEntity().setPageSize((long) Short.MAX_VALUE).limitHandler(size.longValue(), new Limit() { //分页验证所有数据
					@Override
					public void handler(BaseEntity baseEntity) {
						DataList<Entry<Object, Object>> entrys = redisMake.entry(RedisDto.of(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, "*", baseEntity.getPageNum(), baseEntity.getPageSize()));
						handler(entrys.getRows(), redisMake.getNowTime());
					}
					/* 处理分页数据 */
					private void handler(List<Entry<Object, Object>> rows, long nowTime) {
						for (Entry<Object, Object> entry : rows) {
							final String tkId = entry.getKey().toString(); //令牌ID - 用于加锁验证过期时间
							redisMake.tryLock(CommonConstant.REDIS_TOKEN_HKEY + SPLIT + ONLY_ID + tkId, remoteConfigDto.getReadTimeout(), true, () -> {
								Long tkTime = parse(entry.getValue(), Long.class);
								return verifyExpire(tkId, tkTime, nowTime);
							});
						}
					}
				});
			}
		}, () -> String.valueOf(remoteConfigDto.getTokenExpireTime() >> 2), false);
	}
	
	/**
	 * @描述 获取所有令牌数据
	 * @return key=tkId，value=令牌数据
	 */
	public Map<String, Object> getAll() {
		return redisMake.entry(CommonConstant.REDIS_TOKEN_HKEY + SPLIT_DATA + ONLY_ID);
	}
	
	/**
	 * @描述 获取令牌
	 * @param onlyId 唯一ID
	 * @param data 令牌数据
	 * @return 令牌
	 */
	public String getToken(String onlyId, Object data) {
		/* 生成令牌 */
		long nowTime = redisMake.getNowTime();
		String token = MyUtil.md5(JSON.toJSONString(data) + nowTime);
		/* 删除重复的其他令牌数据 */
		deleteByToken(token); deleteByOnlyId(onlyId);
		
		/* 记录过期时间与令牌数据 */
		redisMake.setHValue(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, token + SPLIT + onlyId, nowTime);
		redisMake.setHValue(CommonConstant.REDIS_TOKEN_HKEY + SPLIT_DATA + ONLY_ID, token + SPLIT + onlyId, data);
		return token;
	}
	
	/**
	 * @描述 获取令牌与唯一ID来自令牌
	 * @param token 令牌
	 * @return 令牌与唯一ID
	 */
	public String getTkIdByToken(String token) {
		DataList<Entry<Object, Object>> entrys = redisMake.entry(RedisDto.of(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, token + SPLIT + "*", 1, Short.MAX_VALUE));
		return getTkId(entrys.getRows(), redisMake.getNowTime(), true).stream().findFirst().orElse(null);
	}
	/**
	 * @描述 获取令牌与唯一ID来自唯一ID
	 * @param onlyId 唯一ID
	 * @return 令牌与唯一ID
	 */
	public String getTkIdByOnlyId(String onlyId) {
		DataList<Entry<Object, Object>> entrys = redisMake.entry(RedisDto.of(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, "*" + SPLIT + onlyId, 1, Short.MAX_VALUE));
		return getTkId(entrys.getRows(), redisMake.getNowTime(), true).stream().findFirst().orElse(null);
	}
	/**
	 * @描述 获取多个令牌与唯一ID来自唯一ID
	 * @param onlyId 唯一ID（模糊匹配）
	 * @return 多个令牌与唯一ID（最多[Short.MAX_VALUE]数量，不会为NULL）
	 */
	public List<String> getTkIdsByOnlyId(String onlyId) {
		DataList<Entry<Object, Object>> entrys = redisMake.entry(RedisDto.of(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, "*" + SPLIT + onlyId + "*", 1, Short.MAX_VALUE));
		return getTkId(entrys.getRows(), redisMake.getNowTime(), false);
	}
	/* 获取令牌与唯一ID */
	private List<String> getTkId(List<Entry<Object, Object>> rows, long nowTime, boolean hasOne) {
		List<String> tkIds = new ArrayList<>(); //所有匹配令牌与唯一ID
		for (Entry<Object, Object> entry : rows) {
			final String tkId = entry.getKey().toString(); //令牌ID - 用于加锁验证过期时间
			final String curTkId = redisMake.tryLock(CommonConstant.REDIS_TOKEN_HKEY + SPLIT + ONLY_ID + tkId, remoteConfigDto.getReadTimeout(), true, () -> {
				Long tkTime = redisMake.getHValue(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, tkId, Long.class);
				return verifyExpire(tkId, tkTime, nowTime);
			});
			/* 有令牌ID则添加 - 否则继续查询 */
			if (curTkId != null) {
				tkIds.add(curTkId);
				if (hasOne) {return tkIds;} //获取单个直接返回
			}
		}
		return tkIds;
	}
	
	/**
	 * @描述 获取数据来自令牌
	 * @param <T> 数据类型
	 * @param token 令牌
	 * @param type 数据类型
	 * @return 数据
	 */
	public <T> T getByToken(String token, Type type) {
		return getByTkId(getTkIdByToken(token), type);
	}
	/**
	 * @描述 获取数据来自唯一ID
	 * @param <T> 数据类型
	 * @param onlyId 唯一ID
	 * @param type 数据类型
	 * @return 数据
	 */
	public <T> T getByOnlyId(String onlyId, Type type) {
		return getByTkId(getTkIdByOnlyId(onlyId), type);
	}
	/**
	 * @描述 获取数据来自令牌与唯一ID
	 * @param <T> 数据类型
	 * @param tkId 令牌与唯一ID
	 * @param type 数据类型
	 * @return 数据
	 */
	public <T> T getByTkId(String tkId, Type type) {
		if (tkId == null) {return null;}
		return redisMake.getHValue(CommonConstant.REDIS_TOKEN_HKEY + SPLIT_DATA + ONLY_ID, tkId, type);
	}
	
	/**
	 * @描述 获取运行时间来自令牌
	 * @param token 令牌
	 * @return 运行时间（毫秒）
	 */
	public long getRunTimeByToken(String token) {
		return getRunTimeByTkId(getTkIdByToken(token));
	}
	/**
	 * @描述 获取运行时间来自唯一ID
	 * @param onlyId 唯一ID
	 * @return 运行时间（毫秒）
	 */
	public long getRunTimeByOnlyId(String onlyId) {
		return getRunTimeByTkId(getTkIdByOnlyId(onlyId));
	}
	/**
	 * @描述 获取运行时间来自令牌与唯一ID
	 * @param tkId 令牌与唯一ID
	 * @return 运行时间（毫秒）
	 */
	public long getRunTimeByTkId(String tkId) {
		if (tkId == null) {return -1L;}
		long recordTime = redisMake.getHValue(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, tkId, long.class);
		return redisMake.getNowTime() - recordTime;
	}
	
	/**
	 * @描述 刷新过期时间来自令牌
	 * @param token 令牌
	 */
	public void flushByToken(String token) {
		flushByTkId(getTkIdByToken(token));
	}
	/**
	 * @描述 刷新过期时间来自唯一ID
	 * @param onlyId 唯一ID
	 */
	public void flushByOnlyId(String onlyId) {
		flushByTkId(getTkIdByOnlyId(onlyId));
	}
	/**
	 * @描述 刷新过期时间来自令牌与唯一ID
	 * @param tkId 令牌与唯一ID
	 */
	public void flushByTkId(String tkId) {
		if (tkId != null) {redisMake.setHValue(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, tkId, redisMake.getNowTime());}
	}
	
	/**
	 * @描述 重新设置数据来自令牌
	 * @param token 令牌
	 * @param data 数据
	 */
	public void setByToken(String token, Object data) {
		setByTkId(getTkIdByToken(token), data);
	}
	/**
	 * @描述 重新设置数据来自唯一ID
	 * @param onlyId 唯一ID
	 * @param data 数据
	 */
	public void setByOnlyId(String onlyId, Object data) {
		setByTkId(getTkIdByOnlyId(onlyId), data);
	}
	/**
	 * @描述 重新设置数据来自令牌与唯一ID
	 * @param tkId 令牌与唯一ID
	 * @param data 数据
	 */
	public void setByTkId(String tkId, Object data) {
		if (tkId != null) {redisMake.setHValue(CommonConstant.REDIS_TOKEN_HKEY + SPLIT_DATA + ONLY_ID, tkId, data);}
	}
	
	/**
	 * @描述 删除数据来自令牌
	 * @param token 令牌
	 */
	public void deleteByToken(String token) {
		deleteByTkId(getTkIdByToken(token));
	}
	/**
	 * @描述 删除数据来自唯一ID
	 * @param onlyId 唯一ID
	 */
	public void deleteByOnlyId(String onlyId) {
		deleteByTkId(getTkIdByOnlyId(onlyId));
	}
	/**
	 * @描述 删除数据来自令牌与唯一ID
	 * @param tkId 令牌与唯一ID
	 */
	public void deleteByTkId(String tkId) {
		if (tkId != null) {$delete(tkId);}
	}
	
	/* 验证过期 */
	private String verifyExpire(String tkId, Long recordTime, long nowTime) {
		if (recordTime == null) {return null;} //无时间等于无数据
		boolean expire = (nowTime - recordTime) > remoteConfigDto.getTokenExpireTime(); //是否过期
		if (expire) {deleteByTkId(tkId); return null;} else {return tkId;}
	}
	/* 移除令牌数据与过期时间 */
	private void $delete(String removeTkId) {
		if (!CollectionUtils.isEmpty(redisTokenRemoves)) { //回调自定义移除通知接口
			for (RedisTokenRemove redisTokenRemove : redisTokenRemoves) {
				redisTokenRemove.remove(removeTkId, getByTkId(removeTkId, Object.class));
			}
		}
		/* 删除缓存 */
		redisMake.delHKey(CommonConstant.REDIS_TOKEN_HKEY + SPLIT_DATA + ONLY_ID, Arrays.asList(removeTkId));
		redisMake.delHKey(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, Arrays.asList(removeTkId));
	}
}
