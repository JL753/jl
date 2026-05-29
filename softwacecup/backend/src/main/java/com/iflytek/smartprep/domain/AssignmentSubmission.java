package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_assignment_submission")
public class AssignmentSubmission {
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String status;
    private Integer score;
    private String feedback;
    private LocalDateTime submittedAt;
}
