package com.example.demo.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.example.demo.service.impl.LocalDateRecycleConverter;

import java.time.LocalDate;

public class ScheduleData implements AmountData{
    @ExcelProperty("项目名称")
    private String projectName;

    // 自定义转换器：yyyyMMdd → LocalDate
    @ExcelProperty(value = "计划本期还款日期", converter = LocalDateRecycleConverter.class)
    private LocalDate date;

    @ExcelProperty("银团贷款标志")
    private String sign;

    @ExcelProperty("银团贷款合约类型")
    private String type;

    @ExcelProperty("贷款计划本期还款本金金额")
    private Double S_money;

    @ExcelProperty("贷款已还金额")
    private Double A_money;

    @Override public String getProjectName() { return projectName; }
    @Override public LocalDate getDate() { return date; }
    @Override
    public Double getMoney() {
        // 空值兜底：为 null 时按 0 处理，避免空指针
        double plan = S_money == null ? 0.0 : S_money;
        double actual = A_money == null ? 0.0 : A_money;
        return plan - actual;
    }
    public String getSign(){
        return sign;
    }

    public String getType(){
        return type;
    }

    public void setS_money(Double s_money) {
        S_money = s_money;
    }

    public void setA_money(Double a_money) {
        A_money = a_money;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
