package org.city.common.core.make;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.city.common.api.annotation.excel.ExcelExport;
import org.city.common.api.constant.ExcelDataType;
import org.city.common.api.constant.ExcelType;
import org.city.common.api.dto.DataList;
import org.city.common.api.dto.Response;
import org.city.common.api.entity.BaseEntity;
import org.city.common.api.exception.SkipException;
import org.city.common.api.in.MakeInvoke;
import org.city.common.api.in.excel.ExcelImport;
import org.city.common.api.in.excel.ExcelProcess;
import org.city.common.api.in.function.FunctionRequest;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.parse.TypeParse;
import org.city.common.api.util.FieldUtil;
import org.city.common.api.util.FileUtil;
import org.city.common.api.util.FormatUtil;
import org.city.common.api.util.StreamUtil;
import org.city.common.core.controller.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.data.ClientAnchorData.AnchorType;
import com.alibaba.excel.metadata.data.ImageData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.alibaba.excel.write.style.row.SimpleRowHeightStyleStrategy;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;

/**
 * @作者 ChengShi
 * @日期 2025-07-03 16:29:25
 * @版本 1.0
 * @描述 单元格操作
 */
@Component
public class ExcelMake implements MakeInvoke,TypeParse,JSONParser {
	private final String EXCEL_TYPE_NAME = "ExcelType"; //单元格操作类型
	private final long PAGE_SIZE = 1000L; //分页大小
	private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() << 1);
	@Autowired(required = false)
	private ExcelProcess excelProcess;
	
	@Override
	public void invoke(Process process, int value, String[] values) throws Throwable {
		/* 获取当前请求与响应对象 */
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
		/* 获取单元格操作类型 */
		String excelType = request.getParameter(EXCEL_TYPE_NAME);
		
		/* 执行Excel操作 */
		if (StringUtils.hasText(excelType)) {
			BaseEntity curPage = getPage(process); //当前分页信息
			switch (ExcelType.valueOf(excelType)) {
				case SYEC: handlerExportSyncCurPage(process, curPage, response); return;
				case SYEA: handlerExportSyncAll(process, curPage, response); return;
				case ASYEC: handlerASyncCurPage(process, curPage); return;
				case ASYEA: handlerASyncAll(process, curPage); return;
				case DIT: downImortTemplate(process, response); return;
				default: throw new NullPointerException(String.format("单元格操作类型[%s]不存在！", excelType));
			}
		}
	}
	
	/* 同步导出当前分页数据 */
	private void handlerExportSyncCurPage(Process process, BaseEntity curPage, HttpServletResponse response) throws Throwable {
		handlerSyncCommon(process, curPage, response, false, "同步导出当前分页数据！");
	}
	/* 同步导出全部数据 */
	private void handlerExportSyncAll(Process process, BaseEntity curPage, HttpServletResponse response) throws Throwable {
		handlerSyncCommon(process, curPage, response, true, "同步导出全部数据！");
	}
	/* 同步导出公共处理 */
	private void handlerSyncCommon(Process process, BaseEntity curPage, HttpServletResponse response, boolean allPage, String skipMsg) throws Throwable {
		response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
		response.setHeader("Content-Disposition", "attachment; filename=export-data.xlsx");
		exportData(process, response.getOutputStream(), curPage.getPageSize(), allPage, false, null, handlerPage(process, curPage, false));
		throw new SkipException(skipMsg, null); //跳过其他处理直接返回结果
	}
	
	/* 异步导出当前分页数据 */
	private void handlerASyncCurPage(Process process, BaseEntity curPage) {
		handlerASyncCommon(process, curPage, false, "异步导出当前分页数据！");
	}
	/* 异步导出所有数据 */
	private void handlerASyncAll(Process process, BaseEntity curPage) {
		handlerASyncCommon(process, curPage, true, "异步导出所有数据！");
	}
	/* 异步导出公共处理 */
	private void handlerASyncCommon(Process process, BaseEntity curPage, boolean allPage, String skipMsg) {
		Assert.notNull(excelProcess, "异步处理需要实现单元格进度接口！");
		String processId = excelProcess.init(false); //异步初始阶段
		executorService.execute(() -> {
			File tempFile = FileUtil.getByTimeDay("excel-export"); //导出临时文件
			try (OutputStream out = new FileOutputStream(tempFile)) {
				long realTotal = exportData(process, out, curPage.getPageSize(), allPage, true, processId, handlerPage(process, curPage, allPage));
				excelProcess.finish(processId, realTotal, tempFile); //异步完成阶段
			} catch (Throwable e) {excelProcess.error(processId, e);} //异步异常阶段
			finally {tempFile.delete();} //不保留临时文件
		});
		throw new SkipException(skipMsg, new Response<>((Object) processId)); //跳过其他处理直接返回结果
	}
	
	/* 下载导入模板 */
	private void downImortTemplate(Process process, HttpServletResponse response) throws IOException {
		/* 验证是否继承公共类 */
		boolean flag = process.getTarget() instanceof AbstractController;
		Assert.isTrue(flag, "当前接口未继承公共控制层 - 不可导入！");
		Class<?> head = null;
		
		/* 设置对应单元格导入实现类 */
		if (flag) {
			ParameterizedType superClass = ((ParameterizedType) process.getTarget().getClass().getGenericSuperclass());
			flag = ExcelImport.class.isAssignableFrom((Class<?>) superClass.getActualTypeArguments()[0]);
			Assert.isTrue(flag, "当前接口对应服务层 - 未实现单元格导入接口！");
			
			/* 获取表头信息 */
			Class<?> service = (Class<?>) superClass.getActualTypeArguments()[0];
			for (Type type : service.getGenericInterfaces()) {
				if (type.getTypeName().contains(ExcelImport.class.getName())) {
					head = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
				}
			}
			/* 表头不能为空 */
			Assert.notNull(head, "当前接口对应服务层 - 未实现单元格导入接口！");
		}
		
		/* 头信息 */
		List<ExcelData> excelDatas = new ArrayList<>();
		/* 设置头信息 - 返回工作表名 */
		String sheetName = setImportHead(head, excelDatas);
		
		/* 下载文件 */
		response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
		response.setHeader("Content-Disposition", "attachment; filename=import-template.xlsx");
		ExcelWriter build = EasyExcel.write(response.getOutputStream()).head(excelDatas.stream().map(v -> Arrays.asList(v.headName)).collect(Collectors.toList()))
				.registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()).registerWriteHandler(new SimpleRowHeightStyleStrategy((short) 24, (short) 18)).build();
		/* 单元格信息 */
		WriteSheet writeSheet = EasyExcel.writerSheet(sheetName).build();
		
		/* 写入模板数据 - 只写一次 */
		build.write(Arrays.asList(excelDatas.stream().map(v -> v.demo).collect(Collectors.toList())), writeSheet); //写入数据
		build.finish();
		throw new SkipException("下载导入模板！", null); //跳过其他处理直接返回结果
	}
	
	/* 处理分页 */
	private FunctionRequest<Long, Object> handlerPage(Process process, BaseEntity curPage, boolean allPage) {
		if (allPage) {curPage.setPageNum(1L);} //所有数据分页时 - 设置页码为第一页
		Long oldPageSize = curPage.getPageSize(); //原分页大小
		
		/* 分页处理请求 */
		return (pageSize) -> {
			if (pageSize == null) { //开始分页查询 - 设置固定分页大小
				try {curPage.setPageSize(allPage ? PAGE_SIZE : oldPageSize); return process.process();}
				finally {curPage.setPageNum(curPage.getPageNum() + 1);}
			} else { //测试分页 - 设置参数分页大小
				curPage.setPageSize(pageSize);
				return process.process();
			}
		};
	}
	/* 取出分页参数 */
	private BaseEntity getPage(Process process) {
		BaseEntity pageDto = null;
		for (Object arg : process.getArgs()) {
			if (arg instanceof BaseEntity) {
				pageDto = (BaseEntity) arg;
				break;
			}
		}
		Assert.notNull(pageDto, "方法入参未找到分页参数！");
		return pageDto;
	}
	/* 获取返回值对象 */
	private Class<?> getReturnClass(Process process) {
		/* 获取返回值类型 */
		Type gReturnType = process.getMethod().getGenericReturnType();
		Class<?> returnType = process.getMethod().getReturnType();
		
		/* 如果是响应类型 - 再获取真实返回值类型 */
		if (Response.class.isAssignableFrom(returnType)) {
			gReturnType = ((ParameterizedType) gReturnType).getActualTypeArguments()[0];
			returnType = getClass(gReturnType);
		}
		/* 如果是集合或者分页数据集 - 再获取一次真实返回值类型 */
		if (Collection.class.isAssignableFrom(returnType) || DataList.class.isAssignableFrom(returnType)) {
			returnType = getClass(((ParameterizedType) gReturnType).getActualTypeArguments()[0]);
		}
		
		/* 真实返回值类型 */
		return returnType;
	}
	/* 设置导出头信息 - 返回工作表名 */
	private String setExportHead(Process process, List<ExcelData> excelDatas) {
		Class<?> returnClass = getReturnClass(process);
		/* 获取导入头信息 */
		FieldUtil.getAllDeclaredField(returnClass, true).forEach((k, v) -> {
			ExcelExport excelExport = v.getDeclaredAnnotation(ExcelExport.class);
			if (excelExport != null) {
				String headName = StringUtils.hasText(excelExport.name()) ? excelExport.name() : k;
				excelDatas.add(new ExcelData(k, headName, excelExport.order(), excelExport.dataType(), null));
			}
		});
		
		/* 按order升序 */
		excelDatas.sort((v1, v2) -> {
			return v1.order > v2.order ? 1 : (v1.order == v2.order ? 0 : -1);
		});
		
		/* 工作表名 */
		ExcelExport excelExport = returnClass.getDeclaredAnnotation(ExcelExport.class);
		return excelExport == null ? "详细数据" : excelExport.sheetName();
	}
	/* 设置导入头信息 - 返回工作表名 */
	private String setImportHead(Class<?> head, List<ExcelData> excelDatas) {
		/* 设置导入头信息 */
		$setImportHead(head, excelDatas);
		
		/* 按order升序 */
		excelDatas.sort((v1, v2) -> {
			return v1.order > v2.order ? 1 : (v1.order == v2.order ? 0 : -1);
		});
		
		/* 工作表名 - 导入为必填 */
		org.city.common.api.annotation.excel.ExcelImport excelImport = head.getDeclaredAnnotation(org.city.common.api.annotation.excel.ExcelImport.class);
		Assert.notNull(excelImport, "当前接口对应服务层 - 实现的单元格导入接口表头未定义！");
		Assert.isTrue(StringUtils.hasText(excelImport.sheetName()), "当前接口对应服务层 - 实现的单元格导入接口表头未设置工作表名称！");
		return excelImport.sheetName();
	}
	/* 设置导入头信息 */
	private void $setImportHead(Class<?> head, List<ExcelData> excelDatas) {
		/* 获取导入头信息 */
		FieldUtil.getAllDeclaredField(head, true).forEach((k, v) -> {
			org.city.common.api.annotation.excel.ExcelImport excelImport = v.getDeclaredAnnotation(org.city.common.api.annotation.excel.ExcelImport.class);
			if (excelImport != null) {
				if (excelImport.nesting()) {$setImportHead(v.getType(), excelDatas);}
				else {
					String headName = StringUtils.hasText(excelImport.name()) ? excelImport.name() : k;
					excelDatas.add(new ExcelData(null, headName, excelImport.order(), null, excelImport.demo()));
				}
			}
		});
	}
	/* 导出数据 */
	private long exportData(Process process, OutputStream out, Long oldPageSize, boolean allPage, boolean async, String processId, FunctionRequest<Long, Object> callDatas) throws Throwable {
		/* 头信息 */
		List<ExcelData> excelDatas = new ArrayList<>();
		/* 设置头信息 - 返回工作表名 */
		String sheetName = setExportHead(process, excelDatas);
		
		/* 下载文件 */
		ExcelWriter build = EasyExcel.write(out).head(excelDatas.stream().map(v -> Arrays.asList(v.headName)).collect(Collectors.toList()))
				.registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()).registerWriteHandler(new SimpleRowHeightStyleStrategy((short) 24, (short) 18)).build();
		/* 单元格信息 */
		WriteSheet writeSheet = EasyExcel.writerSheet(sheetName).build();
		
		/* 测试分页 - 防止分页失效导出所有 */
		Object oneData = callDatas.apply(1L);
		if (oneData == null) {build.finish(); return 0;} //无数据
		Collection<?> one = parseData(oneData, excelDatas); //分页1数据
		/* 如果大于1代表分页失效 */
		if (one.size() > 1) {throw new RuntimeException("当前接口对应服务层分页功能失效！");}
		
		/* 取预估总数 */
		if (async) {
			long total = oldPageSize;
			if (allPage) { //所有分页数据使用查询总数
				if (oneData instanceof Response) {oneData = ((Response<?>) oneData).getData();}
				if (oneData instanceof Collection) {total = ((Collection<?>) oneData).size();}
				else if (oneData instanceof DataList) {total = ((DataList<?>) oneData).getTotal();}
			}
			excelProcess.start(processId, sheetName, total); //异步准备阶段
		}
		
		/* 如果与分页2相等代表只有这么多数据 - 将小于等于1条数据直接写入 */
		if (one.size() == parseData(callDatas.apply(2L), excelDatas).size()) {
			build.write(one, writeSheet); //写入数据
			if (async) {excelProcess.handler(processId, one.size());} //异步处理阶段
			build.finish();
			return one.size();
		}
		
		/* 所有数据分页采用循环写入 - 否则只写一次 */
		if (allPage) { //写入模板数据 - 循环写入
			long cur = 0;
			while (true) {
				Object returnVal = callDatas.apply(null);
				if (returnVal == null) {break;} //无数据
				Collection<?> parseData = parseData(returnVal, excelDatas);
				if (parseData.isEmpty()) {break;} //无数据
				build.write(parseData, writeSheet); //写入数据
				cur += parseData.size();
				if (async) {excelProcess.handler(processId, cur);} //异步处理阶段
				if (parseData.size() < PAGE_SIZE) {break;} //分页结束
			}
			build.finish();
			return cur;
		} else { //写入模板数据 - 只写一次
			Collection<?> parseData = parseData(callDatas.apply(null), excelDatas);
			build.write(parseData, writeSheet); //写入数据
			if (async) {excelProcess.handler(processId, parseData.size());} //异步处理阶段
			build.finish();
			return parseData.size();
		}
	}
	/* 解析数据 */
	private Collection<?> parseData(Object returnVal, List<ExcelData> excelDatas) {
		FormatUtil.format(returnVal); //格式化返回值
		JSONArray datas = new JSONArray();
		if (returnVal == null || isBaseType(returnVal.getClass())) { //无数据或基本类型直接返回
			return datas;
		} else if (returnVal instanceof Response) { //响应对象先取出数据
			returnVal = ((Response<?>) returnVal).getData();
		}
		
		if (returnVal == null) { //无数据直接返回
			return datas;
		}else if (returnVal instanceof DataList) { //分页对象转JSON数组对象
			datas = JSONArray.parseArray(JSON.toJSONString(((DataList<?>) returnVal).getRows()));
		} else if (returnVal instanceof Collection || returnVal.getClass().isArray()) { //集合与数组对象转JSON数组对象
			datas = JSONArray.parseArray(JSON.toJSONString(returnVal));
		} else { //其他对象直接添加至JSON数组中
			datas.add(JSON.toJSON(returnVal));
		}
		
		/* 待导出数据 */
		List<List<WriteCellData<?>>> watingExport = new ArrayList<>();
		for (Object data : datas) {
			if (data == null) {continue;}
			if (data instanceof JSONObject) { //必须是JSON对象才处理
				List<WriteCellData<?>> parseDatas = new ArrayList<>();
				/* 按字段顺序添加数据 */
				excelDatas.forEach(v -> {
					Object fieldValue = ((JSONObject) data).get(v.fieldName);
					switch (v.dataType) {
						case FILE_IMAGE: parseDatas.add(getImageCellData(fieldValue, (fv) -> getDataByFile(fv))); break;
						case URL_IMAGE: parseDatas.add(getImageCellData(fieldValue, (fv) -> getDataByUrl(fv))); break;
						default: parseDatas.add(fieldValue == null ? new WriteCellData<>() : new WriteCellData<>(fieldValue.toString())); break;
					}
				});
				watingExport.add(parseDatas);
			}
		}
		return watingExport;
	}
	/* 获取单元格图片数据 */
	private WriteCellData<Void> getImageCellData(Object fieldValue, Function<String, byte[]> callImageDatas) {
		if (fieldValue == null) {return new WriteCellData<>();}
		
		/* 转换成字符串 */
		List<String> fieldValues = new ArrayList<>();
		if (fieldValue instanceof Collection || fieldValue.getClass().isArray()) {
			fieldValue = parse(fieldValue, Collection.class);
			for (Object fv : (Collection<?>) fieldValue) {fieldValues.add(String.valueOf(fv));}
		} else {
			fieldValues.add(String.valueOf(fieldValue));
		}
		
		/* 单元格图片数据 */
		WriteCellData<Void> writeCellData = new WriteCellData<>();
		writeCellData.setType(CellDataTypeEnum.EMPTY);
		writeCellData.setImageDataList(new ArrayList<>());
		
		/* 处理所有图片数据 */
		for (String fv : fieldValues) {
			byte[] imageDatas = callImageDatas.apply(fv);
			writeCellData.getImageDataList().add(getImageData(imageDatas));
		}
		return writeCellData;
	}
	/* 获取图片数据来自文件 */
	private byte[] getDataByFile(String filePath) {
		if (filePath == null) {return new byte[0];}
		try (InputStream in = new FileInputStream(new File(filePath))) {return StreamUtil.readABytes(in);}
		catch (Throwable e) {return new byte[0];}
	}
	/* 获取图片数据来自链接 */
	private byte[] getDataByUrl(String url) {
		if (url == null) {return new byte[0];}
		try (InputStream in = new URL(url).openStream()) {return StreamUtil.readABytes(in);}
		catch (Throwable e) {return new byte[0];}
	}
	/* 获取图片数据对象来自图片数据 */
	private ImageData getImageData(byte[] imageDatas) {
		ImageData imageData = new ImageData();
		imageData.setAnchorType(AnchorType.MOVE_AND_RESIZE);
		imageData.setImage(imageDatas);
		return imageData;
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2025-07-03 22:06:11
	 * @版本 1.0
	 * @parentClass ExcelMake
	 * @描述 单元格数据
	 */
	@AllArgsConstructor
	private class ExcelData {
		/* 字段名称 */
		private String fieldName;
		/* 表头名称 */
		private String headName;
		/* 排序 */
		private int order;
		/* 数据类型 */
		private ExcelDataType dataType;
		/* 示例 */
		private String demo;
	}
}
