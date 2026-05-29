package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sp_user_streak")
public class UserStreak {
    private Long id;
    private Long userId;
    private Integer currentStreak;
    private Integer longestStreak;
    private LocalDate lastCheckinDate;
    private Integer totalCheckins;
    private LocalDateTime updatedAt;
}
