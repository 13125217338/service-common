package org.city.common.api.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.util.StringUtils;

/**
 * @作者 ChengShi
 * @日期 2020-07-23 09:51:13
 * @版本 1.0
 * @描述 我的工具类
 */
public final class MyUtil {
	private final static String MEMBER_VALUES = "memberValues";
	private final static String AES = "AES", MD5 = "MD5", SHA256 = "SHA-256";
	private final static char ZERO = '0';
	private MyUtil() {}
	
	/**
	 * @描述 获取原字符串在索引字符串出现过多少次
	 * @param val 原字符
	 * @param index 索引字符
	 * @return 出现过多少次
	 */
	public static int indexSum(String val, String index) {
		int sum = 0;
		if (StringUtils.hasText(val) && StringUtils.hasText(index)) {
			int cur = 0;
			while((cur = val.indexOf(index, cur)) != -1){cur += index.length(); sum++;}
		}
		return sum;
	}
	
	/**
	 * @描述 获取包含值，如字符串：test1${val}test2，start填'${'，end填'}'，那么结果对象键为${val}，值为val
	 * @param start 前缀包含条件字符串
	 * @param end 后缀包含条件字符串
	 * @param value 原待解析字符串
	 * @return MyMap对象，key为带条件的字符串，value为条件中的字符串（不会为NULL）
	 */
	public static Map<String, String> getContainValue(String start, String end, String value) {
		Map<String, String> vals = new LinkedHashMap<String, String>();
		if (StringUtils.hasText(start) && StringUtils.hasText(end) && StringUtils.hasText(value)) {
			int index = 0;
			while ((index = value.indexOf(start, index)) != -1) {
				int indexOf = value.indexOf(end, index + start.length());
				if (indexOf == -1) {break;}
				vals.put(value.substring(index, indexOf + end.length()), value.substring(index + start.length(), indexOf));
				index = indexOf + end.length();
			}
		}
		return vals;
	}
	
	/**
	 * @描述 替换连续字符，保留一个字符
	 * @param value 原值
	 * @param replaceStr 待替换的连续字符
	 * @return 替换连续字符为一个的原值
	 */
	public static String replaceAllStr(String value, String replaceStr) {
		if (value == null || replaceStr == null) {return null;}
		String reString = replaceStr + replaceStr;
		return replaceStr(value, replaceStr, reString);
	}
	/* 递归替换 */
	private static String replaceStr(String value, String replaceStr, String reString) {
		if (value.contains(reString)) {
			value = value.replace(reString, replaceStr);
			value = replaceStr(value, replaceStr, reString);
		}
		return value;
	}
	
	/**
	 * @描述 获取数据中所有为NULL的字段名
	 * @param data 待获取数据
	 * @return 为NULL的字段名
	 */
	public static String[] getNullName(Object data) {
		if (data == null) {return new String[0];}
		Set<String> collect = FieldUtil.getAllDeclaredField(data.getClass(), true).values().stream().filter(v -> {
			try {return v.get(data) == null;} catch (Exception e) {return false;}
		}).map(v -> v.getName()).collect(Collectors.toSet());
		return collect.toArray(new String[collect.size()]);
	}
	
	/**
	 * @描述 判断任意前缀是否相同
	 * @param value 待判断字符
	 * @param ignore 是否忽略大小写
	 * @param prifixs 前缀
	 * @return 判断结果
	 */
	public static boolean startWithAny(String value, boolean ignore, String...prifixs) {
		if (value == null || prifixs == null) {return false;}
		if (ignore) {
			value = value.toLowerCase();
			for (String prifix : prifixs) {
				 if (value.startsWith(prifix.toLowerCase())) {return true;}
			}
		} else {
			for (String prifix : prifixs) {
				 if (value.startsWith(prifix)) {return true;}
			}
		}
		return false;
	}
	
	/**
	 * @描述 重新设置注解值
	 * @param annotation 注解对象
	 * @param fieldName 字段
	 * @param value 值
	 */
	@SuppressWarnings("all")
	public static void setAnnotationValue(Annotation annotation, String fieldName, Object value) {
		try {
			if (annotation == null || fieldName == null || value == null) {return;}
			InvocationHandler handler = Proxy.getInvocationHandler(annotation);
		    Field field = handler.getClass().getDeclaredField(MEMBER_VALUES);
		    field.setAccessible(true);
		    Map memberValues = (Map) field.get(handler);
		    memberValues.put(fieldName, value);
		} catch (Exception e) {throw new RuntimeException("重新设置注解值出错！", e);}
	}
	
	/**
	 * @描述 获取注解所有值
	 * @param annotation 注解对象
	 * @return 注解所有值
	 */
	@SuppressWarnings("all")
	public static Map<String, Object> getAnnotationVal(Annotation annotation) {
		try {
			InvocationHandler handler = Proxy.getInvocationHandler(annotation);
			Field field = handler.getClass().getDeclaredField(MEMBER_VALUES);
			field.setAccessible(true);
			return (Map<String, Object>) field.get(handler);
		} catch (Exception e) {throw new RuntimeException(e);}
	}
	
	/**
	 * @描述 获取地址端口信息
	 * @param port 端口
	 * @return 地址端口信息
	 */
	public static String getIpPort(int port) {
		try {return InetAddress.getLocalHost().getHostAddress() + ":" + port;}
		catch (Exception e) {throw new RuntimeException(e);}
	}
	
	/**
	 * @描述 获取去除[-]的UUID
	 * @return 去除[-]的UUID
	 */
	public static String getUUID32() {
		return UUID.randomUUID().toString().replace("-", "");
	}
	
	/**
	 * @描述 获取随机进制数值字符
	 * @param length 字符长度
	 * @param radix 进制
	 * @return 进制数值字符
	 */
	public static String getRandomByRadix(int length, int radix) {
		SecureRandom sr = new SecureRandom();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			sb.append(Integer.toString(sr.nextInt(radix), radix));
		}
		return sb.toString();
	}
	
    /**
     * @描述 AES加密
     * @param key 秘钥
     * @param data 原数据
     * @return 加密后数据
     */
    public static String aesEn(String key, String data) {
    	try {
    		Cipher cipher = Cipher.getInstance(AES);
    		SecretKeySpec secretKeySpec = new SecretKeySpec(md5(key).getBytes(), AES);
    		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
    		byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
    		return Base64.getEncoder().encodeToString(encrypted);
		} catch (Exception e) {throw new RuntimeException(e);}
    }
    
    /**
     * @描述 AES解密
     * @param key 秘钥
     * @param data 加密后数据
     * @return 原数据
     */
    public static String aesDe(String key, String data) {
    	try {
    		Cipher cipher = Cipher.getInstance(AES);
    		SecretKeySpec secretKeySpec = new SecretKeySpec(md5(key).getBytes(), AES);
    		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
    		byte[] encrypted = cipher.doFinal(Base64.getDecoder().decode(data));
    		return new String(encrypted, StandardCharsets.UTF_8);
		} catch (Exception e) {throw new RuntimeException(e);}
    }
	
	/**
	 * @描述 MD5加密
	 * @param val 待加密值
	 * @return MD5加密后的值
	 */
	public static String md5(String val) {
		try {
			MessageDigest md = MessageDigest.getInstance(MD5);
			return bytesToHex(md.digest(val.getBytes()));
		} catch (NoSuchAlgorithmException e) {throw new RuntimeException(e);}
	}
	
	/**
	 * @描述 SHA256加密
	 * @param val 待加密值
	 * @return SHA256加密后的值
	 */
	public static String sha256(String val) {
		try {
			MessageDigest md = MessageDigest.getInstance(SHA256);
			return bytesToHex(md.digest(val.getBytes()));
		} catch (NoSuchAlgorithmException e) {throw new RuntimeException(e);}
	}
	
	/**
	 * @描述 字节数组转16进制字符串
	 * @param bytes 字节数组
	 * @return 16进制字符串
	 */
	public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {hexString.append(ZERO);}
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
