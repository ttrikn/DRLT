package com.example.demo.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DispatchData implements AmountData {

    @ExcelProperty("项目名称")
    private String projectName;

    @ExcelProperty("发放日期")
    private LocalDate date;

    @ExcelProperty("本次发放金额（折人民币）")
    private Double money;

    // 实现接口方法
    @Override public String getProjectName() { return projectName; }
    @Override public LocalDate getDate() { return date; }
    @Override public Double getMoney() { return money; }
}
