package com.iflytek.smartprep.dto;

import lombok.Data;
import java.util.List;

@Data
public class BilibiliImportResult {
    private List<BilibiliImportResultItem> results;

    @Data
    public static class BilibiliImportResultItem {
        private String bvid;
        private Long lessonId;
        private String lessonName;
        private String subjectName;
        private String unitName;
        private boolean generated;
        private String error;
    }
}
