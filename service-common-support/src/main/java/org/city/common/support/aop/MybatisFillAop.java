package org.city.common.support.aop;

import java.sql.Timestamp;
import java.time.Instant;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

/**
 * @作者 ChengShi
 * @日期 2024-02-21 12:15:22
 * @版本 1.0
 * @描述 自动填充拦截
 */
@Component
public class MybatisFillAop implements MetaObjectHandler {
	
	@Override
	public void insertFill(MetaObject metaObject) {
		Timestamp nowTime = Timestamp.from(Instant.now());
		if (metaObject.hasSetter("createTime") && metaObject.getSetterType("createTime") == Timestamp.class) {
	        this.setFieldValByName("createTime", nowTime, metaObject);
	    }
		if (metaObject.hasSetter("updateTime") && metaObject.getSetterType("updateTime") == Timestamp.class) {
	        this.setFieldValByName("updateTime", nowTime, metaObject);
	    }
	}
	
	@Override
	public void updateFill(MetaObject metaObject) {
		Timestamp nowTime = Timestamp.from(Instant.now());
		if (metaObject.hasSetter("updateTime") && metaObject.getSetterType("updateTime") == Timestamp.class) {
	        this.setFieldValByName("updateTime", nowTime, metaObject);
	    }
	}
}
