# v3 Hierarchy Migration — 前端路由修复 + 后端字段名修正

## 概述

将全栈数据流从旧 `Lesson/Unit` 实体迁移到新 v3 `Subject → Course → Chapter → SubChapter` 层级。8 步按依赖顺序排列。

---

## 第 1 步：学科页前端修改 + 修复路由

### 文件变更

| 文件 | 改动 |
|------|------|
| `CourseDetail.vue:150-156` | `goToLesson(scId)` 改为 `router.push(\`/student/courses/${courseId}?sc=${scId}\`)` |
| `CourseView.vue:261-263` | `loadCourse()` 从 `route.params.id` 取 courseId，从 `route.query.sc` 取可选 subChapterId |
| `SubjectCatalog.vue:23` | "我的导入" 卡片跳转改为传 courseId+subChapterId |
| `StudentDashboard.vue:208-213` | `goContinue()` 中 lessonId 跳转改为 v3 路由 |
| `KnowledgeStarMap.vue:298-306` | `goToLesson(node)` 中 lessonId 跳转改为正确路由 |

### 效果

学科目录 → 课程卡片 → 章节目录 → 课程学习页（左侧目录树、中间视频/讲义/字幕/练习题、右侧 AI 辅导/笔记/讨论）。首页"继续学习"和星图"去学习"按钮正确跳转。

---

## 第 2 步：修复 LearningDataController 字段名

### 文件变更

| 位置 | 改动 |
|------|------|
| `LearningDataController.java:36` | `body.get("lessonId")` → `body.get("subChapterId")` |
| `LearningDataController.java:69` | 同上 |
| `LearningDataController.java:52-56` | `lessonMapper.selectById(lessonId)` → `subChapterMapper.selectById(lessonId)` |
| `LearningDataController.java:98-99` | 不变 — KnowledgePoint.lessonId 存的就是 subChapterId |

### 效果

做题记录写入 sp_exercise_attempt，学习时长写入 sp_study_duration，知识点掌握度 sp_user_kp_mastery 开始更新。

---

## 第 3 步：修复 StudentAbilityController — 六维能力评估

### 文件变更

| 改动 | 说明 |
|------|------|
| 注入 SubChapterMapper, ChapterMapper, CourseMapper | 替换 LessonMapper, UnitMapper |
| 广度计算重写（第 43-61 行） | 遍历 LessonProgress → SubChapter → Chapter → Course → Subject，广度 = (覆盖学科数/总学科数) × (覆盖课程数/总课程数) × 100 |
| 知识迁移计算（第 95-107 行） | 遍历不变，含义正确（ExerciseAttempt.lessonId 存 subChapterId） |

### 效果

六维雷达图显示真实数据，广度反映跨学科数和课程数。

---

## 第 4 步：修复 DashboardServiceImpl — 首页继续学习和 AI 路径

### 文件变更

| 位置 | 改动 |
|------|------|
| `studentDashboard()` 第 68-95 行 | continueLearning：取 LessonProgress → subChapterMapper → SubChapter → Chapter → Course → Subject，返回 courseId, subChapterId, courseName, chapterName, subChapterTitle |
| 第 97-149 行 | AI 推荐路径重写为 v3 策略：同 Chapter 后续 SubChapter → 同 Course 其他 Chapter 第一个 SubChapter → 同 Subject 其他 Course → 任意其他 Course |

### 效果

首页"继续上次学习"显示正确层级名称；AI 推荐按 Subject→Course→Chapter→SubChapter 层级给出下一步。

---

## 第 5 步：修复 KnowledgeGraphServiceImpl — 星图返回层级数据

### 文件变更

| 改动 | 说明 |
|------|------|
| 注入 SubChapterMapper, ChapterMapper, CourseMapper, SubjectMapper | 新增依赖 |
| `getFullGraph()` 扩展 | 节点加入 Subject/Course/Chapter/SubChapter 四层节点（各带 type 字段）；边加入 BELONGS_TO/CONTAINS 层级边；保留 DEPENDS_ON 边 |

### 效果

API 返回五层节点 + 层级边，force 布局自然聚团。

---

## 第 6 步：修复 KnowledgeStarMap 前端 — 星图分层展示

### 文件变更

| 改动 | 说明 |
|------|------|
| 节点渲染按 type 区分大小/颜色 | Subject=最大金色, Course=较大紫色, Chapter=中等蓝色, SubChapter=较小青色, KP=最小按掌握度着色 |
| 删除右上角图例 | 移除"未学习/学习中/已掌握/精通" legend |
| 修复 `goToLesson()` | KnowledgePoint 节点用 subChapterId，SubChapter 节点直接用 id |

---

## 第 7 步：清理死代码和过期路由

### 文件变更

| 文件 | 改动 |
|------|------|
| `api/index.js:55-64` | 删除 apiLessonDetail, apiLessonExercises, apiCompleteLesson, apiLessonProgress, apiLessonKnowledgePoints, apiRecommendResources |
| `router/index.js:14,27` | 删除 `/lessons/:id` 和 `/student/lessons/:id` 路由 |
| `LessonView.vue` | 删除文件 |
| `PortalHome.vue` | 修复 apiRecommendResources 调用 |

---

## 第 8 步：Neo4j 数据同步

### 文件变更

| 改动 | 说明 |
|------|------|
| 新增 KnowledgeGraphSyncService | 启动时从 MySQL 读取全量 Subject/Course/Chapter/SubChapter/KP，写入 Neo4j 节点和关系边 |
| KnowledgePoint.lessonId → subChapterId | 可选，影响面大可推迟 |

---

## 路由决策

- 旧 `/student/lessons/:id` 和 `/lessons/:id` **直接删除**（不保留 redirect），未匹配路由由 catch-all 兜底到 `/`。

## 不做的

- KnowledgePoint 字段重命名（lessonId → subChapterId）推迟，避免 DB 迁移风险。
