package com.example.demo.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;

/** 结息明细，当前执行利率以百分数数值存储，例如 3.75 表示 3.75%。 */
@Data
public class InterestData {
    @ExcelProperty("项目名称")
    private String projectName;
    @ExcelProperty("可售产品编号")
    private String productCode;
    @ExcelProperty("本金余额")
    private BigDecimal principalBalance;
    @ExcelProperty("当前执行利率")
    private BigDecimal interestRate;
}
