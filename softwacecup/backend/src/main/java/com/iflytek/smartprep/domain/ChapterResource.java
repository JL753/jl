package com.iflytek.smartprep.domain;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_chapter_resource")
public class ChapterResource {
    private Long id;
    private Long courseId;
    private Long chapterId;
    private String type;
    private String title;
    private String description;
    private String url;
    private String size;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
