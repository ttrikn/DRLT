package com.example.demo.service.impl;

import com.example.demo.config.ExcelProperties;
import com.example.demo.entity.*;
import com.example.demo.service.AmountCalculateService;
import com.example.demo.service.ExcelDataReader;
import com.example.demo.service.ExcelProcessService;
import com.example.demo.service.ExcelTargetWriteService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExcelProcessServiceImpl implements ExcelProcessService {

    @Resource(name = "dispatchDataReaderImpl")
    private ExcelDataReader<DispatchData> dispatchDataReader;

    @Resource(name = "recycleDataReaderImpl")
    private ExcelDataReader<RecycleData> recycleDataReader;

    @Resource(name = "scheduleDataReaderImpl")
    private ExcelDataReader<ScheduleData> scheduleDataReader;

    @Resource(name = "fundRecycleDataReaderImpl")
    private ExcelDataReader<FundRecycleData> fundRecycleDataReader;

    @Resource(name = "surplusDataReaderImpl")
    private ExcelDataReader<SurplusData> surplusDataReader;

    @Resource
    private AmountCalculateService amountCalculateService;

    @Resource
    private ExcelTargetWriteService excelTargetWriteService;

    private final ExcelProperties properties;

    public ExcelProcessServiceImpl(ExcelProperties properties) {
        this.properties = properties;
    }

    @Override
    public void processBaseData(){
        List<SurplusData> allDataList = surplusDataReader.readData(properties.source().surplus().path());
        System.out.println("余额表读取完成，共 " + allDataList.size() + " 条数据");

        Map<String, String> projectType = new HashMap<>();
        for (SurplusData data : allDataList){
            projectType.computeIfAbsent(data.getProjectName(), k -> data.getType());
        }
        excelTargetWriteService.writeType(properties.target().path(),
                projectType,
                properties.target().projectNameColIndex(),
                properties.target().amountStartColIndex(),
                properties.target().dataStartRowIndex());
    }

    @Override
    public void processDispatchData() {
        // 1. 读取全量发放数据
        List<DispatchData> allDataList = dispatchDataReader.readData(properties.source().dispatch().path());
        System.out.println("发放情况表读取完成，共 " + allDataList.size() + " 条数据");

        // 2. 筛选2026年及以后的有效数据
        List<DispatchData> validList = AmountDataFilter.filterTimeValid(allDataList);

        // 3. 按项目名称分组
        Map<String, List<DispatchData>> projectGroupMap = new HashMap<>();
        for (DispatchData data : validList) {
            String name = data.getProjectName();
            projectGroupMap.computeIfAbsent(name, k -> new ArrayList<>()).add(data);
        }

        // 4. 逐个项目计算各月累计金额
        Map<String, Map<Integer, Double>> projectMonthAmount = projectGroupMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> amountCalculateService.calMonthAmount(entry.getValue())
                ));

        // 5. 批量写入目标台账
        excelTargetWriteService.writeMonthAmount(
                properties.target().path(),
                projectMonthAmount,
                properties.target().projectNameColIndex(),
                properties.target().amountStartColIndex()+2,
                properties.target().dataStartRowIndex()
        );

        System.out.println("全部项目处理完成！结果已保存到业务台账");
    }

    @Override
    public void processRecycleData(){
        List<RecycleData> allDataList = recycleDataReader.readData(properties.source().recycle().path());
        System.out.println("回收情况表读取完成，共 " + allDataList.size() + " 条数据");

        List<RecycleData> validList = AmountDataFilter.filterTimeValid(allDataList);

        Map<String, List<RecycleData>> projectGroupMap = new HashMap<>();
        for (RecycleData data : validList) {
            String name = data.getProjectName();
            projectGroupMap.computeIfAbsent(name, k -> new ArrayList<>()).add(data);
        }

        Map<String, Map<Integer, Double>> projectMonthAmount = projectGroupMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> amountCalculateService.calMonthAmount(entry.getValue())
                ));

        excelTargetWriteService.writeMonthAmount(
                properties.target().path(),
                projectMonthAmount,
                properties.target().projectNameColIndex(),
                properties.target().amountStartColIndex()+15,
                properties.target().dataStartRowIndex()
        );

        System.out.println("全部项目处理完成！结果已保存到业务台账");
    }

    @Override
    public void processScheduleData(){
        List<ScheduleData> allDataList = scheduleDataReader.readData(properties.source().schedule().path());
        System.out.println("计划回收情况表读取完成，共 " + allDataList.size() + " 条数据");

        List<ScheduleData> validList = AmountDataFilter.filterTypeValid(AmountDataFilter.filterTimeValid(allDataList));

        Map<String, List<ScheduleData>> projectGroupMap = new HashMap<>();
        for (ScheduleData data : validList) {
            String name = data.getProjectName();
            projectGroupMap.computeIfAbsent(name, k -> new ArrayList<>()).add(data);
        }

        Map<String, Map<Integer, Double>> projectMonthAmount = projectGroupMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> amountCalculateService.calMonthAmount(entry.getValue())
                ));

        excelTargetWriteService.writeMonthAmount(
                properties.target().path(),
                projectMonthAmount,
                properties.target().projectNameColIndex(),
                properties.target().amountStartColIndex()+15,
                properties.target().dataStartRowIndex()
        );

        System.out.println("全部项目处理完成！结果已保存到业务台账");
    }

    @Override
    public void processFundRecycleData(){
        List<FundRecycleData> allDataList = fundRecycleDataReader.readData(properties.source().fundRecycle().path());
        System.out.println("基金回收情况表读取完成，共 " + allDataList.size() + " 条数据");

        List<FundRecycleData> validList = AmountDataFilter.filterFundRecycleValid(allDataList);

        Map<String, List<FundRecycleData>> projectGroupMap = new HashMap<>();
        for (FundRecycleData data : validList) {
            String name = data.getProjectName();
            projectGroupMap.computeIfAbsent(name, k -> new ArrayList<>()).add(data);
        }

        Map<String, Map<Integer, Double>> projectMonthAmount = projectGroupMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> amountCalculateService.calMonthAmount(entry.getValue())
                ));

        excelTargetWriteService.writeMonthAmount(
                properties.target().path(),
                projectMonthAmount,
                properties.target().projectNameColIndex(),
                properties.target().amountStartColIndex()+15,
                properties.target().dataStartRowIndex()
        );

        System.out.println("全部项目处理完成！结果已保存到业务台账");
    }
}
