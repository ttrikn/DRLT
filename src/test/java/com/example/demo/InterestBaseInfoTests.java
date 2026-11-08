package com.example.demo;

import com.example.demo.entity.InterestData;
import com.example.demo.entity.ProjectBaseInfo;
import com.example.demo.entity.SurplusData;
import com.example.demo.service.impl.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InterestBaseInfoTests {
    @TempDir Path directory;

    @Test
    void sumsExactlySixFundProductsAndCollectsAllProjectRates() {
        List<InterestData> rows = new ArrayList<>();
        for (String code : List.of("J0120001", "K0820001", "J0122001", "K0822001", "J0125001", "K0825001")) {
            rows.add(interest("基金", code, "10000.01", "2.50"));
        }
        rows.add(interest("基金", "K0620001", "99999", "2.5"));
        rows.add(interest("贷款", "A0120001", "99999", "3.75"));
        rows.add(interest("贷款", "A0120001", "0", "3.545"));
        rows.add(interest("仅有利率", "A0120001", "10", "0"));
        var loan = new SurplusData(); loan.setProjectName("贷款"); loan.setMoney(new BigDecimal("20000"));
        var result = new ProjectBaseInfoAggregator().aggregate(List.of(loan), rows);
        assertEquals(new BigDecimal("60000.06"), result.get("基金").balance());
        assertEquals(1, result.get("基金").interestRates().size());
        assertEquals(new BigDecimal("20000"), result.get("贷款").balance());
        assertEquals(List.of(new BigDecimal("3.545"), new BigDecimal("3.75")), result.get("贷款").interestRates());
        assertNull(result.get("仅有利率").balance());
        assertEquals(0, result.get("仅有利率").interestRates().get(0).compareTo(BigDecimal.ZERO));
    }

    @Test
    void partialMissingFundBalanceIsUnavailableAndMissingRateIsIndependent() {
        var result = new ProjectBaseInfoAggregator().aggregate(List.of(), List.of(
                interest("项目", "J0120001", "10", null),
                interest("项目", "K0820001", null, "2.1")));
        assertNull(result.get("项目").balance());
        assertEquals(List.of(new BigDecimal("2.1")), result.get("项目").interestRates());
    }

    @Test
    void readerUsesNamedDetailSheetAndRejectsMissingHeaders() throws Exception {
        Path source = directory.resolve("interest.xlsx");
        try (Workbook book = new XSSFWorkbook()) {
            book.createSheet("Sheet2").createRow(0).createCell(0).setCellValue("辅助表");
            Sheet sheet = book.createSheet("Sheet1");
            String[] names = {"项目名称", "可售产品编号", "本金余额", "当前执行利率"};
            Row header = sheet.createRow(0);
            for (int c = 0; c < names.length; c++) header.createCell(c).setCellValue(names[c]);
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("项目"); row.createCell(1).setCellValue("J0120001");
            row.createCell(2).setCellValue(123.45); row.createCell(3).setCellValue(3.545);
            try (var out = Files.newOutputStream(source)) { book.write(out); }
            var actual = new InterestDataReaderImpl().readData(source.toString());
            assertEquals(1, actual.size());
            assertEquals(0, actual.get(0).getInterestRate().compareTo(new BigDecimal("3.545")));
            header.getCell(3).setCellValue("错误表头");
            try (var out = Files.newOutputStream(source)) { book.write(out); }
        }
        assertThrows(Exception.class, () -> new InterestDataReaderImpl().readData(source.toString()));
    }

    @Test
    void writesSingleAndMultipleRatesAndReplacesStaleValuesWithoutTouchingSummaries() throws Exception {
        Path target = directory.resolve("output.xlsx");
        try (Workbook book = new XSSFWorkbook()) {
            Sheet sheet = book.createSheet();
            sheet.createRow(0).createCell(2).setCellValue("项目名称");
            sheet.addMergedRegion(new CellRangeAddress(0, 2, 2, 2));
            for (int r = 3; r < 6; r++) {
                Row row = sheet.createRow(r); row.createCell(2).setCellValue("项目" + r);
                row.createCell(5).setCellValue(99); row.createCell(7).setCellValue(99);
            }
            sheet.createRow(6).createCell(0).setCellValue("合计");
            sheet.addMergedRegion(new CellRangeAddress(6, 6, 0, 5));
            sheet.getRow(6).createCell(7).setCellFormula("SUM(H4:H6)");
            try (var out = Files.newOutputStream(target)) { book.write(out); }
        }
        Map<String, ProjectBaseInfo> data = Map.of(
                "项目3", new ProjectBaseInfo(null, BigDecimal.ZERO, List.of(new BigDecimal("3.545"))),
                "项目4", new ProjectBaseInfo(null, null, List.of(new BigDecimal("2.5"), new BigDecimal("3.75"))));
        var writer = new ExcelTargetWriteServiceImpl();
        writer.writeBaseInfo(target.toString(), data, 2, 4, 7, 5, 0);
        try (var in = Files.newInputStream(target); Workbook book = new XSSFWorkbook(in)) {
            Sheet s = book.getSheetAt(0);
            assertEquals("-", s.getRow(3).getCell(4).getStringCellValue());
            assertEquals(0.03545, s.getRow(3).getCell(5).getNumericCellValue(), 1e-10);
            assertEquals("3.545%", new DataFormatter(Locale.US).formatCellValue(s.getRow(3).getCell(5)));
            assertEquals(0, s.getRow(3).getCell(7).getNumericCellValue());
            assertEquals("2.5%；3.75%", s.getRow(4).getCell(5).getStringCellValue());
            assertEquals("-", s.getRow(4).getCell(7).getStringCellValue());
            assertEquals("-", s.getRow(5).getCell(5).getStringCellValue());
            assertEquals("-", s.getRow(5).getCell(7).getStringCellValue());
            assertEquals("SUM(H4:H6)", s.getRow(6).getCell(7).getCellFormula());
            assertNull(s.getRow(0).getCell(5));
        }
        writer.writeBaseInfo(target.toString(), Map.of(), 2, 4, 7, 5, 0);
        try (var in = Files.newInputStream(target); Workbook book = new XSSFWorkbook(in)) {
            assertEquals("-", book.getSheetAt(0).getRow(3).getCell(5).getStringCellValue());
            assertEquals("-", book.getSheetAt(0).getRow(3).getCell(7).getStringCellValue());
        }
    }

    private InterestData interest(String name, String code, String balance, String rate) {
        var row = new InterestData(); row.setProjectName(name); row.setProductCode(code);
        row.setPrincipalBalance(balance == null ? null : new BigDecimal(balance));
        row.setInterestRate(rate == null ? null : new BigDecimal(rate));
        return row;
    }
}
