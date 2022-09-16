package org.city.common.api.dto;

import java.util.List;

import org.city.common.api.exception.AuthNotPassException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2022-06-20 15:40:24
 * @版本 1.0
 * @描述 验证结果
 */
@Data
@Accessors(chain = true)
public class AuthResultDto {
	/* 是否验证成功 */
	private boolean isSuccess;
	/* 不成功时异常 */
	private AuthNotPassException authNotPassException;
	
	/**
	 * @描述 验证成功
	 * @return 成功结果
	 */
	public static AuthResultDto ok() {
		return new AuthResultDto().setSuccess(true);
	}
	
	/**
	 * @描述 验证失败
	 * @param authNotPassException 失败异常
	 * @return 失败结果
	 */
	public static AuthResultDto error(AuthNotPassException authNotPassException) {
		return new AuthResultDto().setAuthNotPassException(authNotPassException);
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2023年5月6日
	 * @版本 1.0
	 * @描述 验证方法
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Accessors(chain = true)
	public static class AuthMethod {
		/* 方法对应类名 */
		private String className;
		/* 方法名 */
		private String name;
		/* 参数类型（泛型名称） */
		private String[] parameterType;
		/* 返回类型（泛型名称） */
		private String returnType;
		/* 验证参数 */
		private List<Auth> auths;
		/* true=抛出自定义异常，false=不抛出异常 */
		private boolean isThrow;
		
		@Override
		public String toString() {
			return String.format("%s %s.%s(%s)", returnType, className, name, String.join(", ", parameterType));
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null) {return false;}
			return this.toString().equals(obj.toString());
		}
		
		/**
		 * @作者 ChengShi
		 * @日期 2023年5月7日
		 * @版本 1.0
		 * @描述 验证参数
		 */
		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		public static class Auth {
			/* 验证回调对应@AuthFilter中的id值 */
			private String id;
			/* 自定义参数 */
			private String[] values;
		}
	}
}
