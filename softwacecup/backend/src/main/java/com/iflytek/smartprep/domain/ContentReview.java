package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_content_review")
public class ContentReview {
    private Long id;
    private Long lessonId;
    private String aiDraftJson;
    private String status;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
}
