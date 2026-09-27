package com.example.demo.service.impl;

import com.example.demo.entity.AmountDataFilter;
import com.example.demo.entity.DispatchData;
import com.example.demo.entity.RecycleData;
import com.example.demo.service.AmountCalculateService;
import com.example.demo.service.ExcelDataReader;
import com.example.demo.service.ExcelProcessService;
import com.example.demo.service.ExcelTargetWriteService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExcelProcessServiceImpl implements ExcelProcessService {

    // 指定注入发放数据读取实现
    @Resource(name = "dispatchDataReaderImpl")
    private ExcelDataReader<DispatchData> dispatchDataReader;

    @Resource(name = "recycleDataReaderImpl")
    private ExcelDataReader<RecycleData> recycleDataReader;

    @Resource
    private AmountCalculateService amountCalculateService;

    @Resource
    private ExcelTargetWriteService excelTargetWriteService;

    // 从配置文件读取参数（配置外部化，不硬编码）
    @Value("${excel.source.dispatch.path}")
    private String dispatchSourcePath;

    @Value("${excel.source.recycle.path}")
    private String recycleSourcePath;

    @Value("${excel.target.path}")
    private String targetPath;

    @Value("${excel.target.project-name-col-index}")
    private int projectNameColIndex;

    @Value("${excel.target.amount-start-col-index}")
    private int amountStartColIndex;

    @Value("${excel.target.data-start-row-index}")
    private int dataStartRowIndex;

    @Override
    public void processDispatchData() {
        // 1. 读取全量发放数据
        List<DispatchData> allDataList = dispatchDataReader.readData(dispatchSourcePath);
        System.out.println("发放情况表读取完成，共 " + allDataList.size() + " 条数据");

        // 2. 筛选2026年及以后的有效数据
        List<DispatchData> validList = AmountDataFilter.filterValid(allDataList);

        // 3. 按项目名称分组
        Map<String, List<DispatchData>> projectGroupMap = validList.stream()
                .collect(Collectors.groupingBy(DispatchData::getProjectName));

        // 4. 逐个项目计算各月累计金额
        Map<String, Map<Integer, Double>> projectMonthAmount = projectGroupMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> amountCalculateService.calMonthAmount(entry.getValue())
                ));

        // 5. 批量写入目标台账
        excelTargetWriteService.writeMonthAmount(
                targetPath,
                projectMonthAmount,
                projectNameColIndex,
                amountStartColIndex,
                dataStartRowIndex
        );

        System.out.println("全部项目处理完成！结果已保存到业务台账");
    }

    @Override
    public void processRecycleData(){
        List<RecycleData> allDataList = recycleDataReader.readData(recycleSourcePath);
        System.out.println("回收情况表读取完成，共 " + allDataList.size() + " 条数据");

        // 2. 筛选2026年及以后的有效数据
        List<RecycleData> validList = AmountDataFilter.filterValid(allDataList);

        // 3. 按项目名称分组
        Map<String, List<RecycleData>> projectGroupMap = validList.stream()
                .collect(Collectors.groupingBy(RecycleData::getProjectName));

        // 4. 逐个项目计算各月累计金额
        Map<String, Map<Integer, Double>> projectMonthAmount = projectGroupMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> amountCalculateService.calMonthAmount(entry.getValue())
                ));

        // 5. 批量写入目标台账
        excelTargetWriteService.writeMonthAmount(
                targetPath,
                projectMonthAmount,
                projectNameColIndex,
                amountStartColIndex+13,
                dataStartRowIndex
        );

        System.out.println("全部项目处理完成！结果已保存到业务台账");
    }
}
