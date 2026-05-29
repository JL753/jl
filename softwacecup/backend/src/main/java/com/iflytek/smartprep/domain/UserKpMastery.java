package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_user_kp_mastery")
public class UserKpMastery {
    private Long id;
    private Long userId;
    private Long knowledgePointId;
    private Double mastery;
    private Integer practiceCount;
    private Integer correctCount;
    private LocalDateTime lastPracticeAt;
}
