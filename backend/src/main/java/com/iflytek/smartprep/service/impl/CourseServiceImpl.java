package com.iflytek.smartprep.service.impl;

import com.iflytek.smartprep.service.ChapterService;
import com.iflytek.smartprep.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final ChapterService chapterService;

    @Override
    public List<Map<String, Object>> getChapterTree(Long courseId) {
        return chapterService.getChapterTree(courseId);
    }

    @Override
    public Map<String, Object> getSubChapterDetail(Long subChapterId) {
        return chapterService.getSubChapterDetail(subChapterId);
    }
}
