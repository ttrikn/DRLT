package com.example.demo.controller;

import com.example.demo.service.ExcelProcessService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/excel")
public class ExcelProcessController {

    @Resource
    private ExcelProcessService excelProcessService;

    /**
     * 访问地址：http://localhost:8080/excel/process/dispatch
     */
    @GetMapping("/process/dispatch")
    public String processDispatch() {
        try {
            excelProcessService.processDispatchData();
            return "✅ 发放数据处理成功，已写入业务台账";
        } catch (Exception e) {
            return "❌ 处理失败：" + e.getMessage();
        }
    }

    /**
     * 访问地址：http://localhost:8080/excel/process/recycle
     */
    @GetMapping("/process/recycle")
    public String processRecycle() {
        try {
            excelProcessService.processRecycleData();
            excelProcessService.processFundRecycleData();
            excelProcessService.processScheduleData();
            return "✅ 回收数据处理成功，已写入业务台账";
        } catch (Exception e) {
            return "❌ 处理失败：" + e.getMessage();
        }
    }

    /**
     * 访问地址：http://localhost:8080/excel/process/baseInfo
     */
    @GetMapping("/process/baseInfo")
    public String processBaseInfo() {
        try {
            excelProcessService.processBaseData();
            return "✅ 项目业务分类、贷款及基金余额、执行利率处理成功，已写入业务台账";
        } catch (Exception e) {
            return "❌ 处理失败：" + e.getMessage();
        }
    }
}
