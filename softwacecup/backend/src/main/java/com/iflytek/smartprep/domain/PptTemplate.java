package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sp_ppt_template")
public class PptTemplate {
    private Long id;
    private String templateName;
    private String templateCode;
    private String coverUrl;
    private String sceneTag;
    private String description;
    private String previewJson;
    private String themeColor;
    private LocalDateTime createdAt;
}
