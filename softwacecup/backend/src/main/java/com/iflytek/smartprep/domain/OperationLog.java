package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_operation_log")
public class OperationLog {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String target;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
