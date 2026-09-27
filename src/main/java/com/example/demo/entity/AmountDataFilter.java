package com.example.demo.entity;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class AmountDataFilter {

    private static final LocalDate CUTOFF_DATE = LocalDate.of(2026, 1, 1);
    public static <T extends AmountData> List<T> filterValid(List<T> dataList) {
        return dataList.stream()
                .filter(data -> data.getDate() != null)
                .filter(data -> !data.getDate().isBefore(CUTOFF_DATE))
                .filter(data -> data.getMoney() != null)
                .collect(Collectors.toList());
    }
}
