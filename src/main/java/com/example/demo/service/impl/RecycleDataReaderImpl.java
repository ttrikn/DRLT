package com.example.demo.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.example.demo.entity.DispatchData;
import com.example.demo.entity.RecycleData;
import com.example.demo.service.ExcelDataReader;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecycleDataReaderImpl implements ExcelDataReader<RecycleData> {

    @Override
    public List<RecycleData> readData(String filePath) {
        return EasyExcel.read(filePath)
                .head(RecycleData.class)
                .excelType(ExcelTypeEnum.XLSX)
                .sheet()
                .doReadSync();
    }
}
