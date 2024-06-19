package com.chunfeng.local.model;

import lombok.Data;

import java.util.List;

@Data
public class QueryDataRequest {
    private String startTime;
    private List<String> tableList;
    private String url;
}