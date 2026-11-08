package com.example.demo.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.example.demo.entity.InterestData;
import com.example.demo.service.ExcelDataReader;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class InterestDataReaderImpl implements ExcelDataReader<InterestData> {
    @Override
    public List<InterestData> readData(String path) {
        return EasyExcel.read(path).head(InterestData.class)
                .registerReadListener(new AnalysisEventListener<InterestData>() {
                    @Override
                    public void invokeHeadMap(Map<Integer, String> headers, AnalysisContext context) {
                        for (String required : List.of("项目名称", "可售产品编号", "本金余额", "当前执行利率")) {
                            if (!headers.containsValue(required)) {
                                throw new IllegalArgumentException("结息表缺少必需表头：" + required);
                            }
                        }
                    }
                    @Override
                    public void invoke(InterestData row, AnalysisContext context) { }
                    @Override
                    public void doAfterAllAnalysed(AnalysisContext context) { }
                })
                // 实际文件的第一个工作表是辅助表 Sheet2。
                .sheet("Sheet1").doReadSync();
    }
}
