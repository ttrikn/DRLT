package com.example.demo.service.impl;

import com.example.demo.service.ExcelTargetWriteService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;

@Service
public class ExcelTargetWriteServiceImpl implements ExcelTargetWriteService {

    @Override
    public void writeMonthAmount(String targetPath,
                                 Map<String, Map<Integer, Double>> projectMonthAmount,
                                 int projectNameColIndex,
                                 int amountStartColIndex,
                                 int dataStartRowIndex) {

        try (
                FileInputStream fis = new FileInputStream(targetPath);
                Workbook workbook = new XSSFWorkbook(fis)
        ) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            // 预创建样式（复用，避免循环内重复创建）
            CellStyle amountStyle = buildAmountStyle(workbook);
            DataFormatter cellFormatter = new DataFormatter();

            // 遍历目标表所有数据行
            for (int rowIndex = dataStartRowIndex; rowIndex <= lastRowNum; rowIndex++) {
                Row currentRow = sheet.getRow(rowIndex);
                if (currentRow == null) {
                    continue;
                }

                // 读取项目名称（兼容数值/字符串格式）
                String projectName = getProjectName(currentRow, projectNameColIndex, cellFormatter);
                if (projectName == null || projectName.isEmpty()) {
                    continue;
                }

                // 匹配该项目的金额数据
                Map<Integer, Double> monthAmountMap = projectMonthAmount.get(projectName);
                if (monthAmountMap == null || monthAmountMap.isEmpty()) {
                    continue;
                }

                // 写入该行各月金额
                writeRowAmounts(currentRow, monthAmountMap, amountStartColIndex, amountStyle);
            }

            // 强制打开时重算所有公式
            workbook.setForceFormulaRecalculation(true);

            // 保存文件
            try (FileOutputStream fos = new FileOutputStream(targetPath)) {
                workbook.write(fos);
            }

        } catch (Exception e) {
            throw new RuntimeException("写入目标台账失败：" + e.getMessage(), e);
        }
    }

    /**
     * 构建金额单元格样式：2位小数 + 四周细边框
     */
    private CellStyle buildAmountStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("0.00"));

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    /**
     * 安全读取项目名称，兼容所有单元格类型
     */
    private String getProjectName(Row row, int colIndex, DataFormatter formatter) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }
        return formatter.formatCellValue(cell).trim();
    }

    /**
     * 写入单行的各月金额 + 单位转换
     */
    private void writeRowAmounts(Row row,
                                 Map<Integer, Double> monthAmountMap,
                                 int startColIndex,
                                 CellStyle style) {
        for (Map.Entry<Integer, Double> entry : monthAmountMap.entrySet()) {
            int month = entry.getKey();
            double amount = entry.getValue() / 10000;

            int colIndex = startColIndex + (month - 1);
            Cell cell = row.getCell(colIndex);
            if (cell == null) {
                cell = row.createCell(colIndex);
            }

            cell.setCellValue(amount);
            cell.setCellStyle(style);
        }
    }
}
