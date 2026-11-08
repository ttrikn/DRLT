package com.example.demo.entity;

import java.math.BigDecimal;
import java.util.List;

/** 项目级基础资料，余额单位为人民币元。 */
public record ProjectBaseInfo(String businessType, BigDecimal balance, List<BigDecimal> interestRates) {
    public ProjectBaseInfo {
        interestRates = List.copyOf(interestRates);
    }

    public ProjectBaseInfo(String businessType, BigDecimal balance) {
        this(businessType, balance, List.of());
    }
}
