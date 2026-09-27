package com.example.demo.service.impl;

import com.example.demo.entity.EmployeeData;
import com.example.demo.service.IClassService;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClassServiceImpl implements IClassService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Map<Integer, Double> Cal(List<EmployeeData> after2026List) {
        Map<Integer, Double> monthAmountMap = new HashMap<>();

        for (EmployeeData data : after2026List) {
            // 1. 关键字段判空：日期、金额任一为空都直接跳过
            if (data.getDate() == null || data.getDate().trim().isEmpty()) {
                continue;
            }
            if (data.getMoney() == null) {
                continue;
            }

            // 2. 解析日期、提取月份
            LocalDate releaseDate = LocalDate.parse(data.getDate().trim(), DATE_FORMATTER);
            int month = releaseDate.getMonthValue();

            // 3. 同月金额累加
            double current = monthAmountMap.getOrDefault(month, 0.0);
            monthAmountMap.put(month, current + data.getMoney());
        }

        return monthAmountMap;
    }
}

