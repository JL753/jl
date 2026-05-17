# 课程功能移植设计文档

**日期**: 2026-05-17
**来源**: ai-assistant-teaching-website → softwacecup
**范围**: 学生端课程浏览、选课、课程详情Tab页、我的课程

---

## 目标

将源项目（ai-assistant-teaching-website）的课程学习相关功能移植到目标项目（softwacecup），在目标项目的学生端新增三个独立页面：课程大厅、课程详情、我的课程。

---

## 架构概览

### 新增前端文件

```
frontend/src/
├── views/student/
│   ├── CourseHall.vue              # 课程大厅 — 浏览所有可用课程
│   ├── CourseInfo.vue              # 课程详情 — 课程信息、加入课程、Tab内容
│   └── MyCourses.vue               # 我的课程 — 已选课程列表
├── components/course/
│   ├── CourseCard.vue              # 课程卡片（课程大厅和我的课程复用）
│   ├── CourseDesign.vue            # Tab: 课程设计（背景/目标/原则）
│   ├── CourseTutorial.vue          # Tab: 在线教程（章节树）
│   ├── CourseAnnouncements.vue     # Tab: 课程公告列表
│   ├── CourseQA.vue                # Tab: 课程问答列表
│   ├── CourseResources.vue         # Tab: 课程资源列表
│   └── CourseHomework.vue          # Tab: 作业测试列表
```

### 新增后端文件

```
backend/src/main/java/com/iflytek/smartprep/
├── domain/
│   ├── CourseAnnouncement.java     # 课程公告实体
│   ├── CourseResource.java         # 课程资源实体
│   ├── CourseAssignment.java       # 课程作业实体
│   ├── CourseQuestion.java         # 课程问答实体
│   └── StudentCourse.java          # 学生选课实体
├── mapper/
│   ├── CourseAnnouncementMapper.java
│   ├── CourseAssignmentMapper.java
│   ├── CourseResourceMapper.java
│   ├── CourseQuestionMapper.java
│   └── StudentCourseMapper.java
└── controller/
    └── CourseCenterController.java  # 课程中心API（选课、公告、资源、问答）
```

### 现有文件修改

- `StudentLayout.vue` — 侧边栏新增"课程大厅"和"我的课程"入口
- `router/index.js` — 新增3条学生端路由
- `api/index.js` — 新增课程相关API函数

---

## 页面设计

### CourseHall.vue — 课程大厅

- 页面标题"课程大厅"+ 搜索输入框
- 响应式网格布局（2-4列），每列一个 CourseCard
- CourseCard 展示：封面图、课程名、简介（截断）、评分、学习人数、资源数、标签
- 点击卡片跳转至 CourseInfo
- 空态："暂无可用课程"

### CourseInfo.vue — 课程详情

- 顶部：渐变色横幅，显示课程名 + 标语
- 横幅下方：课程图片、介绍、统计数据（评分/学习人数/资源数）
- "加入课程"按钮（已加入则禁用，未登录则提示登录）
- 左侧Tab导航（el-tabs），包含6个Tab：
  1. 课程设计 — 课程背景、目标、设计原则（纯文本）
  2. 在线教程 — 章节树（el-tree，默认全部展开）
  3. 课程公告 — 公告卡片列表，类型标签（普通/重要/警示）
  4. 课程问答 — 问答卡片列表，含AI回答入口
  5. 课程资源 — 资源卡片列表，类型图标、大小、下载按钮
  6. 作业测试 — 作业卡片列表，类型图标、开始时间、查看按钮

### MyCourses.vue — 我的课程

- 页面标题"我的课程"+ 数量标签
- 已选课程卡片网格
- 每张卡片：封面图、课程名、进度条、上次访问时间、"继续学习"按钮
- 空态："还没有加入任何课程"+ 跳转课程大厅链接
- 点击卡片跳转至 CourseInfo

---

## 组件详情

### CourseCard.vue
- Props: `course` { id, title, coverImage, description, tag, rating, studentCount, resourceCount }
- 触发: click → 导航至 CourseInfo

### CourseDesign.vue
- Props: `background`, `target`, `principle` (String)
- 渲染: 三个带分隔线的文本区域

### CourseTutorial.vue
- 数据来源: `sp_course.chapters_json` 字段（JSON数组，每项含 title + children 递归结构）
- Props: `chapters` (树形数组，从后端解析好的JSON)
- 渲染: el-tree，默认全部展开

### CourseAnnouncements.vue
- Props: `announcements` [{id, title, type, time}]
- 类型颜色: 普通公告→primary, 重要公告→warning, 警示公告→danger

### CourseQA.vue
- Props: `questions` [{id, question}]
- 每个卡片显示问题文字 + AI回答占位

### CourseResources.vue
- Props: `resources` [{id, title, type, size, time, url}]
- 类型图标: 视频/课件/文档, 下载按钮

### CourseHomework.vue
- 数据来源: 新增 `sp_course_assignment` 表（课程级作业，与现有班级级 sp_assignment 分离）
- Props: `assignments` [{id, title, type, time}]
- 类型图标: 作业/测试, 查看按钮

---

## 数据模型（新增表）

```sql
CREATE TABLE sp_student_course (
  id BIGINT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  school VARCHAR(64),
  enrolled_at DATETIME,
  UNIQUE KEY uk_student_course (student_id, course_id)
);

CREATE TABLE sp_course_announcement (
  id BIGINT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  title VARCHAR(256) NOT NULL,
  type VARCHAR(32) DEFAULT '普通公告',
  content TEXT,
  created_at DATETIME,
  INDEX idx_ann_course (course_id)
);

CREATE TABLE sp_course_question (
  id BIGINT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  question TEXT NOT NULL,
  answer TEXT,
  student_id BIGINT,
  created_at DATETIME,
  INDEX idx_q_course (course_id)
);

CREATE TABLE sp_course_assignment (
  id BIGINT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  title VARCHAR(256) NOT NULL,
  type VARCHAR(32) DEFAULT '作业',
  start_time DATETIME,
  created_at DATETIME,
  INDEX idx_asg_course (course_id)
);

CREATE TABLE sp_course_resource (
  id BIGINT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  title VARCHAR(256) NOT NULL,
  type VARCHAR(32),
  size VARCHAR(32),
  url VARCHAR(512),
  created_at DATETIME,
  INDEX idx_res_course (course_id)
);
```

现有 `sp_course` 表已包含所需字段: id, title, description, cover_image, tag, status, total_hours, target_audience, chapters_json。

课程统计数据（评分/学习人数/资源数）由服务端实时计算：
- 评分 = 该课程所有学生评分的平均值（来自 sp_course_review 或占位默认值）
- 学习人数 = COUNT(sp_student_course WHERE course_id = ?)
- 资源数 = COUNT(sp_course_resource WHERE course_id = ?)

---

## API 端点

| Method | Path | Purpose | Auth |
|--------|------|---------|------|
| GET | `/api/courses/public` | 所有已发布课程 | No *(exists)* |
| GET | `/api/courses/{id}` | 课程详情 | No |
| POST | `/api/courses/{id}/enroll` | 学生选课 | Yes |
| GET | `/api/courses/my` | 我的已选课程 | Yes |
| GET | `/api/courses/{id}/announcements` | 课程公告列表 | No |
| GET | `/api/courses/{id}/resources` | 课程资源列表 | No |
| GET | `/api/courses/{id}/questions` | 课程问答列表 | No |
| GET | `/api/courses/{id}/assignments` | 课程作业列表 | Yes |

---

## 路由新增

```js
// 在学生端 /student 子路由下新增:
{ path: 'course-hall', name: 'course-hall', component: CourseHall }
{ path: 'course-info/:id', name: 'course-info', component: CourseInfo }
{ path: 'my-courses', name: 'my-courses', component: MyCourses }
```

---

## 设计原则

- 所有新页面使用目标项目的 design-tokens.css 和 global.scss
- 复用 glass-card 样式、暗色主题变量
- 使用 Element-Plus 组件（el-tabs, el-tree, el-card, el-tag, el-button 等）
- 不引入 Naive UI（源项目依赖，目标项目不使用）
- API 遵循目标项目 ApiResponse 格式（success/data/message）
- 认证使用目标项目 JWT token + auth store
