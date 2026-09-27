package com.example.demo.service;

import java.util.Map;

public interface ExcelTargetWriteService {
    void writeMonthAmount(String targetPath,
                          Map<String, Map<Integer, Double>> projectMonthAmount,
                          int projectNameColIndex,
                          int amountStartColIndex,
                          int dataStartRowIndex);
}
