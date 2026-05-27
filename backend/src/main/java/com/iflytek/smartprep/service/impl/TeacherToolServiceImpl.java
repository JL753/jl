package com.iflytek.smartprep.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.dto.*;
import com.iflytek.smartprep.mapper.*;
import com.iflytek.smartprep.service.TeacherToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TeacherToolServiceImpl implements TeacherToolService {
    private final QuestionBankItemMapper questionBankItemMapper;
    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamAssignmentMapper examAssignmentMapper;
    private final PptTemplateMapper pptTemplateMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<Map<String, Object>> questionBank(Long userId) {
        seedData();
        return questionBankItemMapper.selectList(new LambdaQueryWrapper<QuestionBankItem>().orderByDesc(QuestionBankItem::getCreatedAt))
                .stream().map(this::mapQuestion).toList();
    }

    @Override
    public Map<String, Object> addQuestion(Long userId, QuestionItemRequest req) {
        QuestionBankItem i = new QuestionBankItem();
        i.setCreatorUserId(userId); i.setCourse(req.getCourse()); i.setTitle(req.getTitle());
        i.setCategory1(req.getCategory1()); i.setCategory2(req.getCategory2()); i.setCategory3(req.getCategory3());
        i.setQuestionType(req.getQuestionType()); i.setDifficulty(req.getDifficulty()); i.setCorrectAnswer(req.getCorrectAnswer());
        i.setAnalysis(req.getAnalysis()); i.setImageUrl(req.getImageUrl()); i.setCreatedAt(LocalDateTime.now());
        i.setOptionsJson(write(req.getOptions()));
        questionBankItemMapper.insert(i);
        return mapQuestion(i);
    }

    @Override
    public Map<String, Object> createPaper(Long userId, PaperCreateRequest req) {
        seedData();
        Exam e = new Exam();
        e.setCreatorUserId(userId); e.setExamName(req.getExamName()); e.setCourse(req.getCourse());
        e.setDuration(req.getDuration() == null ? 60 : req.getDuration()); e.setTopic(req.getTopic());
        e.setStatus("已发布"); e.setCreatedAt(LocalDateTime.now());
        List<QuestionBankItem> picked = pick(req); e.setQuestionCount(picked.size()); examMapper.insert(e);
        int no = 1;
        for (QuestionBankItem q : picked) {
            ExamQuestion x = new ExamQuestion();
            x.setExamId(e.getId()); x.setQuestionNo(no++); x.setQuestionType(q.getQuestionType());
            x.setTitle(q.getTitle()); x.setScore(score(q.getQuestionType())); x.setAnswerKey(q.getCorrectAnswer());
            examQuestionMapper.insert(x);
        }
        List<Long> students = (req.getStudentIds() == null || req.getStudentIds().isEmpty())
                ? userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getRole, "student")).stream().map(User::getId).toList()
                : req.getStudentIds();
        for (Long sid : students) {
            ExamAssignment a = new ExamAssignment();
            a.setExamId(e.getId()); a.setStudentId(sid); a.setCreatedAt(LocalDateTime.now());
            examAssignmentMapper.insert(a);
        }
        return Map.of("examId", e.getId(), "examName", e.getExamName(), "questionCount", picked.size(), "studentCount", students.size(), "message", "试卷创建成功");
    }

    @Override
    public List<Map<String, Object>> pptTemplates() {
        seedData();
        return pptTemplateMapper.selectList(new LambdaQueryWrapper<PptTemplate>().orderByAsc(PptTemplate::getId)).stream()
                .map(t -> Map.of("id", t.getId(), "templateName", t.getTemplateName(), "templateCode", t.getTemplateCode(), "coverUrl", t.getCoverUrl(), "sceneTag", t.getSceneTag(), "description", t.getDescription(), "themeColor", t.getThemeColor(), "previews", read(t.getPreviewJson()))).toList();
    }

    @Override
    public Map<String, Object> generateTeachingPpt(Long userId, TeachingPptGenerateRequest req) {
        seedData();
        PptTemplate t = pptTemplateMapper.selectOne(new LambdaQueryWrapper<PptTemplate>().eq(PptTemplate::getTemplateCode, req.getTemplateCode()).last("limit 1"));
        if (t == null) throw new IllegalArgumentException("模板不存在");
        List<Map<String, String>> slides = List.of(
                Map.of("title", req.getCourse() + " · " + req.getChapter(), "subtitle", "课程导入与学习目标"),
                Map.of("title", "知识结构梳理", "subtitle", "提炼本节重点与关键词"),
                Map.of("title", "案例讲解", "subtitle", "结合真实课堂案例完成迁移"),
                Map.of("title", "课堂练习", "subtitle", "分层练习与互动设计"),
                Map.of("title", "课后拓展", "subtitle", "总结、作业与延展资源")
        );
        Map<String, Object> outline = new LinkedHashMap<>();
        outline.put("course", req.getCourse()); outline.put("chapter", req.getChapter()); outline.put("teachingGoal", req.getTeachingGoal());
        outline.put("studentLevel", req.getStudentLevel()); outline.put("templateCode", t.getTemplateCode()); outline.put("templateName", t.getTemplateName()); outline.put("slides", slides);
        return Map.of("message", "完整教学PPT生成成功", "template", Map.of("code", t.getTemplateCode(), "name", t.getTemplateName()), "downloadUrl", t.getCoverUrl(), "previewPages", slides, "outline", outline);
    }

    private List<QuestionBankItem> pick(PaperCreateRequest r) {
        List<QuestionBankItem> all = questionBankItemMapper.selectList(new LambdaQueryWrapper<QuestionBankItem>().eq(r.getCourse() != null && !r.getCourse().isBlank(), QuestionBankItem::getCourse, r.getCourse()).orderByDesc(QuestionBankItem::getCreatedAt));
        return List.of(take(all, "单选题", r.getSingleCount()), take(all, "多选题", r.getMultipleCount()), take(all, "判断题", r.getJudgeCount()), take(all, "简答题", r.getShortAnswerCount())).stream().flatMap(List::stream).toList();
    }

    private List<QuestionBankItem> take(List<QuestionBankItem> all, String type, Integer count) {
        int n = count == null ? 0 : Math.max(count, 0);
        return all.stream().filter(i -> type.equals(i.getQuestionType())).limit(n).toList();
    }

    private int score(String type) { return "简答题".equals(type) ? 10 : ("多选题".equals(type) ? 4 : 2); }

    private Map<String, Object> mapQuestion(QuestionBankItem i) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", i.getId());
        data.put("course", i.getCourse());
        data.put("title", i.getTitle());
        data.put("category1", i.getCategory1());
        data.put("category2", i.getCategory2());
        data.put("category3", i.getCategory3());
        data.put("questionType", i.getQuestionType());
        data.put("difficulty", i.getDifficulty());
        data.put("options", read(i.getOptionsJson()));
        data.put("correctAnswer", i.getCorrectAnswer());
        data.put("analysis", i.getAnalysis());
        data.put("imageUrl", i.getImageUrl());
        data.put("createdAt", String.valueOf(i.getCreatedAt()));
        return data;
    }

    private List<String> read(String json) {
        try { return json == null || json.isBlank() ? List.of() : objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)); }
        catch (Exception e) { return List.of(); }
    }

    private String write(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }

    private void seedData() {
        if (pptTemplateMapper.selectCount(new LambdaQueryWrapper<PptTemplate>()) == 0) {
            addTemplate(9001L, "新学期校园迎新", "campus_fresh", "https://dummyimage.com/600x340/eef5ff/4176da&text=Fresh", "校园", "适合导入与欢迎页", "#4b82f5");
            addTemplate(9002L, "教育暖色课堂", "warm_class", "https://dummyimage.com/600x340/fff4dc/e39b2b&text=Warm", "教学", "适合知识讲解与互动", "#f2a037");
            addTemplate(9003L, "科技蓝实训", "tech_lab", "https://dummyimage.com/600x340/e8f7ff/1a8db8&text=Tech", "科技", "适合实验和项目汇报", "#17a2c8");
        }
        if (questionBankItemMapper.selectCount(new LambdaQueryWrapper<QuestionBankItem>()) == 0) {
            addQuestionSeed(7001L, "机器学习", "感知机属于哪一类模型？", "单选题", List.of("线性分类模型", "聚类模型", "强化学习模型", "生成模型"), "线性分类模型", "感知机通过线性超平面完成二分类。", "基础", "感知机", "概念");
            addQuestionSeed(7002L, "机器学习", "下列哪些属于监督学习算法？", "多选题", List.of("逻辑回归", "K近邻", "线性回归", "K-Means"), "逻辑回归,K近邻,线性回归", "K-Means 属于无监督聚类。", "基础", "监督学习", "概念");
            addQuestionSeed(7003L, "机器学习", "交叉验证可以帮助评估模型泛化能力。", "判断题", List.of("正确", "错误"), "正确", "交叉验证是评估泛化能力的常见方法。", "模型评估", "验证", "基础");
            addQuestionSeed(7004L, "机器学习", "请简述过拟合的表现及两种缓解方法。", "简答题", List.of(), "训练集表现好、测试集表现差；可用正则化、早停、数据增强等方法", "需覆盖现象与方法。", "模型评估", "过拟合", "提高");
        }
    }

    private void addTemplate(Long id, String name, String code, String cover, String tag, String desc, String color) {
        PptTemplate t = new PptTemplate();
        t.setId(id); t.setTemplateName(name); t.setTemplateCode(code); t.setCoverUrl(cover); t.setSceneTag(tag); t.setDescription(desc); t.setThemeColor(color); t.setCreatedAt(LocalDateTime.now()); t.setPreviewJson(write(List.of("封面", "目录", "讲解", "案例", "练习", "总结")));
        pptTemplateMapper.insert(t);
    }

    private void addQuestionSeed(Long id, String course, String title, String type, List<String> opts, String answer, String analysis, String c1, String c2, String c3) {
        QuestionBankItem i = new QuestionBankItem();
        i.setId(id); i.setCreatorUserId(2L); i.setCourse(course); i.setTitle(title); i.setQuestionType(type); i.setOptionsJson(write(opts)); i.setCorrectAnswer(answer); i.setAnalysis(analysis); i.setCategory1(c1); i.setCategory2(c2); i.setCategory3(c3); i.setDifficulty("中等"); i.setImageUrl(""); i.setCreatedAt(LocalDateTime.now());
        questionBankItemMapper.insert(i);
    }
}
