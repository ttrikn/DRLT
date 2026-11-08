package com.example.demo;

import com.example.demo.entity.ProjectBaseInfo;
import com.example.demo.entity.SurplusData;
import com.example.demo.service.impl.ExcelTargetWriteServiceImpl;
import com.example.demo.service.impl.ProjectBaseInfoAggregator;
import com.example.demo.service.impl.SurplusDataReaderImpl;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BaseInfoTests {
    @TempDir Path directory;

    @Test
    void sumsEveryContractPreciselyAndKeepsDistinctClassifications() {
        var info = new ProjectBaseInfoAggregator().aggregate(List.of(
                row(" 项目A ", "类别一", "10000.10"),
                row("项目A", " 类别一 ", "20000.20"),
                row("项目A", "类别二", "0.03"),
                row("项目B", null, "0"),
                row(" ", "合计", "30000.33")));
        assertEquals(new BigDecimal("30000.33"), info.get("项目A").balance());
        assertEquals("类别一；类别二", info.get("项目A").businessType());
        assertEquals(BigDecimal.ZERO, info.get("项目B").balance());
        assertEquals(2, info.size());
    }

    @Test
    void representsMissingBalanceWithoutReportingAnUnderstatedTotal() {
        assertNull(new ProjectBaseInfoAggregator()
                .aggregate(List.of(row("项目A", "类别一", null))).get("项目A").balance());
    }

    @Test
    void rejectsMissingRequiredHeaders() throws Exception {
        Path source = directory.resolve("missing-header.xlsx");
        try (Workbook book = new XSSFWorkbook()) {
            book.createSheet().createRow(0).createCell(0).setCellValue("项目名称");
            try (var output = Files.newOutputStream(source)) { book.write(output); }
        }
        assertThrows(Exception.class, () -> new SurplusDataReaderImpl().readData(source.toString()));
    }

    @Test
    void readsMultipleContractRowsByHeaderAndWritesBothFieldsWithoutChangingOtherCells() throws Exception {
        Path source = directory.resolve("source.xlsx");
        try (Workbook book = new XSSFWorkbook()) {
            Sheet sheet = book.createSheet();
            String[] headers = {"合同号", "项目名称", "余额（折人民币，元）", "业务分类描述"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            for (int i = 1; i <= 2; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue("合同" + i);
                row.createCell(1).setCellValue("项目A");
                row.createCell(2).setCellValue(i * 10000.25);
                row.createCell(3).setCellValue("业务分类");
            }
            try (var output = Files.newOutputStream(source)) { book.write(output); }
        }
        var rows = new SurplusDataReaderImpl().readData(source.toString());
        assertEquals(2, rows.size());
        var projects = new ProjectBaseInfoAggregator().aggregate(rows);
        assertEquals(0, new BigDecimal("30000.75").compareTo(projects.get("项目A").balance()));

        Path target = directory.resolve("target.xlsx");
        try (Workbook book = new XSSFWorkbook()) {
            Sheet sheet = book.createSheet();
            sheet.createRow(0).createCell(0).setCellValue("表头");
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
            Row row = sheet.createRow(3);
            row.createCell(2).setCellValue(" 项目A ");
            row.createCell(4).setCellValue("旧分类");
            Cell balance = row.createCell(7);
            balance.setCellValue(999);
            CellStyle style = book.createCellStyle();
            style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setBorderBottom(BorderStyle.THIN);
            balance.setCellStyle(style);
            row.createCell(8).setCellValue(88);
            Row unmatched = sheet.createRow(4);
            unmatched.createCell(2).setCellValue("未匹配项目");
            unmatched.createCell(4).setCellValue("保留分类");
            unmatched.createCell(7).setCellValue(77);
            sheet.createRow(5).createCell(7).setCellFormula("SUM(H4:H5)");
            try (var output = Files.newOutputStream(target)) { book.write(output); }
        }
        var writer = new ExcelTargetWriteServiceImpl();
        writer.writeBaseInfo(target.toString(), projects, 2, 4, 7, 5, 3);
        writer.writeBaseInfo(target.toString(), projects, 2, 4, 7, 5, 3);
        try (var input = Files.newInputStream(target); Workbook book = new XSSFWorkbook(input)) {
            Sheet sheet = book.getSheetAt(0);
            assertEquals("业务分类", sheet.getRow(3).getCell(4).getStringCellValue());
            assertEquals("@", sheet.getRow(3).getCell(4).getCellStyle().getDataFormatString());
            Cell balance = sheet.getRow(3).getCell(7);
            assertEquals(3.000075, balance.getNumericCellValue(), 0.000000001);
            assertEquals("0.00", balance.getCellStyle().getDataFormatString());
            assertEquals(IndexedColors.LIGHT_YELLOW.getIndex(), balance.getCellStyle().getFillForegroundColor());
            assertEquals(BorderStyle.THIN, balance.getCellStyle().getBorderBottom());
            assertEquals(88, sheet.getRow(3).getCell(8).getNumericCellValue());
            assertEquals("-", sheet.getRow(4).getCell(4).getStringCellValue());
            assertEquals("-", sheet.getRow(4).getCell(7).getStringCellValue());
            assertEquals("-", sheet.getRow(4).getCell(5).getStringCellValue());
            assertEquals("SUM(H4:H5)", sheet.getRow(5).getCell(7).getCellFormula());
            assertEquals(1, sheet.getNumMergedRegions());
        }
        // 余额变成零后重跑应覆盖旧值，不能累加或跳过零。
        writer.writeBaseInfo(target.toString(), Map.of("项目A", new ProjectBaseInfo(" ", BigDecimal.ZERO)), 2, 4, 7, 5, 3);
        try (var input = Files.newInputStream(target); Workbook book = new XSSFWorkbook(input)) {
            assertEquals(0, book.getSheetAt(0).getRow(3).getCell(7).getNumericCellValue());
            assertEquals("-", book.getSheetAt(0).getRow(3).getCell(4).getStringCellValue());
        }
    }

    private SurplusData row(String name, String type, String balance) {
        SurplusData row = new SurplusData();
        row.setProjectName(name);
        row.setType(type);
        row.setMoney(balance == null ? null : new BigDecimal(balance));
        return row;
    }
}
