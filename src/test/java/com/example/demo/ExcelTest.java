package com.example.demo;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.example.demo.entity.EmployeeData;
import com.example.demo.entity.FundRecycleData;
import com.example.demo.entity.ScheduleData;
import com.example.demo.service.impl.ClassServiceImpl;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ExcelTest {

    public static void main(String[] args) {

        ConfigurableApplicationContext context = SpringApplication.run(Demo1Application.class, args);

        ClassServiceImpl classService = context.getBean(ClassServiceImpl.class);

        // ========== 可配置参数，根据你的表格实际情况调整 ==========
        String sourcePath = "C:/Users/Lenovo/Desktop/work/tool/data/fundrecycledata.xlsx";   // 发放情况表路径
        String targetPath = "C:/Users/Lenovo/Desktop/工作/业务台账.xlsx";   // 目标汇总表路径
        int projectNameColIndex = 2;    // 项目名称所在列：第3列 → 索引2（C列）
        int amountStartColIndex = 6;    // 金额起始列：第7列 → 索引6（G列，对应1月）
        int dataStartRowIndex = 1;      // 数据起始行：第2行 → 索引1（第1行是表头）
        // ==========================================================

        try {
            List<FundRecycleData> rawList = EasyExcel.read(sourcePath)
                .head(FundRecycleData.class)
                .excelType(ExcelTypeEnum.XLSX)
                .sheet()
                // 关闭自动忽略错误，抛出完整异常栈（含行号列号）
                .doReadSync();
            System.out.println(rawList.size());

        } finally {
            // 关闭Spring容器
            context.close();
        }
    }
}
