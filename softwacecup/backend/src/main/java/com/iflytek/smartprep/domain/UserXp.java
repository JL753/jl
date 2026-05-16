package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_user_xp")
public class UserXp {
    private Long id;
    private Long userId;
    private Integer currentXp;
    private Integer level;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
