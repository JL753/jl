package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sp_study_path")
public class StudyPath {
    private Long id;
    private Long userId;
    private String title;
    private String stepsJson;
    private String pushJson;
    private LocalDateTime createdAt;
}
