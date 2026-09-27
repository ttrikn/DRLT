package com.example.demo.service;

import com.example.demo.entity.EmployeeData;

import java.util.List;
import java.util.Map;

public interface IClassService {
    Map<Integer, Double> Cal(List<EmployeeData> after2026List);
}
