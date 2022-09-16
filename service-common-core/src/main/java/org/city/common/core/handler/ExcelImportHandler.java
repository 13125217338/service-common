package org.city.common.core.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.city.common.api.exception.SkipException;
import org.city.common.api.in.excel.ExcelImport;
import org.city.common.api.in.excel.ExcelProcess;
import org.city.common.api.in.parse.JSONParser;
import org.city.common.api.in.util.ThrowableMessage;
import org.city.common.api.util.FieldUtil;
import org.city.common.api.util.FileUtil;
import org.city.common.api.util.HeaderUtil;
import org.city.common.api.util.JsonUtil;
import org.city.common.api.util.StreamUtil;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.alibaba.excel.write.style.row.SimpleRowHeightStyleStrategy;
import com.alibaba.fastjson.JSONObject;

/**
 * @作者 ChengShi
 * @日期 2025-07-03 19:51:43
 * @版本 1.0
 * @描述 单元格导入处理
 */
@Component
public class ExcelImportHandler implements JSONParser,ThrowableMessage {
	private final Map<String, ExcelImport<?>> EXCEL_IMPORT;
	private final Map<String, Class<?>> EXCEL_HEAD;
	private final Map<String, org.city.common.api.annotation.excel.ExcelImport> EXCEL_HEAD_IMPORT;
	private final Map<String, Map<String, String>> EXCEL_DATAS;
	private final ExecutorService EXECUTORS = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() << 1);
	@Autowired(required = false)
	private ExcelProcess excelProcess;
	
	@Autowired
	public ExcelImportHandler(List<ExcelImport<?>> excelImports) {
		EXCEL_HEAD = new HashMap<>(); //工作表对应表头
		EXCEL_HEAD_IMPORT = new HashMap<>(); //工作表对应表头导入信息
		EXCEL_DATAS = new HashMap<>(); //工作表对应字段名称
		EXCEL_IMPORT = excelImports.stream().collect(Collectors.toMap(k -> {
			Class<?> targetClass = ClassUtils.getUserClass(AopProxyUtils.ultimateTargetClass(k));
			/* 获取导入工作表名 */
			for (Type type : targetClass.getGenericInterfaces()) {
				String sheetName = getImportSheetName(type);
				if (sheetName == null) { //当前接口未找到 - 继续查询接口的继承接口
					for (Type tp : getClass(type).getGenericInterfaces()) {
						sheetName = getImportSheetName(tp);
						if (sheetName != null) {return sheetName;} //继承接口已找到
					}
				} else {return sheetName;} //当前接口已找到
			}
			/* 无工作表名 */
			return null;
		}, v -> v, (ov, nv) -> ov));
		EXCEL_IMPORT.remove(null); //不处理空工作表名
	}
	
	/**
	 * @描述 处理导入单元格数据
	 * @param file 单元格文件
	 * @param sheetName 手动指定工作表名
	 * @param async 是否异步运行（true=异步运行）
	 * @return 处理信息
	 * @throws Throwable
	 */
	public String handler(MultipartFile file, String sheetName, boolean async) throws Throwable {
		if (async) {
			Assert.notNull(excelProcess, "异步处理需要实现单元格进度接口！");
			String processId = excelProcess.init(true); //异步初始阶段
			Map<String, String> curHeaders = HeaderUtil.get(); //当前头信息
			EXECUTORS.execute(() -> { //异步运行
				try {HeaderUtil.set(curHeaders); handler(file, sheetName, async, processId);} //设置头信息并运行
				catch (Throwable e) {excelProcess.error(processId, e);} //异步异常阶段
			});
			return processId; //异步处理信息
		} else {
			handler(file, sheetName, async, null); //同步运行
			return null; //同步无处理信息
		}
	}
	
	/* 处理导入单元格数据 */
	private void handler(MultipartFile file, String sheetName, boolean async, String processId) throws Throwable {
		try (InputStream in = file.getInputStream()) {
			File tempFile = FileUtil.getByTimeDay("excel-import"); //错误导入临时文件
			List<List<String>> heads = new ArrayList<>(); //头信息
			WriteSheet writeSheet = EasyExcel.writerSheet().build(); //写出单元格信息
			boolean[] isError = new boolean[] {false}; //导入异常标识
			
			/* 导出异常文件信息 */
			try (OutputStream out = new FileOutputStream(tempFile)) {
				ExcelWriter errorWrite = EasyExcel.write(out).head(heads).registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
						.registerWriteHandler(new SimpleRowHeightStyleStrategy((short) 24, (short) 18)).build();
				/* 开始读取单元格信息 */
				EasyExcel.read(in, new ReadListener<Map<Integer, String>>() {
					private final Map<Integer, String> headMapping = new HashMap<>();
					private ExcelImport<?> excelImport;
					private Class<?> readHead;
					private Map<String, String> nameMapping;
					private short batchSize = 1000;
					private long cur = 0;
					private List<Map<Integer, String>> datas;
					
					@Override
					public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
						/* 导入模板参数配置 */
						String iSheetName = StringUtils.hasText(sheetName) ? sheetName : context.readSheetHolder().getSheetName();
						excelImport = EXCEL_IMPORT.get(iSheetName);
						if (excelImport == null) {throw new NullPointerException(String.format("工作表[%s]模板有误，请重新下载模板！", iSheetName));}
						/* 导入实现类初始阶段 */
						excelImport.init();
						
						/* 准备导入的参数 */
						readHead = EXCEL_HEAD.get(iSheetName);
						batchSize = EXCEL_HEAD_IMPORT.get(iSheetName).batchSize();
						nameMapping = EXCEL_DATAS.get(iSheetName);
						
						/* 错误模板参数配置 */
						writeSheet.setSheetName(iSheetName);
						headMap.forEach((k, v) -> {heads.add(Arrays.asList(v.getStringValue())); headMapping.put(k, v.getStringValue());});
						heads.add(Arrays.asList("[导入异常信息]"));
						batchSize = batchSize > 0 ? batchSize : 1000;
						datas = new ArrayList<>(batchSize);
						
						/* 异步准备阶段 */
						if (async) {excelProcess.start(processId, context.readSheetHolder().getApproximateTotalRowNumber() - 1);}
					}
					
					@Override
					public void invoke(Map<Integer, String> data, AnalysisContext context) {
						datas.add(data); //添加至批量缓存
						if (datas.size() >= batchSize) { //超过阈值批量处理
							cur += datas.size(); handler(); datas.clear();
							if (async) {excelProcess.handler(processId, cur);} //异步处理阶段
						}
					}
					
					@Override
					public void doAfterAllAnalysed(AnalysisContext context) {
						cur += datas.size(); handler(); datas.clear(); errorWrite.finish();
						/* 如果有错误写出错误文件 */
						if (isError[0]) {
							if (async) {excelProcess.finish(processId, cur, tempFile);} //异步完成阶段
							else {
								HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
								response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
								response.setHeader("Content-Disposition", "attachment; filename=import-error.xlsx");
								try (InputStream errorIn = new FileInputStream(tempFile)) {StreamUtil.copy(errorIn, response.getOutputStream());} catch (Exception e) {}
								throw new SkipException(String.format("工作表[%s]导入异常！", file.getOriginalFilename()), null);
							}
						} else {
							if (async) {excelProcess.finish(processId, cur, null);} //异步完成阶段
						}
					}
					
					/* 处理批量行数据 */
					private void handler() {
						try {
							/* 行数据对应参数 */
							List<?> rows = new ArrayList<>(datas.size());
							/* 解析行数据 */
							for (Map<Integer, String> data : datas) {
								JSONObject parseData = new JSONObject();
								for (Entry<Integer, String> dt : data.entrySet()) { //先根据下标获取表头名称 - 在根据表头名称获取字段名称
									JsonUtil.setValue(parseData, nameMapping.get(headMapping.get(dt.getKey())), dt.getValue());
								}
								rows.add(parse(parseData, readHead)); //将解析的数据转换成表头对应的类型
							}
							/* 处理行数据 */
							excelImport.handlerRowDatas(parse(rows, List.class));
						} catch (Throwable e) {
							/* 获取真实异常 */
							e = getRealExcept(e);
							/* 解析错误行数据 */
							List<List<String>> errorRows = new ArrayList<>(datas.size());
							for (Map<Integer, String> data : datas) {
								data.put(data.size(), e.getMessage());
								errorRows.add(new ArrayList<>(data.values()));
							}
							/* 写入错误信息 */
							errorWrite.write(errorRows, writeSheet);
							isError[0] = true; //有异常信息
						}
					}
				}).sheet(0).doRead();
			} finally {tempFile.delete();} //不保留临时文件
		}
	}
	
	/* 获取导入工作表名 */
	private String getImportSheetName(Type type) {
		if (type.getTypeName().contains(ExcelImport.class.getName())) {
			Class<?> head = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
			org.city.common.api.annotation.excel.ExcelImport excelImport = head.getDeclaredAnnotation(org.city.common.api.annotation.excel.ExcelImport.class);
			/* 只需要有工作表名的处理对象 */
			if (excelImport != null && StringUtils.hasText(excelImport.sheetName())) {
				EXCEL_HEAD.put(excelImport.sheetName(), head);
				EXCEL_HEAD_IMPORT.put(excelImport.sheetName(), excelImport);
				EXCEL_DATAS.put(excelImport.sheetName(), getImportHead(head));
				return excelImport.sheetName();
			}
		}
		return null; //未找到工作表名
	}
	/* 获取导入头信息 */
	private Map<String, String> getImportHead(Class<?> head) {
		/* 单元格表头与字段名映射 */
		Map<String, String> nameMapping = new HashMap<>();
		/* 设置导入头信息 */
		setImportHead(null, head, nameMapping);
		/* key=表头名称，value=字段名称 */
		return nameMapping;
	}
	/* 设置导入头信息 */
	private void setImportHead(String parentName, Class<?> head, Map<String, String> nameMapping) {
		/* 获取导入头信息 */
		FieldUtil.getAllDeclaredField(head, true).forEach((k, v) -> {
			org.city.common.api.annotation.excel.ExcelImport excelImport = v.getDeclaredAnnotation(org.city.common.api.annotation.excel.ExcelImport.class);
			if (excelImport != null) {
				String fieldName = parentName == null ? k : parentName + "." + k;
				if (excelImport.nesting()) {setImportHead(fieldName, v.getType(), nameMapping);}
				else {nameMapping.put(excelImport.name(), fieldName);}
			}
		});
	}
}
