# 知域学生端前端设计规格

> **目标**：基于知域 v2 后端 API，重新设计并实现学生端全部 8 个页面的前端，应用星图宇宙设计语言

**架构**：Vue3 + Vite + Vue Router + Pinia。所有页面使用统一设计系统，后端 API 已就绪（SubjectCourseController、StudentAbilityController、AuthController 等）。

**设计语言**：星图宇宙风 — 深色背景 `#080d1f`，液态玻璃卡片，蓝/紫/青三色渐变

---

## 1. 设计系统

### 1.1 色彩体系

```css
/* 背景 */
--bg-primary: #080d1f;
--bg-card: rgba(255, 255, 255, 0.04);
--bg-card-hover: rgba(255, 255, 255, 0.07);
--bg-card-accent: rgba(59, 130, 246, 0.08);
--border-card: rgba(255, 255, 255, 0.06);
--border-card-hover: rgba(255, 255, 255, 0.12);

/* 主色调 */
--blue: #3b82f6;
--blue-dark: #2563eb;
--blue-glow: rgba(59, 130, 246, 0.3);
--purple: #a855f7;
--purple-dark: #7c3aed;
--cyan: #60d9fa;
--cyan-dark: #0bc5ea;

/* 渐变 */
--gradient-blue-cyan: linear-gradient(135deg, #3b82f6, #60d9fa);
--gradient-blue-purple: linear-gradient(135deg, #3b82f6, #a855f7);
--gradient-primary: linear-gradient(135deg, #3b82f6, #06b6d4);

/* 文字 */
--text-primary: #e6edf3;
--text-secondary: rgba(255, 255, 255, 0.6);
--text-tertiary: rgba(255, 255, 255, 0.35);
```

### 1.2 组件风格（混合型）

- **卡片**：`border-radius: 12px`，毛玻璃背景，半透明边框
- **按钮**：`border-radius: 8px`，主按钮用蓝青渐变 + 微光阴影
- **输入框**：`border-radius: 8px`，暗色背景，半透明边框
- **标签/标记**：`border-radius: 4px`，小型圆角
- **底部标签栏**：选中态用蓝色背景 + 文字高亮
- **玻璃效果**：`backdrop-filter: blur(12px)` + `background: rgba(255,255,255,0.04)` + `border: 1px solid rgba(255,255,255,0.06)`

### 1.3 字体系统

- **标题/数字**：Inter（`@fontsource/inter`，加载 400/600/700/800 字重）
- **正文**：系统字体栈 `-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`
- **等宽（代码）**：`'JetBrains Mono', monospace`

### 1.4 布局结构

**顶部导航栏**（所有页面共用）：
```
[Logo:知] [AI 搜索框...                      ] [🔔通知] [👤用户头像▼]
```

**底部标签栏**（学生端页面共用，5项）：
```
[🏠 首页] [🗺️ 星图] [📚 学科] [🤖 AI] [👤 我的]
```

### 1.5 间距系统

```
--space-xs: 4px
--space-sm: 8px
--space-md: 12px
--space-lg: 16px
--space-xl: 24px
--space-2xl: 32px
--space-3xl: 48px
```

### 1.6 过渡动画

- 页面切换：`fade` 300ms ease
- 卡片悬浮：`transform: translateY(-2px)` + `box-shadow` 增强 200ms ease
- 展开/折叠：`max-height` 过渡 300ms ease
- 标签切换：内容区 `opacity` 过渡 200ms
- 加载：3D Spinner（可参考现有 LoadingSpinner.vue）

---

## 2. 页面设计

### 2.1 门户首页（PortalHome）

**组件**：顶部导航栏 → Hero 区域 → 推荐课程网格

**Hero 区域**：
- 左侧标语 + CTA 按钮（"进入知域"）
- 右侧轨道动画（保留现有 orbit animation 概念，更新配色）
- 未登录显示"进入知域"按钮 → 弹出登录 Modal

**推荐课程区域**：
- 紧凑型 CourseCard 网格（4列 → 2列响应式）
- 卡片：图标/缩略图 + 课程名称 + 简短描述 + 难度标签+课时数标签
- 点击卡片 → 若已登录跳转 `/subjects/{id}`，未登录弹出登录

**AI 搜索栏**：顶部搜索框（保留 AIDialogBar 组件）

### 2.2 AI 初始问卷（AIQuestionnaire）

**流程**：对话式，AI 逐个提问 → 用户选择/输入 → 下一步

**问题覆盖**：
1. 当前学习阶段（高中/大学/在职）
2. 已掌握知识领域
3. 学习目标（考试/兴趣/技能提升）
4. 每日可用时间
5. 学习风格偏好（图文/视频/练习/混合）

**UI**：
- 顶部进度条（当前步骤/总步骤）
- 中央对话气泡（AI提问）
- 下方选项按钮/输入框
- 左侧/底部返回上一步
- 完成后显示能力预览柱状图 → "开始学习"按钮跳转仪表盘

### 2.3 学生仪表盘（StudentDashboard）

**布局**：顶部通栏 + 下方滚动内容流

**顶部通栏**（fixed 区域）：
- 左侧：用户头像 + 姓名 + 连续学习天数
- 中间/右侧：六维能力雷达图（使用 Chart.js 或 D3 绘制）

**下方内容流**（可滚动）：
1. **继续学习卡片** — 当前学习的课程/课时，继续按钮
2. **AI 推荐资源** — WebSearchAgent 返回的资源列表（标题 + 平台标识 + 简介）
3. **今日任务** — 待完成课时/待提交作业列表
4. **快捷入口** — 全部课程 / 知识星图 / AI辅导 / 成就中心

### 2.4 课程详情（CourseDetail）

**布局**：标题区（学科名称+描述+总进度条） → 单元列表

**单元列表**（手风琴式）：
- 单元标题（可展开/折叠）
- 展开后显示课时列表，每条包含：
  - 课时名称 + 类型图标（🎬视频 / 📝图文）
  - **进度圆点**（左侧）：灰色=未开始 / 蓝色=进行中 / 绿色=已完成
  - 掌握度百分比（右上）
- 点击课时 → 跳转 `/lessons/{id}`

**顶部进度条**：已完成课时数 / 总课时数 + 百分比

### 2.5 课时学习（LessonView）

**布局**：左右分栏

**左侧主区域**（70%）：
- 顶部：课时标题 + 返回按钮
- 视频播放区（如有视频，否则跳过）
- 图文讲义内容（Markdown 渲染，支持代码高亮）
- AI 标注标签内联显示（🔴重点 / 🟡难点 / ⚠️易错点）

**右侧窄栏**（30%）：
- 顶部标签切换：**练习** / **AI问答**
- 练习标签：显示当前课时练习题列表，点击展开作答
- AI问答标签：显示历史对话 + 底部输入框
- 切换时内容区域过渡动画

**底部 AI 面板**（可展开）：
- 折叠态：底部固定输入条 + 发送按钮
- 展开态：上滑为半屏对话面板，显示历史记录 + 输入区
- 所有对话通过 `POST /api/tutor/ask` 接口发送

**知识点掌握度**：完成练习后自动调 `GET /api/ability/evaluate` 更新

### 2.6 知识星图（KnowledgeStarMap）

**布局**：全屏图谱可视化

**功能**：
- 知识节点以图谱形式展示（力导向图布局）
- 节点颜色表示掌握度：灰色=未学 / 蓝色=学习中 / 绿色=已掌握 / 金色=精通
- 节点大小表示知识点权重/课时数
- 连线表示前置依赖关系
- 点击节点 → 弹出详情卡片（掌握度 + 练习记录 + 跳转课时）
- 缩放/拖拽支持

**数据来源**：`GET /api/knowledge-graph/overview`

### 2.7 AI 辅导（AICompanion）

**布局**：全屏聊天界面

**组件**：
- 顶部：标题 + 清空对话按钮
- 对话区域：聊天气泡（用户右 / AI左），AI回答支持 Markdown+代码渲染
- 底部输入区：文本输入框 + 发送按钮
- 输入框支持 Enter 发送

**功能**：
- 使用 `POST /api/tutor/ask`（RAG 增强）
- 历史记录保持在同一会话中
- 首次进入自动发送欢迎语
- 思考中显示打字机动画

### 2.8 个人中心（ProfileView + AchievementCenter）

**布局**：顶部用户信息 → 标签切换（学习概览 / 成就中心）

**学习概览标签**：
- 六维能力雷达图（同仪表盘，但更大更详细）
- 六维分项柱状图 + LLM 诊断文字
- 学习历史：学习天数、完成课时数、练习正确率等统计数据

**成就中心标签**：
- XP 等级进度条
- 徽章网格展示（已获得 / 未获得区分为彩色/灰色）
- 连续学习天数记录
- 排行榜数据（如班级内排名）

---

## 3. 路由结构

```javascript
// 门户（公开）
/                          → PortalHome
/questionnaire             → AIQuestionnaire (需认证)

// 学生端（需认证, role: student）
/student/dashboard          → StudentDashboard
/student/subjects           → SubjectCatalog
/student/subjects/:id       → CourseDetail
/student/lessons/:id        → LessonView
/student/knowledge-map      → KnowledgeStarMap
/student/companion          → AICompanion
/student/profile            → ProfileView (含成就中心)
/student/achievements       → AchievementCenter
```

---

## 4. 依赖库

**已有依赖**（直接使用）：
- `echarts` (5.5.1) — 六维雷达图、知识星图力导向图、柱状图（替代 chart.js + d3-force）
- `marked` (13.0.2) — Markdown 渲染（讲义 + AI回答）
- `vue-chartjs` — 备选 chart.js 封装（如 echarts 不满足需求）
- `dompurify` — HTML 消毒（安全渲染 AI 内容）
- `vuetify` (3.7.4) — 保留使用其网格系统、过渡动画、对话框等基础设施

**新增依赖**：
```json
{
  "dependencies": {
    "@fontsource/inter": "^5.0.0"
  }
}
```

- `@fontsource/inter` — Inter 字体（标题/数字）
- 代码高亮使用 `marked-highlight` + `highlight.js`（可选，可用 CSS 方案替代）

---

## 5. 实现计划

### 阶段一：设计系统基础设施
构建 CSS 变量、全局样式、导航框架（StudentLayout）、底部标签栏、基础组件（GlassCard、GlassButton、ProgressDot）

### 阶段二：核心页面实现（优先级排序）
1. 学生仪表盘 + 门户首页
2. 课程详情 + 课时学习
3. AI辅导 + AI初始问卷
4. 知识星图
5. 个人中心 + 成就中心

### 阶段三：打磨
过渡动画、响应式适配、加载状态、空状态、错误处理

---

## 6. 与后端 API 对照

| 页面 | 后端 API |
|------|----------|
| 门户首页 | `GET /api/subjects`, `GET /api/courses/public` |
| AI问卷 | `POST /api/profile/dialogue` (ProfileService) |
| 学生仪表盘 | `GET /api/ability/latest`, `POST /api/ability/evaluate`, `GET /api/progress/course/{id}`, `POST /api/lessons/{id}/recommend-resources` |
| 课程详情 | `GET /api/subjects/{id}`, `GET /api/subjects/{id}/courses`, `GET /api/courses/{id}/units`, `GET /api/units/{id}/lessons`, `GET /api/progress/course/{subjectId}` |
| 课时学习 | `GET /api/lessons/{id}`, `GET /api/lessons/{id}/exercises`, `POST /api/tutor/ask` (TutorController), `POST /api/progress/lesson/{id}/complete` |
| 知识星图 | `GET /api/knowledge-graph/overview` (KnowledgeGraphController) |
| AI辅导 | `POST /api/tutor/ask` |
| 个人中心 | `GET /api/auth/me`, `GET /api/ability/latest`, `GET /api/ability/history`, `GET /api/gamification/xp`, `GET /api/gamification/badges` |
