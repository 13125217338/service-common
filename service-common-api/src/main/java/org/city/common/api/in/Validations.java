package org.city.common.api.in;

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
	 * @param data 待验证数据
	 * @param groups 验证分组
	 */
	default void verify(Object data, Class<?>...groups) {
		Set<ConstraintViolation<Object>> validate = validator.validate(data, groups);
		for (ConstraintViolation<Object> constraintViolation : validate) {
			throw new RuntimeException(constraintViolation.getMessage());
		}
	}
}
