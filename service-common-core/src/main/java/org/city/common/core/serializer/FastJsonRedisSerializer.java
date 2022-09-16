package org.city.common.core.serializer;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * @作者 ChengShi
 * @日期 2023-02-10 17:47:54
 * @版本 1.0
 * @描述 FastJson序列化（只有String类型序列化与反序列化是不变的）
 */
public class FastJsonRedisSerializer implements RedisSerializer<Object> {
	@Override
	public byte[] serialize(Object t) throws SerializationException {
		return t == null ? null : JSON.toJSONBytes(t, SerializerFeature.SortField);
	}
	@Override
	public Object deserialize(byte[] bytes) throws SerializationException {
		return bytes == null ? null : JSON.parse(bytes, Feature.OrderedField);
	}
}
