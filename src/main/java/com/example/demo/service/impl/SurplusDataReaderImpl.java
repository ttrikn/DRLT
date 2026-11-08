package com.example.demo.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.example.demo.entity.SurplusData;
import com.example.demo.service.ExcelDataReader;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SurplusDataReaderImpl implements ExcelDataReader<SurplusData> {
    @Override
    public List<SurplusData> readData(String filePath) {
        return EasyExcel.read(filePath)
                .head(SurplusData.class)
                .excelType(ExcelTypeEnum.XLSX)
                .registerReadListener(new AnalysisEventListener<SurplusData>() {
                    @Override
                    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                        for (String required : List.of("项目名称", "余额（折人民币，元）", "业务分类描述")) {
                            if (!headMap.containsValue(required)) {
                                throw new IllegalArgumentException("余额表缺少必需表头：" + required);
                            }
                        }
                    }

                    @Override
                    public void invoke(SurplusData data, AnalysisContext context) { }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext context) { }
                })
                .sheet()
                .doReadSync();
    }
}
