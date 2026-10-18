package com.example.demo.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.example.demo.entity.ScheduleData;
import com.example.demo.entity.SurplusData;
import com.example.demo.service.ExcelDataReader;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SurplusDataReaderImpl implements ExcelDataReader<SurplusData> {
    @Override
    public List<SurplusData> readData(String filePath) {
        return EasyExcel.read(filePath)
                .head(SurplusData.class)
                .excelType(ExcelTypeEnum.XLSX)
                .sheet()
                .doReadSync();
    }
}
