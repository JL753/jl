# B站合集导入重构 —— 合集 = 一门课程

## 概述

将 B 站合集/多P视频导入从"每个视频创建独立 Chapter+SubChapter"改为"合集作为 1 门 Course + N 个 SubChapter（无 Chapter）"。

## 改动范围

### 前端

| 文件 | 改动 |
|------|------|
| `BilibiliImportModal.vue` | 合集 tab 增加课程名输入框，默认填合集第一个视频标题；导入时传 `courseName` |
| `api/index.js` | `apiBilibiliImport` payload 增加 `courseName` 可选字段 |

### 后端

| 文件 | 改动 |
|------|------|
| `BilibiliImportRequest.java` | 增加 `String courseName` 字段 |
| `VideoImportPipeline.java` | 重写合集导入逻辑：1 个 Course → 批量 SubChapter（chapterId=null），AI 内容生成保持不变 |

## 数据流

```
用户粘贴合集URL
  → parsePlaylist → 视频列表
  → 用户确认课程名
  → importVideos(bvids, courseName)
  → 后端：
     1. AI分类第1个视频 → 确定Subject
     2. 创建Course(title=courseName, subjectId)
     3. 遍历视频:
        a. AI生成讲义+练习题+知识点
        b. SubChapter(courseId, chapterId=null, sortOrder=i)
```

## 数据结构

**之前：** Subject → Course → Chapter → SubChapter（每视频一个 Chapter）
**之后：** Subject → Course → SubChapter（每个视频即子章节，chapterId=null）

## 兼容性

- CourseView/CourseDetail 需兼容 `chapterId=null` 的子章节
- 子章节扁平列表中，无章的子章节归入 `chapterName: ''`
- 排序按 `sortOrder` 字段
