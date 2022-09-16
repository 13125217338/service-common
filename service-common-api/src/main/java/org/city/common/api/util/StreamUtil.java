package org.city.common.api.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @作者 ChengShi
 * @日期 2023-04-23 15:33:38
 * @版本 1.0
 * @描述 流工具
 */
public final class StreamUtil {
	private StreamUtil() {}
	
	/**
	 * @描述 读取所有数据（请不要给阻塞流或大数据流，会导致一直阻塞或内存溢出等问题）
	 * @param in 输入流（流不会自动关闭）
	 * @return 所有数据
	 */
	public static byte[] readABytes(InputStream in) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream(Short.MAX_VALUE);
			byte[] datas = new byte[Short.MAX_VALUE]; int len = -1;
			
			/* 全量读取 */
			while ((len = in.read(datas)) != -1) {out.write(datas, 0, len);}
			return out.toByteArray();
		} catch (Exception e) {throw new RuntimeException(e);}
	}
	
	/**
	 * @描述 根据条件写出数据
	 * @param out 原内存输出流（当条件成立时该输出流会减少len长度数据）
	 * @param len 待写出长度
	 * @param writeCall 写出回调（当条件[out.size() >= len]时调用）
	 */
	public static void writeIfBytes(ByteArrayOutputStream out, int len, Consumer<byte[]> writeCall) {
		if (out.size() >= len) {
			byte[] oldDatas = out.toByteArray(), newDatas = Arrays.copyOf(oldDatas, len);
			writeCall.accept(newDatas); out.reset();
			out.writeBytes(Arrays.copyOfRange(oldDatas, newDatas.length, oldDatas.length));
		}
	}
	
	/**
	 * @描述 一次性读取指定长度数据
	 * @param in 输入流（流不会自动关闭）
	 * @param len 指定长度
	 * @return 指定长度数据
	 */
	public static byte[] readNBytes(InputStream in, short len) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(len);
		readNBytes(in, len, (data) -> {out.write(data.getKey(), 0, data.getValue());});
		return out.toByteArray();
	}
	
	/**
	 * @描述 读取指定长度数据
	 * @param in 输入流（流不会自动关闭）
	 * @param len 指定长度
	 * @param readCall 读取回调（key=读取数据，value=读取长度）
	 * @return 是否读取结束
	 */
	public static boolean readNBytes(InputStream in, long len, Consumer<Entry<byte[], Integer>> readCall) {
		try {
			/* 读取详情 */
			Entry<byte[], Integer> data = new Entry<byte[], Integer>() {
				private final byte[] datas = new byte[Short.MAX_VALUE];
				private Integer readLen = -1;
				@Override
				public Integer setValue(Integer value) {return this.readLen = value;}
				@Override
				public Integer getValue() {return this.readLen;}
				@Override
				public byte[] getKey() {return this.datas;}
			};
			
			/* 直到与指定长度一致 */
			while (len > 0) {
				int read = in.read(data.getKey(), 0, (int) Math.min(len, Short.MAX_VALUE));
				if (read == -1) {return true;} //读取结束
				data.setValue(read); //当前读取长度
				
				/* 写入回调并设置新读取长度 */
				readCall.accept(data);
				len -= read;
			}
			return false;
		} catch (IOException e) {throw new RuntimeException(e);}
	}
	
	/**
	 * @描述 读取范围长度数据
	 * @param in 输入流（流不会自动关闭）
	 * @param range 范围数据，请顺序添加（key=起始位置，value=结束位置，key-value都包含本身位置数据）
	 * @param readCall 读取回调（key=读取数据，value=读取长度）
	 */
	public static void readRange(InputStream in, LinkedHashMap<Long, Long> range, Consumer<Entry<byte[], Integer>> readCall) {
		try {
			long endPos = 1;
			for (Entry<Long, Long> entry : range.entrySet()) {
				in.skip(entry.getKey() - endPos - 1); //跳过起始位置
				readNBytes(in, entry.getValue() - entry.getKey() + 1, readCall);
				endPos = entry.getValue();
			}
		} catch (IOException e) {throw new RuntimeException(e);}
	}
	
	/**
	 * @描述 全量流拷贝
	 * @param in 输入流（流不会自动关闭）
	 * @param out 输出流（流不会自动关闭）
	 * @return 拷贝数据量
	 */
	public static long copy(InputStream in, OutputStream out) {
		try {
			BufferedOutputStream bOut = new BufferedOutputStream(out, Short.MAX_VALUE);
			long copySize = 0; byte[] datas = new byte[Short.MAX_VALUE]; int len = -1;
			
			/* 全量写出 */
	        while ((len = in.read(datas)) != -1) {bOut.write(datas, 0, len); copySize += len;}
	        bOut.flush(); return copySize;
		} catch (IOException e) {throw new RuntimeException(e);}
	}
	
	/**
	 * @描述 断点续传
	 * @param in 输入流（流不会自动关闭）
	 * @param total 数据长度
	 * @param response 页面响应
	 */
	public static void writeByRange(InputStream in, long total, HttpServletResponse response) {
		try {
			String range = HeaderUtil.getValue("Range"); //断点标志
			if (range == null) {copy(in, response.getOutputStream());} //不断点直接响应
			else {
				String rgBytes = range.substring(range.indexOf("bytes=") + 6);
				Assert.doesNotContain(rgBytes, ",", "当前请求不支持多个Range范围处理！");
				
				/* 断点响应 */
				response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); //断点状态
				response.setHeader("Accept-Ranges", "bytes"); //可跳转进度
				
				/* 范围处理 */
				String[] rgs = rgBytes.split("-"); //取指定范围
				long start = Long.parseLong(rgs[0].trim()); //起始位置
				
				/* 判断是否范围读取 */
				if (rgs.length > 1 && StringUtils.hasText(rgs[1].trim())) {
					/* 响应长度 */
					long end = Long.parseLong(rgs[1].trim()); //结束位置
					response.setContentLengthLong(end - start + 1); //数据长度
					response.setHeader("Content-Range", String.format("bytes %s/%d", rgBytes, total));
					
					/* 范围数据 */
					LinkedHashMap<Long, Long> rangeData = new LinkedHashMap<>(1);
					rangeData.put(start, end);
					
					/* 范围读取并写出 */
					BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream(), Short.MAX_VALUE);
					readRange(in, rangeData, (data) -> {
						try {out.write(data.getKey(), 0, data.getValue());}
						catch (IOException e) {throw new RuntimeException(e);}
					});
					out.flush();
				} else {
					/* 响应长度 */
					response.setContentLengthLong(total - start); //数据长度
					response.setHeader("Content-Range", String.format("bytes %d-%d/%d", start, total - 1, total));
					
					/* 跳过并写出 */
					in.skip(start); //跳过位置
					copy(in, response.getOutputStream());
				}
			}
		} catch (IOException e) {throw new RuntimeException(e);}
	}
}
