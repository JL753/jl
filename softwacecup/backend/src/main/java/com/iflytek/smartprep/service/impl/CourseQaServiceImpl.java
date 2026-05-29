package com.iflytek.smartprep.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.mapper.*;
import com.iflytek.smartprep.service.CourseQaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseQaServiceImpl implements CourseQaService {

    private final CourseQuestionMapper questionMapper;
    private final CourseAnswerMapper answerMapper;
    private final UserMapper userMapper;

    @Override
    public List<Map<String, Object>> getQuestions(Long courseId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<CourseQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<CourseQuestion>()
                        .eq(CourseQuestion::getCourseId, courseId)
                        .orderByDesc(CourseQuestion::getCreatedAt)
                        .last("LIMIT " + offset + ", " + pageSize));

        return questions.stream().map(q -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", q.getId());
            node.put("courseId", q.getCourseId());
            node.put("userId", q.getUserId());
            node.put("title", q.getTitle());
            node.put("content", q.getContent());
            node.put("createdAt", q.getCreatedAt());

            Long answerCount = answerMapper.selectCount(
                    new LambdaQueryWrapper<CourseAnswer>().eq(CourseAnswer::getQuestionId, q.getId()));
            node.put("answerCount", answerCount);

            User user = userMapper.selectById(q.getUserId());
            node.put("userName", user != null ? user.getDisplayName() : "未知");
            return node;
        }).collect(Collectors.toList());
    }

    @Override
    public CourseQuestion askQuestion(Long courseId, Long userId, String title, String content) {
        CourseQuestion q = new CourseQuestion();
        q.setId(System.currentTimeMillis());
        q.setCourseId(courseId);
        q.setUserId(userId);
        q.setTitle(title);
        q.setContent(content);
        q.setCreatedAt(LocalDateTime.now());
        q.setUpdatedAt(LocalDateTime.now());
        questionMapper.insert(q);
        return q;
    }

    @Override
    public List<Map<String, Object>> getAnswers(Long questionId) {
        List<CourseAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<CourseAnswer>()
                        .eq(CourseAnswer::getQuestionId, questionId)
                        .orderByAsc(CourseAnswer::getCreatedAt));

        return answers.stream().map(a -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", a.getId());
            node.put("questionId", a.getQuestionId());
            node.put("userId", a.getUserId());
            node.put("content", a.getContent());
            node.put("isAi", a.getIsAi());
            node.put("createdAt", a.getCreatedAt());

            if (a.getUserId() != null) {
                User user = userMapper.selectById(a.getUserId());
                node.put("userName", user != null ? user.getDisplayName() : "未知");
            } else {
                node.put("userName", "AI 小慧");
            }
            return node;
        }).collect(Collectors.toList());
    }

    @Override
    public CourseAnswer postAnswer(Long questionId, Long userId, String content) {
        CourseAnswer a = new CourseAnswer();
        a.setId(System.currentTimeMillis());
        a.setQuestionId(questionId);
        a.setUserId(userId);
        a.setContent(content);
        a.setIsAi(false);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        answerMapper.insert(a);
        return a;
    }

    @Override
    public CourseAnswer postAiAnswer(Long questionId, String content) {
        CourseAnswer a = new CourseAnswer();
        a.setId(System.currentTimeMillis());
        a.setQuestionId(questionId);
        a.setUserId(null);
        a.setContent(content);
        a.setIsAi(true);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        answerMapper.insert(a);
        return a;
    }
}
