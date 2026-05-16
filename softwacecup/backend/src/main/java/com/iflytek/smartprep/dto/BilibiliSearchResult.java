package com.iflytek.smartprep.dto;

import lombok.Data;
import java.util.List;

@Data
public class BilibiliSearchResult {
    private int total;
    private List<BilibiliVideoMeta> items;
}
