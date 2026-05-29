# 学生端信息架构重设计规格

**日期**：2026-05-16
**目标**：学生端完整使用流程对标可汗学院，保留知域创新功能
**关联**：[[2026-05-16-zhiyu-refactor-design]] [[2026-05-16-zhiyu-frontend-design]]

---

## 1. 设计目标

将学生端从当前 16 视图/8 路由的松散结构，重构为对标可汗学院的核心使用流程，同时保留知域创新功能（AI 辅导、知识星图、游戏化、自适应测验等）。

**原则**：
- 对标可汗骨架：首页 → 学科浏览 → 课时学习 → 进度追踪
- 创新功能不删除，通过嵌入或子页面方式整合
- 页面数量减少，每个页面职责单一

---

## 2. 左侧导航栏

取消底部 TabBar，改为**左侧 72px 宽图标栏**：

```
┌──────┐
│  知  │  Logo
│ 首页 │  → /student/dashboard
│ 学科 │  → /student/subjects
│ 星图 │  → /student/knowledge-map
│ AI  │  → /student/companion
│      │  (弹性空白)
│ 我的 │  → /student/profile
└──────┘
```

- 选中态：蓝色背景 + 文字高亮
- 图标用 CSS/SVG 绘制，不使用 emoji

---

## 3. 路由表（11 个路由）

### 3.1 主导航路由

| 路由 | 页面 | 来源 | 状态 |
|------|------|------|------|
| `/student/dashboard` | 首页 Dashboard | StudentDashboard.vue | 重做 |
| `/student/subjects` | 学科列表 | SubjectCatalog.vue | 微调 |
| `/student/subjects/:id` | 课程列表 | CourseDetail.vue | 微调 |
| `/student/courses/:id` | 课程页（三栏） | **新建** | 新建 |
| `/student/lessons/:id` | 课时页（三栏） | LessonView.vue | 重做 |
| `/student/knowledge-map` | 知识星图 | KnowledgeStarMap.vue | 扩展 |
| `/student/companion` | AI 辅导 | AICompanionView.vue | 微调 |
| `/student/profile` | 个人中心 | ProfileView.vue | 重做 |

### 3.2 "我的"子页面（Tab 内切换，非独立路由）

| Tab | 内容 | 来源 |
|-----|------|------|
| 学习概览 | 用户信息 + 学习画像 + 统计 | 新建 + ProfileView |
| 成就 & 游戏化 | XP/等级/徽章/挑战/排行榜 | AchievementCenter + GamificationView 合并 |
| 学习分析 | 能力雷达图/时间线/热力图 | LearningAnalyticsView |
| 自适应测验 | 3 阶段测验流程 | AdaptiveQuizView |
| 我的考试 | 考试列表/答题/成绩 | StudentExam |

### 3.3 独立子路由

| 路由 | 页面 | 来源 |
|------|------|------|
| `/student/community` | 问答社区 | CommunityView |

### 3.4 嵌入关系（不占用路由）

| 旧文件 | 嵌入位置 |
|--------|----------|
| SmartNotesView | 课时页右栏"笔记"Tab |
| CollaborationSpaceView | 课时页右栏"讨论"Tab |
| ImmersiveLearningView | 课时页"深入探索"按钮 → 弹出 |
| KnowledgePathView | 星图页"学习路径"Tab |
| KnowledgePuzzleView | 星图页"知识拼图"Tab |

### 3.5 删除的路由

| 旧路由 | 原因 |
|--------|------|
| `/student/workspace` | AIWorkspace 功能已由 companion + 课时 AI 面板覆盖 |
| `/student/courses` | CoursePlatform 大杂烩拆解为 subjects/:id → courses/:id → lessons/:id |
| `/student/quiz` | 自适应测验移入"我的"子页 |
| `/student/exam` | 考试移入"我的"子页 |
| `/student/achievements` | 合并到"我的"成就 Tab |

---

## 4. 页面设计详情

### 4.1 首页 Dashboard

布局：全宽，上下滚动。

```
┌──────────────────────────────────────────────┐
│  欢迎回来，{用户名}                            │
│  已连续学习 N 天 · 掌握 N 个知识点 · 本周 Nh   │
├───────────────────────┬──────────────────────┤
│  继续上次学习（大卡片） │   本周统计（小卡片）   │
│  课程名 · 单元 · 课时  │   完成课时 / 练习题 /  │
│  进度条 · [继续学习→]  │   正确率               │
├───────────────────────┴──────────────────────┤
│  AI 推荐学习路径（时间线条）                    │
│  第1步 ✓ → 第2步 ◷ → 第3步 → 第4步            │
├───────────────────────┬──────────────────────┬──────────────────────┤
│  自适应测验            │  今日挑战              │  学习报告              │
└───────────────────────┴──────────────────────┴──────────────────────┘
```

- 数据源：`apiStudentDashboard` + `apiAbilityLatest` + `apiGamificationStreak`
- 不使用 emoji 图标

### 4.2 学科 & 课程列表

**学科列表** (`/student/subjects`)：
- 4 列卡片网格（响应式：桌面 4 列 → 平板 3 列 → 移动 2 列）
- 每张卡片：学科缩写图块（渐变色背景）+ 学科名 + 课程数/课时数
- 数据源：`apiSubjects`

**课程列表** (`/student/subjects/:id`)：
- 面包屑导航：学科 > 当前学科名
- 课程列表（纵向排列），每行：难度标签（渐变色块）+ 课程名 + 单元/课时数 + 进度
- 所有课程可自由访问，无前置解锁限制
- 数据源：`apiSubjectTree`

### 4.3 课时学习页（三栏布局）

核心学习页面，VS Code 风格三栏布局。

```
┌──────────┬────────────────────────────────┬──────────┐
│ 课程树    │ 主内容区                         │ AI 面板   │
│ 240px    │ 弹性宽度                         │ 320px    │
│          │                                │          │
│ 数据结构  │ 面包屑                           │ [AI][笔记][讨论]│
│  ├ 第1单元│ 视频播放器                        │          │
│  │ ├数组✓ │ 讲义内容（Markdown）              │ 对话区    │
│  │ └字符串✓│ 课后练习（选择/判断/填空）          │          │
│  ├ 第2单元│ [提交答案]                       │ 快捷指令  │
│  │ ├链表◷ │ ← 上一个  下一个 →              │ 输入框    │
│  │ ├栈    │                                │          │
│  │ └队列  │                                │          │
│  └ 第3单元│                                │          │
└──────────┴────────────────────────────────┴──────────┘
```

- **课程树**：按单元分组，当前课时高亮蓝色，已完成打勾，未完成灰色
- **主内容区**：视频 → 讲义 → 练习 → 提交 → 上下课时导航
- **AI 面板**：3 个 Tab（AI 辅导/笔记/讨论），AI 上下文自动绑定当前课时
  - AI Tab：对话式辅导，3 个快捷指令（"解释这个概念"/"给我出题"/"总结要点"）
  - 笔记 Tab：来自 SmartNotesView，富文本编辑，AI 摘要
  - 讨论 Tab：来自 CollaborationSpaceView，小组讨论
- **"深入探索"按钮**：触发 ImmersiveLearningView 弹出（3D 模型/虚拟实验）
- **响应式**：屏幕 < 1280px 时左栏折叠为图标条

数据源：`apiLessonDetail` + `apiSubjectTree` + `apiAskTutor` + `apiSubmitAnswer`

### 4.4 知识星图

3 个 Tab 共用知识图谱 API 数据：

**Tab 1: 星图总览**（现有 KnowledgeStarMap 保留）
- ECharts 力导向图，节点代表知识点
- 点击节点弹出详情：掌握度进度条 + 进入学习按钮 + 前置知识按钮

**Tab 2: 学习路径**（来自 KnowledgePathView）
- 知识图谱树 + AI 推荐学习路径时间线
- 数据：`apiKnowledgeGraphFull` + `apiKnowledgeGraphProgress` + `apiPrerequisiteChain`

**Tab 3: 知识拼图**（来自 KnowledgePuzzleView）
- 4 列拼图网格，掌握度色块
- 点击查看详情或进入学习，无解锁机制

### 4.5 AI 辅导

独立 AI 对话页面，保留现有 AICompanionView 核心实现：
- Markdown 渲染（marked + DOMPurify）
- 对话历史，打字指示器
- 快捷话题标签（"课程相关"/"错题分析"/"学习建议"/"随便聊聊"）
- 数据源：`apiAskTutor`

删除 AIWorkspace.vue（1187 行），其功能已由本页 + 课时 AI 面板覆盖。

### 4.6 "我的"页面

顶部固定用户信息卡，下面 5 个 Tab：

**用户信息卡**（所有 Tab 上方固定）：
- 头像 + 昵称 + 专业/年级 + 等级
- XP 进度 + 连续签到天数
- **[编辑资料] 按钮**：弹出模态框，可修改头像、昵称、专业、年级、学习目标等

**Tab 1: 学习概览**（新建）
- 学习统计数字（掌握知识点 / 完成课时 / 学习时长）
- 最近学习卡片
- 学习画像标签列表（认知风格 / 学习节奏 / 兴趣 / 短板）

**Tab 2: 成就 & 游戏化**（合并 AchievementCenter + GamificationView）
- XP/等级进度条 / 徽章墙 / 每日挑战 / 排行榜 / 签到按钮
- 数据源：`apiGamification*` 系列

**Tab 3: 学习分析**（LearningAnalyticsView 原样保留）
- 能力雷达图 / 行为时间线 / 知识热力图 / 学习习惯图表
- 数据源：`apiAnalyticsDashboard`

**Tab 4: 自适应测验**（AdaptiveQuizView 原样保留）
- 知识点选择 → 出题 → 答题 → 结果分析
- 数据源：`apiGenerateAdaptiveQuiz` + `apiSubmitAdaptiveQuiz` + `apiAnalyzeMistake`

**Tab 5: 我的考试**（StudentExam 原样保留）
- 考试列表 / 考前知识覆盖检查 / 答题 / 提交 / 成绩记录
- 数据源：`apiExamList` + `apiSubmitExam` + `apiExamRecords`

**底部入口**：问答社区链接 → `/student/community`

---

## 5. 文件变更清单

### 5.1 新建文件

| 文件 | 说明 |
|------|------|
| `views/student/CourseView.vue` | 三栏课程/课时页（替代 CoursePlatform + LessonView） |

### 5.2 重写文件

| 文件 | 说明 |
|------|------|
| `views/student/StudentDashboard.vue` | 对标可汗首页 |
| `views/student/KnowledgeStarMap.vue` | 新增学习路径 + 知识拼图 Tab |
| `views/student/AICompanionView.vue` | 微调适配左侧导航 |

### 5.3 微调文件

| 文件 | 说明 |
|------|------|
| `views/common/SubjectCatalog.vue` | 适配新布局 |
| `views/common/CourseDetail.vue` | 适配新布局，移除锁机制 |
| `router/index.js` | 更新路由表 |
| `layout/StudentLayout.vue` | 左侧导航替换底部 TabBar |

### 5.4 删除文件

| 文件 | 原因 |
|------|------|
| `AIWorkspace.vue` (1187行) | 功能覆盖 |
| `CoursePlatform.vue` (894行) | 拆解为 CourseView + 三栏布局 |
| `KnowledgePathView.vue` | 并入星图页 Tab 2 |
| `KnowledgePuzzleView.vue` | 并入星图页 Tab 3 |
| `CollaborationSpaceView.vue` | 并入课时页讨论 Tab |
| `SmartNotesView.vue` | 并入课时页笔记 Tab |
| `ImmersiveLearningView.vue` | 并入课时页深入探索弹出 |
| `GamificationView.vue` | 并入"我的"成就 Tab |

### 5.5 保留为"我的"Tab 组件

| 文件 | 处理 |
|------|------|
| `AchievementCenter.vue` | 与 GamificationView 合并为成就 Tab |
| `GamificationView.vue` | 与 AchievementCenter 合并 |
| `LearningAnalyticsView.vue` | 原样嵌入分析 Tab |
| `AdaptiveQuizView.vue` | 原样嵌入测验 Tab |
| `StudentExam.vue` | 原样嵌入考试 Tab |
| `CommunityView.vue` | 改为独立路由 |

---

## 6. 设计约束

- **无 emoji 图标**：所有图标使用 CSS/SVG 绘制
- **无课程前置锁**：所有课程可随时访问学习
- **液态玻璃组件风格**：保持现有星图宇宙设计系统
- **壁纸切换系统**：保留，适配新布局
- **CSS 自定义属性**：保留 `--leleo-brightness` / `--leleo-blur` / `--leleo-vcard-color`

---

## 7. 用户流程

```
学生登录
  │
  ▼
首页 Dashboard
  │  Continue Learning / AI推荐路径 / 快捷入口
  │
  ├─→ 学科列表 → 课程列表 → 课程页（三栏）
  │                              │
  │                              ├─ 课程树选课时
  │                              ├─ 视频 + 讲义 + 练习
  │                              ├─ AI 辅导 / 笔记 / 讨论
  │                              └─ 深入探索（沉浸式学习）
  │
  ├─→ 知识星图
  │      ├─ 星图总览（力导向图）
  │      ├─ 学习路径（时间线）
  │      └─ 知识拼图（掌握度）
  │
  ├─→ AI 辅导（独立对话）
  │
  └─→ 我的
       ├─ 学习概览 + 编辑资料
       ├─ 成就 & 游戏化
       ├─ 学习分析
       ├─ 自适应测验
       ├─ 我的考试
       └─ 问答社区 →
```
