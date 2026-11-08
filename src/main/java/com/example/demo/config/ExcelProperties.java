package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Excel 文件位置和台账布局；行、列索引均从 0 开始。
 * 在配置绑定时检查必填项，不要求业务文件在应用启动时已经存在。
 */
@ConfigurationProperties(prefix = "excel")
public record ExcelProperties(Source source, Target target) {

    public ExcelProperties {
        if (source == null || target == null) {
            throw new IllegalArgumentException("excel.source 和 excel.target 必须配置");
        }
    }

    public record Source(Input dispatch, Input recycle, Input schedule,
                         Input fundRecycle, Input surplus, Input interest) {
        public Source {
            if (dispatch == null || recycle == null || schedule == null
                    || fundRecycle == null || surplus == null || interest == null) {
                throw new IllegalArgumentException("excel.source 必须配置全部六类数据源（包括 interest 结息表）");
            }
        }
    }

    public record Input(String path) {
        public Input {
            requirePath(path);
        }
    }

    public record Target(String path, Integer projectNameColIndex,
                         Integer amountStartColIndex, Integer dataStartRowIndex,
                         @DefaultValue("7") Integer loanBalanceColIndex,
                         @DefaultValue("5") Integer interestRateColIndex) {
        public Target {
            requirePath(path);
            requireIndex(projectNameColIndex, "project-name-col-index");
            requireIndex(amountStartColIndex, "amount-start-col-index");
            requireIndex(dataStartRowIndex, "data-start-row-index");
            requireIndex(loanBalanceColIndex, "loan-balance-col-index");
            requireIndex(interestRateColIndex, "interest-rate-col-index");
            if (interestRateColIndex.equals(projectNameColIndex)
                    || interestRateColIndex.equals(amountStartColIndex)
                    || interestRateColIndex.equals(loanBalanceColIndex)) {
                throw new IllegalArgumentException("执行利率列不能与其他基础信息列重叠");
            }
            if (loanBalanceColIndex.equals(projectNameColIndex)
                    || loanBalanceColIndex.equals(amountStartColIndex)) {
                throw new IllegalArgumentException("贷款余额列不能与项目名称列或业务分类列重叠");
            }
        }
    }

    private static void requirePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Excel 文件 path 不能为空");
        }
    }

    private static void requireIndex(Integer index, String name) {
        if (index == null || index < 0) {
            throw new IllegalArgumentException("excel.target." + name + " 必须配置且不能为负数");
        }
    }
}
