package com.example.demo.service.impl;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LocalDateRecycleConverter implements Converter<LocalDate> {

    private static final DateTimeFormatter[] FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd")
    };

    @Override
    public Class<LocalDate> supportJavaTypeKey() {
        return LocalDate.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        // 同时兼容字符串、数字（Excel原生日期序列号）两种单元格类型
        return CellDataTypeEnum.STRING;
    }

    @Override
    public LocalDate convertToJavaData(ReadCellData<?> cellData,
                                       ExcelContentProperty contentProperty,
                                       GlobalConfiguration globalConfiguration) {
        String value = cellData.getStringValue();
        // 空值、空白直接返回 null
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String trimValue = value.trim();
        // 依次尝试所有支持的格式
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(trimValue, formatter);
            } catch (DateTimeParseException ignored) {
                // 格式不匹配，尝试下一个
            }
        }

        // 所有格式都解析失败，返回 null，不抛出异常打断读取
        return null;
    }
}
