package com.example.demo.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;


@Data
public class SurplusData {

    @ExcelProperty("项目名称")
    private String projectName;

    @ExcelProperty("余额（折人民币，元）")
    private BigDecimal money;

    @ExcelProperty("业务分类描述")
    private String type;

    @ExcelProperty("项目经办经理A")
    private String name;

    @ExcelProperty("项目经办经理A所属处室")
    private String depart;

    public String getProjectName() { return projectName; }
    public BigDecimal getMoney() { return money; }
    public String getSign() { return type; }
    public String getName() { return name; }
    public String getDepart() { return depart; }
}
