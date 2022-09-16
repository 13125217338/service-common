package org.city.common.api.in.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

/**
 * @作者 ChengShi
 * @日期 2022-10-08 16:11:25
 * @版本 1.0
 * @描述 验证参数
 */
public interface Validations {
	public final static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
	
	/**
	 * @描述 验证参数
	 * @param <T> 待验证数据类型
	 * @param data 待验证数据
	 * @param groups 验证分组
	 * @return 待验证数据
	 */
	default <T> T verify(T data, Class<?>...groups) {
		if (data == null) {throw new RuntimeException("待验证数据不能为空！");}
		if (data instanceof Collection) { //集合验证
			for (Object dt : (Collection<?>) data) {$verify(dt, groups);}
		} else if (data.getClass().isArray()) { //数组验证
			for (int i = 0, j = Array.getLength(data); i < j; i++) {$verify(Array.get(data, i), groups);}
		} else {$verify(data, groups);} //单个验证
		return data;
	}
	/* 对象验证 */
	private void $verify(Object data, Class<?>...groups) {
		Set<ConstraintViolation<Object>> validate = validator.validate(data, groups);
		for (ConstraintViolation<Object> constraintViolation : validate) {
			throw new RuntimeException(constraintViolation.getMessage());
		}
	}
}
