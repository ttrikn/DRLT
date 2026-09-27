package com.example.demo.service.impl;

import com.example.demo.entity.AmountData;
import com.example.demo.entity.DispatchData;
import com.example.demo.service.AmountCalculateService;
import org.springframework.stereotype.Service;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AmountCalculateServiceImpl implements AmountCalculateService {

    @Override
    public Map<Integer, Double> calMonthAmount(List<? extends AmountData> dataList) {
        Map<Integer, Double> monthAmountMap = new HashMap<>();

        for (AmountData data : dataList) {
            if (data.getDate() == null || data.getMoney() == null) {
                continue;
            }
            int month = data.getDate().getMonthValue();
            monthAmountMap.merge(month, data.getMoney(), Double::sum);
        }

        return monthAmountMap;
    }
}
