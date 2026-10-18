package com.example.demo.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.example.demo.entity.DispatchData;
import com.example.demo.entity.ScheduleData;
import com.example.demo.service.ExcelDataReader;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleDataReaderImpl implements ExcelDataReader<ScheduleData> {
    @Override
    public List<ScheduleData> readData(String filePath) {
        return EasyExcel.read(filePath)
                .head(ScheduleData.class)
                .excelType(ExcelTypeEnum.XLSX)
                .sheet()
                .doReadSync();
    }
}
