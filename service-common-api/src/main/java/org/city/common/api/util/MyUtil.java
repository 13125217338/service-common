package org.city.common.api.util;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;


/**
 * @作者 ChengShi
 * @日期 2020-07-23 09:51:13
 * @版本 1.0
 * @描述 我的工具类
 */
public final class MyUtil {
	private MyUtil() {}
	private final static String NULLSTR = "";
	private final static String MEMBER_VALUES = "memberValues";
	private final static String MD5 = "MD5";
	private final static String ZERO = "0";
	
	/** 图片缩放类型-等比向内 */
	public final static byte IMG_ZOOM_RATIO_IN = 1;
	/** 图片缩放类型-等比向外 */
	public final static byte IMG_ZOOM_RATIO_OUT = 2;
	/** 图片缩放类型-相同 */
	public final static byte IMG_ZOOM_EQUAL = 3;
	
	/**
	 * @描述 判断集合不为空或空集合
	 * @param vals 被判断的集合
	 * @return 为NULL或者长度为0则为false
	 */
	public static <T> boolean isNotBlank(Collection<T> vals){
		return !(vals == null || vals.size() == 0);
	}
	
	/**
	 * @描述 判断对象不为NULL或空字符或集合map与数组长度为0
	 * @param obj 被判断的对象
	 * @return 对象为NULL或空字符串或集合map与数组长度为0则返回false
	 */
	public static boolean isNotBlank(Object obj){
		if (obj == null || obj.toString().length() == 0) {return false;}
		else{
			if (obj.getClass().isArray()) {if (Array.getLength(obj) == 0) {return false;}}
			else if(obj instanceof Collection) {return isNotBlank((Collection<?>) obj);}
			else if(obj instanceof Map) {return isNotBlank((Map<?, ?>) obj);}
			return true;
		}
	}
	
	/**
	 * @描述 判断字符串不为空
	 * @param str 被判断的字符串
	 * @return 字符串为NULL或长度为0都是空
	 */
	public static boolean isNotBlank(String str) {
		return !(str == null || str.toString().length() == 0);
	}
	
	/**
	 * @描述 判断map不为空或空map
	 * @param vals 被判断的map
	 * @return 为NULL或者长度为0则为false
	 */
	public static <K, V> boolean isNotBlank(Map<K, V> vals){
		return !(vals == null || vals.size() == 0);
	}
	
	/**
	 * @描述 获取原字符串在索引字符串出现过多少次
	 * @param val 原字符
	 * @param index 索引字符
	 * @return 出现过多少次
	 */
	public static int indexSum(String val, String index){
		int sum = 0;
		if (isNotBlank(val) && isNotBlank(index)) {
			int cur = 0;
			while((cur = val.indexOf(index, cur)) != -1){cur += index.length();sum++;}
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
	public static Map<String, String> getContainValue(String start, String end, String value){
		Map<String, String> vals = new LinkedHashMap<String, String>();
		if (MyUtil.isNotBlank(start) && MyUtil.isNotBlank(end) && MyUtil.isNotBlank(value)) {
			int index = 0;
			while((index = value.indexOf(start, index)) != -1){
				int indexOf = value.indexOf(end, index + start.length());
				if (indexOf == -1) {break;}
				vals.put(value.substring(index, indexOf + end.length()), value.substring(index + start.length(), indexOf));
				index = indexOf + end.length();
			}
		}
		return vals;
	}
	
	/**
	 * @描述 通过输入流获取所有字节，流不会自动关闭（请不要给阻塞流或大数据流，会导致一直阻塞或内存溢出等问题）
	 * @param inputStream 输入流（要有read(byte[] bs)方法）
	 * @param bufferSize 单次读写大小
	 * @return 字节数组（不会为NULL）
	 * @throws IOException
	 */
	public static byte[] getBytes(InputStream inputStream, int bufferSize) throws IOException{
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream(bufferSize);
		if (inputStream != null) {
			byte[] bs = new byte[bufferSize];
			int len = -1;
			while((len = inputStream.read(bs)) != -1){byteOutputStream.write(bs, 0, len);}
		}
		return byteOutputStream.toByteArray();
	}
	
	/**
	 * @描述 等比缩放图片（宽或者高会被缩放到与当前设置宽一致，或高一致）
	 * @param inputStream 图片输入流
	 * @param width 缩放宽
	 * @param height 缩放高
	 * @param imgZoom 缩放方式（用该类的字段）
	 * @param imgType 图片类型（如：JPEG）
	 * @return 缩放后的图片字节（可能宽高只等于其中设置的一个）
	 * @throws IOException
	 */
	public static byte[] imgZoom(InputStream inputStream, int width, int height, byte imgZoom, String imgType) throws IOException{
		/*原图*/
		BufferedImage bufferedImage = ImageIO.read(inputStream);
		int realWidth = bufferedImage.getWidth();
		if (imgZoom == IMG_ZOOM_EQUAL) {realWidth = width;}
		else{
			/*缩小或放大宽*/
			double widthZoom = (double)bufferedImage.getWidth() / bufferedImage.getHeight();
			int curHeight = bufferedImage.getHeight() - height;
			if (curHeight != 0) {realWidth = curHeight > 0 ? (bufferedImage.getWidth() - (int) (curHeight * widthZoom)) : (bufferedImage.getWidth() + (int) (-curHeight * widthZoom));}
			/*缩小或放大高*/
			double heightZoom = (double)bufferedImage.getHeight() / bufferedImage.getWidth();
			int curWidth = realWidth - width;
			if (imgZoom == IMG_ZOOM_RATIO_IN) {
				if (curWidth > 0) {
					realWidth = realWidth - curWidth;
					height = (height - (int) (curWidth * heightZoom));
				}
			}else{
				if (curWidth < 0) {
					realWidth = realWidth - curWidth;
					height = (height + (int) (-curWidth * heightZoom));
				}
			}
		}
		/*获取缩放实例图*/
		Image scaledInstance = bufferedImage.getScaledInstance(realWidth, height, Image.SCALE_SMOOTH);
		/*待画的图*/
		BufferedImage dataImg = new BufferedImage(realWidth, height, BufferedImage.TYPE_INT_RGB);
		Graphics graphics = dataImg.getGraphics();
		graphics.drawImage(scaledInstance, 0, 0, null);
		graphics.dispose();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(dataImg, imgType, outputStream);
		return outputStream.toByteArray();
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
	/*递归替换*/
	private static String replaceStr(String value, String replaceStr, String reString) {
		if (value.contains(reString)) {
			value = value.replace(reString, replaceStr);
			value = replaceStr(value, replaceStr, reString);
		}
		return value;
	}
	
	/**
	 * @描述 判断所有前缀是否相同
	 * @param value 待判断字符
	 * @param isIgnore 是否忽略大小写
	 * @param prifixs 前缀
	 * @return 判断结果
	 */
	public static boolean startWithAny(String value, boolean isIgnore, String...prifixs) {
		if (value == null || prifixs == null) {return false;}
		if (isIgnore) {
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
	 * @描述 拷贝文件夹
	 * @param inDir 输入文件夹
	 * @param outDir 输出文件夹
	 * @param notEndWidth 不拷贝包含此后缀的文件
	 * @throws Exception
	 */
	public static void copyDir(String inDir, String outDir, String...notEndWidth) throws Exception{
		if (inDir == null || outDir == null) {return;}
		File inDirFile = new File(inDir);
		File outDirFile = new File(outDir);
		if (!inDirFile.exists() || inDirFile.isFile()) {throw new FileNotFoundException("输入的文件夹不存在或者非文件夹！");}
		if (!outDirFile.exists()) {outDirFile.mkdirs();}
		
		outDir = outDir + File.separator;
		notEndWidth = notEndWidth == null ? new String[0] : notEndWidth;
		File[] listFiles = inDirFile.listFiles();
		for (File file : listFiles) {copyDir(file, outDir, NULLSTR, notEndWidth);}
	}
	private static void copyDir(File inFile, String outDir, String curPath, String...notEndWidth) throws Exception{
		curPath += inFile.getName();
		if (inFile.isDirectory()) {
			curPath += File.separator;
			File[] listFiles = inFile.listFiles();
			for (File file : listFiles) {copyDir(file, outDir, curPath, notEndWidth);}
		}else{
			for (String NEW : notEndWidth) {if (inFile.getName().endsWith(NEW)) {return;}}
			copyFile(inFile.getAbsolutePath(), outDir + curPath);
		}
	}
	
	/**
	 * @描述 拷贝文件
	 * @param inFilePathName 输入文件地址与名称
	 * @param outFilePathName 输出文件地址与名称
	 * @throws Exception
	 */
	public static void copyFile(String inFilePathName, String outFilePathName) throws Exception{
		if (inFilePathName == null || outFilePathName == null) {return;}
		File inFile = new File(inFilePathName);
		File outFile = new File(outFilePathName);
		if (!inFile.exists() || inFile.isDirectory()) {throw new FileNotFoundException("输入的文件不存在或者非文件！");}
		if (!outFile.getParentFile().exists()) {outFile.getParentFile().mkdirs();}
		/*零拷贝*/
		try(FileInputStream inputStream = new FileInputStream(inFile);FileOutputStream outputStream = new FileOutputStream(outFile)){
			inputStream.getChannel().transferTo(0, inFile.length(), outputStream.getChannel());
		}
	}
	
	/**
	 * @描述 重新设置注解值
	 * @param annotation 注解对象
	 * @param fieldName 字段
	 * @param value 值
	 * @throws Exception 
	 */
	@SuppressWarnings("all")
	public static void setAnnotationValue(Annotation annotation, String fieldName, Object value) throws Exception {
		try {
			if (annotation == null || fieldName == null || value == null) {return;}
			InvocationHandler handler = Proxy.getInvocationHandler(annotation);
		    Field field = handler.getClass().getDeclaredField(MEMBER_VALUES);
		    field.setAccessible(true);
		    Map memberValues = (Map) field.get(handler);
		    memberValues.put(fieldName, value);
		} catch (Exception e) {throw new Exception("重新设置注解值出错！Msg：" + e.getMessage());}
	}
	
	/*获取注解所有值*/
	@SuppressWarnings("all")
	public static Map<String, Object> getAnnotationVal(Annotation att) {
		try {
			InvocationHandler handler = Proxy.getInvocationHandler(att);
			Field field = handler.getClass().getDeclaredField(MEMBER_VALUES);
			field.setAccessible(true);
			return (Map<String, Object>) field.get(handler);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @描述 获取地址端口信息
	 * @param port 端口
	 * @return 地址端口信息
	 * @throws UnknownHostException
	 */
	public static String getIpPort(int port) throws UnknownHostException {
		return InetAddress.getLocalHost().getHostAddress() + ":" + port;
	}
	
	/**
	 * @描述 MD5加密
	 * @param val 待加密值
	 * @return MD5加密后的值
	 */
	public static String md5(String val) {
		if (val == null) {return null;}
		
		/* MD5加密器 */
		MessageDigest md = null;
		try {md = MessageDigest.getInstance(MD5);}
		catch (NoSuchAlgorithmException e) {throw new RuntimeException(e);}
		
		/* 加密 */
		String md5code = new BigInteger(1, md.digest(val.getBytes())).toString(16);
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code = ZERO + md5code;
        }
		return md5code;
	}
}
