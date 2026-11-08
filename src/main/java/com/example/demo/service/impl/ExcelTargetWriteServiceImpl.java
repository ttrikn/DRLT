package com.example.demo.service.impl;

import com.example.demo.service.ExcelTargetWriteService;
import com.example.demo.entity.ProjectBaseInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
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

    @Override
    public void writeBaseInfo(String targetPath,
                              Map<String, ProjectBaseInfo> projectInfo,
                              int projectNameColIndex,
                              int businessTypeColIndex,
                              int loanBalanceColIndex,
                              int interestRateColIndex,
                              int dataStartRowIndex) {
        Path target = Path.of(targetPath).toAbsolutePath();
        Path temporary = null;
        try {
            // 先关闭输入流，完成全部修改后再发布文件。
            Workbook loaded;
            try (var input = Files.newInputStream(target)) {
                loaded = new XSSFWorkbook(input);
            }
            try (Workbook workbook = loaded) {
                Sheet sheet = workbook.getSheetAt(0);
                DataFormatter formatter = new DataFormatter();
                Map<String, CellStyle> styles = new HashMap<>();
                for (int rowIndex = dataStartRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }
                    String projectName = getProjectName(row, projectNameColIndex, formatter);
                    if (projectName == null || projectName.isEmpty()) {
                        continue;
                    }
                    // 表头和小结可能在项目名称列内合并，不能填入缺失标记。
                    if (sheet.getMergedRegions().stream().anyMatch(region ->
                            region.isInRange(row.getRowNum(), projectNameColIndex))) {
                        continue;
                    }
                    ProjectBaseInfo info = projectInfo.get(projectName);
                    Cell typeCell = row.getCell(businessTypeColIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String businessType = info == null ? null : info.businessType();
                    typeCell.setCellValue(businessType == null || businessType.isBlank() ? "-" : businessType);
                    applyNumberFormat(workbook, typeCell, "@", styles);

                    Cell balanceCell = row.getCell(loanBalanceColIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    if (info == null || info.balance() == null) {
                        balanceCell.setCellValue("-");
                        applyNumberFormat(workbook, balanceCell, "@", styles);
                    } else {
                        balanceCell.setCellValue(info.balance().movePointLeft(4).doubleValue());
                        applyNumberFormat(workbook, balanceCell, "0.00", styles);
                    }
                    Cell rateCell = row.getCell(interestRateColIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    if (info == null || info.interestRates().isEmpty()) {
                        rateCell.setCellValue("-");
                        applyNumberFormat(workbook, rateCell, "@", styles);
                    } else if (info.interestRates().size() == 1) {
                        rateCell.setCellValue(info.interestRates().get(0).movePointLeft(2).doubleValue());
                        applyNumberFormat(workbook, rateCell, "0.00#####%", styles);
                    } else {
                        rateCell.setCellValue(info.interestRates().stream()
                                .map(rate -> rate.stripTrailingZeros().toPlainString() + "%")
                                .collect(java.util.stream.Collectors.joining("；")));
                        applyNumberFormat(workbook, rateCell, "@", styles);
                    }
                }
                workbook.setForceFormulaRecalculation(true);
                temporary = Files.createTempFile(target.getParent(), "base-info-", ".xlsx");
                try (var output = Files.newOutputStream(temporary)) {
                    workbook.write(output);
                }
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("写入目标台账失败：" + e.getMessage(), e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (java.io.IOException ignored) {
                    // 清理失败不覆盖原始写入异常。
                }
            }
        }
    }

    /** 仅调整显示格式，复用原有字体、边框、底色和对齐方式。 */
    private void applyNumberFormat(Workbook workbook, Cell cell, String format,
                                   Map<String, CellStyle> styles) {
        if (format.equals(cell.getCellStyle().getDataFormatString())) {
            return;
        }
        String key = cell.getCellStyle().getIndex() + ":" + format;
        CellStyle style = styles.computeIfAbsent(key, ignored -> {
            CellStyle copy = workbook.createCellStyle();
            copy.cloneStyleFrom(cell.getCellStyle());
            copy.setDataFormat(workbook.createDataFormat().getFormat(format));
            return copy;
        });
        cell.setCellStyle(style);
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
