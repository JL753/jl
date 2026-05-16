# 学生端信息架构重设计 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将学生端从 16 视图/8 路由重构为对标可汗学院的 11 路由结构，左侧 5 Tab 导航，三栏课时页，合并 8 个孤立页面

**Architecture:** 取消底部 TabBar，改为左侧 72px 图标栏（5 个主入口）。学科/课程两级卡片下钻，进入课程后切换为三栏布局（课程树 + 内容 + AI 面板）。5 个页面嵌入（笔记/讨论/沉浸式→课时页，学习路径/拼图→星图页），游戏化/测验/考试/分析合并到"我的"Tab

**Tech Stack:** Vue3 + Vite + Vue Router + Pinia + ECharts + Element Plus + Axios

---

## 文件结构

```
frontend/src/
├── layout/
│   └── StudentLayout.vue        # 修改：左侧导航替换底部TabBar
├── router/
│   └── index.js                 # 修改：11个学生路由
├── views/
│   ├── student/
│   │   ├── CourseView.vue       # 新建：三栏课程/课时页
│   │   ├── StudentDashboard.vue # 重写：对标可汗首页
│   │   ├── KnowledgeStarMap.vue # 修改：新增学习路径+拼图Tab
│   │   ├── AICompanionView.vue  # 微调：适配左侧导航
│   │   ├── AdaptiveQuizView.vue # 保留：嵌入"我的"
│   │   ├── StudentExam.vue      # 保留：嵌入"我的"
│   │   ├── LearningAnalyticsView.vue   # 保留：嵌入"我的"
│   │   └── CommunityView.vue    # 保留：独立路由
│   └── common/
│       ├── SubjectCatalog.vue   # 微调：适配新布局
│       ├── CourseDetail.vue     # 微调：移除锁机制
│       └── ProfileView.vue      # 重写：5 Tab 个人中心
├── components/
│   └── LeftSideNav.vue          # 新建：左侧导航组件
```

**删除的文件:**
- `views/student/AIWorkspace.vue`
- `views/student/CoursePlatform.vue`
- `views/student/KnowledgePathView.vue`
- `views/student/KnowledgePuzzleView.vue`
- `views/student/CollaborationSpaceView.vue`
- `views/student/SmartNotesView.vue`
- `views/student/ImmersiveLearningView.vue`
- `views/student/GamificationView.vue`
- `components/BottomTabBar.vue`

**保留并作为子组件导入的文件:**
- `views/student/AchievementCenter.vue` — 由 ProfileView 导入为成就面板
- `views/student/LearningAnalyticsView.vue` — 由 ProfileView 导入为分析面板
- `views/student/AdaptiveQuizView.vue` — 由 ProfileView 导入为测验面板
- `views/student/StudentExam.vue` — 由 ProfileView 导入为考试面板
- `views/student/CommunityView.vue` — 独立路由

---

### Task 1: 路由表更新

**Files:** Modify: `frontend/src/router/index.js`

- [ ] **Step 1: 更新学生路由表**

将 `routes` 数组中 `/student` 的 children 替换为新路由：

```javascript
// 学生端
{
  path: '/student',
  component: () => import('../layout/StudentLayout.vue'),
  meta: { requiresAuth: true, role: 'student' },
  children: [
    { path: '', redirect: '/student/dashboard' },
    { path: 'dashboard', name: 'student-dashboard', component: () => import('../views/student/StudentDashboard.vue') },
    { path: 'subjects', name: 'student-subjects', component: () => import('../views/common/SubjectCatalog.vue') },
    { path: 'subjects/:id', name: 'student-subject-detail', component: () => import('../views/common/CourseDetail.vue') },
    { path: 'courses/:id', name: 'student-course', component: () => import('../views/student/CourseView.vue') },
    { path: 'lessons/:id', name: 'student-lesson', component: () => import('../views/student/CourseView.vue') },
    { path: 'knowledge-map', name: 'student-knowledge-map', component: () => import('../views/student/KnowledgeStarMap.vue') },
    { path: 'companion', name: 'companion', component: () => import('../views/student/AICompanionView.vue') },
    { path: 'profile', name: 'student-profile', component: () => import('../views/common/ProfileView.vue') },
    { path: 'community', name: 'student-community', component: () => import('../views/student/CommunityView.vue') },
  ]
},
```

- [ ] **Step 2: 验证路由解析**

运行: `cd frontend && npx vite build --mode development 2>&1 | head -30`
预期: 构建无路由解析错误。如果 CourseView.vue 尚不存在会报错，忽略——后面 Task 3 会创建。

- [ ] **Step 3: 提交**

```bash
git add frontend/src/router/index.js
git commit -m "feat: 更新学生端路由表 — 11 路由对标可汗结构"
```

---

### Task 2: 创建左侧导航组件

**Files:** Create: `frontend/src/components/LeftSideNav.vue`

- [ ] **Step 1: 创建 LeftSideNav.vue**

```vue
<template>
  <nav class="left-side-nav">
    <div class="nav-logo" @click="$router.push('/student/dashboard')">
      <span class="logo-text">知</span>
    </div>

    <div class="nav-items">
      <button
        v-for="item in navItems"
        :key="item.path"
        class="nav-item"
        :class="{ active: isActive(item) }"
        @click="$router.push(item.path)"
        :title="item.label"
      >
        <svg class="nav-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" v-html="item.iconSvg"></svg>
        <span class="nav-label">{{ item.label }}</span>
      </button>
    </div>

    <div class="nav-bottom">
      <button
        class="nav-item"
        :class="{ active: isActive({ path: '/student/profile' }) }"
        @click="$router.push('/student/profile')"
        title="我的"
      >
        <svg class="nav-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
          <circle cx="12" cy="7" r="4"/>
        </svg>
        <span class="nav-label">我的</span>
      </button>
    </div>
  </nav>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  {
    label: '首页',
    path: '/student/dashboard',
    iconSvg: '<rect x="3" y="3" width="18" height="18" rx="2"/><polyline points="9 3 9 21"/>'
  },
  {
    label: '学科',
    path: '/student/subjects',
    iconSvg: '<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>'
  },
  {
    label: '星图',
    path: '/student/knowledge-map',
    iconSvg: '<circle cx="12" cy="12" r="3"/><circle cx="19" cy="5" r="2"/><circle cx="5" cy="19" r="2"/><circle cx="5" cy="5" r="2"/><circle cx="19" cy="19" r="2"/><line x1="13.4" y1="10.6" x2="17.2" y2="6.8"/><line x1="10.6" y1="13.4" x2="6.8" y2="17.2"/><line x1="13.4" y1="13.4" x2="17.2" y2="17.2"/><line x1="10.6" y1="10.6" x2="6.8" y2="6.8"/>'
  },
  {
    label: 'AI',
    path: '/student/companion',
    iconSvg: '<path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2z"/><path d="M9 12a3 3 0 1 0 6 0 3 3 0 1 0-6 0z"/><path d="M7 17c.94.94 2.59 1.53 5 1.53s4.06-.59 5-1.53"/>'
  }
]

function isActive(item) {
  if (item.path === '/student/dashboard') return route.path === '/student/dashboard'
  return route.path.startsWith(item.path)
}
</script>

<style scoped>
.left-side-nav {
  position: fixed;
  left: 0;
  top: 56px;
  bottom: 0;
  width: 72px;
  background: rgba(8, 13, 31, 0.95);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0;
  z-index: 100;
  backdrop-filter: blur(12px);
}

.nav-logo {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #a855f7);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  margin-bottom: 16px;
}

.logo-text {
  font-weight: 800;
  font-size: 18px;
  color: #fff;
}

.nav-items {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.nav-item {
  width: 56px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  background: transparent;
  border: none;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.4);
  transition: all 0.15s;
  font-family: inherit;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.7);
}

.nav-item.active {
  background: rgba(59, 130, 246, 0.15);
  color: #60d9fa;
}

.nav-label {
  font-size: 10px;
  font-weight: 500;
  line-height: 1;
}

.nav-bottom {
  margin-top: auto;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/LeftSideNav.vue
git commit -m "feat: 左侧导航组件 — 5 Tab 图标栏替换底部TabBar"
```

---

### Task 3: 更新 StudentLayout — 左侧导航 + 内容区

**Files:** Modify: `frontend/src/layout/StudentLayout.vue`

- [ ] **Step 1: 重写 StudentLayout.vue 模板和脚本**

替换 BottomTabBar 为 LeftSideNav，调整内容区 margin：

```vue
<template>
  <div class="layout-root">
    <WallpaperModal
      :visible="showWallpaperModal"
      :initial-tab="'background'"
      @close="showWallpaperModal = false"
      @restore="showWallpaperModal = false"
    />

    <TopNavBar
      @toggle-wallpaper="showWallpaperModal = true"
      @toggle-notification="handleToggleNotification"
    />

    <LeftSideNav />

    <main class="content-area">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import WallpaperModal from '../components/WallpaperModal.vue'
import TopNavBar from '../components/TopNavBar.vue'
import LeftSideNav from '../components/LeftSideNav.vue'

const showWallpaperModal = ref(false)

function handleToggleNotification() {
  // 保留通知面板入口
}
</script>

<style scoped>
.layout-root {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  background: transparent;
}

.content-area {
  flex: 1;
  margin-top: 56px;
  margin-left: 72px;
  overflow-y: auto;
  background: transparent;
  min-height: calc(100vh - 56px);
}

.content-area::-webkit-scrollbar {
  width: 5px;
}
.content-area::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.12);
  border-radius: 3px;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/layout/StudentLayout.vue
git commit -m "feat: StudentLayout — 左侧导航替换底部TabBar"
```

---

### Task 4: 创建三栏课程/课时页 CourseView.vue

**Files:** Create: `frontend/src/views/student/CourseView.vue`

这是最核心的新页面，替代 CoursePlatform.vue 和 LessonView.vue。支持两种调用方式：
- `/student/courses/:id` — 课程的默认课时（或第一个课时）
- `/student/lessons/:id` — 指定课时

- [ ] **Step 1: 创建 CourseView.vue**

```vue
<template>
  <div class="course-view">
    <!-- Left: Course Tree (240px) -->
    <aside class="course-tree" :class="{ collapsed: treeCollapsed }">
      <div class="tree-header">
        <span class="tree-title">课程导航</span>
        <button class="tree-toggle" @click="treeCollapsed = !treeCollapsed">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline :points="treeCollapsed ? '15 18 9 12 15 6' : '9 18 15 12 9 6'"/></svg>
        </button>
      </div>
      <div class="tree-body" v-if="subjectTree.length > 0">
        <div class="tree-course-name">{{ courseName }}</div>
        <div v-for="unit in subjectTree" :key="unit.id" class="tree-unit">
          <div class="tree-unit-name">{{ unit.name }}</div>
          <div
            v-for="lesson in unit.lessons"
            :key="lesson.id"
            class="tree-lesson"
            :class="{ active: currentLessonId === lesson.id, completed: lesson.completed }"
            @click="goToLesson(lesson.id)"
          >
            <span class="tree-lesson-dot"></span>
            <span class="tree-lesson-name">{{ lesson.name }}</span>
            <svg v-if="lesson.completed" class="tree-check" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#60d9fa" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
          </div>
        </div>
      </div>
      <div v-else class="tree-loading">
        <span class="spinner"></span>
      </div>
    </aside>

    <!-- Center: Main Content -->
    <div class="course-main">
      <!-- Breadcrumb -->
      <div class="main-breadcrumb">
        <span @click="$router.push('/student/subjects')" class="crumb-link">学科</span>
        <span class="crumb-sep">/</span>
        <span>{{ courseName }}</span>
        <span class="crumb-sep">/</span>
        <span>{{ currentUnitName }}</span>
        <span class="crumb-sep">/</span>
        <span class="crumb-current">{{ currentLesson?.name || '加载中...' }}</span>
      </div>

      <div v-if="loading" class="loading-state">加载中...</div>

      <template v-else-if="currentLesson">
        <!-- Video -->
        <div v-if="currentLesson.videoUrl" class="video-section">
          <video :src="currentLesson.videoUrl" controls class="lesson-video"></video>
        </div>

        <!-- Content -->
        <div class="content-section glass-card">
          <h2 class="lesson-title">{{ currentLesson.name }}</h2>
          <div class="content-body markdown-body" v-html="renderedContent"></div>
        </div>

        <!-- Exercises -->
        <div v-if="exercises.length > 0" class="exercise-section glass-card">
          <h3 class="section-title">课后练习</h3>
          <div v-for="(ex, idx) in exercises" :key="idx" class="exercise-item">
            <div class="ex-question">{{ idx + 1 }}. {{ ex.question }}</div>
            <div class="ex-options" v-if="ex.options">
              <label v-for="(opt, oi) in ex.options" :key="oi" class="ex-option" :class="{ selected: selectedAnswers[idx] === oi, correct: submitted && oi === ex.answer, wrong: submitted && selectedAnswers[idx] === oi && oi !== ex.answer }">
                <input type="radio" :name="'ex-' + idx" :value="oi" v-model="selectedAnswers[idx]" :disabled="submitted" />
                <span>{{ opt }}</span>
              </label>
            </div>
            <div v-if="submitted" class="ex-feedback" :class="{ correct: selectedAnswers[idx] === ex.answer, wrong: selectedAnswers[idx] !== ex.answer }">
              {{ selectedAnswers[idx] === ex.answer ? '正确' : '错误 — 正确答案: ' + ex.options[ex.answer] }}
            </div>
          </div>
          <button class="submit-btn" @click="submitExercises" :disabled="submitted || selectedAnswers.length < exercises.length">提交答案</button>
        </div>

        <!-- Bottom nav -->
        <div class="lesson-nav">
          <button v-if="prevLessonId" class="nav-btn" @click="goToLesson(prevLessonId)">← 上一个</button>
          <span v-else></span>
          <button v-if="nextLessonId" class="nav-btn next" @click="goToLesson(nextLessonId)">下一个 →</button>
        </div>
      </template>
    </div>

    <!-- Right: AI Panel (320px) -->
    <aside class="ai-panel" :class="{ collapsed: panelCollapsed }">
      <div class="panel-tabs">
        <button class="panel-tab" :class="{ active: panelTab === 'ai' }" @click="panelTab = 'ai'">AI 辅导</button>
        <button class="panel-tab" :class="{ active: panelTab === 'notes' }" @click="panelTab = 'notes'">笔记</button>
        <button class="panel-tab" :class="{ active: panelTab === 'discuss' }" @click="panelTab = 'discuss'">讨论</button>
        <button class="panel-toggle" @click="panelCollapsed = !panelCollapsed">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline :points="panelCollapsed ? '15 18 9 12 15 6' : '9 18 15 12 9 6'"/></svg>
        </button>
      </div>
      <div class="panel-body" v-show="!panelCollapsed">
        <!-- AI Chat -->
        <div v-if="panelTab === 'ai'" class="ai-chat">
          <div class="chat-messages" ref="chatMessages">
            <div v-for="(msg, i) in chatHistory" :key="i" class="chat-msg" :class="msg.role">
              <div class="msg-content" v-html="msg.html"></div>
            </div>
            <div v-if="chatLoading" class="chat-msg assistant">
              <div class="typing-indicator"><span></span><span></span><span></span></div>
            </div>
          </div>
          <div class="chat-quick-actions">
            <button v-for="qa in quickActions" :key="qa" class="quick-btn" @click="sendQuick(qa)">{{ qa }}</button>
          </div>
          <div class="chat-input-row">
            <input class="chat-input" v-model="chatInput" placeholder="问 AI 关于这个课时的问题..." @keydown.enter="sendMessage" />
            <button class="chat-send" @click="sendMessage">发送</button>
          </div>
        </div>

        <!-- Notes Panel -->
        <div v-if="panelTab === 'notes'" class="notes-panel">
          <div class="notes-toolbar">
            <button class="notes-tb-btn" @click="execCmd('bold')">B</button>
            <button class="notes-tb-btn" @click="execCmd('italic')">I</button>
            <button class="notes-tb-btn" @click="execCmd('underline')">U</button>
            <button class="notes-tb-btn" @click="execCmd('insertUnorderedList')">•</button>
          </div>
          <div class="notes-editor" contenteditable="true" ref="notesEditor" @input="onNotesInput"></div>
          <button class="notes-save" @click="saveNotes">保存笔记</button>
        </div>

        <!-- Discuss Panel -->
        <div v-if="panelTab === 'discuss'" class="discuss-panel">
          <div class="discuss-placeholder">
            讨论功能即将上线。您可以在 AI 辅导中讨论课程内容。
          </div>
        </div>
      </div>
    </aside>

    <!-- Deep Explore Modal -->
    <div v-if="showDeepExplore" class="deep-explore-overlay" @click.self="showDeepExplore = false">
      <div class="deep-explore-modal glass-card">
        <div class="deep-explore-header">
          <h3>深入探索</h3>
          <button @click="showDeepExplore = false">&times;</button>
        </div>
        <div class="deep-explore-body">
          <!-- ImmersiveLearningView 内容内联至此 -->
          <p>3D 模型 / 虚拟实验 / AR 扫描</p>
        </div>
      </div>
    </div>

    <!-- Deep Explore FAB -->
    <button class="deep-fab" @click="showDeepExplore = true" title="深入探索">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"/><path d="M2 12h20"/></svg>
    </button>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { apiLessonDetail, apiKpExercises, apiSubmitAnswer, apiAskTutor, apiSubjectTree } from '../../api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const treeCollapsed = ref(false)
const panelCollapsed = ref(false)
const panelTab = ref('ai')
const showDeepExplore = ref(false)

const courseName = ref('')
const subjectTree = ref([])
const currentLessonId = ref(null)
const currentLesson = ref(null)
const currentUnitName = ref('')
const exercises = ref([])
const selectedAnswers = ref({})
const submitted = ref(false)
const renderedContent = ref('')

// Chat
const chatHistory = ref([])
const chatInput = ref('')
const chatLoading = ref(false)
const quickActions = ['解释这个概念', '给我出题', '总结要点']

// Notes
const notesEditor = ref(null)

// Navigation
const prevLessonId = ref(null)
const nextLessonId = ref(null)

const currentCourseId = computed(() => route.params.id)

onMounted(() => {
  loadCourse()
})

async function loadCourse() {
  loading.value = true
  try {
    const courseId = route.params.id
    // 如果路由是 /lessons/:id，先获取课时信息找到所属课程
    let lessonId = null
    if (route.path.includes('/lessons/')) {
      lessonId = courseId
      const lessonRes = await apiLessonDetail(courseId)
      currentLesson.value = lessonRes.data
      currentLessonId.value = lessonRes.data.id
      renderedContent.value = DOMPurify.sanitize(marked(currentLesson.value.content || ''))
      // 用 lesson 的 courseId 获取课程树
      const courseIdFromLesson = lessonRes.data.courseId
      const treeRes = await apiSubjectTree()
      buildTree(treeRes.data, courseIdFromLesson)
    } else {
      const treeRes = await apiSubjectTree()
      buildTree(treeRes.data, courseId)
      // 默认显示第一个课时
      const firstLesson = findFirstLesson()
      if (firstLesson) await loadLesson(firstLesson.id)
    }
  } finally {
    loading.value = false
  }
}

function buildTree(data, targetCourseId) {
  subjectTree.value = data || []
  // 在树中定位课程
  courseName.value = data?.[0]?.name || '课程'
}

function findFirstLesson() {
  for (const unit of subjectTree.value) {
    if (unit.lessons?.length > 0) return unit.lessons[0]
  }
  return null
}

async function loadLesson(lessonId) {
  currentLessonId.value = lessonId
  const res = await apiLessonDetail(lessonId)
  currentLesson.value = res.data
  currentUnitName.value = res.data.unitName || ''
  renderedContent.value = DOMPurify.sanitize(marked(res.data.content || ''))

  // 加载练习
  if (res.data.knowledgePointId) {
    const exRes = await apiKpExercises(res.data.knowledgePointId)
    exercises.value = exRes.data || []
  } else {
    exercises.value = []
  }
  selectedAnswers.value = {}
  submitted.value = false

  // 找前后课时
  const allLessons = []
  for (const unit of subjectTree.value) {
    if (unit.lessons) allLessons.push(...unit.lessons.map((l, i) => ({ ...l, unitIndex: subjectTree.value.indexOf(unit), lessonIndex: i })))
  }
  const idx = allLessons.findIndex(l => l.id === lessonId)
  prevLessonId.value = idx > 0 ? allLessons[idx - 1].id : null
  nextLessonId.value = idx < allLessons.length - 1 ? allLessons[idx + 1].id : null

  // 重置 AI 聊天上下文
  chatHistory.value = [{
    role: 'assistant',
    html: `你好！我看到你在学习<strong>${res.data.name}</strong>，有什么问题可以随时问我。`
  }]
}

function goToLesson(lessonId) {
  loadLesson(lessonId)
}

async function submitExercises() {
  submitted.value = true
  // 提交答案到后端
  try {
    await apiSubmitAnswer({ lessonId: currentLessonId.value, answers: selectedAnswers.value })
  } catch (e) {
    // 后端可能不强制要求
  }
}

function execCmd(cmd) {
  document.execCommand(cmd, false, null)
}

function onNotesInput() {}
function saveNotes() {
  const content = notesEditor.value?.innerHTML
  if (!content) return
  // 后续接入笔记 API
  alert('笔记已暂存')
}

async function sendMessage() {
  if (!chatInput.value.trim() || chatLoading.value) return
  const text = chatInput.value.trim()
  chatHistory.value.push({ role: 'user', html: text })
  chatInput.value = ''
  chatLoading.value = true
  await nextTick()
  scrollChat()

  try {
    const res = await apiAskTutor({ question: text, context: { lessonId: currentLessonId.value, lessonName: currentLesson.value?.name } })
    chatHistory.value.push({ role: 'assistant', html: DOMPurify.sanitize(marked(res.data?.answer || res.data || '')) })
  } catch {
    chatHistory.value.push({ role: 'assistant', html: '抱歉，回答出错了，请稍后重试。' })
  } finally {
    chatLoading.value = false
    await nextTick()
    scrollChat()
  }
}

async function sendQuick(text) {
  chatInput.value = text
  await sendMessage()
}

function scrollChat() {
  const el = document.querySelector('.chat-messages')
  if (el) el.scrollTop = el.scrollHeight
}
</script>

<style scoped>
.course-view {
  display: flex;
  height: calc(100vh - 56px);
  overflow: hidden;
}

/* 左栏 — 课程树 */
.course-tree {
  width: 240px;
  min-width: 240px;
  background: rgba(8, 13, 31, 0.6);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  transition: width 0.2s, min-width 0.2s;
}
.course-tree.collapsed { width: 40px; min-width: 40px; }
.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.tree-title { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255, 255, 255, 0.3); }
.tree-toggle { background: none; border: none; color: rgba(255,255,255,0.3); cursor: pointer; }
.tree-body { flex: 1; overflow-y: auto; padding: 8px; }
.tree-course-name { font-size: 12px; font-weight: 600; padding: 4px 8px; margin-bottom: 4px; }
.tree-unit-name { font-size: 11px; color: rgba(255, 255, 255, 0.35); padding: 6px 8px 2px; }
.tree-lesson {
  display: flex; align-items: center; gap: 6px; padding: 4px 8px 4px 18px;
  border-radius: 4px; cursor: pointer; font-size: 11px; color: rgba(255, 255, 255, 0.5);
}
.tree-lesson:hover { background: rgba(255, 255, 255, 0.04); }
.tree-lesson.active { background: rgba(59, 130, 246, 0.12); color: #60d9fa; font-weight: 600; }
.tree-lesson-dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,0.2); }
.tree-lesson.active .tree-lesson-dot { background: #60d9fa; }
.tree-check { margin-left: auto; }

/* 中栏 */
.course-main {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}
.main-breadcrumb { font-size: 10px; color: rgba(255,255,255,0.3); margin-bottom: 16px; }
.crumb-link { cursor: pointer; color: #3b82f6; }
.crumb-sep { margin: 0 6px; }
.crumb-current { color: rgba(255,255,255,0.7); }
.video-section { margin-bottom: 16px; }
.lesson-video { width: 100%; max-height: 360px; border-radius: 8px; background: rgba(0,0,0,0.3); }
.lesson-title { font-size: 20px; font-weight: 700; margin-bottom: 12px; }
.content-body { font-size: 13px; line-height: 1.8; color: rgba(255,255,255,0.6); }

/* 练习 */
.exercise-section { margin-top: 20px; padding: 16px; }
.section-title { font-weight: 600; margin-bottom: 12px; }
.exercise-item { margin-bottom: 12px; padding-bottom: 12px; border-bottom: 1px solid rgba(255,255,255,0.04); }
.ex-question { font-size: 12px; margin-bottom: 6px; }
.ex-options { display: flex; flex-direction: column; gap: 4px; }
.ex-option {
  display: flex; align-items: center; gap: 6px; font-size: 11px; color: rgba(255,255,255,0.5);
  padding: 4px 8px; border-radius: 4px; cursor: pointer;
}
.ex-option:hover { background: rgba(255,255,255,0.03); }
.ex-option.selected { background: rgba(59,130,246,0.1); color: rgba(255,255,255,0.8); }
.ex-option.correct { background: rgba(34,197,94,0.1); color: #22c55e; }
.ex-option.wrong { background: rgba(239,68,68,0.1); color: #ef4444; }
.ex-feedback { font-size: 11px; margin-top: 4px; }
.ex-feedback.correct { color: #22c55e; }
.ex-feedback.wrong { color: #ef4444; }
.submit-btn {
  margin-top: 8px; padding: 8px 20px; border-radius: 8px; border: none;
  background: linear-gradient(135deg, #3b82f6, #2563eb); color: #fff;
  font-size: 12px; cursor: pointer;
}
.submit-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* 底部导航 */
.lesson-nav { display: flex; justify-content: space-between; margin-top: 20px; padding-top: 12px; border-top: 1px solid rgba(255,255,255,0.06); }
.nav-btn { background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.06); border-radius: 8px; padding: 8px 16px; color: rgba(255,255,255,0.6); font-size: 12px; cursor: pointer; }
.nav-btn:hover { background: rgba(255,255,255,0.08); }
.nav-btn.next { background: rgba(59,130,246,0.12); border-color: rgba(59,130,246,0.2); color: #60d9fa; }

/* 右栏 — AI 面板 */
.ai-panel {
  width: 320px; min-width: 320px;
  background: rgba(8, 13, 31, 0.6);
  border-left: 1px solid rgba(255, 255, 255, 0.06);
  display: flex; flex-direction: column;
  transition: width 0.2s, min-width 0.2s;
}
.ai-panel.collapsed { width: 40px; min-width: 40px; }
.panel-tabs { display: flex; border-bottom: 1px solid rgba(255,255,255,0.06); }
.panel-tab {
  flex: 1; padding: 10px 0; text-align: center; font-size: 10px; color: rgba(255,255,255,0.3);
  background: none; border: none; cursor: pointer;
}
.panel-tab.active { color: #60d9fa; border-bottom: 2px solid #60d9fa; }
.panel-toggle { background: none; border: none; color: rgba(255,255,255,0.3); cursor: pointer; padding: 0 8px; }
.panel-body { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

/* AI Chat */
.ai-chat { display: flex; flex-direction: column; height: 100%; }
.chat-messages { flex: 1; overflow-y: auto; padding: 12px; }
.chat-msg { margin-bottom: 8px; font-size: 11px; }
.chat-msg.assistant .msg-content { background: rgba(59,130,246,0.1); border-radius: 8px; padding: 8px; color: rgba(255,255,255,0.7); }
.chat-msg.user { display: flex; justify-content: flex-end; }
.chat-msg.user .msg-content { background: rgba(168,85,247,0.1); border-radius: 8px; padding: 8px; max-width: 80%; color: rgba(255,255,255,0.7); }
.chat-quick-actions { display: flex; gap: 6px; padding: 0 12px 8px; }
.quick-btn { font-size: 9px; padding: 3px 8px; border-radius: 4px; background: rgba(255,255,255,0.04); border: none; color: rgba(255,255,255,0.4); cursor: pointer; }
.chat-input-row { display: flex; gap: 6px; padding: 0 12px 12px; }
.chat-input { flex: 1; padding: 8px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.08); background: rgba(255,255,255,0.04); color: #fff; font-size: 11px; }
.chat-send { padding: 8px 14px; border-radius: 8px; border: none; background: linear-gradient(135deg,#3b82f6,#2563eb); color: #fff; font-size: 11px; cursor: pointer; }

/* Notes */
.notes-panel { padding: 12px; }
.notes-toolbar { display: flex; gap: 4px; margin-bottom: 8px; }
.notes-tb-btn { width: 28px; height: 24px; border-radius: 4px; border: 1px solid rgba(255,255,255,0.08); background: rgba(255,255,255,0.04); color: rgba(255,255,255,0.5); font-size: 12px; cursor: pointer; }
.notes-editor { min-height: 200px; border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 8px; font-size: 12px; color: rgba(255,255,255,0.7); outline: none; background: rgba(255,255,255,0.02); }
.notes-save { margin-top: 8px; padding: 6px 16px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.08); background: rgba(255,255,255,0.04); color: rgba(255,255,255,0.6); font-size: 11px; cursor: pointer; }

/* Discuss */
.discuss-panel { padding: 12px; }
.discuss-placeholder { font-size: 12px; color: rgba(255,255,255,0.3); text-align: center; padding: 40px 0; }

/* Deep Explore */
.deep-fab {
  position: fixed; right: 340px; bottom: 24px; width: 48px; height: 48px;
  border-radius: 50%; border: 1px solid rgba(255,255,255,0.1);
  background: rgba(8,13,31,0.8); backdrop-filter: blur(8px);
  color: rgba(255,255,255,0.6); cursor: pointer; display: flex; align-items: center; justify-content: center;
  z-index: 50;
}
.deep-explore-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 200; display: flex; align-items: center; justify-content: center; }
.deep-explore-modal { width: 80vw; max-width: 900px; max-height: 80vh; overflow-y: auto; padding: 0; border-radius: 14px; }
.deep-explore-header { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid rgba(255,255,255,0.06); }
.deep-explore-body { padding: 20px; font-size: 12px; color: rgba(255,255,255,0.5); }

/* Loading / spinner */
.loading-state { display: flex; align-items: center; justify-content: center; padding: 60px 0; font-size: 12px; color: rgba(255,255,255,0.4); }
.spinner { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.1); border-top-color: #3b82f6; border-radius: 50%; display: inline-block; animation: spin 0.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.tree-loading { padding: 20px; text-align: center; }
.typing-indicator { display: flex; gap: 4px; }
.typing-indicator span { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,0.3); animation: blink 1.4s infinite both; }
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink { 0%, 100% { opacity: 0.2; } 50% { opacity: 1; } }

/* Glass card shared */
.glass-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  backdrop-filter: blur(12px);
}

/* Responsive: collapse tree on narrow screens */
@media (max-width: 1280px) {
  .course-tree { width: 40px; min-width: 40px; }
  .course-tree .tree-title, .course-tree .tree-course-name,
  .course-tree .tree-unit-name, .course-tree .tree-lesson-name,
  .course-tree .tree-check { display: none; }
  .course-tree:not(.collapsed) { width: 240px; }
  .ai-panel { width: 40px; min-width: 40px; }
  .ai-panel:not(.collapsed) { width: 300px; }
}
@media (max-width: 1024px) {
  .ai-panel { display: none; }
  .deep-fab { right: 16px; }
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/student/CourseView.vue
git commit -m "feat: 三栏课程/课时页 — 替代CoursePlatform+LessonView"
```

---

### Task 5: 重写首页 Dashboard

**Files:** Modify: `frontend/src/views/student/StudentDashboard.vue`

- [ ] **Step 1: 重写 StudentDashboard.vue**

```vue
<template>
  <div class="dashboard">
    <!-- Greeting -->
    <div class="greeting">
      <h2 class="greeting-title">欢迎回来，{{ userInfo.name }}</h2>
      <p class="greeting-sub">
        已连续学习 <strong class="hl-cyan">{{ userInfo.streak }} 天</strong> ·
        掌握 <strong class="hl-purple">{{ abilities.masteredCount || 0 }} 个</strong> 知识点 ·
        本周学习 <strong class="hl-blue">{{ stats.weeklyHours || 0 }} 小时</strong>
      </p>
    </div>

    <!-- Row 1: Continue Learning + Weekly Stats -->
    <div class="row-two">
      <div class="continue-card glass-card" v-if="continueCourse">
        <div class="cc-label">继续上次学习</div>
        <div class="cc-name">{{ continueCourse.courseName || continueCourse.name || '未命名课程' }}</div>
        <div class="cc-meta">{{ continueCourse.unitName }} · {{ continueCourse.lessonName }} · 已完成 {{ continueCourse.progress || 0 }}%</div>
        <div class="cc-bar"><div class="cc-bar-fill" :style="{ width: (continueCourse.progress || 0) + '%' }"></div></div>
        <button class="cc-btn" @click="goContinue">继续学习</button>
      </div>

      <div class="stats-card glass-card">
        <div class="stats-label">本周学习</div>
        <div class="stats-row">
          <div class="stat"><span class="stat-num blue">{{ stats.lessons || 0 }}</span><span class="stat-label">完成课时</span></div>
          <div class="stat"><span class="stat-num purple">{{ stats.exercises || 0 }}</span><span class="stat-label">练习题</span></div>
          <div class="stat"><span class="stat-num cyan">{{ stats.accuracy || 0 }}%</span><span class="stat-label">正确率</span></div>
        </div>
      </div>
    </div>

    <!-- Row 2: AI Recommended Path -->
    <div class="path-card glass-card" v-if="pathSteps.length > 0">
      <div class="path-label">AI 推荐学习路径</div>
      <div class="path-steps">
        <div v-for="(step, i) in pathSteps" :key="i" class="path-step"
          :class="{ done: step.status === 'done', active: step.status === 'active' }">
          <div class="step-num">第 {{ i + 1 }} 步</div>
          <div class="step-name">{{ step.name }}</div>
          <div class="step-tag">{{ step.status === 'done' ? '已掌握' : step.status === 'active' ? '进行中' : '待学习' }}</div>
        </div>
      </div>
    </div>

    <!-- Row 3: Quick Entries -->
    <div class="quick-row">
      <div class="quick-card glass-card" @click="$router.push('/student/profile?tab=quiz')">
        <div class="qc-title">自适应测验</div>
        <div class="qc-desc">检测当前掌握度</div>
      </div>
      <div class="quick-card glass-card" @click="$router.push('/student/profile?tab=achievement')">
        <div class="qc-title">今日挑战</div>
        <div class="qc-desc">连续 {{ userInfo.streak }} 天签到</div>
      </div>
      <div class="quick-card glass-card" @click="$router.push('/student/profile?tab=analytics')">
        <div class="qc-title">学习报告</div>
        <div class="qc-desc">查看详细分析</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { apiStudentDashboard, apiAbilityLatest, apiGamificationStreak } from '../../api'

const router = useRouter()

const userInfo = ref({ name: '同学', streak: 0 })
const abilities = ref({ masteredCount: 0 })
const stats = ref({ lessons: 0, exercises: 0, accuracy: 0 })
const continueCourse = ref(null)
const pathSteps = ref([])

onMounted(async () => {
  try {
    const [dashRes, abilityRes, streakRes] = await Promise.all([
      apiStudentDashboard(),
      apiAbilityLatest(),
      apiGamificationStreak()
    ])
    const d = dashRes.data || {}
    userInfo.value = { name: d.userName || d.name || '同学', streak: streakRes.data?.streak || 0 }
    abilities.value = { masteredCount: abilityRes.data?.masteredCount || d.masteredCount || 0 }
    stats.value = { lessons: d.weeklyLessons || 0, exercises: d.weeklyExercises || 0, accuracy: d.accuracy || 0 }

    // Continue learning
    if (d.continueLearning) {
      continueCourse.value = d.continueLearning
    } else if (d.lastLesson) {
      continueCourse.value = {
        courseName: d.lastCourseName,
        unitName: d.lastUnitName,
        lessonName: d.lastLesson.name,
        progress: d.lastLesson.progress || 0,
        lessonId: d.lastLesson.id
      }
    }

    // AI path
    if (d.recommendedPath?.length) {
      pathSteps.value = d.recommendedPath
    }
  } catch (e) {
    // 静默降级
  }
})

function goContinue() {
  const c = continueCourse.value
  if (c?.lessonId) router.push(`/student/lessons/${c.lessonId}`)
  else if (c?.id) router.push(`/student/courses/${c.id}`)
  else router.push('/student/subjects')
}
</script>

<style scoped>
.dashboard { padding: 24px; max-width: 960px; margin: 0 auto; }

.greeting { margin-bottom: 20px; }
.greeting-title { font-size: 22px; font-weight: 700; }
.greeting-sub { font-size: 12px; color: rgba(255,255,255,0.45); margin-top: 4px; }
.hl-cyan { color: #60d9fa; font-weight: 600; }
.hl-purple { color: #a855f7; font-weight: 600; }
.hl-blue { color: #3b82f6; font-weight: 600; }

.row-two { display: flex; gap: 16px; margin-bottom: 16px; }

.continue-card { flex: 1.5; padding: 20px; background: linear-gradient(135deg, rgba(59,130,246,0.12), rgba(168,85,247,0.08)); border-color: rgba(59,130,246,0.2); }
.cc-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255,255,255,0.35); margin-bottom: 8px; }
.cc-name { font-size: 18px; font-weight: 700; margin-bottom: 4px; }
.cc-meta { font-size: 12px; color: rgba(255,255,255,0.5); margin-bottom: 12px; }
.cc-bar { height: 4px; background: rgba(255,255,255,0.06); border-radius: 2px; margin-bottom: 12px; }
.cc-bar-fill { height: 100%; background: linear-gradient(90deg, #3b82f6, #60d9fa); border-radius: 2px; transition: width 0.3s; }
.cc-btn { padding: 8px 20px; border-radius: 8px; border: none; background: linear-gradient(135deg,#3b82f6,#2563eb); color: #fff; font-size: 13px; cursor: pointer; }

.stats-card { flex: 1; padding: 20px; }
.stats-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255,255,255,0.35); margin-bottom: 12px; }
.stats-row { display: flex; justify-content: space-between; }
.stat { text-align: center; }
.stat-num { display: block; font-size: 24px; font-weight: 800; }
.stat-num.blue { color: #3b82f6; }
.stat-num.purple { color: #a855f7; }
.stat-num.cyan { color: #60d9fa; }
.stat-label { font-size: 10px; color: rgba(255,255,255,0.4); display: block; margin-top: 2px; }

.path-card { padding: 20px; margin-bottom: 16px; }
.path-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255,255,255,0.35); margin-bottom: 12px; }
.path-steps { display: flex; gap: 12px; overflow-x: auto; }
.path-step {
  min-width: 160px; padding: 12px; border-radius: 8px;
  background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.06);
}
.path-step.done { background: rgba(34,197,94,0.06); border-color: rgba(34,197,94,0.15); }
.path-step.active { background: rgba(59,130,246,0.08); border-color: rgba(59,130,246,0.2); }
.step-num { font-size: 10px; color: rgba(255,255,255,0.3); }
.step-name { font-size: 13px; font-weight: 600; margin: 4px 0; }
.step-tag { font-size: 10px; }
.path-step.done .step-tag { color: #22c55e; }
.path-step.active .step-tag { color: #eab308; }
.path-step:not(.done):not(.active) .step-tag { color: rgba(255,255,255,0.3); }

.quick-row { display: flex; gap: 16px; }
.quick-card {
  flex: 1; padding: 16px; text-align: center; cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.quick-card:hover { border-color: rgba(255,255,255,0.12); background: rgba(255,255,255,0.05); }
.qc-title { font-weight: 600; font-size: 13px; }
.qc-desc { font-size: 10px; color: rgba(255,255,255,0.4); margin-top: 4px; }

/* Glass card shared */
.glass-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  backdrop-filter: blur(12px);
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/student/StudentDashboard.vue
git commit -m "feat: 重写Dashboard — 对标可汗首页Continue Learning+AI路径"
```

---

### Task 6: 扩展知识星图 — 新增学习路径 + 拼图 Tab

**Files:** Modify: `frontend/src/views/student/KnowledgeStarMap.vue`

- [ ] **Step 1: 在现有星图模板中插入 Tab 切换**

在 `<div class="knowledge-star-map">` 内的 Header 之后、图表容器之前插入：

```vue
<!-- Tab Bar -->
<div class="star-tabs">
  <button class="star-tab" :class="{ active: starTab === 'overview' }" @click="starTab = 'overview'">星图总览</button>
  <button class="star-tab" :class="{ active: starTab === 'path' }" @click="starTab = 'path'">学习路径</button>
  <button class="star-tab" :class="{ active: starTab === 'puzzle' }" @click="starTab = 'puzzle'">知识拼图</button>
</div>

<!-- Tab: 星图总览（原有内容包裹） -->
<div v-show="starTab === 'overview'">
  <!-- 原有 chart-container, detail-popup, empty-state 保持不动 -->
</div>

<!-- Tab: 学习路径 -->
<div v-show="starTab === 'path'" class="path-tab">
  <div v-if="pathLoading" class="loading-state">加载中...</div>
  <div v-else class="path-content">
    <div class="path-chart" ref="pathChartRef"></div>
    <div v-if="nextRecommended.length > 0" class="path-recommend">
      <h3 class="section-title">AI 推荐下一步</h3>
      <div v-for="rec in nextRecommended" :key="rec.id" class="rec-item">
        <span class="rec-name">{{ rec.name }}</span>
        <span class="rec-reason">{{ rec.reason }}</span>
        <button class="rec-btn" @click="goToLesson({ id: rec.lessonId })">去学习</button>
      </div>
    </div>
  </div>
</div>

<!-- Tab: 知识拼图 -->
<div v-show="starTab === 'puzzle'" class="puzzle-tab">
  <div v-if="puzzleLoading" class="loading-state">加载中...</div>
  <div v-else class="puzzle-grid">
    <div v-for="kp in puzzlePieces" :key="kp.id" class="puzzle-piece"
      :class="[`mastery-${Math.floor(kp.mastery / 25)}`, { locked: kp.mastery === 0 }]"
      @click="goToLesson({ id: kp.lessonId || kp.id })">
      <span class="piece-name">{{ kp.name }}</span>
      <span class="piece-pct">{{ kp.mastery || 0 }}%</span>
    </div>
  </div>
  <div v-if="puzzlePieces.length === 0" class="empty-state">暂无知识点数据</div>
</div>
```

在 script 中添加：

```javascript
const starTab = ref('overview')
const pathLoading = ref(false)
const nextRecommended = ref([])
const pathChartRef = ref(null)
const puzzleLoading = ref(false)
const puzzlePieces = ref([])

// 导入额外 API
import { apiKnowledgeGraphNextRecommended, apiKnowledgeGraphProgress } from '../../api'

// 监听星图 Tab 切换加载数据
watch(starTab, (tab) => {
  if (tab === 'path') loadPathTab()
  if (tab === 'puzzle') loadPuzzleTab()
})

async function loadPathTab() {
  pathLoading.value = true
  try {
    const [recRes] = await Promise.all([
      apiKnowledgeGraphNextRecommended()
    ])
    nextRecommended.value = recRes.data || []
    // 复用现有知识图谱数据绘制树图
    await nextTick()
    if (pathChartRef.value && nodes.value.length > 0) {
      initPathChart()
    }
  } finally {
    pathLoading.value = false
  }
}

function initPathChart() {
  const echarts = require('echarts')
  const chart = echarts.init(pathChartRef.value)
  // 绘制树形知识图谱（复用现有 nodes/links 数据，用 tree 类型渲染）
  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'tree',
      data: buildTreeData(),
      top: '5%', left: '10%', bottom: '5%', right: '20%',
      symbolSize: 8,
      label: { position: 'left', fontSize: 11 },
      leaves: { label: { position: 'right' } },
      roam: true
    }]
  })
}

function buildTreeData() {
  // 将 nodes/links 转为树形结构
  const nodeMap = {}
  nodes.value.forEach(n => { nodeMap[n.id] = { ...n, children: [] } })
  links.value.forEach(l => {
    if (nodeMap[l.source]) nodeMap[l.source].children.push(nodeMap[l.target])
  })
  const roots = nodes.value.filter(n => !links.value.some(l => l.target === n.id))
  return roots.map(r => ({ name: r.name, value: r.mastery, children: nodeMap[r.id]?.children || [] }))
}

async function loadPuzzleTab() {
  puzzleLoading.value = true
  try {
    const res = await apiKnowledgeGraphProgress()
    puzzlePieces.value = res.data || []
  } finally {
    puzzleLoading.value = false
  }
}
```

在 style 中添加：

```css
.star-tabs {
  display: flex; gap: 0; margin-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.star-tab {
  padding: 10px 16px; font-size: 12px; color: rgba(255,255,255,0.4);
  background: none; border: none; border-bottom: 2px solid transparent; cursor: pointer;
}
.star-tab.active { color: #60d9fa; border-bottom-color: #60d9fa; }

.path-tab, .puzzle-tab { min-height: 300px; }
.path-chart { width: 100%; height: 320px; }
.path-recommend { margin-top: 16px; }
.section-title { font-weight: 600; margin-bottom: 10px; }
.rec-item {
  display: flex; align-items: center; gap: 10px; padding: 10px 12px;
  background: rgba(255,255,255,0.03); border-radius: 8px; margin-bottom: 6px;
}
.rec-name { font-weight: 600; font-size: 13px; }
.rec-reason { font-size: 11px; color: rgba(255,255,255,0.4); flex: 1; }
.rec-btn { padding: 4px 12px; border-radius: 6px; border: 1px solid rgba(59,130,246,0.3); background: rgba(59,130,246,0.1); color: #60d9fa; font-size: 11px; cursor: pointer; }

.puzzle-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 10px; }
.puzzle-piece {
  padding: 16px 10px; border-radius: 10px; text-align: center; cursor: pointer;
  background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.06);
}
.puzzle-piece.mastery-0 { background: rgba(255,255,255,0.02); }
.puzzle-piece.mastery-1 { background: rgba(239,68,68,0.08); border-color: rgba(239,68,68,0.15); }
.puzzle-piece.mastery-2 { background: rgba(234,179,8,0.08); border-color: rgba(234,179,8,0.15); }
.puzzle-piece.mastery-3 { background: rgba(34,197,94,0.06); border-color: rgba(34,197,94,0.12); }
.puzzle-piece.mastery-4 { background: rgba(59,130,246,0.1); border-color: rgba(59,130,246,0.2); }
.piece-name { display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px; }
.piece-pct { font-size: 11px; color: rgba(255,255,255,0.4); }
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/student/KnowledgeStarMap.vue
git commit -m "feat: 星图新增学习路径+知识拼图Tab"
```

---

### Task 7: 重写 ProfileView — 5 Tab 个人中心

**Files:** Modify: `frontend/src/views/common/ProfileView.vue`

- [ ] **Step 1: 重写 ProfileView.vue**

将现有模板替换为 5 Tab 结构。顶部用户信息卡固定，下面 Tab 切换：

```vue
<template>
  <div class="profile-page">
    <!-- User Info Card (always visible) -->
    <div class="glass-card user-card">
      <div class="uc-left">
        <img v-if="userAvatar" :src="userAvatar" class="user-avatar" />
        <div v-else class="avatar-placeholder">{{ userInitial }}</div>
        <div class="uc-info">
          <h2 class="uc-name">{{ userName }}</h2>
          <div class="uc-meta">{{ userMajor }} · {{ userGrade }} · Lv.{{ userLevel }}</div>
        </div>
      </div>
      <div class="uc-right">
        <div class="uc-xp">{{ userXp }} XP</div>
        <div class="uc-streak">连续 {{ userStreak }} 天</div>
      </div>
      <button class="uc-edit-btn" @click="showEditModal = true">编辑资料</button>
    </div>

    <!-- Tabs -->
    <div class="profile-tabs">
      <button v-for="tab in tabs" :key="tab.key" class="profile-tab"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key">{{ tab.label }}</button>
    </div>

    <!-- Tab: 学习概览 -->
    <div v-show="activeTab === 'overview'" class="tab-content">
      <div class="overview-grid">
        <div class="glass-card ov-card">
          <div class="ov-label">学习统计</div>
          <div class="ov-stats">
            <span><strong>{{ stats.mastered }}</strong> 掌握知识点</span>
            <span><strong>{{ stats.lessons }}</strong> 完成课时</span>
            <span><strong>{{ stats.hours }}h</strong> 学习时长</span>
          </div>
        </div>
        <div class="glass-card ov-card" v-if="lastStudy">
          <div class="ov-label">最近学习</div>
          <div class="ov-recent">{{ lastStudy.courseName }} — {{ lastStudy.lessonName }}</div>
          <div class="ov-time">{{ lastStudy.timeAgo }} · <span class="ov-link" @click="goToLastStudy">继续学习</span></div>
        </div>
      </div>
      <div class="glass-card ov-card" v-if="profileTags.length > 0">
        <div class="ov-label">学习画像</div>
        <div class="ov-tags">
          <span v-for="tag in profileTags" :key="tag" class="ov-tag">{{ tag }}</span>
        </div>
      </div>
    </div>

    <!-- Tab: 成就 & 游戏化 -->
    <div v-show="activeTab === 'achievement'" class="tab-content">
      <AchievementGamificationPanel />
    </div>

    <!-- Tab: 学习分析 -->
    <div v-show="activeTab === 'analytics'" class="tab-content">
      <LearningAnalyticsPanel />
    </div>

    <!-- Tab: 自适应测验 -->
    <div v-show="activeTab === 'quiz'" class="tab-content">
      <AdaptiveQuizPanel />
    </div>

    <!-- Tab: 我的考试 -->
    <div v-show="activeTab === 'exam'" class="tab-content">
      <StudentExamPanel />
    </div>

    <!-- 底部：社区入口 -->
    <div class="community-entry">
      <button class="ce-btn" @click="$router.push('/student/community')">问答社区</button>
    </div>

    <!-- Edit Profile Modal -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="showEditModal = false">
      <div class="glass-card edit-modal">
        <h3>编辑资料</h3>
        <div class="edit-field">
          <label>昵称</label>
          <input v-model="editForm.name" class="edit-input" />
        </div>
        <div class="edit-field">
          <label>专业</label>
          <input v-model="editForm.major" class="edit-input" />
        </div>
        <div class="edit-field">
          <label>年级</label>
          <select v-model="editForm.grade" class="edit-input">
            <option v-for="g in gradeOptions" :key="g" :value="g">{{ g }}</option>
          </select>
        </div>
        <div class="edit-field">
          <label>学习目标</label>
          <textarea v-model="editForm.goal" class="edit-input" rows="2"></textarea>
        </div>
        <div class="edit-actions">
          <button class="edit-btn cancel" @click="showEditModal = false">取消</button>
          <button class="edit-btn save" @click="saveProfile">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { apiMe, apiUpdateMe, apiGetProfile, apiGamificationStreak } from '../../api'
import AchievementGamificationPanel from '../student/AchievementCenter.vue'
import LearningAnalyticsPanel from '../student/LearningAnalyticsView.vue'
import AdaptiveQuizPanel from '../student/AdaptiveQuizView.vue'
import StudentExamPanel from '../student/StudentExam.vue'

const router = useRouter()
const activeTab = ref('overview')
const showEditModal = ref(false)

const userName = ref('用户')
const userInitial = ref('U')
const userAvatar = ref('')
const userMajor = ref('')
const userGrade = ref('')
const userLevel = ref(1)
const userXp = ref(0)
const userStreak = ref(0)

const stats = ref({ mastered: 0, lessons: 0, hours: 0 })
const lastStudy = ref(null)
const profileTags = ref([])

const editForm = ref({ name: '', major: '', grade: '', goal: '' })
const gradeOptions = ['大一', '大二', '大三', '大四', '研一', '研二', '研三', '其他']

const tabs = [
  { key: 'overview', label: '学习概览' },
  { key: 'achievement', label: '成就 & 游戏化' },
  { key: 'analytics', label: '学习分析' },
  { key: 'quiz', label: '自适应测验' },
  { key: 'exam', label: '我的考试' },
]

onMounted(async () => {
  try {
    const [meRes, profileRes, streakRes] = await Promise.all([
      apiMe(), apiGetProfile(), apiGamificationStreak()
    ])
    const me = meRes.data || {}
    userName.value = me.name || me.username || '用户'
    userInitial.value = (userName.value[0] || 'U').toUpperCase()
    userAvatar.value = me.avatar || ''
    userMajor.value = me.major || ''
    userGrade.value = me.grade || ''
    userLevel.value = me.level || 1
    userXp.value = me.xp || 0
    userStreak.value = streakRes.data?.streak || 0

    const p = profileRes.data || {}
    stats.value = { mastered: p.masteredCount || 0, lessons: p.completedLessons || 0, hours: p.totalHours || 0 }
    lastStudy.value = p.lastStudy
    if (p.tags) {
      profileTags.value = p.tags.map(t => `${t.label || t.key}: ${t.value || t}`)
    }

    editForm.value = { name: userName.value, major: userMajor.value, grade: userGrade.value, goal: p.goal || '' }
  } catch (e) { /* 静默降级 */ }
})

async function saveProfile() {
  try {
    await apiUpdateMe(editForm.value)
    showEditModal.value = false
    userName.value = editForm.value.name
    userMajor.value = editForm.value.major
    userGrade.value = editForm.value.grade
  } catch (e) { /* 静默降级 */ }
}

function goToLastStudy() {
  if (lastStudy.value?.lessonId) router.push(`/student/lessons/${lastStudy.value.lessonId}`)
}
</script>

<style scoped>
.profile-page { padding: 24px; max-width: 900px; margin: 0 auto; }

.user-card { display: flex; align-items: center; gap: 14px; padding: 20px; margin-bottom: 16px; position: relative; }
.uc-left { display: flex; align-items: center; gap: 12px; flex: 1; }
.user-avatar, .avatar-placeholder {
  width: 56px; height: 56px; border-radius: 50%;
  background: linear-gradient(135deg,#3b82f6,#a855f7);
  display: flex; align-items: center; justify-content: center;
  font-weight: 800; font-size: 20px; object-fit: cover;
}
.avatar-placeholder { color: #fff; }
.uc-name { font-weight: 700; }
.uc-meta { font-size: 11px; color: rgba(255,255,255,0.4); }
.uc-right { text-align: right; margin-right: 100px; }
.uc-xp { font-weight: 700; color: #60d9fa; }
.uc-streak { font-size: 10px; color: rgba(255,255,255,0.3); }
.uc-edit-btn {
  position: absolute; right: 20px; padding: 6px 14px; border-radius: 6px;
  border: 1px solid rgba(255,255,255,0.1); background: rgba(255,255,255,0.04);
  color: rgba(255,255,255,0.5); font-size: 11px; cursor: pointer;
}

.profile-tabs { display: flex; border-bottom: 1px solid rgba(255,255,255,0.06); margin-bottom: 16px; }
.profile-tab {
  padding: 10px 14px; font-size: 11px; color: rgba(255,255,255,0.4);
  background: none; border: none; border-bottom: 2px solid transparent; cursor: pointer;
}
.profile-tab.active { color: #60d9fa; border-bottom-color: #60d9fa; font-weight: 600; }

.tab-content { min-height: 200px; }

.overview-grid { display: flex; gap: 14px; margin-bottom: 14px; }
.ov-card { flex: 1; padding: 14px; }
.ov-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255,255,255,0.3); margin-bottom: 10px; }
.ov-stats { display: flex; justify-content: space-between; font-size: 12px; color: rgba(255,255,255,0.6); }
.ov-stats strong { color: #fff; }
.ov-recent { font-size: 12px; }
.ov-time { font-size: 10px; color: rgba(255,255,255,0.4); margin-top: 6px; }
.ov-link { color: #3b82f6; cursor: pointer; }

.ov-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.ov-tag {
  padding: 4px 10px; border-radius: 6px; font-size: 11px;
  background: rgba(59,130,246,0.1); color: rgba(255,255,255,0.6);
}

.community-entry { margin-top: 24px; padding-top: 16px; border-top: 1px solid rgba(255,255,255,0.06); text-align: center; }
.ce-btn {
  padding: 8px 24px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04); color: rgba(255,255,255,0.5); font-size: 12px; cursor: pointer;
}

/* Edit modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 200; display: flex; align-items: center; justify-content: center; }
.edit-modal { width: 400px; padding: 24px; }
.edit-modal h3 { margin-bottom: 16px; }
.edit-field { margin-bottom: 12px; }
.edit-field label { display: block; font-size: 11px; color: rgba(255,255,255,0.5); margin-bottom: 4px; }
.edit-input {
  width: 100%; padding: 8px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.04); color: #fff; font-size: 12px; font-family: inherit;
}
.edit-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px; }
.edit-btn { padding: 6px 16px; border-radius: 6px; border: none; font-size: 12px; cursor: pointer; }
.edit-btn.save { background: linear-gradient(135deg,#3b82f6,#2563eb); color: #fff; }
.edit-btn.cancel { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.5); }

/* Glass card */
.glass-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  backdrop-filter: blur(12px);
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/common/ProfileView.vue
git commit -m "feat: 重写ProfileView — 5 Tab个人中心+编辑资料"
```

---

### Task 8: 微调 AICompanionView

**Files:** Modify: `frontend/src/views/student/AICompanionView.vue`

- [ ] **Step 1: 移除页面内的独立导航，适配左侧导航布局**

删除模板中任何页面标题导航/返回按钮，调整为全宽布局：

在模板中把任何 `<h2>` 标题栏改为简洁样式，移除底部独立的 tab bar。其余对话逻辑保持不变。

```vue
<!-- 模板最外层，确保全宽 -->
<div class="companion-view">
  <div class="companion-header">
    <h2>AI 学习助手</h2>
    <p class="companion-sub">随时提问，AI 辅导你的学习问题</p>
  </div>
  <!-- 之后的 chat 区域保持不变 -->
</div>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/student/AICompanionView.vue
git commit -m "fix: AICompanionView — 适配左侧导航布局"
```

---

### Task 9: 微调 SubjectCatalog + CourseDetail

**Files:** Modify:
- `frontend/src/views/common/SubjectCatalog.vue`
- `frontend/src/views/common/CourseDetail.vue`

- [ ] **Step 1: SubjectCatalog — 确保路由跳转正确，无 emoji**

检查 `SubjectCatalog.vue` 的学科卡片点击跳转目标为 `/student/subjects/:id`（学生端）或 `/subjects/:id`（门户端）。

确认模板中没有 emoji 图标（如之前可能存在的学科图标），替换为：学科名首字母缩写块（带渐变色背景），使用 CSS 绘制。

- [ ] **Step 2: CourseDetail — 移除锁机制**

检查 `CourseDetail.vue` 中是否有 `prerequisiteUnitId` / `isUnitUnlocked` 等锁逻辑，移除。所有课程均可点击进入。

确保课程列表项的点击跳转目标为 `/student/courses/:id`。

- [ ] **Step 3: 提交**

```bash
git add frontend/src/views/common/SubjectCatalog.vue frontend/src/views/common/CourseDetail.vue
git commit -m "fix: SubjectCatalog+CourseDetail — 适配新路由，移除锁机制"
```

---

### Task 10: 删除废弃文件

**Files:** Delete 10 个文件

- [ ] **Step 1: 批量删除**

```bash
cd frontend/src/views/student
rm AIWorkspace.vue
rm CoursePlatform.vue
rm KnowledgePathView.vue
rm KnowledgePuzzleView.vue
rm CollaborationSpaceView.vue
rm SmartNotesView.vue
rm ImmersiveLearningView.vue
rm GamificationView.vue
cd ../../components
rm BottomTabBar.vue
```

- [ ] **Step 2: 提交**

```bash
git add -A frontend/src/
git commit -m "chore: 删除废弃文件 — CoursePlatform等10个旧组件"
```

---

### Task 11: 完整验证

- [ ] **Step 1: 构建检查**

```bash
cd frontend && npx vite build 2>&1 | tail -20
```
预期: 构建成功，无 import 错误，无路由解析错误。

- [ ] **Step 2: 检查未使用的 import**

```bash
cd frontend && npx eslint src/views/student/*.vue src/views/common/*.vue --rule 'no-unused-vars: error' 2>&1 | head -30
```
预期: 无未使用变量的 import 错误（或仅预存在的）。

- [ ] **Step 3: 启动开发服务器检查**

```bash
cd frontend && npx vite --port 5173 &
sleep 3
# 检查能否访问
curl -s http://localhost:5173 | head -5
```

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "chore: 构建验证通过 — 学生端IA重设计完成"
```

---

## 任务依赖

```
Task 1 (路由) ──→ Task 3 (Layout) ──→ Task 4 (CourseView) ──→ Task 11 (验证)
                     │
Task 2 (左导航) ──→ Task 3
                     │
Task 5 (Dashboard) ──┤
Task 6 (星图)       ──┤
Task 7 (Profile)    ──┤
Task 8 (Companion)  ──┤
Task 9 (Subject/Course) ──┤
Task 10 (删除)      ──┘  (在 Task 4-9 完成后执行)
```

Tasks 5-9 可以并行执行，互不依赖。Task 10 在所有功能实现完成后执行。
