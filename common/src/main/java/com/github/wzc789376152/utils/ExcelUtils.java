package com.github.wzc789376152.utils;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.enums.CellExtraTypeEnum;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.CellExtra;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.github.wzc789376152.annotation.ExcelExtra;
import com.github.wzc789376152.dto.ExcelDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * excel导入导出
 */
@Slf4j
public class ExcelUtils {

    public static <T> void write(String filename, List<T> dataList, HttpServletResponse response) throws IOException {
        if (!filename.endsWith(ExcelTypeEnum.XLSX.getValue())) {
            filename = filename + ExcelTypeEnum.XLSX.getValue();
        }
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment;fileName=" + URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20"));
        ServletOutputStream outputStream = response.getOutputStream();
        ExcelWriter writer = EasyExcel.write(outputStream).build();
        try {
            WriteSheet orderSheet = EasyExcel.writerSheet(0, filename).head(dataList.get(0).getClass()).build();
            writer.write(dataList, orderSheet);
        } finally {
            writer.finish();
            outputStream.close();
        }
    }

    public static ExcelDto readRaw(InputStream inputStream, Integer sheet) {
        ExcelRawListener listener = new ExcelRawListener();

        ExcelReader build = EasyExcelFactory.read(inputStream).build();
        ReadSheet build1 = EasyExcel
                //第一个工作区间的表
                .readSheet(sheet)
                //跳过第一条标题
                .headRowNumber(1)
                .registerReadListener(listener)
                .build();
        //合并两个
        build.read(build1);
        ExcelDto excelDto = new ExcelDto<>();
        excelDto.setHead(listener.getHeadMap());
        excelDto.setDataList(listener.getDataList());

        return excelDto;
    }

    private static class ExcelRawListener extends AnalysisEventListener<Map<Integer, String>> {
        private final List<Map<Integer, String>> dataList         = new ArrayList<>();
        private   Map<Integer, String> headMap = new HashMap<>();

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            this.headMap = headMap;
        }

        @Override
        public void invoke(Map<Integer, String> t, AnalysisContext analysisContext) {
            dataList.add(t);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        }

        public List<Map<Integer, String>> getDataList() {
            return dataList;
        }

        public Map<Integer, String> getHeadMap() {
            return  headMap;
        }
    }


    public static <T> List<T> read(InputStream inputStream, Class<T> clazz) {
        ExcelListener<T> listener = new ExcelListener<>(clazz);
        EasyExcelFactory.read(inputStream, clazz, listener)
                .extraRead(CellExtraTypeEnum.COMMENT) // 需要读取批注 默认不读取
                .extraRead(CellExtraTypeEnum.HYPERLINK)// 需要读取超链接 默认不读取
                .extraRead(CellExtraTypeEnum.MERGE)// 需要读取合并单元格信息 默认不读取
                .headRowNumber(1).build().readAll();
        //获取数据
        return listener.getDataList();
    }

    private static class ExcelListener<T> extends AnalysisEventListener<T> {
        ExcelListener(Class<T> tClass) {
            this.tClass = tClass;
        }

        private final Class<T> tClass;
        private final List<T> dataList = new LinkedList<>();
        private final List<CellExtra> cellExtraList = new LinkedList<>();
        private Map<Integer, String> headMap;

        @Override
        public void invoke(T t, AnalysisContext analysisContext) {
            dataList.add(t);
        }

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext analysisContext) {
            this.headMap = headMap;
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        }

        /**
         * 读取额外信息,在invoke方法执行完成后,doAfterAllAnalysed方法前执行
         * 需要增加重写extra方法
         *
         * @param extra
         * @param context
         */
        @Override
        public void extra(CellExtra extra, AnalysisContext context) {
            Integer firstRowIndex = extra.getFirstRowIndex();
            Integer lastRowIndex = extra.getLastRowIndex();
            Integer firstColumnIndex = extra.getFirstColumnIndex();
            Integer lastColumnIndex = extra.getLastColumnIndex();
            String head = null;
            for (int i = firstColumnIndex; i <= lastColumnIndex; i++) {
                String headStr = headMap.get(firstColumnIndex);
                if (StringUtils.isNotEmpty(headStr)) {
                    head = headStr;
                }
            }
            String extraText = extra.getText();
            if (StringUtils.isNotEmpty(extraText)) {
                for (Field field : tClass.getDeclaredFields()) {
                    ExcelExtra annotation1 = field.getAnnotation(ExcelExtra.class);
                    if (annotation1 != null && annotation1.type().equals(extra.getType()) && annotation1.value().equals(head)) {
                        field.setAccessible(true);
                        for (int i = firstRowIndex; i <= lastRowIndex; i++) {
                            try {
                                field.set(dataList.get(i - 1), extraText);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
            if (extra.getType().equals(CellExtraTypeEnum.MERGE)) {
                cellExtraList.add(extra);
            }
        }

        public List<T> getDataList() {
            for (CellExtra extra : cellExtraList) {
                Integer firstRowIndex = extra.getFirstRowIndex();
                Integer lastRowIndex = extra.getLastRowIndex();
                Integer firstColumnIndex = extra.getFirstColumnIndex();
                Integer lastColumnIndex = extra.getLastColumnIndex();
                String head = null;
                for (int i = firstColumnIndex; i <= lastColumnIndex; i++) {
                    String headStr = headMap.get(firstColumnIndex);
                    if (StringUtils.isNotEmpty(headStr)) {
                        head = headStr;
                    }
                }
                //获取初始值
                Object initValue = getInitValueFromList(firstRowIndex, head);
                //设置值
                for (int i = firstRowIndex; i <= lastRowIndex; i++) {
                    setInitValueToList(initValue, i, head);
                }
            }
            return dataList.stream().distinct().collect(Collectors.toList());
        }

        /**
         * 设置合并单元格的值
         *
         * @param filedValue 值
         * @param rowIndex   行
         */
        private void setInitValueToList(Object filedValue, Integer rowIndex, String head) {
            if (filedValue == null) {
                return;
            }
            T object = dataList.get(rowIndex - 1);
            for (Field field : object.getClass().getDeclaredFields()) {
                //提升反射性能，关闭安全检查
                field.setAccessible(true);
                ExcelProperty annotation = field.getAnnotation(ExcelProperty.class);
                ExcelExtra excelExtra = null;
                if (annotation == null) {
                    excelExtra = field.getAnnotation(ExcelExtra.class);
                }
                if ((annotation != null && Arrays.stream(annotation.value()).collect(Collectors.toList()).contains(head)) || (excelExtra != null && excelExtra.value().equals(head))) {
                    try {
                        field.set(object, filedValue);
                        break;
                    } catch (IllegalAccessException e) {
                        log.error("设置合并单元格的值异常：" + e.getMessage());
                    }

                }
            }
        }


        /**
         * 获取合并单元格的初始值
         * rowIndex对应list的索引
         * columnIndex对应实体内的字段
         *
         * @param firstRowIndex 起始行
         * @return 初始值
         */
        private Object getInitValueFromList(Integer firstRowIndex, String head) {
            Object filedValue = null;
            T object = dataList.get(firstRowIndex - 1);
            for (Field field : object.getClass().getDeclaredFields()) {
                //提升反射性能，关闭安全检查
                field.setAccessible(true);
                ExcelProperty annotation = field.getAnnotation(ExcelProperty.class);
                ExcelExtra excelExtra = null;
                if (annotation == null) {
                    excelExtra = field.getAnnotation(ExcelExtra.class);
                }
                if ((annotation != null && Arrays.stream(annotation.value()).collect(Collectors.toList()).contains(head)) || (excelExtra != null && excelExtra.value().equals(head))) {
                    try {
                        filedValue = field.get(object);
                        break;
                    } catch (IllegalAccessException e) {
                        log.error("设置合并单元格的初始值异常：" + e.getMessage());
                    }
                }
            }
            return filedValue;
        }
    }
}
