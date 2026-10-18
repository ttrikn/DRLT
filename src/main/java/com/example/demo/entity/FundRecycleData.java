package com.example.demo.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.example.demo.service.impl.LocalDateRecycleConverter;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FundRecycleData implements AmountData {

    @ExcelProperty("项目名称")
    private String projectName;

    // 自定义转换器：yyyyMMdd → LocalDate
    @ExcelProperty("回收记账日期")
    private LocalDate date;

    @ExcelProperty("实际回收金额(折人民币)")
    private Double money;

    @ExcelProperty("贷款产品种类描述")
    private String sign;

    @Override public String getProjectName() { return projectName; }
    @Override public LocalDate getDate() { return date; }
    @Override public Double getMoney() { return money == null ? 0.0 : money; }
    public String getSign(){ return sign; }
}
