package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubjectCourseController {

    private final SubjectMapper subjectMapper;
    private final SubChapterMapper subChapterMapper;
    private final ChapterMapper chapterMapper;
    private final CourseMapper courseMapper;

    @GetMapping("/subjects")
    public ApiResponse<List<Subject>> listSubjects() {
        return ApiResponse.ok(subjectMapper.selectList(
                new LambdaQueryWrapper<Subject>().orderByAsc(Subject::getSortOrder)));
    }

    @GetMapping("/subjects/{id}")
    public ApiResponse<Subject> getSubject(@PathVariable Long id) {
        Subject s = subjectMapper.selectById(id);
        return s != null ? ApiResponse.ok(s) : ApiResponse.fail("学科不存在");
    }

    @GetMapping("/lessons/my-imports")
    public ApiResponse<List<Map<String, Object>>> getMyImports() {
        Long userId = LoginUserHolder.get().getUserId();
        List<SubChapter> mySubs = subChapterMapper.selectList(
                new LambdaQueryWrapper<SubChapter>()
                        .eq(SubChapter::getUserId, userId)
                        .orderByDesc(SubChapter::getId));

        Map<String, List<SubChapter>> grouped = new LinkedHashMap<>();
        for (SubChapter sc : mySubs) {
            String key = sc.getVideoUrl() != null ? sc.getVideoUrl() : "";
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(sc);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            List<SubChapter> subs = entry.getValue();
            SubChapter first = subs.get(0);

            String chapterName = "";
            String courseName = "";
            String subjectName = "";
            Long courseId = null;
            if (first.getChapterId() != null) {
                Chapter chapter = chapterMapper.selectById(first.getChapterId());
                if (chapter != null) {
                    chapterName = chapter.getTitle();
                    courseId = chapter.getCourseId();
                    Course course = courseMapper.selectById(courseId);
                    if (course != null) {
                        courseName = course.getTitle();
                        Subject subject = subjectMapper.selectById(course.getSubjectId());
                        if (subject != null) subjectName = subject.getName();
                    }
                }
            }

            String bvid = "";
            String url = first.getVideoUrl();
            if (url != null && url.contains("BV")) {
                bvid = url.substring(url.indexOf("BV"));
                if (bvid.length() > 12) bvid = bvid.substring(0, 12);
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("bvid", bvid);
            item.put("title", first.getTitle() != null ? first.getTitle() : "");
            item.put("subjectName", subjectName);
            item.put("courseName", courseName);
            item.put("chapterName", chapterName);
            item.put("courseId", courseId);
            item.put("lessonCount", subs.size());
            item.put("firstSubChapterId", first.getId());
            item.put("coverUrl", first.getCoverUrl() != null ? first.getCoverUrl() : "");
            result.add(item);
        }
        return ApiResponse.ok(result);
    }
}
