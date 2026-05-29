# B站学习视频一键导入设计规格

**日期**：2026-05-16
**目标**：教师/管理员一键从B站爬取学习视频，AI自动分类+生成讲义练习，导入学科课程
**关联**：[[2026-05-16-student-ia-redesign]]

---

## 1. 功能概述

教师或管理员通过 BilibiliImportModal（三 Tab 弹窗），从B站获取学习视频元数据，AI 全自动完成分类归入课程 + 生成课时讲义 + 练习题 + 知识点标签，一键创建完整课时。

**三种导入方式：**
- 粘贴单个/多个视频链接
- 关键词搜索B站视频后勾选
- 粘贴合集/播放列表链接批量解析

---

## 2. 架构

```
┌─ 前端 ──────────────────────────────────────┐
│  BilibiliImportModal.vue（新增）              │
│  ├─ Tab 1: 粘贴链接 → /api/bilibili/parse    │
│  ├─ Tab 2: 关键词搜索 → /api/bilibili/search  │
│  ├─ Tab 3: 合集导入 → /api/bilibili/playlist  │
│  └─ 预览 + 一键导入 → /api/bilibili/import    │
├──────────────────────────────────────────────┤
│  后端（新增）                                  │
│  BilibiliController                           │
│  ├─ POST /parse     → BilibiliService         │
│  ├─ POST /search    → BilibiliService         │
│  ├─ POST /playlist  → BilibiliService         │
│  └─ POST /import    → VideoImportPipeline     │
│        ├─ LLMClient（分类+生成内容）            │
│        ├─ LessonMapper（创建课时）              │
│        ├─ ExerciseMapper（创建练习）            │
│        └─ KnowledgePointMapper（关联知识点）     │
└──────────────────────────────────────────────┘
```

**新增 5 个文件：**

| 文件 | 位置 | 职责 |
|------|------|------|
| `BilibiliController.java` | `controller/` | 4 个 REST 端点 |
| `BilibiliService.java` | `service/` | B站视频信息抓取（API调用/页面解析） |
| `VideoImportPipeline.java` | `service/` | 串联 AI 分类 → 生成 → 创建课时 |
| `BilibiliImportRequest.java` | `dto/` | 导入请求 DTO |
| `BilibiliImportModal.vue` | `frontend/src/components/` | 前端三 Tab 导入弹窗 |

---

## 3. API 合约

### 3.1 POST /api/bilibili/parse

```json
// 请求
{ "url": "https://www.bilibili.com/video/BV1xx411c7mD" }

// 响应
{
  "bvid": "BV1xx411c7mD",
  "title": "数据结构与算法 — 链表详解",
  "description": "本视频详细讲解...",
  "duration": 2732,
  "coverUrl": "https://i0.hdslb.com/...",
  "authorName": "计算机科学讲师",
  "tags": ["数据结构", "链表", "算法"],
  "cid": 12345678,
  "partTitle": "链表详解"
}
```

### 3.2 POST /api/bilibili/search

```json
// 请求
{ "keyword": "数据结构 链表", "page": 1, "pageSize": 10 }

// 响应
{
  "total": 42,
  "items": [
    {
      "bvid": "BV1xx411c7mD",
      "title": "数据结构与算法 — 链表详解",
      "coverUrl": "https://...",
      "duration": 2732,
      "authorName": "计算机科学讲师",
      "playCount": 125000
    }
  ]
}
```

### 3.3 POST /api/bilibili/playlist

```json
// 请求
{ "url": "https://space.bilibili.com/xxx/channel/seriesdetail?sid=xxx" }

// 响应
{
  "playlistTitle": "数据结构精讲合集",
  "total": 12,
  "items": [
    { "bvid": "BV1xx...", "title": "1. 数组基础", "duration": 1800, "coverUrl": "..." }
  ]
}
```

### 3.4 POST /api/bilibili/import

```json
// 请求
{
  "bvids": ["BV1xx", "BV2yy"],
  "autoGenerate": true
}

// 响应
{
  "results": [
    {
      "bvid": "BV1xx",
      "lessonId": 101,
      "lessonName": "链表详解",
      "subjectName": "计算机科学",
      "unitName": "链表",
      "generated": true
    },
    {
      "bvid": "BV2yy",
      "lessonId": null,
      "error": "视频信息获取失败"
    }
  ]
}
```

---

## 4. B站数据获取

### 4.1 主方案：公开 API

```
GET https://api.bilibili.com/x/web-interface/view?bvid={bvid}
Headers: User-Agent: Mozilla/5.0 ... , Referer: https://www.bilibili.com
```

响应字段：`data.bvid`, `data.title`, `data.desc`, `data.duration`, `data.pic`, `data.owner.name`, `data.tid`, `data.cid`

### 4.2 回退方案：页面解析

若 API 返回 412（被限），改用 HTTP GET 视频页面 HTML，正则提取 `window.__INITIAL_STATE__` 中的 JSON。

### 4.3 搜索

使用 B站公开搜索 API：`https://api.bilibili.com/x/web-interface/search/type?search_type=video&keyword={keyword}&page={page}`

### 4.4 合集

使用 B站合集 API：`https://api.bilibili.com/x/series/archives?mid={mid}&series_id={sid}`

合集链接格式 `https://space.bilibili.com/{mid}/channel/seriesdetail?sid={sid}`，从 URL 中提取 mid 和 sid。

---

## 5. AI 工作流（VideoImportPipeline）

### 5.1 分类匹配

调用 LLMClient，传入：
- 系统 prompt：课程分类助手，返回 JSON
- 现有学科/课程/单元结构（从 SubjectTree 获取）
- 视频标题 + 标签 + 描述

期望输出：
```json
{ "subjectId": 1, "unitId": 5, "confidence": 0.85, "reason": "视频标签包含'链表'，归属数据结构单元" }
```

如果 confidence 低于 0.3 但 autoGenerate=true，仍强制分配最相似单元（方案 2：智能默认+可纠正）。如果 unitId 指向不存在单元，自动创建新单元。

### 5.2 内容生成

调用 LLMClient，传入：
- 系统 prompt：课程内容生成助手
- 视频标题、描述、时长
- 目标：生成课时讲义（Markdown）+ 练习题 3-5 道 + 知识点标签

期望输出：
```json
{
  "content": "# 链表详解\n\n## 重点\n- ...\n\n## 难点\n- ...\n\n## 正文\n...",
  "exercises": [
    { "question": "...", "options": ["A", "B", "C", "D"], "answer": 0, "explanation": "..." }
  ],
  "knowledgePoints": ["链表", "指针", "内存管理"]
}
```

### 5.3 事务流程

```
对每个 BVID:
  1. BilibiliService 获取视频元数据
  2. 如果 autoGenerate:
     a. LLM 分类 → subjectId + unitId
     b. 如果 unitId 不存在 → 自动创建 Unit
     c. LLM 生成讲义 + 练习题 + 知识点
  3. INSERT Lesson (videoUrl, content, status=draft, unitId)
  4. INSERT Exercise × N
  5. INSERT/UPDATE KnowledgePoint 关联
  6. 收集结果
返回汇总结果列表
```

---

## 6. 前端组件

### 6.1 BilibiliImportModal.vue

三 Tab 弹窗，入口在 `ContentManagement.vue` 中添加 [B站导入] 按钮。

**Tab 1: 粘贴链接**
- 多行 textarea，每行一个 URL
- 点击"解析"调用 `/api/bilibili/parse`（并发）
- 展示视频列表：封面 + 标题 + 时长 + 作者 + AI 分类预测
- 分类置信度高（>0.7）显示绿色，低显示黄色
- 解析失败显示红色

**Tab 2: 搜索视频**
- 搜索框 + 搜索结果列表
- 每条结果：复选框 + 封面 + 标题 + 时长 + 播放量
- 底部 [导入选中视频（N）] 按钮

**Tab 3: 合集导入**
- 合集链接输入框 + 解析按钮
- 解析后展示合集标题 + 视频列表（全选，可取消）
- 底部 [一键导入全部] 按钮

**导入结果弹窗：**
导入完成后展示汇总：X 个成功 / Y 个失败 / 创建了 N 个知识点 / M 道练习题

### 6.2 入口位置

`ContentManagement.vue` 顶部工具栏添加 `[B站导入]` 按钮（仅在教师/管理员角色下可见）。

---

## 7. 错误处理

| 场景 | 处理 |
|------|------|
| 单个视频 API 请求失败 | 跳过该视频，continue 处理其他 |
| 单个视频页面解析也失败 | 标记 error，不阻塞其他 |
| LLM 超时（>30s） | 重试 1 次；仍失败则仅导入元数据，无 AI 内容 |
| LLM 返回 JSON 解析失败 | 用正则提取 + 退回到仅元数据导入 |
| 合集链接 mid/sid 提取失败 | 返回错误给前端，不尝试导入 |
| 搜索无结果 | 返回空列表 |
| 分类结果 unitId 不存在 | 自动创建新 Unit，归入预测的 Subject |

---

## 8. 安全考虑

- BilibiliController 所有端点添加 `@RoleCheck("teacher")`（也兼容 admin）
- B站 API 请求使用合理频率，不加并发锁，单用户单次导入上限 50 个视频
- LLM 生成的内容标记 `status=draft`，教师必须在 ContentManagement 审阅发布
- B站公开 API 无认证，后台使用通用 User-Agent，不做身份伪装
- 视频链接仅保存 URL，不下载视频文件到服务器
