package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sp_exam_assignment")
public class ExamAssignment {
    private Long id;
    private Long examId;
    private Long studentId;
    private LocalDateTime createdAt;
}
