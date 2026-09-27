package com.example.demo;

import com.alibaba.excel.EasyExcel;
import com.example.demo.entity.EmployeeData;
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
        String sourcePath = "C:/Users/Lenovo/Desktop/工作/发放情况表.xlsx";   // 发放情况表路径
        String targetPath = "C:/Users/Lenovo/Desktop/工作/业务台账.xlsx";   // 目标汇总表路径
        int projectNameColIndex = 2;    // 项目名称所在列：第3列 → 索引2（C列）
        int amountStartColIndex = 6;    // 金额起始列：第7列 → 索引6（G列，对应1月）
        int dataStartRowIndex = 1;      // 数据起始行：第2行 → 索引1（第1行是表头）
        // ==========================================================

        try {
            // 2. 一次性读取全量发放数据（只读取一次，复用）
            List<EmployeeData> allDataList = EasyExcel.read(sourcePath)
                    .head(EmployeeData.class)
                    .sheet()
                    .doReadSync();
            System.out.println("发放情况表读取完成，共 " + allDataList.size() + " 条数据");

            // 3. 打开目标汇总表（读写模式）
            try (
                    FileInputStream fis = new FileInputStream(targetPath);
                    Workbook workbook = new XSSFWorkbook(fis)
            ) {
                Sheet sheet = workbook.getSheetAt(0);
                int lastRowNum = sheet.getLastRowNum();

                // 提前创建数字格式，循环内复用（避免重复创建样式）
                CellStyle amountStyle = workbook.createCellStyle();
                DataFormat dataFormat = workbook.createDataFormat();
                amountStyle.setDataFormat(dataFormat.getFormat("0.00"));

                // 4. 遍历目标表每一行数据
                for (int rowIndex = dataStartRowIndex; rowIndex <= lastRowNum; rowIndex++) {
                    Row currentRow = sheet.getRow(rowIndex);
                    // 行直接为空，跳过
                    if (currentRow == null) {
                        continue;
                    }

                    // 【在循环外面提前创建，复用即可】放在 lastRowNum 那一行下面就行
                    DataFormatter cellFormatter = new DataFormatter();

                    // ========== 4.1 获取当前行的项目名称（修改后） ==========
                    Cell projectNameCell = currentRow.getCell(projectNameColIndex);
                    String projectName = null;
                    if (projectNameCell != null) {
                        // 自动适配所有单元格类型：字符串/数值/日期都转成显示的字符串
                        projectName = cellFormatter.formatCellValue(projectNameCell).trim();
                    }
                    // 项目名为空，跳过该行
                    if (projectName == null || projectName.isEmpty()) {
                        continue;
                    }

                    System.out.println("正在处理项目：" + projectName);

                    // 4.2 从全量数据中筛选该项目的2026年后有效数据
                    String finalProjectName = projectName;
                    List<EmployeeData> projectDataList = allDataList.stream()
                            .filter(data -> finalProjectName.equals(data.getProjectName())) // 匹配项目名
                            .filter(data -> data.getDate() != null && !data.getDate().trim().isEmpty()) // 日期非空
                            .filter(data -> data.getDate().compareTo("2026-01-01") >= 0) // 2026年及以后
                            .filter(data -> data.getMoney() != null) // 金额非空
                            .toList();

                    // 该项目无符合条件的数据，跳过
                    if (projectDataList.isEmpty()) {
                        System.out.println("  该项目无2026年后发放数据，跳过");
                        continue;
                    }

                    // 4.3 调用方法计算各月累计金额
                    Map<Integer, Double> monthAmountMap = classService.Cal(projectDataList);

                    // 4.4 把各月金额写入对应列
                    for (Map.Entry<Integer, Double> entry : monthAmountMap.entrySet()) {
                        int month = entry.getKey();
                        double amount = entry.getValue()/10000;

                        // 计算当前月份对应的列索引：1月→起始列，2月→起始列+1，以此类推
                        int colIndex = amountStartColIndex + (month - 1);

                        // 获取单元格，不存在则创建
                        Cell cell = currentRow.getCell(colIndex);
                        if (cell == null) {
                            cell = currentRow.createCell(colIndex);
                        }

                        // 写入金额并设置格式
                        cell.setCellValue(amount);
                        cell.setCellStyle(amountStyle);
                    }

                    System.out.println("  处理完成，共 " + monthAmountMap.size() + " 个月份有数据");
                }
                workbook.setForceFormulaRecalculation(true);

                // 5. 全部处理完成后保存文件
                try (FileOutputStream fos = new FileOutputStream(targetPath)) {
                    workbook.write(fos);
                }

                System.out.println("\n全部项目处理完成！结果已保存到目标汇总表");

            } catch (Exception e) {
                System.err.println("处理目标表失败：" + e.getMessage());
                e.printStackTrace();
            }

        } finally {
            // 关闭Spring容器
            context.close();
        }
    }
}
