package com.example.demo.entity;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AmountDataFilter {

    private static final LocalDate CUTOFF_DATE_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate CUTOFF_DATE_2 = LocalDate.of(2026, 12, 31);
    public static <T extends AmountData> List<T> filterTimeValid(List<T> dataList) {
        return dataList.stream()
                .filter(data -> data.getDate() != null)
                .filter(data -> !data.getDate().isBefore(CUTOFF_DATE_1))
                .filter(data -> !data.getDate().isAfter(CUTOFF_DATE_2))
                .filter(data -> data.getMoney() != null)
                .collect(Collectors.toList());
    }

    public static List<ScheduleData> filterTypeValid(List<ScheduleData> dataList) {
        return dataList.stream()
                .filter(data -> {
                    String sign = data.getSign();
                    if (sign == null || sign.trim().isEmpty() || "否".equals(sign.trim())) {
                        return true;
                    }
                    if ("是".equals(sign.trim())) {
                        String type = data.getType();
                        return type != null && "本行代理参与行贷款合约".equals(type.trim());
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    public static List<FundRecycleData> filterFundRecycleValid(List<FundRecycleData> dataList) {
        return dataList.stream()
                .filter(data -> data.getDate() != null)
                .filter(data -> data.getSign() != null && (
                        data.getSign().equals("国开发展基金股东借款")
                                || data.getSign().equals("国开基础设施基金股东借款")
                                || data.getSign().equals("国开新型政策性金融工具股东借款")
                ))
                .filter(data -> !data.getDate().isBefore(CUTOFF_DATE_1))
                .filter(data -> !data.getDate().isAfter(CUTOFF_DATE_2))
                .filter(data -> data.getMoney() != null)
                .collect(Collectors.toList());
    }
}
