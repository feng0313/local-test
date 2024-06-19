package com.chunfeng.local.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class DynamicDataRow {
    private String tableName;
    private Map<String, Object> dataFields = new HashMap<>();

    public DynamicDataRow(String tableName) {
        this.tableName = tableName;
    }

    // 修改此方法以正确处理null值
    public void addField(String fieldName, Object value) {
        dataFields.put(fieldName, value); // 直接放入原始值，包括null
    }
}