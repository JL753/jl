package com.iflytek.smartprep.domain;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_chapter")
public class Chapter {
    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private Integer sortOrder;
    private Long prerequisiteChapterId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
