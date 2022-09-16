package org.city.common.core.auth;

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

import javax.annotation.PostConstruct;

import org.city.common.api.constant.CommonConstant;
import org.city.common.api.dto.DataList;
import org.city.common.api.dto.RedisDto;
import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.api.in.Runnable;
import org.city.common.api.in.Task;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.redis.RedisMake;
import org.city.common.api.in.redis.RedisTokenMake;
import org.city.common.api.in.redis.RedisTokenRemove;
import org.city.common.api.util.MyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;

/**
 * @作者 ChengShi
 * @日期 2023年6月18日
 * @版本 1.0
 * @描述 缓存令牌操作验证
 */
@Component
public class RedisTokenMakeAuth implements RedisTokenMake,JSONParser {
	private final String SPLIT = "_"; //令牌与唯一ID分割线
	@Autowired
	private RedisMake redisMake;
	@Autowired
	private Task task;
	@Autowired
	private RemoteConfigDto remoteConfigDto;
	@Autowired(required = false)
	private RedisTokenRemove redisTokenRemove;
	@PostConstruct
	private void init() {
		task.schedula(CommonConstant.REDIS_TOKEN_TIME_EXPIRE_ID, new Runnable() {
			@Override
			public void run() throws Throwable {
				redisMake.tryLock(CommonConstant.REDIS_TOKEN_HKEY + SPLIT, remoteConfigDto.getReadTimeout(), true, new Supplier<String>() {
					@Override
					public String get() {
						DataList<Entry<Object, Object>> entry = redisMake.entry(RedisDto.of(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY, "*", 1, Short.MAX_VALUE));
						return verifyExpire(entry.getRows(), false); //验证过期操作 - 过期数据会自动删除
					}
				});
			}
		}, () -> String.valueOf(remoteConfigDto.getTokenExpireTime() >> 2), false);
	}
	
	@Override
	public String getToken(String onlyId, Object data) {
		/* 生成令牌 */
		String token = MyUtil.md5(JSON.toJSONString(data) + System.currentTimeMillis() + remoteConfigDto.getVerify());
		/* 删除重复的其他令牌数据 */
		deleteByToken(token); deleteByOnlyId(onlyId);
		
		/* 记录过期时间与令牌数据 */
		redisMake.setHValue(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY, token + SPLIT + onlyId, System.currentTimeMillis());
		redisMake.setHValue(CommonConstant.REDIS_TOKEN_HKEY, token + SPLIT + onlyId, data);
		return token;
	}
	
	@Override
	public String getTkIdByToken(String token) {
		return redisMake.tryLock(CommonConstant.REDIS_TOKEN_HKEY + SPLIT, remoteConfigDto.getReadTimeout(), true, new Supplier<String>() {
			@Override
			public String get() {
				DataList<Entry<Object, Object>> entry = redisMake.entry(RedisDto.of(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY, token + SPLIT + "*", 1, Short.MAX_VALUE));
				return verifyExpire(entry.getRows(), true); //验证过期操作 - 过期数据会自动删除 - 多余数据也会删除
			}
		});
	}
	
	@Override
	public String getTkIdByOnlyId(String onlyId) {
		return redisMake.tryLock(CommonConstant.REDIS_TOKEN_HKEY + SPLIT, remoteConfigDto.getReadTimeout(), true, new Supplier<String>() {
			@Override
			public String get() {
				DataList<Entry<Object, Object>> entry = redisMake.entry(RedisDto.of(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY, "*" + SPLIT + onlyId, 1, Short.MAX_VALUE));
				return verifyExpire(entry.getRows(), true); //验证过期操作 - 过期数据会自动删除 - 多余数据也会删除
			}
		});
	}
	
	@Override
	public <T> T getByToken(String token, Type type) {
		return get(getTkIdByToken(token), type);
	}
	@Override
	public <T> T getByOnlyId(String onlyId, Type type) {
		return get(getTkIdByOnlyId(onlyId), type);
	}
	/* 统一获取数据 */
	private <T> T get(String hKey, Type type) {
		if (hKey == null) {return null;}
		return redisMake.getHValue(CommonConstant.REDIS_TOKEN_HKEY, hKey, type);
	}
	
	@Override
	public long getRunTimeByToken(String token) {
		return getRunTime(getTkIdByToken(token));
	}
	@Override
	public long getRunTimeByOnlyId(String onlyId) {
		return getRunTime(getTkIdByOnlyId(onlyId));
	}
	/* 统一获取运行时间 */
	private long getRunTime(String hKey) {
		if (hKey == null) {return -1L;}
		long recordTime = redisMake.getHValue(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY, hKey, long.class);
		return System.currentTimeMillis() - recordTime;
	}
	
	@Override
	public void flushByToken(String token) {
		flush(getTkIdByToken(token));
	}
	@Override
	public void flushByOnlyId(String onlyId) {
		flush(getTkIdByOnlyId(onlyId));
	}
	/* 统一刷新时间 */
	private void flush(String hKey) {
		if (hKey != null) {redisMake.setHValue(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY, hKey, System.currentTimeMillis());}
	}
	
	@Override
	public void setByToken(String token, Object data) {
		set(getTkIdByToken(token), data);
	}
	@Override
	public void setByOnlyId(String onlyId, Object data) {
		set(getTkIdByOnlyId(onlyId), data);
	}
	/* 统一设置数据 */
	private void set(String hKey, Object data) {
		if (hKey != null) {redisMake.setHValue(CommonConstant.REDIS_TOKEN_HKEY, hKey, data);}
	}
	
	@Override
	public void deleteByToken(String token) {
		delete(getTkIdByToken(token));
	}
	@Override
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
			if (redisTokenRemove != null) { //令牌移除回调事件处理
				Map<String, Object> rms = new LinkedHashMap<>(removes.size());
				for (String remove : removes) {
					rms.put(remove, redisMake.getHValue(CommonConstant.REDIS_TOKEN_HKEY, remove, Object.class));
				}
				redisTokenRemove.remove(rms);
			}
			
			redisMake.delHKey(CommonConstant.REDIS_TOKEN_HKEY, removes);
			redisMake.delHKey(CommonConstant.REDIS_TOKEN_EXPIRE_TIME_HKEY, removes);
		}
	}
}
