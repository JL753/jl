package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("sp_study_duration")
public class StudyDuration {
    private Long id;
    private Long userId;
    private Long lessonId;
    private LocalDate studyDate;
    private Integer durationSeconds;
}
