package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_student_ability")
public class StudentAbility {
    private Long id;
    private Long userId;
    private Integer breadthScore;
    private Integer depthScore;
    private Integer problemScore;
    private Integer activityScore;
    private Integer transferScore;
    private Integer resilienceScore;
    private String diagnosis;
    private LocalDateTime evaluatedAt;
}
