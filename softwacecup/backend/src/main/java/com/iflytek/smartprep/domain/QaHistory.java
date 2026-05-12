package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sp_qa_history")
public class QaHistory {
    private Long id;
    private Long userId;
    private String question;
    private String answer;
    private String summary;
    private String sessionId;
    private LocalDateTime createdAt;
}
