package org.city.common.api.in.redis;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.city.common.api.dto.DataList;
import org.city.common.api.dto.RedisDto;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @作者 ChengShi
 * @日期 2023年5月9日
 * @版本 1.0
 * @描述 缓存操作
 */
public interface RedisMake {
	/**
	 * @描述 分页获取键
	 * @param redisDto Redis参数
	 * @return 分页键
	 */
	public DataList<String> keys(RedisDto redisDto);
	/**
	 * @描述 判断是否有该key
	 * @param key 键
	 * @return true=有
	 */
	public boolean hasKey(String key);
	/**
	 * @描述 判断是否有该Hkey
	 * @param key 键
	 * @param hKey H键
	 * @return true=有
	 */
	public boolean hasHKey(String key, String hKey);
	/**
	 * @描述 删除key
	 * @param key 键
	 */
	public void delKey(String key);
	
	/**
	 * @描述 获取值（所有类型的key）
	 * @param key 键
	 * @return 值（全部取出，注意内存问题）
	 */
	public Object get(String key);
	
	/**
	 * @描述 获取值对象
	 * @param <T> 待转换类型
	 * @param key 键
	 * @param type 值类型
	 * @return 值对象
	 */
	public <T> T getValue(String key, Type type);
	/**
	 * @描述 设置值
	 * @param key 键
	 * @param value 值
	 */
	public void setValue(String key, Object value);
	/**
	 * @描述 设置值
	 * @param key 键
	 * @param value 值
	 * @param timeout 过期时间
	 * @param timeUnit 时间单元
	 */
	public void setValue(String key, Object value, long timeout, TimeUnit timeUnit);
	
	/**
	 * @描述 获取Hash值对象
	 * @param <T> 待转换类型
	 * @param key 键
	 * @param hKey Hash键
	 * @param type 值类型
	 * @return 值对象
	 */
	public <T> T getHValue(String key, String hKey, Type type);
	/**
	 * @描述 迭代Hash对象
	 * @param key 键
	 * @return Hash对象
	 */
	public Map<String, Object> entry(String key);
	/**
	 * @描述 分页获取Hash值
	 * @param redisDto Redis参数
	 * @return 分页值
	 */
	public DataList<Entry<Object, Object>> entry(RedisDto redisDto);
	/**
	 * @描述 设置Hash值
	 * @param key 键
	 * @param hKey Hash键
	 * @param value 值
	 */
	public void setHValue(String key, String hKey, Object value);
	/**
	 * @描述 设置Hash值
	 * @param key 键
	 * @param hKey Hash键
	 * @param value 值
	 * @param timeout 过期时间
	 * @param timeUnit 时间单元
	 */
	public void setHValue(String key, String hKey, Object value, long timeout, TimeUnit timeUnit);
	/**
	 * @描述 删除多个Hash键
	 * @param key 键
	 * @param hKeys 多个Hash键
	 */
	public void delHKey(String key, List<String> hKeys);
	
	/**
	 * @描述 分页获取List值
	 * @param redisDto Redis参数
	 * @return 分页值
	 */
	public DataList<String> getList(RedisDto redisDto);
	/**
	 * @描述 获取左List值
	 * @param key 键
	 * @return 左List值
	 */
	public String leftPop(String key);
	/**
	 * @描述 获取右List值
	 * @param key 键
	 * @return 右List值
	 */
	public String rightPop(String key);
	/**
	 * @描述 添加List值
	 * @param key 键
	 * @param values 多个值
	 */
	public void addList(String key, List<String> values);
	/**
	 * @描述 添加List值
	 * @param key 键
	 * @param timeout 过期时间
	 * @param timeUnit 时间单元
	 * @param values 多个值
	 */
	public void addList(String key, long timeout, TimeUnit timeUnit, List<String> values);
	/**
	 * @描述 修改List值
	 * @param key 键
	 * @param index 下标
	 * @param value List值
	 */
	public void setList(String key, long index, String value);
	/**
	 * @描述 删除相同List值
	 * @param key 键
	 * @param count 指定数量
	 * @param value 相同List值
	 */
	public void delList(String key, long count, String value);
	
	/**
	 * @描述 分页获取Set值
	 * @param redisDto Redis参数
	 * @return 分页值
	 */
	public DataList<String> getSet(RedisDto redisDto);
	/**
	 * @描述 添加Set值
	 * @param key 键
	 * @param values 多个值
	 */
	public void addSet(String key, List<String> values);
	/**
	 * @描述 添加Set值
	 * @param key 键
	 * @param timeout 过期时间
	 * @param timeUnit 时间单元
	 * @param values 多个值
	 */
	public void addSet(String key, long timeout, TimeUnit timeUnit, List<String> values);
	/**
	 * @描述 删除多个Set值
	 * @param key 键
	 * @param values 多个Set值
	 */
	public void delSet(String key, List<String> values);
	
	/**
	 * @描述 分页获取ZSet值
	 * @param redisDto Redis参数
	 * @return 分页值
	 */
	public DataList<String> getZSet(RedisDto redisDto);
	/**
	 * @描述 获取分数
	 * @param key 键
	 * @param value 值
	 * @return 分数
	 */
	public double getScore(String key, String value);
	/**
	 * @描述 添加ZSet值
	 * @param key 键
	 * @param value 值
	 * @param score 顺序分数
	 */
	public void addZSet(String key, String value, double score);
	/**
	 * @描述 添加ZSet值
	 * @param key 键
	 * @param value 值
	 * @param score 顺序分数
	 * @param timeout 过期时间
	 * @param timeUnit 时间单元
	 */
	public void addZSet(String key, String value, double score, long timeout, TimeUnit timeUnit);
	/**
	 * @描述 删除多个ZSet值
	 * @param key 键
	 * @param values 多个ZSet值
	 */
	public void delZSet(String key, List<String> values);
	
	/**
	 * @描述 设置key过期时间
	 * @param key 键
	 * @param timeout 过期时间
	 * @param timeUnit 时间单元
	 * @return true=设置成功
	 */
	public boolean expire(String key, long timeout, TimeUnit timeUnit);
	/**
	 * @描述 自增key数值
	 * @param key 键
	 * @param delta 待自增数值
	 * @return 自增后的数值
	 */
	public long increment(String key, long delta);
	/**
	 * @描述 自减key数值
	 * @param key 键
	 * @param delta 待自减数值
	 * @return 自减后的数值
	 */
	public long decrement(String key, long delta);
	
	/**
	 * @描述 获取Redis模板
	 * @return Redis模板
	 */
	public RedisTemplate<String, Object> getRedisTemplate();
	
	/**
	 * @描述 获取当前时间
	 * @return 当前时间（毫秒）
	 */
	public long getNowTime();
	
	/**
	 * @描述 分布式加锁执行方法（该方法适用短时间持有锁的业务逻辑）
	 * @param <T> 返回值类型
	 * @param key 锁键
	 * @param timeout 同步锁过期时间，请不要设置太长（毫秒）
	 * @param mini 循环等待获取锁周期，true=20-100ms，false=200-1000ms
	 * @param run 获得锁后执行的方法
	 * @return 执行方法的返回值（未获取到锁会抛出异常）
	 */
	public <T> T tryLock(String key, int timeout, boolean mini, Supplier<T> run);
}
