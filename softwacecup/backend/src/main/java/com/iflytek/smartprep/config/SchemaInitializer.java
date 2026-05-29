package com.iflytek.smartprep.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.dto.DialogueProfileRequest;
import com.iflytek.smartprep.dto.ResourceGenerateRequest;
import com.iflytek.smartprep.dto.StudyPathRequest;
import com.iflytek.smartprep.dto.AssessmentRequest;
import com.iflytek.smartprep.mapper.*;
import com.iflytek.smartprep.service.AssessmentService;
import com.iflytek.smartprep.service.ProfileService;
import com.iflytek.smartprep.service.ResourceService;
import com.iflytek.smartprep.service.StudyPathService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SchemaInitializer {

    private final JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);
    private final UserMapper userMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final LearningResourceMapper learningResourceMapper;
    private final LearningAssessmentMapper learningAssessmentMapper;
    private final StudyPathMapper studyPathMapper;
    private final KnowledgeDocMapper knowledgeDocMapper;
    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final CourseMapper courseMapper;
    private final OperationLogMapper operationLogMapper;
    private final ProfileService profileService;
    private final ResourceService resourceService;
    private final StudyPathService studyPathService;
    private final AssessmentService assessmentService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        // v3 表始终创建（独立于旧的 v1/v2 门控）
        ensureV3Tables();

        // 检查 v2 表是否存在，存在则跳过旧的 v1 初始化
        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sp_lesson_progress", Integer.class);
            log.info("v2 表已存在，跳过 SchemaInitializer 旧版初始化");
            return;
        } catch (Exception e) {
            log.info("v2 表不存在，执行 SchemaInitializer 旧版初始化");
        }
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_user (id BIGINT PRIMARY KEY, username VARCHAR(128), password VARCHAR(255), role VARCHAR(32), display_name VARCHAR(255), avatar_url VARCHAR(512))");
        ensureColumnExists("sp_user", "display_name", "ALTER TABLE sp_user ADD COLUMN display_name VARCHAR(255)");
        ensureColumnExists("sp_user", "avatar_url", "ALTER TABLE sp_user ADD COLUMN avatar_url VARCHAR(512)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_exam (id BIGINT PRIMARY KEY, creator_user_id BIGINT, exam_name VARCHAR(255), course VARCHAR(255), duration INT, topic VARCHAR(255), status VARCHAR(32), question_count INT, created_at DATETIME)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_exam_question (id BIGINT PRIMARY KEY, exam_id BIGINT, question_no INT, question_type VARCHAR(64), title TEXT, score INT, answer_key TEXT)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_exam_record (id BIGINT PRIMARY KEY, exam_id BIGINT, user_id BIGINT, score INT, review TEXT, answers_json LONGTEXT, submitted_at DATETIME)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_wrong_question (id BIGINT PRIMARY KEY, user_id BIGINT, exam_record_id BIGINT, exam_question_id BIGINT, question_title TEXT, my_answer TEXT, correct_answer TEXT, analysis TEXT, created_at DATETIME)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_course (id BIGINT PRIMARY KEY, title VARCHAR(255), category VARCHAR(128), description TEXT, cover_image VARCHAR(512), price VARCHAR(64), tag VARCHAR(64), status VARCHAR(32), total_hours INT, target_audience VARCHAR(255), chapters_json LONGTEXT, created_by BIGINT, created_at DATETIME, updated_at DATETIME)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_operation_log (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, username VARCHAR(128), action VARCHAR(128), target VARCHAR(255), details TEXT, ip_address VARCHAR(64), created_at DATETIME)");

        seedDemoAccounts();
    }

    /** Always create v3 tables and migrate data from old sp_unit/sp_lesson if needed */
    private void ensureV3Tables() {
        // Create v3 tables
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_chapter (id BIGINT PRIMARY KEY, course_id BIGINT NOT NULL, title VARCHAR(256) NOT NULL, description TEXT, sort_order INT DEFAULT 0, prerequisite_chapter_id BIGINT, created_at DATETIME, updated_at DATETIME)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_sub_chapter (id BIGINT PRIMARY KEY, chapter_id BIGINT NULL, title VARCHAR(256) NOT NULL, description TEXT, sort_order INT DEFAULT 0, type VARCHAR(32) DEFAULT 'doc', video_url VARCHAR(512), duration INT, content LONGTEXT, cover_url VARCHAR(512), status VARCHAR(32) DEFAULT 'published', user_id BIGINT, course_id BIGINT NULL)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_chapter_resource (id BIGINT PRIMARY KEY, course_id BIGINT NOT NULL, chapter_id BIGINT NOT NULL, type VARCHAR(32), title VARCHAR(256) NOT NULL, description TEXT, url VARCHAR(512), size VARCHAR(32), created_at DATETIME, updated_at DATETIME)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_course_announcement (id BIGINT PRIMARY KEY, course_id BIGINT NOT NULL, type VARCHAR(32) DEFAULT '普通公告', title VARCHAR(256) NOT NULL, content TEXT, created_at DATETIME, updated_at DATETIME)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_course_question (id BIGINT PRIMARY KEY, course_id BIGINT NOT NULL, user_id BIGINT NOT NULL, title VARCHAR(256) NOT NULL, content TEXT, created_at DATETIME, updated_at DATETIME)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_course_answer (id BIGINT PRIMARY KEY, question_id BIGINT NOT NULL, user_id BIGINT, content TEXT NOT NULL, is_ai TINYINT(1) DEFAULT 0, created_at DATETIME, updated_at DATETIME)");

        // 确保基础表存在（init.sql 可能未执行）
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_subject (id BIGINT PRIMARY KEY, name VARCHAR(128) NOT NULL, icon VARCHAR(32), color VARCHAR(16), description VARCHAR(512), sort_order INT DEFAULT 0)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_course (id BIGINT PRIMARY KEY, title VARCHAR(255), category VARCHAR(128), description TEXT, cover_image VARCHAR(512), price VARCHAR(64), tag VARCHAR(64), status VARCHAR(32), total_hours INT, target_audience VARCHAR(255), chapters_json LONGTEXT, created_by BIGINT, created_at DATETIME, updated_at DATETIME)");

        // 确保 sp_user 有 display_name 和 avatar_url 列（init.sql 创建的表没有这些列）
        ensureColumnExists("sp_user", "display_name", "ALTER TABLE sp_user ADD COLUMN display_name VARCHAR(255)");
        ensureColumnExists("sp_user", "avatar_url", "ALTER TABLE sp_user ADD COLUMN avatar_url VARCHAR(512)");

        // 确保"我的导入"学科存在
        jdbcTemplate.execute("INSERT IGNORE INTO sp_subject (id, name, icon, color, description, sort_order) " +
                "VALUES (9999, '我的导入', '📥', '#f59e0b', '未能自动归类的视频合集', 999)");

        // Ensure sp_course has new columns
        ensureColumnExists("sp_course", "subject_id", "ALTER TABLE sp_course ADD COLUMN subject_id BIGINT AFTER id");
        ensureColumnExists("sp_course", "background", "ALTER TABLE sp_course ADD COLUMN background VARCHAR(512)");
        ensureColumnExists("sp_course", "target", "ALTER TABLE sp_course ADD COLUMN target VARCHAR(512)");
        ensureColumnExists("sp_course", "principle", "ALTER TABLE sp_course ADD COLUMN principle VARCHAR(512)");

        // Ensure sub_chapter has course_id for playlist imports, and chapter_id can be null
        ensureColumnExists("sp_sub_chapter", "course_id", "ALTER TABLE sp_sub_chapter ADD COLUMN course_id BIGINT AFTER chapter_id");
        try { jdbcTemplate.execute("ALTER TABLE sp_sub_chapter MODIFY COLUMN chapter_id BIGINT NULL"); } catch (Exception e) { /* already nullable */ }

        // Ensure sp_knowledge_point has sort_order column
        ensureColumnExists("sp_knowledge_point", "sort_order", "ALTER TABLE sp_knowledge_point ADD COLUMN sort_order INT DEFAULT 0");

        // Ensure dependent tables have sub_chapter_id
        ensureColumnExists("sp_lesson_progress", "sub_chapter_id", "ALTER TABLE sp_lesson_progress ADD COLUMN sub_chapter_id BIGINT");
        ensureColumnExists("sp_knowledge_point", "sub_chapter_id", "ALTER TABLE sp_knowledge_point ADD COLUMN sub_chapter_id BIGINT");

        // Data migration: sp_unit → sp_chapter, sp_lesson → sp_sub_chapter
        try {
            Integer chapterCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sp_chapter", Integer.class);
            if (chapterCount != null && chapterCount == 0) {
                log.info("sp_chapter 为空，尝试从 sp_unit/sp_lesson 迁移数据...");
                // Backfill subject_id on courses (match by category = subject name)
                jdbcTemplate.execute("UPDATE sp_course c JOIN sp_subject s ON c.category = s.name SET c.subject_id = s.id WHERE c.subject_id IS NULL");
                // Create placeholder courses for subjects without one
                jdbcTemplate.execute("INSERT IGNORE INTO sp_course (id, subject_id, title, category, status, created_at, updated_at) " +
                    "SELECT s.id + 7000, s.id, CONCAT(s.name, ' - 课程'), s.name, '已发布', NOW(), NOW() FROM sp_subject s " +
                    "WHERE NOT EXISTS (SELECT 1 FROM sp_course c WHERE c.subject_id = s.id)");
                // Migrate units to chapters
                jdbcTemplate.execute("INSERT IGNORE INTO sp_chapter (id, course_id, title, description, sort_order, prerequisite_chapter_id, created_at, updated_at) " +
                    "SELECT u.id, COALESCE((SELECT c.id FROM sp_course c WHERE c.subject_id = u.subject_id LIMIT 1), u.subject_id + 7000), " +
                    "u.name, u.description, u.sort_order, u.prerequisite_unit_id, NOW(), NOW() FROM sp_unit u");
                // Migrate lessons to sub_chapters
                jdbcTemplate.execute("INSERT IGNORE INTO sp_sub_chapter (id, chapter_id, title, sort_order, type, video_url, duration, content, status) " +
                    "SELECT l.id, l.unit_id, l.name, l.sort_order, l.type, l.video_url, l.duration, l.content, 'published' FROM sp_lesson l");
                log.info("数据迁移完成");
            }
        } catch (Exception e) {
            log.warn("数据迁移失败（可能旧表不存在）: {}", e.getMessage());
        }
    }

    private void ensureColumnExists(String tableName, String columnName, String alterSql) {
        try {
            // 先确认表存在，避免在空数据库上 ALTER 时报错
            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                    Integer.class, tableName);
            if (tableCount == null || tableCount == 0) {
                log.debug("表 {} 不存在，跳过添加列 {}", tableName, columnName);
                return;
            }
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class,
                    tableName,
                    columnName
            );
            if (count == null || count == 0) {
                jdbcTemplate.execute(alterSql);
            }
        } catch (Exception e) {
            log.warn("确保列 {}.{} 失败: {}", tableName, columnName, e.getMessage());
        }
    }

    private void seedDemoAccounts() {
        User teacher = ensureUser(1001L, "teacher", "123456", "teacher", "陈教师");
        User student = ensureUser(1002L, "student", "123456", "student", "林学生");
        User admin = ensureUser(1003L, "admin", "123456", "admin", "系统管理员");

        seedProfile(teacher, "人工智能教育", "人工智能导论", "扎实", "图解优先", "课堂节奏设计、跨案例串联", "项目实战", "每周4次", "课堂完成度95+");
        seedProfile(student, "软件工程", "人工智能导论", "中等", "图文结合", "模型评估与知识迁移", "讲义+练习混合", "每周3次", "期末85分以上");
        seedProfile(admin, "教育信息化", "智慧教学平台运营", "扎实", "数据看板优先", "跨角色数据联动分析", "平台巡检", "每周2次", "平台稳定运行");

        seedResourcesIfEmpty(teacher.getId(), true);
        seedResourcesIfEmpty(student.getId(), false);
        seedResourcesIfEmpty(admin.getId(), true);

        seedStudyPathIfEmpty(teacher.getId(), "完善备课与课堂组织能力", "8");
        seedStudyPathIfEmpty(student.getId(), "完成课程考核并提升综合应用能力", "6");
        seedStudyPathIfEmpty(admin.getId(), "形成平台运营闭环与数据治理方案", "4");

        seedAssessmentIfEmpty(teacher.getId(), 8, 36, 4, 260, 14);
        seedAssessmentIfEmpty(student.getId(), 5, 24, 5, 180, 9);
        seedAssessmentIfEmpty(admin.getId(), 6, 18, 2, 140, 10);

        seedKnowledgeDocsIfEmpty();
        seedExamSuiteIfEmpty(teacher, student);
        seedCoursesIfEmpty(admin);
    }

    private User ensureUser(Long id, String username, String password, String role, String displayName) {
        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username).last("limit 1"));
        if (exists != null) {
            boolean changed = false;
            if (!password.equals(exists.getPassword())) { exists.setPassword(password); changed = true; }
            if (!role.equals(exists.getRole())) { exists.setRole(role); changed = true; }
            if (!displayName.equals(exists.getDisplayName())) { exists.setDisplayName(displayName); changed = true; }
            String avatarUrl = defaultAvatar(role, displayName);
            if (exists.getAvatarUrl() == null || exists.getAvatarUrl().isBlank()) { exists.setAvatarUrl(avatarUrl); changed = true; }
            if (changed) userMapper.updateById(exists);
            return exists;
        }
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user.setDisplayName(displayName);
        user.setAvatarUrl(defaultAvatar(role, displayName));
        userMapper.insert(user);
        return user;
    }

    private void seedProfile(User user, String major, String course, String base, String style, String weak, String interest, String pace, String goal) {
        StudentProfile profile = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, user.getId()).last("limit 1"));
        if (profile == null) {
            profile = new StudentProfile();
        }
        profile.setMajor(major);
        profile.setCourse(course);
        profile.setKnowledgeBase(base);
        profile.setCognitiveStyle(style);
        profile.setWeakPoints(weak);
        profile.setInterestPreference(interest);
        profile.setPacePreference(pace);
        profile.setExamGoal(goal);
        profileService.saveOrUpdateProfile(user.getId(), profile);
    }

    private void seedResourcesIfEmpty(Long userId, boolean teacherLike) {
        if (learningResourceMapper.selectCount(new LambdaQueryWrapper<LearningResource>().eq(LearningResource::getUserId, userId)) > 0) {
            return;
        }
        ResourceGenerateRequest req = new ResourceGenerateRequest();
        req.setMajor(teacherLike ? "人工智能教育" : "软件工程");
        req.setCourse("人工智能导论");
        req.setTopic(teacherLike ? "课程导入、案例组织与课堂互动" : "人工智能基础概念与模型评估");
        req.setWeakness(teacherLike ? "案例串联与课堂节奏" : "模型理解与知识迁移");
        req.setLearningStage(teacherLike ? "授课准备阶段" : "入门阶段");
        req.setTargetScore(teacherLike ? "课堂完成度95+" : "85+");
        req.setPreferredMode(teacherLike ? "图文+案例" : "图文结合");
        req.setRequiredTypes(teacherLike ? List.of("document", "mindmap", "question", "media") : List.of("document", "mindmap", "media"));
        resourceService.generate(userId, req);
    }

    private void seedStudyPathIfEmpty(Long userId, String target, String hours) {
        if (studyPathMapper.selectCount(new LambdaQueryWrapper<StudyPath>().eq(StudyPath::getUserId, userId)) > 0) {
            return;
        }
        StudyPathRequest req = new StudyPathRequest();
        req.setCurrentStage("基础阶段");
        req.setAvailableHoursPerWeek(hours);
        req.setTarget(target);
        studyPathService.generate(userId, req);
    }

    private void seedAssessmentIfEmpty(Long userId, int finishedTasks, int practiceCount, int wrongCount, int studyMinutes, int resourceUseCount) {
        if (learningAssessmentMapper.selectCount(new LambdaQueryWrapper<LearningAssessment>().eq(LearningAssessment::getUserId, userId)) > 0) {
            return;
        }
        AssessmentRequest req = new AssessmentRequest();
        req.setFinishedTasks(finishedTasks);
        req.setPracticeCount(practiceCount);
        req.setWrongCount(wrongCount);
        req.setStudyMinutes(studyMinutes);
        req.setResourceUseCount(resourceUseCount);
        assessmentService.evaluate(userId, req);
    }

    private void seedKnowledgeDocsIfEmpty() {
        if (knowledgeDocMapper.selectCount(new LambdaQueryWrapper<KnowledgeDoc>()) > 0) {
            return;
        }
        addDoc(5001L, "人工智能导论", "课堂导入设计模板", "课程目标、导入案例、课堂互动问题与板书建议", "教案");
        addDoc(5002L, "机器学习基础", "监督学习讲义", "监督学习分类、训练流程、评估指标与课堂案例", "讲义");
        addDoc(5003L, "深度学习实践", "神经网络实验指导", "神经网络结构、损失函数、实验现象与调参建议", "实验");
        addDoc(5004L, "智慧教学运营", "平台运营周报模板", "平台监控指标、异常说明、优化建议与复盘节奏", "运营");
    }

    private void addDoc(Long id, String course, String title, String content, String tag) {
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setId(id);
        doc.setCourse(course);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setTag(tag);
        knowledgeDocMapper.insert(doc);
    }

    private void seedCoursesIfEmpty(User admin) {
        if (courseMapper.selectCount(new LambdaQueryWrapper<Course>()) > 0) {
            return;
        }
        Course c1 = new Course();
        c1.setId(7001L);
        c1.setTitle("人工智能导论");
        c1.setCategory("人工智能");
        c1.setDescription("覆盖AI基础概念、机器学习入门、深度学习实践、自然语言处理与计算机视觉等核心知识体系。通过理论+实验结合的方式，帮助学生建立完整的AI知识框架，并掌握Python工具链进行项目实战。");
        c1.setCoverImage("https://images.unsplash.com/photo-1677442136019-21780ecad995?w=600");
        c1.setPrice("免费");
        c1.setTag("AI");
        c1.setStatus("已发布");
        c1.setTotalHours(48);
        c1.setTargetAudience("零基础或有一定编程基础的本科生/研究生");
        c1.setSubjectId(1004L);
        c1.setBackground("人工智能技术的快速发展对教育领域产生了深远影响");
        c1.setTarget("建立完整的AI知识框架，掌握Python工具链进行项目实战");
        c1.setPrinciple("理论+实验结合，循序渐进的教学设计");
        c1.setCreatedBy(admin.getId());
        c1.setCreatedAt(LocalDateTime.now().minusDays(30));
        c1.setUpdatedAt(LocalDateTime.now());
        courseMapper.insert(c1);

        Course c2 = new Course();
        c2.setId(7002L);
        c2.setTitle("机器学习基础");
        c2.setCategory("机器学习");
        c2.setDescription("系统讲解机器学习的数学基础、算法原理与实践应用。围绕分类、回归、聚类等核心任务组织多模态资源，包含大量案例和代码实操环节。");
        c2.setCoverImage("https://images.unsplash.com/photo-1555949963-aa79dcee981c?w=600");
        c2.setPrice("¥299");
        c2.setTag("ML");
        c2.setStatus("已发布");
        c2.setTotalHours(36);
        c2.setTargetAudience("有Python基础和高等数学基础的学习者");
        c2.setSubjectId(1004L);
        c2.setCreatedBy(admin.getId());
        c2.setCreatedAt(LocalDateTime.now().minusDays(20));
        c2.setUpdatedAt(LocalDateTime.now());
        courseMapper.insert(c2);

        Course c3 = new Course();
        c3.setId(7003L);
        c3.setTitle("深度学习实践");
        c3.setCategory("深度学习");
        c3.setDescription("面向竞赛冲刺和项目实训的高级课程。涵盖PyTorch框架使用、CNN/RNN/Transformer架构设计、模型调优与部署全流程。");
        c3.setCoverImage("https://images.unsplash.com/photo-1614027164847-1b28cfe1df60?w=600");
        c3.setPrice("¥399");
        c3.setTag("DL");
        c3.setStatus("已发布");
        c3.setTotalHours(42);
        c3.setTargetAudience("已掌握ML基础知识的研究生/工程师");
        c3.setSubjectId(1004L);
        c3.setCreatedBy(admin.getId());
        c3.setCreatedAt(LocalDateTime.now().minusDays(10));
        c3.setUpdatedAt(LocalDateTime.now());
        courseMapper.insert(c3);
    }

    private void seedExamSuiteIfEmpty(User teacher, User student) {
        if (examMapper.selectCount(new LambdaQueryWrapper<Exam>()) > 0) {
            return;
        }
        Exam exam = new Exam();
        exam.setId(6001L);
        exam.setCreatorUserId(teacher.getId());
        exam.setExamName("人工智能导论阶段测试");
        exam.setCourse("人工智能导论");
        exam.setDuration(50);
        exam.setTopic("基础概念、监督学习与模型评估");
        exam.setStatus("已发布");
        exam.setQuestionCount(3);
        exam.setCreatedAt(LocalDateTime.now().minusDays(3));
        examMapper.insert(exam);

        addExamQuestion(6101L, exam.getId(), 1, "简答题", "请说明监督学习与无监督学习的核心区别。", 10, "监督学习使用带标签数据训练模型，无监督学习主要从无标签数据中发现结构。", LocalDateTime.now().minusDays(3));
        addExamQuestion(6102L, exam.getId(), 2, "简答题", "交叉验证在模型评估中的作用是什么？", 10, "用于更稳定地评估模型泛化能力，减少单次划分带来的偶然性。", LocalDateTime.now().minusDays(3));
        addExamQuestion(6103L, exam.getId(), 3, "简答题", "请列举两种缓解过拟合的方法。", 10, "可采用正则化、早停、数据增强、降低模型复杂度等方法。", LocalDateTime.now().minusDays(3));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("answers", Map.of(
                "6101", "监督学习依赖标签数据训练分类或回归模型，无监督学习不需要标签，重点是聚类和降维。",
                "6102", "交叉验证能够多次划分数据集，帮助我们更稳定地判断模型是否真正适合新样本。",
                "6103", "可以通过正则化和增加训练轮次来解决过拟合。"
        ));
        payload.put("annotations", List.of(
                Map.of("questionId", 6101L, "score", 10, "comment", "概念完整，监督/无监督区别表述清楚。"),
                Map.of("questionId", 6102L, "score", 9, "comment", "答到了泛化能力评估，若补充「减少偶然性」会更完整。"),
                Map.of("questionId", 6103L, "score", 6, "comment", "正则化回答正确，但「增加训练轮次」可能加重过拟合，建议改为早停或数据增强。")
        ));

        ExamRecord record = new ExamRecord();
        record.setId(6201L);
        record.setExamId(exam.getId());
        record.setUserId(student.getId());
        record.setScore(25);
        record.setReview("整体基础较好，概念理解清晰，但在过拟合缓解策略上仍需加强辨析。建议重点复习模型评估与正则化章节。 ");
        record.setAnswersJson(writeJson(payload));
        record.setSubmittedAt(LocalDateTime.now().minusDays(2));
        examRecordMapper.insert(record);

        WrongQuestion wrong = new WrongQuestion();
        wrong.setId(6301L);
        wrong.setUserId(student.getId());
        wrong.setExamRecordId(record.getId());
        wrong.setExamQuestionId(6103L);
        wrong.setQuestionTitle("请列举两种缓解过拟合的方法。");
        wrong.setMyAnswer("可以通过正则化和增加训练轮次来解决过拟合。");
        wrong.setCorrectAnswer("可采用正则化、早停、数据增强、降低模型复杂度等方法。");
        wrong.setAnalysis("你已经答对了正则化，但「增加训练轮次」并不是常规缓解过拟合的方法，建议结合早停与数据增强重新理解。 ");
        wrong.setCreatedAt(LocalDateTime.now().minusDays(2));
        wrongQuestionMapper.insert(wrong);
    }

    private void addExamQuestion(Long id, Long examId, int no, String type, String title, int score, String answerKey, LocalDateTime createdAt) {
        ExamQuestion q = new ExamQuestion();
        q.setId(id);
        q.setExamId(examId);
        q.setQuestionNo(no);
        q.setQuestionType(type);
        q.setTitle(title);
        q.setScore(score);
        q.setAnswerKey(answerKey);
        examQuestionMapper.insert(q);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String defaultAvatar(String role, String displayName) {
        String seed = (displayName == null || displayName.isBlank()) ? role : displayName;
        return "https://api.dicebear.com/7.x/initials/svg?seed=" + seed;
    }
}
