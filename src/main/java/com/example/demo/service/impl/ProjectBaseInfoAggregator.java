package com.example.demo.service.impl;

import com.example.demo.entity.ProjectBaseInfo;
import com.example.demo.entity.SurplusData;
import com.example.demo.entity.InterestData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Component
public class ProjectBaseInfoAggregator {

    private static final Set<String> FUND_PRODUCTS = Set.of(
            "J0120001", "K0820001", "J0122001", "K0822001", "J0125001", "K0825001");

    public Map<String, ProjectBaseInfo> aggregate(List<SurplusData> loans, List<InterestData> interestRows) {
        Map<String, ProjectBaseInfo> result = aggregate(loans);
        Map<String, BigDecimal> fundBalances = new LinkedHashMap<>();
        Set<String> fundProjects = new LinkedHashSet<>();
        Set<String> incompleteBalances = new LinkedHashSet<>();
        Map<String, Set<BigDecimal>> rates = new LinkedHashMap<>();
        for (InterestData row : interestRows) {
            if (row.getProjectName() == null || row.getProjectName().isBlank()) continue;
            String name = row.getProjectName().trim();
            Set<BigDecimal> projectRates = rates.computeIfAbsent(name, key -> new TreeSet<>());
            if (row.getInterestRate() != null) projectRates.add(row.getInterestRate());
            if (row.getProductCode() != null && FUND_PRODUCTS.contains(row.getProductCode().trim())) {
                fundProjects.add(name);
                if (row.getPrincipalBalance() == null) incompleteBalances.add(name);
                else fundBalances.merge(name, row.getPrincipalBalance(), BigDecimal::add);
            }
        }
        Set<String> projects = new LinkedHashSet<>(result.keySet());
        projects.addAll(rates.keySet());
        for (String name : projects) {
            ProjectBaseInfo loan = result.get(name);
            BigDecimal balance = loan == null ? null : loan.balance();
            if (fundProjects.contains(name)) {
                // 基金余额只取六类基金产品本金，不与人民币贷款余额重复相加。
                balance = incompleteBalances.contains(name) ? null : fundBalances.get(name);
            }
            result.put(name, new ProjectBaseInfo(loan == null ? null : loan.businessType(), balance,
                    List.copyOf(rates.getOrDefault(name, Set.of()))));
        }
        return result;
    }

    public Map<String, ProjectBaseInfo> aggregate(List<SurplusData> rows) {
        Map<String, BigDecimal> balances = new LinkedHashMap<>();
        Map<String, Set<String>> types = new LinkedHashMap<>();
        Set<String> incompleteBalances = new LinkedHashSet<>();
        for (SurplusData row : rows) {
            String name = row.getProjectName();
            if (name == null || name.isBlank()) {
                continue; // 空行及无项目名称的表尾汇总不参与项目汇总
            }
            name = name.trim();
            if (row.getMoney() == null) {
                incompleteBalances.add(name);
            } else {
                balances.merge(name, row.getMoney(), BigDecimal::add);
            }
            // 每条明细均参与求和，不按合同号去重，避免遗漏合同下的多笔余额。
            Set<String> projectTypes = types.computeIfAbsent(name, key -> new LinkedHashSet<>());
            if (row.getType() != null && !row.getType().isBlank()) {
                projectTypes.add(row.getType().trim());
            }
        }
        Map<String, ProjectBaseInfo> result = new LinkedHashMap<>();
        types.forEach((name, projectTypes) -> result.put(name,
                new ProjectBaseInfo(String.join("；", projectTypes),
                        incompleteBalances.contains(name) ? null : balances.get(name))));
        return result;
    }
}
