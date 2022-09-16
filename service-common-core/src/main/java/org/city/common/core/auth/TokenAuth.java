package org.city.common.core.auth;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeoutException;

/**
 * @作者 ChengShi
 * @日期 2021-08-12 09:50:14
 * @版本 1.0
 * @描述 加密token令牌
 */
public class TokenAuth<T>{
	/* 盐令牌 */
	private final byte[] token;
	/* 序列化实现的类 */
	private final Serialize<T> serialize;
	/* 常量 */
	private final String MH = ":";
	/* 过期时间 */
	private final long timeout;
	
	/**
	 * @param token 盐
	 * @param timeout 过期时间（毫秒）
	 * @param serialize 序列化方式
	 */
	public TokenAuth(byte[] token, long timeout, Serialize<T> serialize) {this.token = token; this.timeout = timeout; this.serialize = serialize;}
	/**
	 * @描述 获取盐令牌
	 * @return 盐令牌
	 */
	public byte[] getToken() {return token;}
	/**
	 * @描述 获取过期时间
	 * @return 过期时间（毫秒）
	 */
	public long getTimeout() {return this.timeout;}
	
	/*加密验证数据*/
	private String encode(byte[] xlh, String type) throws NoSuchAlgorithmException{
		MessageDigest instance = MessageDigest.getInstance(type); 
		instance.update(xlh);instance.update(token);
		return Base64.getEncoder().encodeToString(instance.digest()) ;
	}
	
	/**
	 * @描述 获取加密数据串
	 * @param data 待加密数据
	 * @return 加密后的数据串
	 */
	public String getToken(Token<T> data){
		try {
			if (data == null) {throw new RuntimeException("请先设置数据在加密！");}
			/*序列化数据*/
			byte[] xlh = serialize.xlh(data);
			/*原数据base64*/
			String val = Base64.getEncoder().encodeToString(xlh);
			/*加密数据验证*/
			String encode = encode(xlh, data.type);
			return val + MH + encode;
		} catch (Exception e) {throw new RuntimeException("加密失败！" + e.getMessage(), e);}
	}
	
	/**
	 * @描述 获取加密数据
	 * @param token 原加密数据串
	 * @param msg 验证不通过错误信息
	 * @return 加密数据
	 */
	public T getData(String token, String msg){
		try {
			if (token == null) {throw new RuntimeException(msg);}
			String[] vals = token.split(MH);
			/*原序列化数据*/
			byte[] decode = Base64.getDecoder().decode(vals[0]);
			/*反序列化数据*/
			Token<T> fxl = serialize.fxl(decode);
			/*超时判断*/
			if ((System.currentTimeMillis() - fxl.recordTime) > this.timeout) {throw new TimeoutException("令牌已超时，请重新获取！");}
			/*如果验证成功*/
			if (auth(vals[1], decode, fxl.type)) {return fxl.data;} else {throw new RuntimeException(msg);}
		} catch (Exception e) {throw new RuntimeException("解析出错！" + e.getMessage(), e);}
	}
	/*验证*/
	private boolean auth(String token, byte[] xlh, String type) {
		try {
			/*加密数据验证*/
			return encode(xlh, type).equals(token);
		} catch (Exception e) {return false;}
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2021-08-16 11:35:08
	 * @版本 1.0
	 * @parentClass IceToken
	 * @描述 序列化对象
	 */
	public static interface Serialize<T>{
		/** 序列化对象 */
		public byte[] xlh(Token<T> data) throws Exception;
		/** 反序列化对象 */
		public Token<T> fxl(byte[] vals) throws Exception;
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2021-08-12 10:44:28
	 * @版本 1.0
	 * @parentClass IceToken
	 * @描述 数据集（用于自定义序列化）
	 */
	public final static class Token<T> implements Serializable{
		private static final long serialVersionUID = 20200101;
		private String type;
		private long recordTime = System.currentTimeMillis();
		private T data;
		/**
		 * @param type 加密类型
		 * @param data 待加密数据
		 */
		public Token(String type, T data) {this.type = type;this.data = data;}
		public Token() {}
		/*获取*/
		public String getType() {return type;}
		public Object getData() {return data;}
		/*设置*/
		public void setType(String type) {this.type = type;}
		public void setData(T data) {this.data = data;}
	}
}
