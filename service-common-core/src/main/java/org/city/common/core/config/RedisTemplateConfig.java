package org.city.common.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;

/**
 * @作者 ChengShi
 * @日期 2022年7月24日
 * @版本 1.0
 * @描述 RedisTemplate配置
 */
@Configuration
public class RedisTemplateConfig {
	@Bean
	public RedisTemplate<String, Object> redisTempalte(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		/* 键值序列化方式 */
		StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
		FastJsonRedisSerializer<Object> fastJsonRedisSerializer = new FastJsonRedisSerializer<>(Object.class);
		
		/* FastJson序列化配置 */
		FastJsonConfig fastJsonConfig = fastJsonRedisSerializer.getFastJsonConfig();
		fastJsonConfig.setSerializerFeatures(SerializerFeature.WriteClassName); //反序列化自动转换类型
		fastJsonConfig.setFeatures(Feature.SupportAutoType);
		fastJsonConfig.setDateFormat("yyyy-MM-dd HH:mm:ss");
		
		/* 配置序列化方式 */
		redisTemplate.setKeySerializer(stringRedisSerializer);
		redisTemplate.setValueSerializer(fastJsonRedisSerializer);
		redisTemplate.setHashKeySerializer(stringRedisSerializer);
		redisTemplate.setHashValueSerializer(fastJsonRedisSerializer);
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		/* 自定义 */
		return redisTemplate;
	}
}
