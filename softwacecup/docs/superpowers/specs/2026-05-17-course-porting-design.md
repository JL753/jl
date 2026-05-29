# 课程功能移植与重构设计文档

**日期**: 2026-05-17
**来源**: ai-assistant-teaching-website → softwacecup
**范围**: 层级重构(Subject→Course→Chapter→SubChapter) + 课程Q&A论坛 + 章节资源/公告

---

## 目标

将源项目的章节式课程结构和课程Q&A功能移植到目标项目，同时重构现有层级：
- 现有: Subject → Unit → Lesson
- 目标: Subject → Course → Chapter → SubChapter

---

## 数据模型

### 增强现有表

**sp_course** — 新增字段:
| 字段 | 类型 | 说明 |
|------|------|------|
| subject_id | BIGINT FK→sp_subject | 关联学科 |
| background | VARCHAR(512) | 课程背景（源 Course.background） |
| target | VARCHAR(512) | 教学目标（源 Course.target） |
| principle | VARCHAR(512) | 设计原则（源 Course.principle） |

已有字段保留: id, title, category, description, cover_image, price, tag, status, total_hours, target_audience, created_by, created_at, updated_at。移除 chapters_json 字段（由 sp_chapter 表替代）。

### 新增表

**sp_chapter** — 合并 Unit + 源 CourseChapter:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| course_id | BIGINT FK→sp_course | 所属课程 |
| title | VARCHAR(256) | 章节标题 |
| description | TEXT | 章节描述 |
| sort_order | INT | 排序 |
| prerequisite_chapter_id | BIGINT | 前置章节（来自 Unit.prerequisiteUnitId） |
| created_at | DATETIME | |

**sp_sub_chapter** — 合并 Lesson + 源 ChildChapter:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| chapter_id | BIGINT FK→sp_chapter | 所属章节 |
| title | VARCHAR(256) | 子章节标题 |
| description | TEXT | 子章节描述 |
| sort_order | INT | 排序 |
| type | VARCHAR(32) | 类型（视频/文档/等） |
| video_url | VARCHAR(512) | 视频链接 |
| duration | INT | 时长(秒) |
| content | TEXT | 正文内容 |
| cover_url | VARCHAR(512) | 封面图 |
| status | VARCHAR(32) | 状态 |
| user_id | BIGINT | 创建者(private课) |

**sp_chapter_resource** — 章节资源（源 ChapterResource）:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| course_id | BIGINT FK→sp_course | |
| chapter_id | BIGINT FK→sp_chapter | |
| type | VARCHAR(32) | 资源类型（视频/课件/文档） |
| title | VARCHAR(256) | |
| description | TEXT | |
| url | VARCHAR(512) | 下载/预览URL |
| size | VARCHAR(32) | 文件大小 |
| created_at | DATETIME | |

**sp_course_announcement** — 课程公告（源 CourseAnnouncement）:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| course_id | BIGINT FK→sp_course | |
| type | VARCHAR(32) | 普通公告/重要公告/警示公告 |
| title | VARCHAR(256) | |
| content | TEXT | |
| created_at | DATETIME | |

**sp_course_question** — 课程问答（源 CourseQuestion）:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| course_id | BIGINT FK→sp_course | |
| user_id | BIGINT FK→sp_user | 提问者 |
| title | VARCHAR(256) | |
| content | TEXT | |
| created_at | DATETIME | |

**sp_course_answer** — 问答回复（源 QuestionAnswer）:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| question_id | BIGINT FK→sp_course_question | |
| user_id | BIGINT | 回答者（NULL=AI回答） |
| content | TEXT | |
| is_ai | TINYINT(1) | 是否AI生成 |
| created_at | DATETIME | |

### 移除表

- sp_unit → 数据迁移至 sp_chapter
- sp_lesson → 数据迁移至 sp_sub_chapter

### 迁移策略

1. 创建新表 sp_chapter, sp_sub_chapter
2. 将 sp_unit 数据迁入 sp_chapter（unit.subject_id → 查找或创建对应 Course，course.id → chapter.course_id）
3. 将 sp_lesson 数据迁入 sp_sub_chapter（lesson.unit_id → sub_chapter.chapter_id）
4. 验证数据完整性后删除 sp_unit, sp_lesson
5. 为 sp_course 添加 subject_id FK，填充数据后删除 sp_course.category（由 subject_id 替代）

---

## API 端点

所有端点遵循现有 ApiResponse 格式 `{success, data, message}`。

### 学科与课程

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/subjects | No | 学科列表(保留) |
| GET | /api/subjects/{id} | No | 学科详情(保留) |
| GET | /api/subjects/{id}/courses | No | 学科下课程列表 |
| GET | /api/courses/public | No | 所有已发布课程 |
| GET | /api/courses/{id} | No | 课程详情(含background/target/principle) |
| POST | /api/courses | teacher | 创建课程 |
| PUT | /api/courses/{id} | teacher | 更新课程 |
| DELETE | /api/courses/{id} | teacher | 删除课程 |
| POST | /api/courses/{id}/enroll | student | 学生选课 |
| GET | /api/courses/my | student | 我的已选课程 |

### 章节(替换原Unit端点)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/courses/{id}/chapters | No | 课程章节树(含嵌套sub-chapters) |
| GET | /api/chapters/{id} | No | 章节详情 |
| POST | /api/chapters | teacher | 创建章节 |
| PUT | /api/chapters/{id} | teacher | 更新章节 |
| DELETE | /api/chapters/{id} | teacher | 删除章节 |
| PUT | /api/chapters/reorder | teacher | 章节排序 |

### 子章节(替换原Lesson端点)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/sub-chapters/{id} | No | 子章节详情(含video/content/knowledge-points/exercises) |
| POST | /api/sub-chapters | teacher | 创建子章节 |
| PUT | /api/sub-chapters/{id} | teacher | 更新子章节 |
| DELETE | /api/sub-chapters/{id} | teacher | 删除子章节 |
| GET | /api/sub-chapters/{id}/exercises | No | 子章节练习列表 |
| GET | /api/sub-chapters/{id}/knowledge-points | No | 子章节知识点 |

### 章节资源

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/chapters/{id}/resources | No | 章节资源列表 |
| POST | /api/chapters/{id}/resources | teacher | 上传资源 |
| DELETE | /api/resources/{id} | teacher | 删除资源 |
| GET | /api/courses/{id}/resources | No | 课程所有资源 |

### 课程公告

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/courses/{id}/announcements | No | 课程公告列表 |
| POST | /api/courses/{id}/announcements | teacher | 发布公告 |
| PUT | /api/announcements/{id} | teacher | 更新公告 |
| DELETE | /api/announcements/{id} | teacher | 删除公告 |

### 课程Q&A

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/courses/{id}/questions | No | 问题列表(分页) |
| POST | /api/courses/{id}/questions | student | 提问 |
| GET | /api/questions/{id}/answers | No | 问题回复列表 |
| POST | /api/questions/{id}/answers | student | 回答(人工) |
| POST | /api/questions/{id}/ai-answer | student | 请求AI回答 |

### 进度(保留，适配SubChapter)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/progress/sub-chapter/{id} | student | 子章节学习进度 |
| POST | /api/progress/sub-chapter/{id}/complete | student | 标记子章节完成 |
| GET | /api/progress/course/{id} | student | 课程总进度 |

### 保留不变

TutorController (AI辅导私聊), AdaptiveQuizController, KnowledgeMapController, AnalyticsController — 均保持不变，仅将内部引用从 Lesson 改为 SubChapter。

---

## 前端变更

### 新增文件

```
frontend/src/
├── components/course/
│   ├── ChapterTree.vue          # 章节树(Course→Chapter→SubChapter, el-tree)
│   ├── AnnouncementList.vue     # 公告卡片列表(类型颜色标签)
│   ├── ResourceList.vue         # 资源卡片(类型图标+下载)
│   ├── CourseQA.vue             # Q&A论坛(问题列表+提问+AI回答标记)
│   ├── CourseDesign.vue         # 课程设计Tab(背景/目标/原则)
│   └── CourseHomework.vue       # 章节作业列表
```

### 修改文件

| 文件 | 变更 |
|------|------|
| CourseView.vue | 左侧树：Subject→Unit→Lesson 替换为 Course→Chapter→SubChapter；讨论Tab：从占位符替换为 CourseQA 组件 |
| CourseDetail.vue | 添加 background/target/principle 展示、公告列表、章节预览、加入课程按钮 |
| SubjectCatalog.vue | 验证路由路径一致性 |
| ContentManagement.vue | 重写：章节/子章节CRUD编辑器、资源上传、公告管理 |
| TeacherManage.vue | 课程创建表单添加 background/target/principle 字段和章节编辑器 |
| router/index.js | 更新课程相关路由路径 |
| api/index.js | 新增章节/资源/公告/Q&A API函数 |

### 页面结构

**CourseView.vue (3栏布局,复用SplitPaneStudy):**
```
┌──────────┬────────────────────┬──────────┐
│ Chapter  │ SubChapter Content │ 右侧面板  │
│ Tree     │ (video/text/ex)   │ AI辅导    │
│          │                   │ 笔记      │
│ 课程A    │                   │ 讨论(Q&A) │
│ ├ 第1章  │                   │           │
│ │ ├ 1.1  │                   │           │
│ │ └ 1.2  │                   │           │
│ └ 第2章  │                   │           │
└──────────┴────────────────────┴──────────┘
```

**CourseDetail.vue (公开页):**
- 渐变色横幅(课程名 + 标语)
- 课程图片 + 介绍 + 统计数据
- "加入课程"按钮
- Tab导航: 课程设计 | 章节预览 | 公告 | Q&A | 资源

### 设计约束

- 使用目标项目现有 design-tokens.css 和 global.scss
- 复用 glass-card 样式和暗色主题变量
- 组件库: Element-Plus (el-tabs, el-tree, el-card, el-tag, el-button)
- 不引入 Naive UI
- API遵循 ApiResponse 格式
- 认证使用 JWT + auth store

---

## 后端变更

### 新增文件

```
backend/src/main/java/com/iflytek/smartprep/
├── domain/
│   ├── Chapter.java
│   ├── SubChapter.java
│   ├── ChapterResource.java
│   ├── CourseAnnouncement.java
│   ├── CourseQuestion.java
│   └── CourseAnswer.java
├── mapper/
│   ├── ChapterMapper.java
│   ├── SubChapterMapper.java
│   ├── ChapterResourceMapper.java
│   ├── CourseAnnouncementMapper.java
│   ├── CourseQuestionMapper.java
│   └── CourseAnswerMapper.java
├── controller/
│   ├── ChapterController.java
│   └── CourseQaController.java
└── service/
    ├── ChapterService.java
    ├── ChapterServiceImpl.java
    └── CourseQaService.java
```

### 修改文件

| 文件 | 变更 |
|------|------|
| Course.java | 添加 subjectId, background, target, principle; 移除 chaptersJson |
| CourseController.java | 添加 subject_id 过滤、课程CRUD |
| SubjectCourseController.java | 替换 Unit/Lesson 端点为 Chapter/SubChapter |
| CourseServiceImpl.java | 重构 getSubjectTree → getChapterTree; 替换 Unit→Chapter, Lesson→SubChapter |
| LessonProgress → SubChapterProgress | 进度实体/表指向 sub_chapter_id |
| Exercise.java | lessonId → subChapterId |
| KnowledgePoint.java | lessonId → subChapterId |
| schema.sql | 新增表DDL，移除 sp_unit/sp_lesson |

### 架构分层

```
Controller → Service → Mapper(MyBatis-Plus) → DB(MySQL)
     ↓
  ApiResponse<T>
```

所有新端点遵循现有分层模式，Service层处理业务逻辑，Mapper层继承 BaseMapper<T>。

---

## 测试要点

1. 数据迁移: sp_unit→sp_chapter, sp_lesson→sp_sub_chapter 数据完整性
2. 章节树API: 嵌套子章节正确返回
3. 进度跟踪: SubChapterProgress 替代 LessonProgress 正常工作
4. AI辅导: TutorService 在 sub-chapter 上下文中正确回答
5. Q&A论坛: 提问/回答/AI回答 三级流程
6. 角色权限: teacher可CRUD章节/资源/公告, student只能读+选课+提问
7. 前端路由: 新旧路径兼容, 导航守卫正确
