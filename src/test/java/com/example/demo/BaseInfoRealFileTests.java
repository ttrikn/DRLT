package com.example.demo;

import com.example.demo.controller.ExcelProcessController;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/** 可选真实文件回归：通过系统属性传入源表和目标表，所有写入仅作用于临时副本。 */
@EnabledIfSystemProperty(named = "baseinfo.source", matches = ".+")
class BaseInfoRealFileTests {
    @TempDir Path directory;

    @Test
    void baseInfoEndpointMatchesIndependentSourceTotals() throws Exception {
        String source = System.getProperty("baseinfo.source");
        Path original = Path.of(System.getProperty("baseinfo.target"));
        Path copy = directory.resolve("output.xlsx");
        Files.copy(original, copy);
        Map<String, BigDecimal> expected = new HashMap<>();
        Map<String, Set<BigDecimal>> expectedRates = new HashMap<>();
        try (var input = Files.newInputStream(Path.of(source)); Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headers = new HashMap<>();
            for (Cell cell : sheet.getRow(0)) headers.put(cell.getStringCellValue(), cell.getColumnIndex());
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Cell name = row.getCell(headers.get("项目名称"));
                if (name == null || name.getStringCellValue().isBlank()) continue;
                Cell balance = row.getCell(headers.get("余额（折人民币，元）"));
                BigDecimal value = balance.getCellType() == CellType.NUMERIC
                        ? BigDecimal.valueOf(balance.getNumericCellValue())
                        : new BigDecimal(balance.getStringCellValue());
                expected.merge(name.getStringCellValue().trim(), value, BigDecimal::add);
            }
        }
        String interestSource = System.getProperty("baseinfo.interest");
        Map<String, BigDecimal> fundBalances = new HashMap<>();
        try (var input = Files.newInputStream(Path.of(interestSource)); Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheet("Sheet1");
            Map<String, Integer> headers = new HashMap<>();
            DataFormatter formatter = new DataFormatter();
            for (Cell cell : sheet.getRow(0)) headers.put(formatter.formatCellValue(cell), cell.getColumnIndex());
            Set<String> funds = Set.of("J0120001", "K0820001", "J0122001", "K0822001", "J0125001", "K0825001");
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String name = formatter.formatCellValue(row.getCell(headers.get("项目名称"))).trim();
                if (name.isBlank()) continue;
                BigDecimal rate = number(row.getCell(headers.get("当前执行利率")));
                if (rate != null) expectedRates.computeIfAbsent(name, key -> new TreeSet<>()).add(rate);
                if (funds.contains(formatter.formatCellValue(row.getCell(headers.get("可售产品编号"))).trim())) {
                    fundBalances.merge(name, number(row.getCell(headers.get("本金余额"))), BigDecimal::add);
                }
            }
        }
        expected.putAll(fundBalances);
        try (var context = SpringApplication.run(Demo1Application.class,
                "--spring.main.web-application-type=none", "--excel.source.surplus.path=" + source,
                "--excel.source.interest.path=" + interestSource,
                "--excel.target.path=" + copy.toAbsolutePath())) {
            String response = context.getBean(ExcelProcessController.class).processBaseInfo();
            assertTrue(response.startsWith("✅"), response);
        }
        int matched = 0;
        try (var beforeIn = Files.newInputStream(original); Workbook before = new XSSFWorkbook(beforeIn);
             var afterIn = Files.newInputStream(copy); Workbook after = new XSSFWorkbook(afterIn)) {
            Sheet sheet = after.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            assertEquals(before.getSheetAt(0).getMergedRegions(), sheet.getMergedRegions());
            for (Row row : sheet) {
                String name = formatter.formatCellValue(row.getCell(2)).trim();
                boolean detail = !name.isEmpty() && sheet.getMergedRegions().stream()
                        .noneMatch(region -> region.isInRange(row.getRowNum(), 2));
                boolean match = expected.containsKey(name);
                if (match) {
                    matched++;
                    assertEquals(expected.get(name).movePointLeft(4).doubleValue(),
                            row.getCell(7).getNumericCellValue(), 0.000001, "余额应等于全部明细之和");
                }
                if (detail) {
                    if (!match) assertEquals("-", row.getCell(7).getStringCellValue());
                    Set<BigDecimal> rates = expectedRates.getOrDefault(name, Set.of());
                    Cell rateCell = row.getCell(5);
                    if (rates.isEmpty()) assertEquals("-", rateCell.getStringCellValue());
                    else if (rates.size() == 1) assertEquals(rates.iterator().next().movePointLeft(2).doubleValue(),
                            rateCell.getNumericCellValue(), 1e-10);
                    else assertEquals(rates.stream().map(rate -> rate.stripTrailingZeros().toPlainString() + "%")
                            .collect(java.util.stream.Collectors.joining("；")), rateCell.getStringCellValue());
                }
                for (Cell cell : row) {
                    if (detail && (cell.getColumnIndex() == 4 || cell.getColumnIndex() == 5 || cell.getColumnIndex() == 7)) continue;
                    Cell old = before.getSheetAt(0).getRow(row.getRowNum()).getCell(cell.getColumnIndex());
                    assertEquals(formatter.formatCellValue(old), formatter.formatCellValue(cell),
                            "非目标单元格被改变：" + cell.getAddress());
                }
            }
        }
        assertTrue(matched > 0, "真实样表至少应匹配一个项目");
        System.out.println("Real-file baseInfo verification: " + matched + " matched projects; original file unchanged.");
    }

    private BigDecimal number(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        return cell.getCellType() == CellType.NUMERIC ? BigDecimal.valueOf(cell.getNumericCellValue())
                : new BigDecimal(cell.getStringCellValue());
    }
}
