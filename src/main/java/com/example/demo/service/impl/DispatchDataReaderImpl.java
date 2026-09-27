package com.example.demo.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.example.demo.entity.DispatchData;
import com.example.demo.service.ExcelDataReader;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DispatchDataReaderImpl implements ExcelDataReader<DispatchData> {

    @Override
    public List<DispatchData> readData(String filePath) {
        return EasyExcel.read(filePath)
                .head(DispatchData.class)
                .excelType(ExcelTypeEnum.XLSX)
                .sheet()
                .doReadSync();
    }
}
