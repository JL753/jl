package com.iflytek.smartprep.dto;

import lombok.Data;
import java.util.List;

@Data
public class BilibiliImportRequest {
    private List<String> bvids;
    private boolean autoGenerate = true;
}
