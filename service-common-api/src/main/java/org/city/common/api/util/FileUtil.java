package org.city.common.api.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

/**
 * @作者 ChengShi
 * @日期 2023-04-03 14:32:20
 * @版本 1.0
 * @描述 文件工具
 */
public final class FileUtil {
	private final static String NULLSTR = "";
	private FileUtil() {}
	
	/**
	 * @描述 当前运行路径创建一个以天为单位的文件
	 * @param prefixFolder 前缀文件夹路径
	 * @return 以天为单位的文件
	 */
	public static File getByTimeDay(String prefixFolder) {
		try {
			SimpleDateFormat yyyyMMdd = new SimpleDateFormat("yyyyMMdd");
			File classPath = new File(FileUtil.class.getResource("/").toURI()); //当前运行路径
			File parentFile = new File(new File(classPath, prefixFolder), yyyyMMdd.format(new Date()));
			if (!parentFile.exists()) {parentFile.mkdirs();} //创建父目录
			return new File(parentFile, MyUtil.getUUID32());
		} catch (Exception e) {throw new RuntimeException(e);}
	}
	
	/**
	 * @描述 断点续传
	 * @param file 待写出文件
	 * @param response 页面响应
	 */
	public static void writeByRange(File file, HttpServletResponse response) {
		try (FileInputStream in = new FileInputStream(file)) {
			StreamUtil.writeByRange(in, file.length(), response);
		} catch (IOException e) {throw new RuntimeException(e);}
	}
	
	/**
	 * @描述 拷贝文件夹
	 * @param inDir 输入文件夹
	 * @param outDir 输出文件夹
	 * @param notEndWith 不拷贝包含此后缀的文件
	 */
	public static void copyDir(String inDir, String outDir, String...notEndWith) {
		if (inDir == null || outDir == null) {return;}
		File inDirFile = new File(inDir);
		File outDirFile = new File(outDir);
		if (!inDirFile.exists() || inDirFile.isFile()) {throw new RuntimeException("输入的文件夹不存在或者非文件夹！");}
		if (!outDirFile.exists()) {outDirFile.mkdirs();}
		
		outDir = outDir + File.separator;
		notEndWith = notEndWith == null ? new String[0] : notEndWith;
		File[] listFiles = inDirFile.listFiles();
		for (File file : listFiles) {copyDir(file, outDir, NULLSTR, notEndWith);}
	}
	/* 拷贝文件夹 */
	private static void copyDir(File inFile, String outDir, String curPath, String...notEndWidth) {
		curPath += inFile.getName();
		if (inFile.isDirectory()) {
			curPath += File.separator;
			File[] listFiles = inFile.listFiles();
			for (File file : listFiles) {copyDir(file, outDir, curPath, notEndWidth);}
		} else {
			for (String NEW : notEndWidth) {if (inFile.getName().endsWith(NEW)) {return;}}
			copyFile(inFile.getAbsolutePath(), outDir + curPath);
		}
	}
	
	/**
	 * @描述 拷贝文件
	 * @param inFilePathName 输入文件地址与名称
	 * @param outFilePathName 输出文件地址与名称
	 */
	public static void copyFile(String inFilePathName, String outFilePathName) {
		if (inFilePathName == null || outFilePathName == null) {return;}
		File inFile = new File(inFilePathName);
		File outFile = new File(outFilePathName);
		if (!inFile.exists() || inFile.isDirectory()) {throw new RuntimeException("输入的文件不存在或者非文件！");}
		if (!outFile.getParentFile().exists()) {outFile.getParentFile().mkdirs();}
		/* 零拷贝 */
		try (FileInputStream inputStream = new FileInputStream(inFile); FileOutputStream outputStream = new FileOutputStream(outFile)) {
			inputStream.getChannel().transferTo(0, inFile.length(), outputStream.getChannel());
		} catch (Exception e) {throw new RuntimeException(e);}
	}
}
