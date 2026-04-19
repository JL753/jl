package com.iflytek.smartprep.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardStats {
    private Map<String, Object> summary;
    private List<Map<String, Object>> bar;
    private List<Map<String, Object>> line;
    private List<Map<String, Object>> radar;
    private List<Map<String, Object>> pie;
    private List<Map<String, Object>> cards;
    private List<Map<String, Object>> table;
    private List<Map<String, Object>> notices;
}
