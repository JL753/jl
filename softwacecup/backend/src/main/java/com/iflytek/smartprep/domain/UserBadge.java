package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_user_badge")
public class UserBadge {
    private Long id;
    private Long userId;
    private Long badgeId;
    private LocalDateTime unlockedAt;
}
