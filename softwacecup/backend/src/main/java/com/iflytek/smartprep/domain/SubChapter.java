package com.iflytek.smartprep.domain;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sp_sub_chapter")
public class SubChapter {
    private Long id;
    private Long chapterId;
    private String title;
    private String description;
    private Integer sortOrder;
    private String type;
    private String videoUrl;
    private Integer duration;
    private String content;
    private String coverUrl;
    private String status;
    private Long userId;
}
