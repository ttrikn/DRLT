package com.example.demo.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.example.demo.service.impl.LocalDateRecycleConverter;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RecycleData implements AmountData {

    @ExcelProperty("项目名称")
    private String projectName;

    // 自定义转换器：yyyyMMdd → LocalDate
    @ExcelProperty("还款日期")
    private LocalDate date;

    @ExcelProperty("还款金额")
    private Double money;

    @Override public String getProjectName() { return projectName; }
    @Override public LocalDate getDate() { return date; }
    @Override public Double getMoney() { return money; }
}
