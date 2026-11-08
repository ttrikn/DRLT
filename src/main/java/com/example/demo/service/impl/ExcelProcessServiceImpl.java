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
import java.util.function.Function;
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

    @Resource(name = "interestDataReaderImpl")
    private ExcelDataReader<InterestData> interestDataReader;

    @Resource
    private AmountCalculateService amountCalculateService;

    @Resource
    private ExcelTargetWriteService excelTargetWriteService;

    @Resource
    private ProjectBaseInfoAggregator projectBaseInfoAggregator;

    private final ExcelProperties properties;

    public ExcelProcessServiceImpl(ExcelProperties properties) {
        this.properties = properties;
    }

    @Override
    public void processBaseData(){
        List<SurplusData> allDataList = surplusDataReader.readData(properties.source().surplus().path());
        System.out.println("余额表读取完成，共 " + allDataList.size() + " 条数据");

        List<InterestData> interestList = interestDataReader.readData(properties.source().interest().path());
        Map<String, ProjectBaseInfo> projectInfo = projectBaseInfoAggregator.aggregate(allDataList, interestList);
        excelTargetWriteService.writeBaseInfo(properties.target().path(),
                projectInfo,
                properties.target().projectNameColIndex(),
                properties.target().amountStartColIndex(),
                properties.target().loanBalanceColIndex(),
                properties.target().interestRateColIndex(),
                properties.target().dataStartRowIndex());
    }

    @Override
    public void processDispatchData() {
        processAmountData(dispatchDataReader, properties.source().dispatch().path(),
                AmountDataFilter::filterTimeValid,
                properties.target().amountStartColIndex() + 4, "发放情况表");
    }

    @Override
    public void processRecycleData() {
        processAmountData(recycleDataReader, properties.source().recycle().path(),
                AmountDataFilter::filterTimeValid,
                properties.target().amountStartColIndex() + 17, "回收情况表");
    }

    @Override
    public void processScheduleData() {
        processAmountData(scheduleDataReader, properties.source().schedule().path(),
                rows -> AmountDataFilter.filterTypeValid(AmountDataFilter.filterTimeValid(rows)),
                properties.target().amountStartColIndex() + 17, "计划回收情况表");
    }

    @Override
    public void processFundRecycleData() {
        processAmountData(fundRecycleDataReader, properties.source().fundRecycle().path(),
                AmountDataFilter::filterFundRecycleValid,
                properties.target().amountStartColIndex() + 17, "基金回收情况表");
    }

    /**
     * 统一金额处理流程；各业务入口提供对应的数据源、完整筛选规则及写入位置。
     */
    private <T extends AmountData> void processAmountData(
            ExcelDataReader<T> reader,
            String sourcePath,
            Function<List<T>, List<T>> filter,
            int amountStartColIndex,
            String sourceName) {
        List<T> allDataList = reader.readData(sourcePath);
        System.out.println(sourceName + "读取完成，共 " + allDataList.size() + " 条数据");

        List<T> validList = filter.apply(allDataList);
        Map<String, List<T>> projectGroupMap = new HashMap<>();
        for (T data : validList) {
            projectGroupMap.computeIfAbsent(data.getProjectName(), k -> new ArrayList<>()).add(data);
        }

        Map<String, Map<Integer, Double>> projectMonthAmount = projectGroupMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> amountCalculateService.calMonthAmount(entry.getValue())
                ));

        ExcelProperties.Target target = properties.target();
        excelTargetWriteService.writeMonthAmount(
                target.path(), projectMonthAmount, target.projectNameColIndex(),
                amountStartColIndex, target.dataStartRowIndex());

        System.out.println("全部项目处理完成！结果已保存到业务台账");
    }
}
