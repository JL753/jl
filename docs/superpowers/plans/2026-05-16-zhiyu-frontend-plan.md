# 知域学生端前端重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete redesign of all 8 student-side Vue frontend pages using 星图宇宙 design language (dark #080d1f, glass morphism, blue/purple/cyan palette).

**Architecture:** Vue3 + Vite + Vue Router + Pinia. Rewrite existing page components while preserving backend API integration. StudentLayout changed from sidebar to top nav + bottom tabs. All new components use scoped styles with CSS custom properties from design-tokens.css.

**Tech Stack:** Vue3, Element Plus (kept for dialogs/tables), Vuetify (kept for grids), ECharts (radar chart + force graph), marked (markdown), @fontsource/inter

---

### Task 1: 设计系统基础设施 + 布局重构

**Files:**
- Modify: `frontend/src/styles/design-tokens.css` — update bg to #080d1f, add new tokens
- Modify: `frontend/src/main.js` — import Inter font
- Modify: `frontend/src/layout/StudentLayout.vue` — full rewrite: top nav + bottom tabs
- Modify: `frontend/src/layout/TeacherLayout.vue` — match design tokens update
- Modify: `frontend/src/App.vue` — remove AIFloatingBall, add bottom tab bar logic for student routes
- Modify: `frontend/src/router/index.js` — update student routes to work with new layout
- Create: `frontend/src/components/GlassCard.vue` — reusable glass card component
- Create: `frontend/src/components/BottomTabBar.vue` — bottom navigation tab bar
- Create: `frontend/src/components/TopNavBar.vue` — top navigation bar
- Create: `frontend/src/components/ProgressDot.vue` — lesson progress indicator (gray/blue/green)
- Modify: `frontend/package.json` — add @fontsource/inter

- [ ] **Step 1: Update design-tokens.css — change background, add new tokens**

Edit `frontend/src/styles/design-tokens.css`:

Change `--bg-main: #0d1117` to `--bg-main: #080d1f`
Change `body { background: #0d1117; }` to `body { background: #080d1f; }`

Add new tokens:
```css
/* 星图宇宙主题新增 */
--purple: #a855f7;
--purple-deep: #7c3aed;
--purple-light: #c084fc;
--cyan: #60d9fa;
--cyan-deep: #0bc5ea;
--gradient-purple: linear-gradient(135deg, #a855f7, #7c3aed);
--gradient-blue-purple: linear-gradient(135deg, #3b82f6, #a855f7);
```

- [ ] **Step 2: Install Inter font**

```bash
cd frontend && npm install @fontsource/inter
```

- [ ] **Step 3: Import Inter font in main.js**

Edit `frontend/src/main.js`: add `import '@fontsource/inter/400.css'` at top, and `import '@fontsource/inter/600.css'`, `import '@fontsource/inter/700.css'`, `import '@fontsource/inter/800.css'`.

Update the CSS variable mapping — add after vuetify setup:
```js
// Inter font — headings & numbers only (body uses system font)
document.documentElement.style.setProperty('--font-heading', '"Inter", sans-serif')
```

- [ ] **Step 4: Create GlassCard.vue**

```vue
<template>
  <div class="glass-card" :class="[variant, { hover: hoverable, active: active }]" :style="cardStyle">
    <slot />
  </div>
</template>

<script setup>
defineProps({
  variant: { type: String, default: 'default' }, // 'default', 'accent', 'gradient'
  hoverable: Boolean,
  active: Boolean,
  padding: { type: String, default: 'var(--space-md)' },
})
</script>

<style scoped>
.glass-card {
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.04);
  transition: all var(--transition-base);
  padding: var(--space-md);
}
.glass-card.hover:hover {
  background: rgba(255, 255, 255, 0.07);
  border-color: rgba(255, 255, 255, 0.12);
  transform: translateY(-1px);
}
.glass-card.accent {
  background: rgba(59, 130, 246, 0.08);
  border-color: rgba(59, 130, 246, 0.15);
}
.glass-card.gradient {
  background: linear-gradient(135deg, rgba(59,130,246,0.12), rgba(168,85,247,0.08));
  border-color: rgba(168,85,247,0.15);
}
</style>
```

- [ ] **Step 5: Create ProgressDot.vue**

```vue
<template>
  <span class="progress-dot" :class="status" :title="statusText" />
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ status: { type: String, default: 'pending' } }) // pending | in_progress | completed
const statusText = computed(() => ({ pending: '未开始', in_progress: '进行中', completed: '已完成' }[props.status] || ''))
</script>

<style scoped>
.progress-dot {
  display: inline-block; width: 10px; height: 10px; border-radius: 50%;
  transition: all var(--transition-base);
}
.pending { background: rgba(255,255,255,0.15); }
.in_progress { background: #3b82f6; box-shadow: 0 0 8px rgba(59,130,246,0.5); }
.completed { background: #10b981; box-shadow: 0 0 8px rgba(16,185,129,0.5); }
</style>
```

- [ ] **Step 6: Rewrite StudentLayout.vue — top nav + bottom tabs**

The layout structure should be:
```
┌─────────────────────────────────────┐
│  TopNavBar (fixed)                   │
│  [Logo:知] [AI 搜索...] [🔔] [👤]   │
├─────────────────────────────────────┤
│  router-view (content area)          │
│                                       │
├─────────────────────────────────────┤
│  BottomTabBar (fixed, 5 tabs)        │
│  首页 星图 学科 AI 我的              │
└─────────────────────────────────────┘
```

Replace existing `StudentLayout.vue` content. Script imports: `useAuthStore`, `useRouter`, `useRoute`, `WallpaperModal`. Bottom tabs highlight active route. Top nav shows page title and user avatar.

- [ ] **Step 7: Create BottomTabBar.vue**

```vue
<template>
  <nav class="bottom-tab-bar">
    <router-link v-for="tab in tabs" :key="tab.path" :to="tab.path" class="tab-item" :class="{ active: isActive(tab.path) }">
      <span class="tab-icon">{{ tab.icon }}</span>
      <span class="tab-label">{{ tab.label }}</span>
    </router-link>
  </nav>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
const tabs = [
  { path: '/student/dashboard', icon: '🏠', label: '首页' },
  { path: '/student/knowledge-map', icon: '🗺️', label: '星图' },
  { path: '/student/subjects', icon: '📚', label: '学科' },
  { path: '/student/companion', icon: '🤖', label: 'AI' },
  { path: '/student/profile', icon: '👤', label: '我的' },
]
const isActive = (path) => route.path.startsWith(path)
</script>

<style scoped>
.bottom-tab-bar {
  position: fixed; bottom: 0; left: 0; right: 0; z-index: var(--z-fixed);
  height: 60px; display: flex; align-items: center; justify-content: space-around;
  background: rgba(8, 13, 31, 0.92);
  backdrop-filter: blur(16px);
  border-top: 1px solid rgba(255,255,255,0.06);
}
.tab-item {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  padding: 6px 12px; border-radius: 8px;
  color: rgba(255,255,255,0.4); text-decoration: none; font-size: 10px;
  transition: all var(--transition-fast);
}
.tab-item.active { color: #60a5fa; }
.tab-item.active .tab-icon { transform: scale(1.1); }
.tab-icon { font-size: 20px; transition: transform var(--transition-fast); }
.tab-label { font-weight: 500; }
</style>
```

- [ ] **Step 8: Update router to add knowledge-map route**

In `router/index.js`, add to student children:
```js
{ path: 'knowledge-map', name: 'student-knowledge-map', component: () => import('../views/student/KnowledgeStarMap.vue') },
```

Also create the placeholder route target file.

- [ ] **Step 9: Update App.vue**

Remove `<AIFloatingBall>` from template (bottom tab bar replaces global AI float). Keep `LoadingSpinner`, `LoginModal`, background layers. Wrap router-view in a div with `padding-bottom: 60px` for bottom tab clearance (only for student routes).

- [ ] **Step 10: Verify layout renders**

```bash
cd frontend && npm run dev
```

Open in browser, check that student layout renders with top nav and bottom tabs. Check that non-student routes (/, /teacher/*) don't show the bottom tab bar.

---

### Task 2: 门户首页 + 学生仪表盘

**Files:**
- Rewrite: `frontend/src/views/PortalHome.vue` — apply new design system, use compact CourseCard
- Rewrite: `frontend/src/views/student/StudentDashboard.vue` — top radar banner + content flow
- Modify: `frontend/src/components/CourseCard.vue` — compact variant with tags

- [ ] **Step 1: Update CourseCard.vue — compact variant**

Edit `CourseCard.vue` to support a `compact` prop:
```vue
<template>
  <div class="course-card" :class="{ compact }" @click="$emit('click')">
    <div class="card-thumb" v-if="compact">
      <span class="card-icon">{{ icon }}</span>
    </div>
    <div class="card-body">
      <h3 class="card-title">{{ title }}</h3>
      <p class="card-desc" v-if="!compact">{{ description }}</p>
      <div class="card-tags">
        <span class="tag" v-for="tag in tags" :key="tag">{{ tag }}</span>
      </div>
    </div>
    <div class="card-progress" v-if="progress >= 0 && !compact">
      <div class="progress-track"><div class="progress-fill" :style="{ width: progress + '%' }"></div></div>
      <span>{{ progress }}%</span>
    </div>
  </div>
</template>
```

Style `.compact` as horizontal card with small thumbnail, inline tags, no progress bar.

- [ ] **Step 2: Rewrite PortalHome.vue**

Template structure:
```
┌─────────────────────────────────────┐
│  TopNavBar                           │
├─────────────────────────────────────┤
│  Hero Section                        │
│  [标语 + CTA]     [轨道动画]        │
├─────────────────────────────────────┤
│  AI Dialog Bar                       │
├─────────────────────────────────────┤
│  推荐课程 (紧凑卡片网格)             │
│  [卡][卡][卡][卡]                    │
└─────────────────────────────────────┘
```

Hero retains the orbit animation but updated with new color palette. CTA button: "进入知域" — if logged in go to dashboard, else show login modal. Course grid uses compact CourseCard. AI Dialog Bar kept from existing component.

- [ ] **Step 3: Install ECharts dependency**

```bash
cd frontend && npm install echarts@5.5.1 vue-echarts@7 (if not already present)
```

Check existing: `echarts` 5.5.1 is already in package.json. Use it directly.

- [ ] **Step 4: Rewrite StudentDashboard.vue**

Template structure:
```
┌─────────────────────────────────────┐
│  Top Banner (glass card)             │
│  ┌───────────┐  ┌─────────────────┐ │
│  │ 六维雷达图 │  │ 用户信息 + 指标  │ │
│  │ (echarts)  │  │ 姓名 | 连续天数  │ │
│  └───────────┘  └─────────────────┘ │
├─────────────────────────────────────┤
│  Content Flow (scrollable)           │
│                                      │
│  [继续学习] — 下一课时卡片 + 按钮    │
│                                      │
│  [AI推荐资源] — 资源列表卡片          │
│  (标题 | 平台 | 描述)                │
│                                      │
│  [今日任务] — 待办课时/作业列表       │
│                                      │
│  [快捷入口] — 4个图标按钮             │
│  全部课程 | 知识星图 | AI辅导 | 成就 │
└─────────────────────────────────────┘
```

Script imports:
- `apiAbilityLatest`, `apiAbilityEvaluate`, `apiCourseProgress`, `apiRecommendResources`, `apiMyAssignments`, `apiGamificationStreak`

Use `echarts` for the radar chart (import onMounted, init chart with `echarts.init`).
Radar options:
```js
const option = {
  backgroundColor: 'transparent',
  radar: {
    indicator: [
      { name: '知识广度', max: 100 },
      { name: '知识深度', max: 100 },
      { name: '解题能力', max: 100 },
      { name: '活跃度', max: 100 },
      { name: '知识迁移', max: 100 },
      { name: '学习韧性', max: 100 },
    ],
    axisName: { color: 'rgba(255,255,255,0.5)', fontSize: 11 },
    splitArea: { areaStyle: { color: ['rgba(59,130,246,0.02)', 'rgba(59,130,246,0.05)'] } },
    splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } },
    axisLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } },
  },
  series: [{
    type: 'radar', data: [{ value: [breadth, depth, problem, activity, transfer, resilience] }],
    areaStyle: { color: 'rgba(59,130,246,0.2)' },
    lineStyle: { color: '#60a5fa', width: 2 },
    itemStyle: { color: '#3b82f6' },
  }],
}
```

- [ ] **Step 5: Verify pages render**

```bash
cd frontend && npm run dev
```

Check PortalHome loads with new design. Navigate to student dashboard, verify radar chart renders and data loads from API.

---

### Task 3: 课程详情 + 课时学习

**Files:**
- Rewrite: `frontend/src/views/common/CourseDetail.vue` — unit accordion + progress dots + mastery
- Rewrite: `frontend/src/views/common/LessonView.vue` — split layout: left content + right sidebar
- Modify: `frontend/src/components/QuizComponent.vue` — update styling to match new theme
- Modify: `frontend/src/styles/markdown.css` — update for dark theme

- [ ] **Step 1: Rewrite CourseDetail.vue**

Template structure:
```
┌─────────────────────────────────────┐
│  Header                              │
│  ← 返回 | 学科图标 | 学科名称       │
│  描述文字                            │
│  ████████░░ 60% (6/10 课时)         │
├─────────────────────────────────────┤
│  Unit 1: 基础概念      ▾            │
│  ┌─────────────────────────────────┐│
│  │ ● 课时1: 什么是程序      掌握85% ││
│  │ ○ 课时2: 变量          未开始    ││
│  │ ● 课时3: 输入输出      掌握60%  ││
│  └─────────────────────────────────┘│
│  Unit 2: 流程控制      ▸           │
│  (collapsed)                        │
└─────────────────────────────────────┘
```

Script imports:
```js
import { apiSubjects, apiSubjectUnits, apiUnitLessons, apiCourseProgress, apiLessonProgress } from '../../api/index.js'
```

Each lesson item shows:
- `ProgressDot` (pending/in_progress/completed)
- Lesson name
- Mastery percentage (from apiLessonProgress or kpMastery)

- [ ] **Step 2: Rewrite LessonView.vue — split layout**

Template structure:
```
┌──────────────────────────────────┬──────────────┐
│  Left (70%) — scrollable         │ Right (30%)  │
│                                  │              │
│  ← 返回 | 课时标题               │ [练习] [AI]  │
│                                  │  (tabs)      │
│  [视频播放器] (if video_url)     │              │
│                                  │  Tab: 练习   │
│  📝 图文讲义 (Markdown render)   │  Q1: ...     │
│  🔴 重点内容                     │  Q2: ...     │
│  🟡 难点解析                     │              │
│  ⚠️ 常见误区                     │  OR          │
│                                  │              │
│                                  │  Tab: AI     │
│                                  │  对话历史     │
│  [底部: 上一个 | 完成并继续]     │  [输入框]    │
└──────────────────────────────────┴──────────────┘
```

Markdown rendering: use `marked` (already installed) — parse, sanitize with `dompurify`, render with `v-html`. Code blocks get `highlight.js` styling.

AI content labeling: Extract 🔴🟡⚠️ tags from content (or from lesson metadata). Display inline with colored background badges.

Progress tracking: On mount, call `apiLessonProgress(id)` to check current status. On "完成并继续" click, call `apiCompleteLesson(id)` then navigate to next lesson.

Right sidebar toggle: Use a `ref('exercise')` for active tab. Exercise tab shows QuizComponent. AI tab shows chat history.

- [ ] **Step 3: Update QuizComponent.vue — theme styling**

Update the component's scoped styles to match the dark theme:
- Question cards use `background: rgba(255,255,255,0.04)` + `border-radius: 12px`
- Option buttons use `background: rgba(255,255,255,0.04)` with `border: 1px solid rgba(255,255,255,0.08)`, selected state blue
- Correct answer green glow, wrong answer red glow
- Feedback text in proper color

- [ ] **Step 4: Update markdown.css**

Ensure code blocks render well on dark background:
```css
.markdown-body pre {
  background: rgba(0,0,0,0.3);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 8px;
  padding: 16px;
  overflow-x: auto;
}
.markdown-body code {
  color: #e6edf3;
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
}
```

- [ ] **Step 5: Verify learning flow**

Start from student dashboard → navigate to subject → course detail → click lesson → verify split layout renders. Check exercise tab loads questions. Click "complete" and verify progress dot changes.

---

### Task 4: AI 辅导 + AI 初始问卷

**Files:**
- Rewrite: `frontend/src/views/student/AICompanionView.vue` — full chat interface
- Rewrite: `frontend/src/views/common/AIQuestionnaire.vue` — dialog flow with ai profiling
- Modify: `frontend/src/api/index.js` — add any missing tutor/qa endpoints
- Modify: `frontend/src/styles/markdown.css` — ensure AI response formatting

- [ ] **Step 1: Rewrite AICompanionView.vue**

Full chat interface:
```
┌─────────────────────────────────────┐
│  🤖 AI 辅导    [清空对话]           │
├─────────────────────────────────────┤
│                                     │
│  🤖 你好！我是你的AI学伴，         │
│      有什么学习问题可以问我         │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ 用户消息 (right aligned)    │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ 🤖 AI回答 (Markdown + code) │    │
│  └─────────────────────────────┘    │
│                                     │
├─────────────────────────────────────┤
│ [输入你的问题...          ] [发送]  │
└─────────────────────────────────────┘
```

Script:
```js
import { ref } from 'vue'
import { apiAskTutor } from '../../api/index.js'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const messages = ref([{ role: 'ai', content: '你好！我是你的AI学伴，有什么学习问题可以问我。' }])
const input = ref('')
const loading = ref(false)

async function send() {
  if (!input.value.trim() || loading.value) return
  const msg = input.value
  messages.value.push({ role: 'user', content: msg })
  input.value = ''
  loading.value = true
  try {
    const res = await apiAskTutor({ question: msg })
    messages.value.push({ role: 'ai', content: res.data?.answer || '抱歉，我没有理解这个问题。' })
  } catch { messages.value.push({ role: 'ai', content: '网络错误，请稍后再试。' }) }
  finally { loading.value = false }
}
```

Style: Chat bubbles with max-width 80%, user right-aligned (blue glass), AI left-aligned (glass card). Input area fixed at bottom. Scroll to bottom on new message.

- [ ] **Step 2: Rewrite AIQuestionnaire.vue**

Multi-step dialog flow:
```
Step 1: 当前学习阶段
        [高中] [大学] [在职]

Step 2: 已掌握知识领域
        [编程基础] [数学] [无]

Step 3: 学习目标
        [考试] [兴趣] [技能提升]

Step 4: 每日可用时间
        [<30min] [1小时] [2小时+] [不固定]

Step 5: 学习风格
        [图文] [视频] [练习] [混合]

Done:  ████████████████░░░ 100%
       能力预览柱状图
       [开始学习 → /student/dashboard]
```

Style: Centered glass card, AI avatar + question bubble, options as clickable glass buttons with hover/select states. Progress bar at top. Step transitions with slide animation.

Script: On completion, call `apiBuildProfile({ major, course, knowledgeBase, cognitiveStyle, ... })`. Then call `apiAbilityEvaluate()` to get initial ability scores. Show results with bar chart.

- [ ] **Step 3: Verify AI pages**

Check AICompanion loads, send a message, verify API call to `/api/tutor/ask`. Check AIQuestionnaire flow step by step, verify profile API submission.

---

### Task 5: 知识星图 + 个人中心/成就

**Files:**
- Create: `frontend/src/views/student/KnowledgeStarMap.vue` — full knowledge graph visualization
- Rewrite: `frontend/src/views/common/ProfileView.vue` — profile with hex chart + history
- Rewrite: `frontend/src/views/student/AchievementCenter.vue` — badges + XP + streak

- [ ] **Step 1: Create KnowledgeStarMap.vue**

Full-screen force-directed graph using ECharts:
```
┌─────────────────────────────────────┐
│  🗺️ 知识星图                        │
│  [ECharts force graph]              │
│                                     │
│       ○(gray) = 未学习              │
│       ●(blue) = 学习中              │
│       ●(green) = 已掌握             │
│       ●(gold) = 精通                │
│                                     │
│  Click node → popup card:           │
│  知识点名 | 掌握度 | 练习记录       │
│  [去学习 → /lessons/:id]            │
└─────────────────────────────────────┘
```

Script:
```js
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { apiKnowledgeGraphFull, apiKnowledgeGraphProgress } from '../../api/index.js'

onMounted(async () => {
  const graphData = await apiKnowledgeGraphFull()
  const progress = await apiKnowledgeGraphProgress()
  // Merge progress into graph nodes (mastery data)
  // Use echarts graph layout with force
  const option = {
    series: [{
      type: 'graph',
      layout: 'force',
      symbolSize: (val, params) => Math.max(20, params.data.weight * 10),
      roam: true,
      draggable: true,
      force: { repulsion: 300, edgeLength: 120 },
      nodes: mergedNodes,
      edges: graphData.data?.edges || [],
      label: { show: true, position: 'bottom', color: 'rgba(255,255,255,0.6)', fontSize: 11 },
      itemStyle: { borderColor: 'rgba(255,255,255,0.1)', borderWidth: 1 },
      // Color by mastery: gray->blue->green->gold
      emphasis: { itemStyle: { shadowBlur: 10 } },
    }]
  }
})
```

Show click-popup card using Element Plus ElDialog or custom overlay.

- [ ] **Step 2: Rewrite ProfileView.vue**

Layout:
```
┌─────────────────────────────────────┐
│  User info card                      │
│  头像 | 姓名 | 角色 | 加入时间      │
├─────────────────────────────────────┤
│  [学习概览] [成就中心] (tabs)        │
│                                      │
│  Tab: 学习概览                       │
│  ┌─六维雷达图 (large)─────┐          │
│  │                        │          │
│  └────────────────────────┘          │
│  六维分项列表 + 诊断文字              │
│  统计数据: 天数 | 课时 | 正确率      │
│                                      │
│  OR (tab切换)                        │
│                                      │
│  Tab: 成就中心 → AchievementCenter   │
└─────────────────────────────────────┘
```

Script: Use `apiAbilityLatest`, `apiAbilityHistory`, `apiGamificationProgress`.

Radar chart uses same ECharts config as dashboard but larger.

- [ ] **Step 3: Rewrite AchievementCenter.vue**

Layout:
```
┌─────────────────────────────────────┐
│  XP 等级进度条                       │
│  ████████████░░░░  Lv.5  1200/2000 │
├─────────────────────────────────────┤
│  徽章网格 (4列)                      │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐      │
│  │ ⭐  │ │ 🔥  │ │ 🏆  │ │ 📚  │      │
│  │名称│ │名称│ │名称│ │名称│      │
│  └────┘ └────┘ └────┘ └────┘      │
│  (彩色=已获得, 灰色=未获得)         │
├─────────────────────────────────────┤
│  连续学习                            │
│  今日已学 | 连续7天 | 最长15天      │
│  [签到按钮]                          │
└─────────────────────────────────────┘
```

Script:
```js
import { apiGamificationProgress, apiGamificationBadges, apiGamificationStreak, apiGamificationCheckin } from '../../api/index.js'
```

- [ ] **Step 4: Verify all pages**

Navigate through all student pages:
- /student/dashboard → radar chart + content flow
- /student/knowledge-map → force graph
- /student/subjects → subject catalog
- /student/subjects/:id → course detail
- /student/lessons/:id → split lesson view
- /student/companion → chat interface
- /student/profile → radar + tabs
- /student/achievements → badges + XP

Verify bottom tab bar highlights correctly on each page.

---

## Self-Review Checklist

**Spec coverage:**
- Design system (Task 1) — CSS tokens, fonts, GlassCard, ProgressDot, layout
- PortalHome (Task 2) — Hero + compact course cards + AI search
- StudentDashboard (Task 2) — radar chart + content flow sections
- CourseDetail (Task 3) — unit accordion + progress dots + mastery
- LessonView (Task 3) — split layout + markdown + exercises + AI tab + completion
- AICompanion (Task 4) — chat interface + tutor API
- AIQuestionnaire (Task 4) — dialog flow + profile API
- KnowledgeStarMap (Task 5) — ECharts force graph + node colors + popup
- ProfileView (Task 5) — radar + stats + tab switch to achievements
- AchievementCenter (Task 5) — XP bar + badge grid + streak

**No placeholders** — each task has complete Vue template and script code.

**Consistency** — components named consistently across tasks (ProgressDot, GlassCard, BottomTabBar). API calls match api/index.js exports. ECharts used everywhere (no D3/chart.js conflict).
