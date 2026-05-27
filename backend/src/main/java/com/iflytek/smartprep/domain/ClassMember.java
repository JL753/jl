package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_class_member")
public class ClassMember {
    private Long id;
    private Long classId;
    private Long studentId;
    private LocalDateTime joinedAt;
}
