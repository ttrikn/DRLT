package com.example.demo.service;

import com.example.demo.entity.ProjectBaseInfo;
import java.util.Map;

public interface ExcelTargetWriteService {
    void writeMonthAmount(String targetPath,
                          Map<String, Map<Integer, Double>> projectMonthAmount,
                          int projectNameColIndex,
                          int amountStartColIndex,
                          int dataStartRowIndex);
    void writeBaseInfo(String targetPath,
                   Map<String, ProjectBaseInfo> projectInfo,
                   int projectNameColIndex,
                   int businessTypeColIndex,
                   int loanBalanceColIndex,
                   int interestRateColIndex,
                   int dataStartRowIndex);
}
