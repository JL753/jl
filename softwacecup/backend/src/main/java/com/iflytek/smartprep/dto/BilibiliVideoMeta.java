package com.iflytek.smartprep.dto;

import lombok.Data;
import java.util.List;

@Data
public class BilibiliVideoMeta {
    private String bvid;
    private String title;
    private String description;
    private Integer duration;
    private String coverUrl;
    private String authorName;
    private List<String> tags;
    private Long cid;
    private String partTitle;
    private Integer playCount;
}
