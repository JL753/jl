# 学生端B站视频导入设计规格

**日期**：2026-05-16
**目标**：学生端"学科"页面新增B站视频导入入口，导入后进入个人学习空间
**关联**：[[2026-05-16-bilibili-video-import-design]]

---

## 1. 功能概述

学生在"学科"页面点击 [B站导入] 按钮，粘贴/搜索/合集导入B站视频。AI 分类+生成讲义练习后，课时进入学生的私有学习空间（仅自己可见），在"学科"页面的"我的导入视频"区域展示。

教师导入 → 公共课程库（userId=null）。学生导入 → 个人空间（userId=学生ID）。

---

## 2. 后端改动

### 2.1 Lesson 新增字段

`sp_lesson` 表新增 `user_id` 字段（BIGINT, nullable）：
- NULL = 公共课程库课时
- 有值 = 该用户个人导入的课时

### 2.2 VideoImportPipeline 改动

`importVideos` 方法新增 `Long userId` 参数。写 Lesson 时设置 `lesson.setUserId(userId)`。

### 2.3 BilibiliController 改动

`/import` 端点从 `LoginUserHolder.get().getUserId()` 获取当前用户 ID，传入 pipeline。

### 2.4 新增查询端点

```
GET /api/lessons/my-imports
  → 查询 Lesson.userId = 当前用户 的记录
  → 按 videoUrl 去重分组（同一 BVID 只显示一次）
  → 返回：{ bvid, title, coverUrl, subjectName, unitName, lessonCount, completedCount }
```

### 2.5 CourseService 改动

学生端查课（getSubjectTree 等）时，合并公共课时 + userId 匹配的个人课时。

---

## 3. 前端改动

### 3.1 SubjectCatalog.vue

- 标题行："探索知识领域" 右侧添加 [B站导入] 按钮
- 学科卡片网格下方添加 "我的导入视频" 区域：
  - 一行卡片，水平滚动
  - 每张卡片：封面图 + 视频标题 + 归属学科/单元 + 进度（N/M 课时）
  - 点击卡片 → `/student/lessons/:firstLessonId`
- [B站导入] 点击 → 弹出 `BilibiliImportModal`（复用）
- 导入完成后刷新列表

### 3.2 BilibiliImportModal

复用，无需改动。

---

## 4. 改动清单

| 层 | 文件 | 操作 |
|----|------|------|
| 后端 | `domain/Lesson.java` | 新增 userId 字段 |
| 后端 | `service/VideoImportPipeline.java` | 新增 userId 参数 |
| 后端 | `controller/BilibiliController.java` | 从 token 取 userId |
| 后端 | `controller/SubjectCourseController.java` | 新增 GET /api/lessons/my-imports |
| 后端 | `sql/migrate-v2.sql` | 新增 ALTER TABLE sp_lesson ADD user_id |
| 前端 | `views/common/SubjectCatalog.vue` | 新增 [B站导入] + "我的导入视频" |
| 前端 | `api/index.js` | 新增 apiMyImports |
