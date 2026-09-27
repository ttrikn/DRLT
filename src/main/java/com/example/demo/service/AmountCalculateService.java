package com.example.demo.service;

import com.example.demo.entity.AmountData;

import java.util.List;
import java.util.Map;

public interface AmountCalculateService {
    Map<Integer, Double> calMonthAmount(List<? extends AmountData> dataList);
}
