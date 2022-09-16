package org.city.common.api.in.redis;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.DataList;
import org.city.common.api.dto.RedisDto;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.dto.sql.BaseDto;
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
				redisMake.tryLock(CommonConstant.REDIS_TOKEN_HKEY + SPLIT + ONLY_ID, remoteConfigDto.getReadTimeout(), true, new Supplier<String>() {
					@Override
					public String get() {
						Long size = redisMake.getRedisTemplate().opsForHash().size(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID);
						new BaseDto().setPageSize((long) Short.MAX_VALUE).limitHandler(size.longValue(), new Limit() {
							@Override
							public void handler(BaseDto baseDto) {
								DataList<Entry<Object, Object>> entry = redisMake.entry(RedisDto.of(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, "*", baseDto.getPageNum(), baseDto.getPageSize()));
								verifyExpire(entry.getRows(), false); //验证过期操作 - 过期数据会自动删除
							}
						});
						return null;
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
		String token = MyUtil.md5(JSON.toJSONString(data) + System.currentTimeMillis() + remoteConfigDto.getVerify());
		/* 删除重复的其他令牌数据 */
		deleteByToken(token); deleteByOnlyId(onlyId);
		
		/* 记录过期时间与令牌数据 */
		redisMake.setHValue(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, token + SPLIT + onlyId, System.currentTimeMillis());
		redisMake.setHValue(CommonConstant.REDIS_TOKEN_HKEY + SPLIT_DATA + ONLY_ID, token + SPLIT + onlyId, data);
		return token;
	}
	
	/**
	 * @描述 获取令牌与唯一ID来自令牌
	 * @param token 令牌
	 * @return 令牌与唯一ID
	 */
	public String getTkIdByToken(String token) {
		return redisMake.tryLock(CommonConstant.REDIS_TOKEN_HKEY + SPLIT + ONLY_ID, remoteConfigDto.getReadTimeout(), true, new Supplier<String>() {
			@Override
			public String get() {
				DataList<Entry<Object, Object>> entry = redisMake.entry(RedisDto.of(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, token + SPLIT + "*", 1, Short.MAX_VALUE));
				return verifyExpire(entry.getRows(), true); //验证过期操作 - 过期数据会自动删除 - 多余数据也会删除
			}
		});
	}
	/**
	 * @描述 获取令牌与唯一ID来自唯一ID
	 * @param onlyId 唯一ID
	 * @return 令牌与唯一ID
	 */
	public String getTkIdByOnlyId(String onlyId) {
		return redisMake.tryLock(CommonConstant.REDIS_TOKEN_HKEY + SPLIT + ONLY_ID, remoteConfigDto.getReadTimeout(), true, new Supplier<String>() {
			@Override
			public String get() {
				DataList<Entry<Object, Object>> entry = redisMake.entry(RedisDto.of(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, "*" + SPLIT + onlyId, 1, Short.MAX_VALUE));
				return verifyExpire(entry.getRows(), true); //验证过期操作 - 过期数据会自动删除 - 多余数据也会删除
			}
		});
	}
	
	/**
	 * @描述 获取数据来自令牌
	 * @param <T> 数据类型
	 * @param token 令牌
	 * @param type 数据类型
	 * @return 数据
	 */
	public <T> T getByToken(String token, Type type) {
		return get(getTkIdByToken(token), type);
	}
	/**
	 * @描述 获取数据来自唯一ID
	 * @param <T> 数据类型
	 * @param onlyId 唯一ID
	 * @param type 数据类型
	 * @return 数据
	 */
	public <T> T getByOnlyId(String onlyId, Type type) {
		return get(getTkIdByOnlyId(onlyId), type);
	}
	/* 统一获取数据 */
	private <T> T get(String hKey, Type type) {
		if (hKey == null) {return null;}
		return redisMake.getHValue(CommonConstant.REDIS_TOKEN_HKEY + SPLIT_DATA + ONLY_ID, hKey, type);
	}
	
	/**
	 * @描述 获取运行时间来自令牌
	 * @param token 令牌
	 * @return 运行时间（毫秒）
	 */
	public long getRunTimeByToken(String token) {
		return getRunTime(getTkIdByToken(token));
	}
	/**
	 * @描述 获取运行时间来自唯一ID
	 * @param onlyId 唯一ID
	 * @return 运行时间（毫秒）
	 */
	public long getRunTimeByOnlyId(String onlyId) {
		return getRunTime(getTkIdByOnlyId(onlyId));
	}
	/* 统一获取运行时间 */
	private long getRunTime(String hKey) {
		if (hKey == null) {return -1L;}
		long recordTime = redisMake.getHValue(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, hKey, long.class);
		return System.currentTimeMillis() - recordTime;
	}
	
	/**
	 * @描述 刷新过期时间来自令牌
	 * @param token 令牌
	 */
	public void flushByToken(String token) {
		flush(getTkIdByToken(token));
	}
	/**
	 * @描述 刷新过期时间来自唯一ID
	 * @param onlyId 唯一ID
	 */
	public void flushByOnlyId(String onlyId) {
		flush(getTkIdByOnlyId(onlyId));
	}
	/* 统一刷新时间 */
	private void flush(String hKey) {
		if (hKey != null) {redisMake.setHValue(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, hKey, System.currentTimeMillis());}
	}
	
	/**
	 * @描述 重新设置数据来自令牌
	 * @param token 令牌
	 * @param data 数据
	 */
	public void setByToken(String token, Object data) {
		set(getTkIdByToken(token), data);
	}
	/**
	 * @描述 重新设置数据来自唯一ID
	 * @param onlyId 唯一ID
	 * @param data 数据
	 */
	public void setByOnlyId(String onlyId, Object data) {
		set(getTkIdByOnlyId(onlyId), data);
	}
	/* 统一设置数据 */
	private void set(String hKey, Object data) {
		if (hKey != null) {redisMake.setHValue(CommonConstant.REDIS_TOKEN_HKEY + SPLIT_DATA + ONLY_ID, hKey, data);}
	}
	
	/**
	 * @描述 删除数据来自令牌
	 * @param token 令牌
	 */
	public void deleteByToken(String token) {
		delete(getTkIdByToken(token));
	}
	/**
	 * @描述 删除数据来自唯一ID
	 * @param onlyId 唯一ID
	 */
	public void deleteByOnlyId(String onlyId) {
		delete(getTkIdByOnlyId(onlyId));
	}
	/* 统一删除 */
	private void delete(String hKey) {
		if (hKey != null) {deletes(Arrays.asList(hKey));}
	}
	
	/* 验证过期操作 */
	private String verifyExpire(List<Entry<Object, Object>> entries, boolean isReturnValue) {
		String hKey = null; //待返回的HKey
		List<String> removes = new ArrayList<>(); //待移除列表
		Iterator<Entry<Object, Object>> kvs = entries.iterator();
		
		/* 迭代所有KeyValue数据并判断是否过期操作 */
		while(kvs.hasNext()) {
			Entry<Object, Object> kv = kvs.next();
			long recordTime = parse(kv.getValue(), long.class);
			if ((System.currentTimeMillis() - recordTime) > remoteConfigDto.getTokenExpireTime()) {removes.add(kv.getKey().toString()); kvs.remove();}
		}
		
		/* 如果需要返回值 - 则保留一个未过期的HKey用于返回 */
		if (isReturnValue) {
			List<String> lastHKeys = entries.stream().map(v -> v.getKey().toString()).collect(Collectors.toList());
			Iterator<String> hKyes = lastHKeys.iterator();
			if (hKyes.hasNext()) {
				hKey = hKyes.next(); hKyes.remove(); //保留一个未过期的HKey用于返回
				if (hKyes.hasNext()) {removes.addAll(lastHKeys);} //删除未过期的其他HKey
			} 
		}
		
		/* 移除令牌数据与过期时间 */
		deletes(removes);
		return hKey;
	}
	/* 移除令牌数据与过期时间 */
	private void deletes(List<String> removes) {
		if (removes.size() > 0) {
			if (!CollectionUtils.isEmpty(redisTokenRemoves)) { //回调自定义移除通知接口
				Map<String, Object> rms = new LinkedHashMap<>(removes.size());
				for (String remove : removes) {
					rms.put(remove, get(remove, Object.class));
				}
				for (RedisTokenRemove redisTokenRemove : redisTokenRemoves) {
					redisTokenRemove.remove(rms);
				}
			}
			
			redisMake.delHKey(CommonConstant.REDIS_TOKEN_HKEY + SPLIT_DATA + ONLY_ID, removes);
			redisMake.delHKey(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY + SPLIT_DATA + ONLY_ID, removes);
		}
	}
}
