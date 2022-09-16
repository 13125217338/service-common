package org.city.common.core.config;

import org.city.common.api.dto.remote.RemoteConfigDto;
import org.city.common.core.serializer.FastJsonRedisSerializer;
import org.redisson.Redisson;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @作者 ChengShi
 * @日期 2022年7月24日
 * @版本 1.0
 * @描述 缓存模板配置
 */
@Configuration
public class RedisTemplateConfig {
	@Autowired
	private RemoteConfigDto remoteConfigDto;
	
	@Bean
	public RedisTemplate<String, Object> redisTempalte(RedissonConnectionFactory redissonConnectionFactory, Redisson redisson) {
		/* 重新设置读取时间与连接时间 */
		redisson.getConnectionManager().getConfig().setTimeout(remoteConfigDto.getReadTimeout());
		redisson.getConnectionManager().getConfig().setConnectTimeout(remoteConfigDto.getConnectTimeout());
		
		/* 键值序列化方式 */
		StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
		FastJsonRedisSerializer fastJsonRedisSerializer = new FastJsonRedisSerializer();
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		
		/* 配置序列化方式 */
		redisTemplate.setKeySerializer(stringRedisSerializer);
		redisTemplate.setValueSerializer(fastJsonRedisSerializer);
		redisTemplate.setHashKeySerializer(stringRedisSerializer);
		redisTemplate.setHashValueSerializer(fastJsonRedisSerializer);
		redisTemplate.setConnectionFactory(redissonConnectionFactory);
		/* 自定义 */
		return redisTemplate;
	}
}
