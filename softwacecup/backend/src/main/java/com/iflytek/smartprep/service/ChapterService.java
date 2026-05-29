package com.iflytek.smartprep.service;

import com.iflytek.smartprep.domain.Chapter;
import com.iflytek.smartprep.domain.SubChapter;
import java.util.List;
import java.util.Map;

public interface ChapterService {
    List<Map<String, Object>> getChapterTree(Long courseId);
    Map<String, Object> getSubChapterDetail(Long subChapterId);
    Chapter createChapter(Chapter chapter);
    Chapter updateChapter(Long id, Chapter chapter);
    void deleteChapter(Long id);
    SubChapter createSubChapter(SubChapter subChapter);
    SubChapter updateSubChapter(Long id, SubChapter subChapter);
    void deleteSubChapter(Long id);
}
