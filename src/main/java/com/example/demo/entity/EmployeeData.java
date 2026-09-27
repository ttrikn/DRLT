package com.example.demo.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data // 自动生成getter/setter，没装Lombok就手动写getter和setter
public class EmployeeData {

    // value 对应 Excel 里的表头名称，必须完全一致
    @ExcelProperty("客户名称")
    private String CustomerName;

    @ExcelProperty("项目名称")
    private String ProjectName;

    @ExcelProperty("本次发放金额（折人民币）")
    private Long money;

    @ExcelProperty("发放日期")
    private String date;

    @ExcelProperty("贷款余额（表内）")
    private Long balance;
}
