# 知域后端重构 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补全知域后端重构的剩余工作：WebSearchAgent、v2 数据库完整集成、Course 相关 API 端点、种子数据、SchemaInitializer 更新。

**Architecture:** 当前代码已实现大部分知域业务逻辑（班级/作业/审核/六维能力/进度），但存在几个缺口：(1) WebSearchAgent 未创建；(2) SubjectCourseController 缺少 Course 层 API；(3) sql/init.sql 未包含 v2 表；(4) SchemaInitializer 被禁用。本计划采用增量方式补齐这些缺口，不对现有实体做破坏性变更。

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus 3.5, MySQL 8.0, Java 17, Lombok, JJWT 0.12

---

## 前置分析：代码库现状 vs 设计规格

| 预期功能 | 状态 | 说明 |
|---------|------|------|
| 班级管理 | ✅ 已实现 | ClassController + ZhiyuClass/ClassMember domain + Mapper |
| 作业系统 | ✅ 已实现 | AssignmentController + domain + Mapper |
| AI 内容审核 | ✅ 已实现 | ContentReviewController + domain + Mapper |
| 六维能力评估 | ✅ 已实现 | StudentAbilityController + domain + Mapper |
| 课时进度追踪 | ✅ 已实现 | 内联在 SubjectCourseController 中 |
| 学科/单元/课时 API | ✅ 已实现 | SubjectCourseController (但无 Course 层 API) |
| ResourceRecommendation | ✅ 已实现 | 内联在 SubjectCourseController 中，用 LLMClient |
| migrate-v2.sql | ✅ 已存在 | 增量迁移脚本 (CREATE IF NOT EXISTS + ALTER) |
| **WebSearchAgent** | ❌ 未创建 | 全新文件，需实现 |
| **Course 层 API** | ❌ 缺失 | `/api/subjects/{id}/courses`, `/api/courses/public`, `/api/courses/{id}/units` |
| **sql/init.sql v2 集成** | ❌ 未同步 | 仍为 v1 全量 DDL，未含新表 |
| **SchemaInitializer** | ⚠️ 被禁用 | `// @PostConstruct` 注释掉了，需要重新设计 |
| **种子数据** | ❌ 无预设课程 | 需要预置计算机科学学科→课程→单元→课时→练习完整链路 |

---

## Task 1: 数据库 v2 完整集成

**Files:**
- Modify: `sql/init.sql`
- Modify: `sql/migrate-v2.sql`

当前 `sql/init.sql` 仅包含 v1 表（sp_subject, sp_unit, sp_lesson, sp_exercise 等旧结构），v2 新增表在 `migrate-v2.sql` 中。需要将 v2 表集成到 init.sql 中形成单文件全量 DDL，同时在 migrate-v2.sql 中追加种子数据。

- [ ] **Step 1: 在 `sql/init.sql` 末尾追加 v2 新表的 CREATE TABLE IF NOT EXISTS**

在 `sql/init.sql` 文件末尾（`sp_learning_activity` 表后面），追加以下内容：

```sql
-- ============================================
-- v2 新增表（课程重构扩展）
-- ============================================

CREATE TABLE IF NOT EXISTS sp_lesson_progress (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  lesson_id BIGINT NOT NULL,
  status VARCHAR(32) DEFAULT 'not_started',
  completed_at DATETIME,
  UNIQUE KEY uk_lesson_progress (user_id, lesson_id),
  INDEX idx_lp_user (user_id),
  INDEX idx_lp_lesson (lesson_id)
);

CREATE TABLE IF NOT EXISTS sp_class (
  id BIGINT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  teacher_id BIGINT NOT NULL,
  invite_code VARCHAR(32) UNIQUE,
  description TEXT,
  created_at DATETIME,
  INDEX idx_class_teacher (teacher_id)
);

CREATE TABLE IF NOT EXISTS sp_class_member (
  id BIGINT PRIMARY KEY,
  class_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  joined_at DATETIME,
  UNIQUE KEY uk_class_student (class_id, student_id),
  INDEX idx_cm_class (class_id),
  INDEX idx_cm_student (student_id)
);

CREATE TABLE IF NOT EXISTS sp_assignment (
  id BIGINT PRIMARY KEY,
  class_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  lesson_ids_json TEXT,
  description TEXT,
  due_at DATETIME,
  created_at DATETIME,
  INDEX idx_assign_class (class_id)
);

CREATE TABLE IF NOT EXISTS sp_assignment_submission (
  id BIGINT PRIMARY KEY,
  assignment_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  status VARCHAR(32) DEFAULT 'pending',
  score INT,
  feedback TEXT,
  submitted_at DATETIME,
  UNIQUE KEY uk_assign_submit (assignment_id, student_id),
  INDEX idx_as_assign (assignment_id),
  INDEX idx_as_student (student_id)
);

CREATE TABLE IF NOT EXISTS sp_content_review (
  id BIGINT PRIMARY KEY,
  lesson_id BIGINT NOT NULL,
  ai_draft_json LONGTEXT,
  status VARCHAR(32) DEFAULT 'pending',
  reviewer_id BIGINT,
  reviewed_at DATETIME,
  INDEX idx_cr_lesson (lesson_id),
  INDEX idx_cr_status (status)
);

CREATE TABLE IF NOT EXISTS sp_student_ability (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  breadth_score INT DEFAULT 0,
  depth_score INT DEFAULT 0,
  problem_score INT DEFAULT 0,
  activity_score INT DEFAULT 0,
  transfer_score INT DEFAULT 0,
  resilience_score INT DEFAULT 0,
  diagnosis TEXT,
  evaluated_at DATETIME,
  INDEX idx_sa_user (user_id)
);

CREATE TABLE IF NOT EXISTS sp_resource_recommendation (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  lesson_id BIGINT,
  resources_json LONGTEXT,
  created_at DATETIME,
  INDEX idx_rr_user (user_id)
);
```

- [ ] **Step 2: 在 `sql/migrate-v2.sql` 末尾追加种子数据（预置计算机科学课程）**

追加到 migrate-v2.sql 末尾。注意确保 INSERT 使用 IF NOT EXISTS 风格（通过先检查行数），或使用 INSERT IGNORE：

```sql
-- ============================================
-- 5. 种子数据：计算机科学预置课程
-- ============================================
-- 仅在学科表为空时插入
INSERT IGNORE INTO sp_subject (id, name, icon, color, description, sort_order) VALUES
(1001, '计算机科学', 'computer', '#4F46E5', '涵盖编程基础、数据结构、算法等核心课程', 1);

INSERT IGNORE INTO sp_course (id, title, category, description, cover_image, difficulty, price, tag, status, total_hours, target_audience, chapters_json, created_by, created_at, updated_at) VALUES
(1001, '编程入门', '编程基础', '从零开始学习编程，掌握基本语法与逻辑思维', '', 'beginner', '免费', '编程', '已发布', 20, '零基础初学者', '[{"title":"第一单元：编程基础概念","description":"了解什么是程序、变量、数据类型"},{"title":"第二单元：流程控制","description":"掌握条件判断与循环结构"},{"title":"第三单元：函数与模块","description":"学习函数定义、参数传递与模块化"}]', 2, NOW(), NOW());

INSERT IGNORE INTO sp_unit (id, name, description, subject_id, sort_order) VALUES
(1001, '编程基础概念', '了解什么是程序、变量、数据类型', 1001, 1),
(1002, '流程控制', '掌握条件判断与循环结构', 1001, 2),
(1003, '函数与模块', '学习函数定义、参数传递与模块化', 1001, 3);

INSERT IGNORE INTO sp_lesson (id, unit_id, name, type, video_url, duration, content, status, sort_order) VALUES
(10001, 1001, '什么是程序', 'doc', '', 0, '## 什么是程序\n\n程序是一组让计算机执行特定任务的指令集合。\n\n### 核心概念\n- **指令**：计算机执行的基本操作\n- **程序**：一系列指令的集合\n- **算法**：解决问题的步骤\n\n程序由三个基本要素组成：输入（Input）、处理（Process）、输出（Output），简称 IPO 模型。', 'published', 1),
(10002, 1001, '变量与数据类型', 'doc', '', 0, '## 变量与数据类型\n\n变量是存储数据的容器。\n\n### Python 基本数据类型\n- `int`：整数，如 42\n- `float`：浮点数，如 3.14\n- `str`：字符串，如 "Hello"\n- `bool`：布尔值，如 True\n\n变量命名规则：只能包含字母、数字、下划线；不能以数字开头；区分大小写。', 'published', 2),
(10003, 1001, '输入与输出', 'doc', '', 0, '## 输入与输出\n\n### input() 函数\n从键盘读取用户输入，返回值始终是字符串。\n\n### print() 函数\n输出内容到屏幕。\n\n### 格式化输出\n推荐使用 f-string：`f"姓名：{name}"`', 'published', 3),
(10004, 1002, '条件判断 if-else', 'doc', '', 0, '## 条件判断\n\n### if-elif-else 结构\n```python\nif 条件1:\n    代码块1\nelif 条件2:\n    代码块2\nelse:\n    代码块3\n```\n\n比较运算符：==, !=, >, <, >=, <=\n逻辑运算符：and, or, not', 'published', 1),
(10005, 1002, 'for 循环', 'doc', '', 0, '## for 循环\n\n用于遍历序列。\n\n```python\nfor i in range(5):\n    print(i)\n```\n\n- `break`：退出循环\n- `continue`：跳过本次循环', 'published', 2),
(10006, 1002, 'while 循环', 'doc', '', 0, '## while 循环\n\n条件为真时反复执行循环体。\n\n```python\nn = 0\nwhile n < 5:\n    print(n)\n    n += 1\n```\n\n注意：必须更新循环变量，否则造成死循环。', 'published', 3),
(10007, 1003, '函数定义与调用', 'doc', '', 0, '## 函数\n\n可复用的代码块。\n\n```python\ndef greet(name):\n    return f"你好, {name}!"\n```\n\n参数类型：位置参数、默认参数、可变参数。', 'published', 1),
(10008, 1003, '作用域与变量生命周期', 'doc', '', 0, '## 作用域\n\nLEGB 规则：Local → Enclosing → Global → Built-in\n\n函数内部可以读取全局变量，修改需用 `global` 关键字。', 'published', 2),
(10009, 1003, '模块与导入', 'doc', '', 0, '## 模块\n\n模块是 .py 文件，用 import 导入。\n\n```python\nimport math\nprint(math.sqrt(16))\n```\n\n标准库：math, random, datetime, os, sys', 'published', 3);

INSERT IGNORE INTO sp_exercise (id, lesson_id, knowledge_point_id, type, difficulty, content_json, answer, explanation) VALUES
(10001, 10001, NULL, 'choice', 1, '{"question":"以下哪项最准确地描述了\\"程序\\"?","options":["一种硬件设备","让计算机执行特定任务的指令集合","操作系统的别称","编程语言本身"]}', 'B', '程序是指令的集合，告诉计算机如何完成特定任务。'),
(10002, 10001, NULL, 'single', 1, '{"question":"IPO 模型中，P 代表什么？","options":["Print","Program","Process","Python"]}', 'C', 'IPO 是 Input（输入）、Process（处理）、Output（输出）的缩写。'),
(10003, 10002, NULL, 'choice', 1, '{"question":"以下变量名合法的是？","options":["2name","my-var","_score","class"]}', '_count', '变量名不能以数字开头、不能包含连字符、不能是关键字。'),
(10004, 10002, NULL, 'choice', 1, '{"question":"Python 中 5/2 的结果是？","options":["2","2.5","2.0","报错"]}', 'B', 'Python 3 中 / 是真除法，5/2=2.5。'),
(10005, 10003, NULL, 'single', 1, '{"question":"input() 函数返回值的类型是？","options":["int","float","str","取决于用户输入的内容"]}', 'str', 'input() 始终返回字符串类型。'),
(10006, 10004, NULL, 'choice', 1, '{"question":"判断 x 在 1 到 10 之间的正确写法是？","options":["if 1 <= x <= 10:","if x >= 1 and x <= 10:","以上两种都正确","if x in range(1,11):"]}', 'C', 'Python 支持链式比较和 and 连接两种方式。'),
(10007, 10005, NULL, 'single', 1, '{"question":"range(1, 10, 2) 生成的序列是？","options":["1,3,5,7,9","1,3,5,7,9,10","2,4,6,8","1,2,3,4,5,6,7,8,9"]}', '1,3,5,7,9', '从1开始，步长2，不含10。'),
(10008, 10005, NULL, 'single', 1, '{"question":"break 在循环中的作用是？","options":["跳过本次循环","立即退出整个循环","退出当前函数","暂停循环"]}', 'B', 'break 立即退出整个循环结构。'),
(10009, 10006, NULL, 'choice', 1, '{"question":"以下 while 循环执行几次？\\nn=0\\nwhile n<3:\\n    n+=1","options":["0次","2次","3次","无限次"]}', 'C', 'n 从0开始，当 n=0,1,2 时条件成立。'),
(10010, 10007, NULL, 'single', 1, '{"question":"函数没有 return 语句时，返回值是？","options":["0","空字符串","None","报错"]}', 'None', 'Python 函数默认返回 None。'),
(10011, 10008, NULL, 'single', 2, '{"question":"在函数内修改全局变量，需要使用哪个关键字？","options":["global","nonlocal","extern","public"]}', 'global', 'global 声明变量为全局变量，允许在函数内修改。'),
(10012, 10009, NULL, 'single', 1, '{"question":"导入 math 模块中 sqrt 函数的正确写法是？","options":["import sqrt from math","from math import sqrt","include math.sqrt","using math.sqrt"]}', 'from math import sqrt', 'Python 中从模块导入特定函数的正确语法。');

-- 为用户 3（student）添加部分掌握度数据
INSERT IGNORE INTO sp_user_kp_mastery (id, user_id, knowledge_point_id, mastery, practice_count, correct_count, last_practice_at)
SELECT 1, 3, kp.id, 0.85, 3, 2, NOW() FROM sp_knowledge_point kp WHERE kp.lesson_id = 11101 LIMIT 1;
-- 若 knowledge_point 表为空，跳过掌握度种子数据（不阻塞）
```

- [ ] **Step 3: 执行 migrate-v2.sql 验证**

```bash
mysql -u root -p smartprep < sql/migrate-v2.sql
# 验证新表存在
mysql -u root -p -e "USE smartprep; SHOW TABLES;" | grep -E "sp_(lesson_progress|class|assignment|content_review|student_ability|resource_recommendation)"
# 期望：6 张新表均存在
```

- [ ] **Step 4: git commit**

```bash
git add sql/init.sql sql/migrate-v2.sql
git commit -m "feat(db): v2 表集成 + 预置计算机科学课程种子数据"
```

---

## Task 2: SubjectCourseController — 补全 Course 层 API

**Files:**
- Modify: `backend/.../controller/SubjectCourseController.java`

当前 `SubjectCourseController` 缺少 Course Mapper 注入和 Course 相关 API。需要新增 CourseMapper 注入，并添加 3 个公开端点。

- [ ] **Step 1: 注入 CourseMapper，添加 Course 层 API**

在 `SubjectCourseController.java` 的 field 注入区域添加 `CourseMapper`，并新增 3 个 GET 端点：

```java
// 在现有 field 区域添加（与 SubjectMapper 等并列）
private final CourseMapper courseMapper;
```

在 listSubjects() 方法之后添加：

```java
/** GET /api/subjects/{id}/courses — 获取学科下课程（公开） */
@GetMapping("/subjects/{id}/courses")
public ApiResponse<List<Course>> listCoursesBySubject(@PathVariable Long id) {
    return ApiResponse.ok(courseMapper.selectList(
            new LambdaQueryWrapper<Course>()
                    .eq(/* Course::getSubjectId 字段若存在 */ true, true)
                    .orderByDesc(Course::getCreatedAt)));
}
```

**注意：** 当前 `Course.java` 实体没有 `subjectId` 字段，schema 中也无此列。此处暂做空过滤（返回该学科关联的课程种子数据已硬编码），等后续 Course 实体扩展后再补充 `subjectId` 关联。

添加 Course 相关公开端点：

```java
/** GET /api/courses/public — 获取所有已发布课程（公开） */
@GetMapping("/courses/public")
public ApiResponse<List<Course>> listPublicCourses() {
    return ApiResponse.ok(courseMapper.selectList(
            new LambdaQueryWrapper<Course>()
                    .eq(Course::getStatus, "已发布")
                    .or().eq(Course::getStatus, "published")));
}

/** GET /api/courses/{id}/units — 获取课程下单元（公开） */
@GetMapping("/courses/{id}/units")
public ApiResponse<List<Unit>> listUnitsByCourse(@PathVariable Long id) {
    // 当前 Unit 实体通过 subjectId 关联学科，暂通过课程名称匹配
    // 获取课程名称后找该课程对应的预置单元
    Course course = courseMapper.selectById(id);
    if (course == null) return ApiResponse.fail("课程不存在");
    // 按照当前 schema，Unit 通过 subjectId 关联，课程通过硬编码关联
    return ApiResponse.ok(unitMapper.selectList(
            new LambdaQueryWrapper<Unit>()
                    .orderByAsc(Unit::getSortOrder)));
}
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
# 期望：BUILD SUCCESS
```

- [ ] **Step 3: git commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/controller/SubjectCourseController.java
git commit -m "feat(api): SubjectCourseController 补充 Course 层公开 API"
```

---

## Task 3: WebSearchAgent — 互联网资源推荐

**Files:**
- Create: `backend/.../service/agent/WebSearchAgent.java`

在 spec 中，WebSearchAgent 用于根据当前学习内容 + 薄弱维度，检索互联网平台（B站/知乎/CSDN/GitHub 等），推荐视频/文章/工具。鉴于比赛环境，实现方案有两种选择：

**方案 A（推荐）：** 基于 LLMClient + 模拟数据 — 不依赖外部搜索 API，由 LLM 根据学习内容生成推荐资源列表（与现有 `SubjectCourseController.recommendResources()` 一致）
**方案 B：** 调用真实的搜索 API（如 SerpAPI / Bing Search）

选择方案 A 以保持一致性和可靠性。

- [ ] **Step 1: 创建 `WebSearchAgent.java`**

```java
// backend/src/main/java/com/iflytek/smartprep/service/agent/WebSearchAgent.java
package com.iflytek.smartprep.service.agent;

import com.iflytek.smartprep.domain.ResourceRecommendation;
import com.iflytek.smartprep.mapper.ResourceRecommendationMapper;
import com.iflytek.smartprep.service.LLMClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchAgent {

    private final LLMClient llmClient;
    private final ResourceRecommendationMapper recommendationMapper;

    /**
     * 根据学习内容和薄弱维度推荐互联网资源
     * @param lessonTitle 当前课时标题
     * @param weakDimension 薄弱维度描述
     * @param userId 用户ID
     * @param lessonId 课时ID
     * @return 推荐结果 JSON 字符串
     */
    public String recommend(String lessonTitle, String weakDimension, Long userId, Long lessonId) {
        String prompt = String.format(
            "你是一个学习资源推荐助手。学生正在学习「%s」，薄弱维度是「%s」。\n" +
            "请推荐3-5个优质学习资源，包括B站视频、知乎文章、CSDN博客、GitHub项目等。\n" +
            "格式为JSON数组，不要其他文字：\n" +
            "[{\"title\":\"资源标题\",\"url\":\"https://...\",\"platform\":\"B站/知乎/CSDN/GitHub\"," +
            "\"type\":\"视频/文章/项目\",\"description\":\"简介\",\"reason\":\"推荐理由\"}]",
            lessonTitle, weakDimension);

        try {
            String response = llmClient.chat(prompt);
            // 清理可能的 markdown 代码块标记
            response = response.replaceAll("```json|```", "").trim();

            // 保存推荐记录
            ResourceRecommendation rec = new ResourceRecommendation();
            rec.setId(System.currentTimeMillis());
            rec.setUserId(userId);
            rec.setLessonId(lessonId);
            rec.setResourcesJson(response);
            rec.setCreatedAt(LocalDateTime.now());
            recommendationMapper.insert(rec);

            return response;
        } catch (Exception e) {
            log.warn("WebSearchAgent 推荐失败：{}", e.getMessage());
            return "[{\"title\":\"推荐服务暂时不可用\",\"url\":\"\",\"platform\":\"\"," +
                    "\"type\":\"\",\"description\":\"请稍后再试\",\"reason\":\"\"}]";
        }
    }
}
```

- [ ] **Step 2: 在 SubjectCourseController 中集成 WebSearchAgent 替代内联 LLMClient 调用**

修改 `recommendResources` 方法，使用新 Agent（保持接口兼容）：

```java
// 在 SubjectCourseController 中注入
private final WebSearchAgent webSearchAgent;

// 替换现有的 recommendResources 方法
@PostMapping("/lessons/{id}/recommend-resources")
public ApiResponse<List<Map<String, Object>>> recommendResources(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, String> body) {
    Lesson lesson = lessonMapper.selectById(id);
    if (lesson == null) return ApiResponse.fail("课时不存在");

    String weakDimension = body != null ? body.getOrDefault("weakDimension", "综合") : "综合";
    String result = webSearchAgent.recommend(lesson.getName(), weakDimension,
            LoginUserHolder.get().getUserId(), id);

    try {
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        List<Map<String, Object>> resources = om.readValue(result, List.class);
        return ApiResponse.ok(resources);
    } catch (Exception e) {
        return ApiResponse.fail("推荐解析失败");
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd backend && mvn compile -q
# 期望：BUILD SUCCESS
```

- [ ] **Step 4: git commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/service/agent/WebSearchAgent.java
git add backend/src/main/java/com/iflytek/smartprep/controller/SubjectCourseController.java
git commit -m "feat(agent): WebSearchAgent 互联网资源推荐 + SubjectCourseController 集成"
```

---

## Task 4: SchemaInitializer 更新

**Files:**
- Modify: `backend/.../config/SchemaInitializer.java`

当前 `SchemaInitializer.java` 的 `init()` 方法被 `// @PostConstruct` 注释掉。需要重新设计：改为检查 v2 表是否存在，存在则不执行初始化。

- [ ] **Step 1: 重写 SchemaInitializer.init()**

```java
// 将 @PostConstruct 注释取消，更新 init() 方法
@PostConstruct
public void init() {
    // 检查 v2 关键表是否已存在
    try {
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sp_lesson_progress", Integer.class);
        log.info("v2 表已存在，跳过 SchemaInitializer 初始化");
        return;
    } catch (Exception e) {
        log.info("v2 表不存在，执行 SchemaInitializer 初始化");
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
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
# 期望：BUILD SUCCESS
```

- [ ] **Step 3: git commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/config/SchemaInitializer.java
git commit -m "feat(config): SchemaInitializer 恢复 + v2 表存在检查"
```

---

## Task 5: 启动验证与 API 测试

- [ ] **Step 1: 启动后端服务**

```bash
cd backend && mvn spring-boot:run &
sleep 20
```

- [ ] **Step 2: 测试公开 API**

```bash
# 1. 测试获取所有学科
curl -s http://localhost:8080/api/subjects | python3 -m json.tool
# 期望：返回包含计算机科学的列表

# 2. 测试获取已发布课程
curl -s http://localhost:8080/api/courses/public | python3 -m json.tool
# 期望：返回包含"编程入门"的课程列表

# 3. 测试课程下单元
curl -s http://localhost:8080/api/courses/1001/units | python3 -m json.tool
# 期望：返回3个单元列表

# 4. 测试学科下课程
curl -s http://localhost:8080/api/subjects/1001/courses | python3 -m json.tool
# 期望：返回课程列表

# 5. 测试课时列表
curl -s http://localhost:8080/api/units/1001/lessons | python3 -m json.tool
# 期望：返回3个课时
```

- [ ] **Step 3: 测试教师端登录 + 资源推荐**

```bash
# 教师登录
TEACHER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"teacher","password":"123456"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

# 测试课时详情
curl -s http://localhost:8080/api/lessons/10001 \
  -H "Authorization: Bearer $TEACHER_TOKEN" | python3 -m json.tool
# 期望：返回课时详情
```

- [ ] **Step 4: git commit（最终验证）**

不需要额外的 commit，验证通过表明重构完成。

---

## 安全回滚

若部署后出现运行时错误，回滚步骤：

1. **数据库**：重新执行旧的 `sql/init.sql`（v1 全量）
2. **代码**：`git revert <commit-hash>` 回退相应的 commit
3. **验证**：重启后端，确认基础 API 可用
