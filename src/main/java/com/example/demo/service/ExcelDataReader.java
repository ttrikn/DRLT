package com.example.demo.service;

import org.apache.poi.ss.formula.functions.T;

import java.util.List;

public interface ExcelDataReader<T> {
    List<T> readData(String filePath);
}
