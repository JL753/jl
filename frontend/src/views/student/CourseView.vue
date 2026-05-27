<template>
  <div class="course-view">
    <!-- LEFT: Course Tree -->
    <aside class="course-tree" :class="{ collapsed: treeCollapsed }">
      <div class="tree-header">
        <span class="tree-title">目录</span>
        <button class="tree-toggle" @click="treeCollapsed = !treeCollapsed">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline :points="treeCollapsed ? '15 18 9 12 15 6' : '9 18 15 12 9 6'"/></svg>
        </button>
      </div>
      <div class="tree-body">
        <ChapterTree
          :chapters="chapters"
          :active-id="currentSubChapterId"
          :loading="treeLoading"
          @select="(sc) => loadSubChapter(sc.id)"
        />
      </div>
    </aside>

    <!-- CENTER: Main Content -->
    <div class="course-main">
      <div class="main-breadcrumb">
        <span @click="$router.push('/student/subjects')" class="crumb-link">学科</span>
        <span class="crumb-sep">/</span>
        <span>{{ courseName }}</span>
        <span class="crumb-sep">/</span>
        <span>{{ currentChapterName }}</span>
        <span class="crumb-sep">/</span>
        <span class="crumb-current">{{ currentSubChapter?.title || '加载中...' }}</span>
      </div>

      <div v-if="loading" class="loading-state">加载中...</div>

      <template v-else-if="currentSubChapter">
        <div v-if="currentSubChapter.videoUrl" class="video-section">
          <div class="video-wrapper">
            <iframe
              v-if="isBilibiliUrl(currentSubChapter.videoUrl)"
              :src="videoBiliSrc || bilibiliEmbedUrl(currentSubChapter.videoUrl)"
              class="lesson-video"
              frameborder="0"
              allowfullscreen
            ></iframe>
            <video v-else :src="currentSubChapter.videoUrl" controls ref="videoPlayer" class="lesson-video"></video>
          </div>
        </div>

        <!-- Content Tabs -->
        <div class="content-tabs">
          <button class="content-tab" :class="{ active: contentTab === 'lecture' }" @click="switchTab('lecture')">讲义</button>
          <button class="content-tab" :class="{ active: contentTab === 'subtitle' }" @click="switchTab('subtitle')">字幕</button>
          <button class="content-tab" :class="{ active: contentTab === 'exercise' }" @click="switchTab('exercise')">练习题</button>
        </div>

        <!-- 讲义 Tab -->
        <div v-show="contentTab === 'lecture'" class="content-section glass-card">
          <h2 class="lesson-title">{{ currentSubChapter.title }}</h2>
          <div class="content-body markdown-body" v-html="renderedContent"></div>
        </div>

        <!-- 字幕 Tab -->
        <div v-show="contentTab === 'subtitle'" class="content-section glass-card">
          <div v-if="subtitleLoading" class="loading-state">{{ subtitleLoadingText }}</div>
          <div v-else-if="subtitleText" class="subtitle-content" v-html="subtitleText"></div>
          <div v-else class="subtitle-empty">
            <p>{{ subtitleError || '暂无字幕数据' }}</p>
            <button class="generate-btn" @click="generateSubtitle" :disabled="subtitleLoading">AI 生成讲解稿</button>
          </div>
        </div>

        <!-- 练习题 Tab -->
        <div v-show="contentTab === 'exercise'" class="content-section glass-card">
          <div v-if="practiceLoading" class="loading-state">AI 正在生成练习题...</div>
          <template v-else-if="practiceQuestions.length > 0">
            <div class="exercise-type-stats">
              共 {{ practiceQuestions.length }} 题 ·
              {{ typeCounts.choice || 0 }} 选择 ·
              {{ typeCounts.truefalse || 0 }} 判断 ·
              {{ typeCounts.fill || 0 }} 填空 ·
              {{ typeCounts.short || 0 }} 简答
            </div>
            <div v-for="(q, qi) in practiceQuestions" :key="qi" class="exercise-item">
              <div class="ex-question">
                <span class="ex-type-tag">{{ typeTag(q.type) }}</span>
                {{ qi + 1 }}. {{ q.question }}
              </div>

              <!-- 选择题 -->
              <div class="ex-options" v-if="q.type === 'choice' && q.options">
                <label v-for="(opt, oi) in q.options" :key="oi" class="ex-option"
                  :class="{ selected: practiceAnswers[qi] === oi, correct: practiceSubmitted && oi === q.answer, wrong: practiceSubmitted && practiceAnswers[qi] === oi && oi !== q.answer }">
                  <input type="radio" :name="'ex-'+qi" :value="oi" v-model="practiceAnswers[qi]" :disabled="practiceSubmitted" />
                  <span>{{ opt }}</span>
                </label>
              </div>

              <!-- 判断题 -->
              <div class="ex-options" v-if="q.type === 'truefalse'">
                <label class="ex-option" :class="{ selected: practiceAnswers[qi] === 0, correct: practiceSubmitted && 0 === q.answer, wrong: practiceSubmitted && practiceAnswers[qi] === 0 && 0 !== q.answer }">
                  <input type="radio" :name="'ex-'+qi" :value="0" v-model="practiceAnswers[qi]" :disabled="practiceSubmitted" /> 正确
                </label>
                <label class="ex-option" :class="{ selected: practiceAnswers[qi] === 1, correct: practiceSubmitted && 1 === q.answer, wrong: practiceSubmitted && practiceAnswers[qi] === 1 && 1 !== q.answer }">
                  <input type="radio" :name="'ex-'+qi" :value="1" v-model="practiceAnswers[qi]" :disabled="practiceSubmitted" /> 错误
                </label>
              </div>

              <!-- 填空题 -->
              <div v-if="q.type === 'fill'" class="ex-fill">
                <input class="fill-input" v-model="practiceAnswers[qi]" :disabled="practiceSubmitted" placeholder="请输入答案..." />
              </div>

              <!-- 简答题 -->
              <div v-if="q.type === 'short'" class="ex-fill">
                <textarea class="fill-textarea" v-model="practiceAnswers[qi]" :disabled="practiceSubmitted" placeholder="请输入你的回答..." rows="3"></textarea>
              </div>

              <div v-if="practiceSubmitted" class="ex-feedback"
                :class="{ correct: isAnswerCorrect(q, practiceAnswers[qi]), wrong: !isAnswerCorrect(q, practiceAnswers[qi]) }">
                {{ isAnswerCorrect(q, practiceAnswers[qi]) ? '✓ 正确' : '✗ 错误' }}
                <span v-if="q.explanation" class="ex-explain"> — {{ q.explanation }}</span>
                <span v-if="!isAnswerCorrect(q, practiceAnswers[qi]) && q.answer !== undefined" class="ex-explain">
                  正确答案: {{ typeof q.answer === 'number' && q.options ? q.options[q.answer] : q.answer }}
                </span>
              </div>
            </div>
            <div class="exercise-actions">
              <button v-if="!practiceSubmitted" class="submit-btn" @click="submitPractice"
                :disabled="Object.keys(practiceAnswers).length < practiceQuestions.length">提交答案</button>
              <button v-else class="retry-btn" @click="generatePractice">重新出题</button>
            </div>
          </template>
          <div v-else class="exercise-empty">
            <p>点击下方按钮，AI 将为你生成专属练习题</p>
            <button class="generate-btn" @click="generatePractice" :disabled="exerciseLoading">AI 生成练习题</button>
          </div>
        </div>

        <div class="master-btn-row">
          <button class="master-btn" @click="markMastered" :disabled="mastering">
            {{ mastering ? '提交中...' : '我已掌握' }}
          </button>
        </div>

        <div class="lesson-nav">
          <button v-if="prevLessonId" class="nav-btn" @click="goToSubChapter(prevLessonId)">上一个</button>
          <span v-else></span>
          <button v-if="nextLessonId" class="nav-btn next" @click="goToSubChapter(nextLessonId)">下一个</button>
        </div>
      </template>
    </div>

    <!-- RIGHT: AI Panel -->
    <aside class="ai-panel" :class="{ collapsed: panelCollapsed, dragging: isDragging }" :style="panelCollapsed ? {} : { width: panelWidth + 'px', minWidth: panelWidth + 'px' }">
      <div class="panel-drag-handle" @mousedown="startDrag" @touchstart="startDrag">
        <div class="drag-handle-inner"><span></span><span></span><span></span></div>
      </div>
      <div class="panel-tabs">
        <button class="panel-tab" :class="{ active: panelTab === 'ai' }" @click="panelTab = 'ai'">AI</button>
        <button class="panel-tab" :class="{ active: panelTab === 'notes' }" @click="panelTab = 'notes'">笔记</button>
        <button class="panel-tab" :class="{ active: panelTab === 'discuss' }" @click="panelTab = 'discuss'">讨论</button>
        <button class="panel-tab" :class="{ active: panelTab === 'announce' }" @click="panelTab = 'announce'">公告</button>
        <button class="panel-tab" :class="{ active: panelTab === 'resources' }" @click="panelTab = 'resources'">资源</button>
        <button class="panel-tab" :class="{ active: panelTab === 'homework' }" @click="panelTab = 'homework'">作业</button>
        <button class="panel-tab" :class="{ active: panelTab === 'design' }" @click="panelTab = 'design'">设计</button>
        <button class="panel-tab" :class="{ active: panelTab === 'search' }" @click="panelTab = 'search'">搜索</button>
<button class="panel-toggle" @click="panelCollapsed = !panelCollapsed">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline :points="panelCollapsed ? '15 18 9 12 15 6' : '9 18 15 12 9 6'"/></svg>
        </button>
      </div>
      <div class="panel-body" v-show="!panelCollapsed">
        <!-- AI Chat tab -->
        <div v-if="panelTab === 'ai'" class="ai-chat">
          <div class="chat-messages" ref="chatMessagesRef">
            <div v-for="(msg, i) in chatHistory" :key="i" class="chat-msg" :class="msg.role">
              <div class="msg-content" v-html="msg.html"></div>
              <div v-if="msg.citations?.length" class="citations-wrap">
                <div class="citations-header" @click="msg._citationsOpen = msg._citationsOpen === false ? true : false">
                  <span class="citations-toggle">{{ msg._citationsOpen === false ? '+' : '\u2212' }}</span>
                  <span>\u53c2\u8003\u6765\u6e90 {{ msg.citations.length }}</span>
                </div>
                <div v-if="msg._citationsOpen !== false" class="citations-list">
                  <div v-for="(cite, ci) in msg.citations" :key="ci" class="citation-item" @click="handleCitationClick(cite)">
                    <span class="citation-badge">{{ ci + 1 }}</span>
                    <span class="citation-text">{{ citationLabel(cite) }}</span>
                    <span class="citation-arrow">\u2192</span>
                  </div>
                </div>
              </div>
              <button v-if="msg.role === 'assistant' && msg.html" class="msg-copy" @click="copyMsg(msg.html)" title="\u590d\u5236">\u590d\u5236</button>
            </div>
            <div v-if="chatLoading" class="chat-msg assistant">
              <div class="typing-indicator"><span></span><span></span><span></span></div>
            </div>
          </div>
          <div class="chat-quick-actions">
            <button v-for="qa in quickActions" :key="qa" class="quick-btn" @click="sendQuick(qa)">{{ qa }}</button>
          </div>
          <div class="chat-input-row">
            <textarea class="chat-textarea" v-model="chatInput" placeholder="\u95ee AI \u5173\u4e8e\u8fd9\u4e2a\u8bfe\u65f6\u7684\u95ee\u9898... / \u641c\u7d22 xxx \u89e6\u53d1\u8054\u7f51\u641c\u7d22" rows="2" @keydown.enter.exact.prevent="sendMessage" @keydown.ctrl.enter.prevent="sendMessage"></textarea>
            <button class="chat-send" @click="sendMessage">\u53d1\u9001</button>
          </div>
        </div>

        <!-- Notes tab -->
        <div v-if="panelTab === 'notes'" class="notes-panel">
          <div class="notes-toolbar">
            <button class="notes-tb-btn" @click="execCmd('bold')">B</button>
            <button class="notes-tb-btn" @click="execCmd('italic')">I</button>
            <button class="notes-tb-btn" @click="execCmd('underline')">U</button>
            <button class="notes-tb-btn" @click="execCmd('insertUnorderedList')">-</button>
          </div>
          <div class="notes-editor" contenteditable="true" ref="notesEditor"></div>
          <button class="notes-save" @click="saveNotes">\u4fdd\u5b58\u7b14\u8bb0</button>
        </div>

        <!-- Discuss tab -->
        <div v-if="panelTab === 'discuss'" class="discuss-panel">
          <CourseQA :course-id="courseId" />
        </div>

        <!-- Announcements tab -->
        <div v-if="panelTab === 'announce'" class="tab-panel">
          <h4 class="tab-panel-title">\u8bfe\u7a0b\u516c\u544a</h4>
          <AnnouncementList :items="announcements" />
        </div>

        <!-- Resources tab -->
        <div v-if="panelTab === 'resources'" class="tab-panel">
          <h4 class="tab-panel-title">\u8bfe\u7a0b\u8d44\u6e90</h4>
          <ResourceList :items="resources" />
        </div>

        <!-- Homework tab -->
        <div v-if="panelTab === 'homework'" class="tab-panel">
          <h4 class="tab-panel-title">\u8bfe\u7a0b\u4f5c\u4e1a</h4>
          <div v-if="homeworkItems.length === 0" class="tab-empty">\u6682\u65e0\u4f5c\u4e1a</div>
          <div v-for="item in homeworkItems" :key="item.id" class="hw-card glass-card">
            <div class="hw-info">
              <div class="hw-title">{{ item.title }}</div>
              <div class="hw-meta">
                <span>{{ item.description ? item.description.substring(0, 50) + (item.description.length > 50 ? '...' : '') : '' }}</span>
                <span v-if="item.dueAt">\u622a\u6b62: {{ formatTime(item.dueAt) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Course Design tab -->
        <div v-if="panelTab === 'design'" class="tab-panel">
          <h4 class="tab-panel-title">\u8bfe\u7a0b\u8bbe\u8ba1</h4>
          <CourseDesign
            :background="courseDesign?.background"
            :target="courseDesign?.target"
            :principle="courseDesign?.principle"
          />
        </div>

        <!-- Search tab -->
        <div v-if="panelTab === 'search'" class="tab-panel search-panel">
          <h4 class="tab-panel-title">\u8054\u7f51\u77e5\u8bc6\u641c\u7d22</h4>
          <div class="search-input-row">
            <input class="search-input" v-model="searchQuery" placeholder="\u8f93\u5165\u77e5\u8bc6\u70b9\u5173\u952e\u8bcd..." @keydown.enter="startSearch" />
            <button class="search-btn" @click="startSearch" :disabled="isSearching || !searchQuery.trim()">{{ isSearching ? '\u641c\u7d22\u4e2d...' : '\u5f00\u59cb\u641c\u7d22' }}</button>
          </div>
          <div v-if="isSearching" class="search-progress">
            <div v-for="(step, i) in searchSteps" :key="i" class="search-step" :class="{ active: i === searchStepIndex, done: i < searchStepIndex }">
              <span class="step-icon">{{ i < searchStepIndex ? '\u2713' : (i === searchStepIndex ? '\u27f3' : '\u25cb') }}</span>
              <span>{{ step }}</span>
            </div>
          </div>
          <div v-else-if="searchResult" class="search-result-content markdown-body" v-html="DOMPurify.sanitize(marked(searchResult))"></div>
          <div v-else-if="!isSearching" class="tab-empty">\u8f93\u5165\u5173\u952e\u8bcd\u5f00\u59cb\u641c\u7d22\uff0c\u6216\u5728AI\u5bf9\u8bdd\u4e2d\u4f7f\u7528 / \u641c\u7d22 xxx</div>
        </div>
      </div>
    </aside>

    <!-- Deep Explore FAB -->
    <button class="deep-fab" @click="showDeepExplore = true" title="深入探索">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"/><path d="M2 12h20"/></svg>
    </button>

    <!-- Deep Explore Modal -->
    <div v-if="showDeepExplore" class="deep-explore-overlay" @click.self="showDeepExplore = false">
      <div class="deep-explore-modal glass-card">
        <div class="deep-explore-header">
          <h3>深入探索</h3>
          <button @click="showDeepExplore = false" class="deep-close-btn">&times;</button>
        </div>
        <div class="deep-explore-body">
          <p>3D 模型 / 虚拟实验 / AR 扫描</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { apiAskTutor, apiAbilityEvaluate, apiExerciseSubmit, apiStudyHeartbeat, apiCourseChapters, apiSubChapterDetail, apiCompleteSubChapter, apiCourseDetail, apiBilibiliSubtitles, apiCourseAnnouncements, apiCourseResources, apiMyAssignments } from '../../api'
import { openTutorSSE } from '../../api/sse'
import ChapterTree from '../../components/course/ChapterTree.vue'
import CourseQA from '../../components/course/CourseQA.vue'
import AnnouncementList from '../../components/course/AnnouncementList.vue'
import ResourceList from '../../components/course/ResourceList.vue'
import CourseDesign from '../../components/course/CourseDesign.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const treeCollapsed = ref(false)
const panelCollapsed = ref(false)
const panelTab = ref('ai')
const showDeepExplore = ref(false)

// Content tabs
const contentTab = ref('lecture')
const mastering = ref(false)

// Subtitle
const subtitleLoading = ref(false)
const subtitleLoadingText = ref('正在获取字幕...')
const subtitleText = ref('')
const subtitleError = ref('')

// Practice (inline in 练习题 tab)
const practiceLoading = ref(false)
const practiceQuestions = ref([])
const practiceAnswers = ref({})
const practiceSubmitted = ref(false)

const courseName = ref('')
const courseId = ref(null)
const chapters = ref([])
const treeLoading = ref(false)
const currentSubChapterId = ref(null)
const currentSubChapter = ref(null)
const currentChapterName = ref('')
const renderedContent = ref('')

// Panel tabs data (announcements, resources, homework, design)
const announcements = ref([])
const resources = ref([])
const homeworkItems = ref([])
const courseDesign = ref(null)
const highlightResource = ref(-1)

// Chat
const chatHistory = ref([])
const chatInput = ref('')
const chatLoading = ref(false)
const chatMessagesRef = ref(null)
const quickActions = ['解释这个概念', '总结要点', '举例说明', '给我出题', '生成练习']

// Video refs (for citation timestamp seek)
const videoPlayer = ref(null)
const videoBiliSrc = ref('')

// Search
const searchQuery = ref('')
const searchResult = ref(null)
const isSearching = ref(false)
const searchStepIndex = ref(0)
const searchSteps = ['正在连接知识源...', '爬取 Wikipedia 百科...', '聚合公开学习资源...', '整理并格式化结果...']

// Panel resize
const panelWidth = ref(320)
const isDragging = ref(false)
const DRAG_MIN = 280
const DRAG_MAX_PERCENT = 0.5 // max 50vw

// Notes
const notesEditor = ref(null)
const notesContent = ref('')

// Navigation
const prevLessonId = ref(null)
const nextLessonId = ref(null)

let heartbeatTimer = null

onMounted(() => {
  loadCourse()
  // 每30秒发送学习时长心跳
  heartbeatTimer = setInterval(() => {
    if (currentSubChapterId.value) {
      apiStudyHeartbeat(currentSubChapterId.value, 30).catch(() => {})
    }
  }, 30000)
})

onUnmounted(() => {
  if (heartbeatTimer) clearInterval(heartbeatTimer)
})

async function loadCourse() {
  loading.value = true
  courseId.value = Number(route.params.id)
  const targetScId = route.query.sc ? Number(route.query.sc) : null

  try {
    treeLoading.value = true
    const [chRes, courseRes] = await Promise.all([
      apiCourseChapters(courseId.value),
      apiCourseDetail(courseId.value).catch(() => ({ data: null }))
    ])
    chapters.value = chRes.data || []
    courseName.value = courseRes.data?.title || '课程'

    if (targetScId) {
      await loadSubChapter(targetScId)
    } else {
      const first = findFirstSubChapter()
      if (first) await loadSubChapter(first.id)
    }
  } finally {
    loading.value = false
    treeLoading.value = false
  }

  loadCourseExtras()
}

function findFirstSubChapter() {
  for (const ch of chapters.value) {
    if (ch.subChapters?.length > 0) return ch.subChapters[0]
  }
  return null
}

async function loadSubChapter(subChapterId) {
  currentSubChapterId.value = subChapterId
  const res = await apiSubChapterDetail(subChapterId)
  currentSubChapter.value = res.data
  currentChapterName.value = findChapterName(subChapterId)

  // Render markdown with heading anchors for citation jumping
  const renderer = new marked.Renderer()
  renderer.heading = ({ text, depth }) => {
    const id = text.replace(/[^a-zA-Z0-9\u4e00-\u9fa5]+/g, '-').replace(/(^-|-$)/g, '')
    return `<h${depth} id="${id}">${text}</h${depth}>`
  }
  renderedContent.value = DOMPurify.sanitize(marked(res.data.content || '', { renderer }))

  // Reset tabs
  contentTab.value = 'lecture'
  subtitleText.value = ''
  subtitleError.value = ''
  practiceQuestions.value = []
  practiceAnswers.value = {}
  practiceSubmitted.value = false

  computeNav()
  chatHistory.value = [{
    role: 'assistant',
    html: `你好，可以看到你在学习<strong>${res.data.title}</strong>，有什么问题可以随时问我。`
  }]
}

function findChapterName(subChapterId) {
  for (const ch of chapters.value) {
    if (ch.subChapters) {
      for (const sc of ch.subChapters) {
        if (sc.id === subChapterId) return ch.title
      }
    }
  }
  return ''
}

function computeNav() {
  const allSubChapters = []
  for (const ch of chapters.value) {
    if (ch.subChapters) {
      for (const sc of ch.subChapters) allSubChapters.push(sc)
    }
  }
  const idx = allSubChapters.findIndex(sc => sc.id === currentSubChapterId.value)
  prevLessonId.value = idx > 0 ? allSubChapters[idx - 1].id : null
  nextLessonId.value = idx < allSubChapters.length - 1 ? allSubChapters[idx + 1].id : null
}

function goToSubChapter(subChapterId) {
  loadSubChapter(subChapterId)
}

async function loadCourseExtras() {
  if (!courseId.value) return
  try {
    const [annRes, resRes, hwRes] = await Promise.all([
      apiCourseAnnouncements(courseId.value).catch(() => ({ data: [] })),
      apiCourseResources(courseId.value).catch(() => ({ data: [] })),
      apiMyAssignments().catch(() => ({ data: [] })),
    ])
    announcements.value = annRes.data || []
    resources.value = resRes.data || []
    homeworkItems.value = (hwRes.data || []).filter(a => a.title)
  } catch { /* ignore panel data errors */ }

  // Load course design from already-fetched detail
  try {
    const detailRes = await apiCourseDetail(courseId.value)
    const d = detailRes.data?.data || detailRes.data || {}
    if (d.background || d.target || d.principle) {
      courseDesign.value = d
    }
  } catch { /* ignore */ }
}

function formatTime(iso) {
  if (!iso) return ''
  try {
    const d = new Date(iso)
    return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  } catch { return iso }
}

async function markMastered() {
  if (!currentSubChapterId.value || mastering.value) return
  mastering.value = true
  try {
    console.log('[CourseView] marking subChapter as mastered:', currentSubChapterId.value)
    const completeRes = await apiCompleteSubChapter(currentSubChapterId.value)
    console.log('[CourseView] completeSubChapter response:', completeRes)
    try {
      const evalRes = await apiAbilityEvaluate()
      console.log('[CourseView] ability evaluate response:', evalRes)
    } catch (e) {
      console.warn('[CourseView] ability evaluate failed:', e.message)
    }
  } catch (e) {
    console.error('[CourseView] markMastered failed:', e.message)
  } finally { mastering.value = false }
}

function switchTab(tab) {
  contentTab.value = tab
  if (tab === 'subtitle' && !subtitleText.value) {
    fetchBilibiliSubtitle()
  }
  if (tab === 'exercise' && practiceQuestions.value.length === 0) {
    generatePractice()
  }
}

async function fetchBilibiliSubtitle() {
  const videoUrl = currentSubChapter.value?.videoUrl || ''
  const bvidMatch = videoUrl.match(/BV[a-zA-Z0-9]{10}/)
  if (!bvidMatch) {
    subtitleError.value = '当前视频非B站链接，可选择AI生成讲解稿'
    return
  }
  subtitleLoading.value = true
  subtitleLoadingText.value = '正在获取B站字幕...'
  subtitleError.value = ''
  try {
    const subRes = await apiBilibiliSubtitles(bvidMatch[0])
    if (subRes.data) {
      subtitleText.value = DOMPurify.sanitize(marked(subRes.data))
      return
    }
  } catch { /* failed */ }
  finally { subtitleLoading.value = false }
  subtitleError.value = '该视频暂无字幕，可选择AI生成讲解稿'
}

async function generateSubtitle() {
  subtitleLoading.value = true
  subtitleLoadingText.value = 'AI 正在生成讲解稿...'
  subtitleError.value = ''
  try {
    const title = currentSubChapter.value?.title || ''
    const content = currentSubChapter.value?.content || ''
    const prompt = `你是一位课程助教。请根据以下讲义内容，生成一份"视频讲解稿"——模拟老师对着黑板讲课的口语化讲稿，包含开场白、知识点逐个讲解、举例说明和小结。使用自然的课堂教学语言，适当加入"同学们注意""我们来看"等过渡语。输出Markdown格式。\n\n课程标题：${title}\n讲义内容：\n${content.substring(0, 5000)}`
    const res = await apiAskTutor({ question: prompt, context: JSON.stringify({ subChapterId: currentSubChapterId.value, lessonName: currentSubChapter.value?.title }) })
    const text = res.data?.markdown || res.data?.answer || res.data || ''
    subtitleText.value = DOMPurify.sanitize(marked(text))
  } catch {
    subtitleText.value = '<p>生成失败，请重试</p>'
  } finally {
    subtitleLoading.value = false
  }
}

const typeCounts = computed(() => {
  const counts = { choice: 0, truefalse: 0, fill: 0, short: 0 }
  for (const q of practiceQuestions.value) {
    const t = q.type || 'choice'
    if (counts[t] !== undefined) counts[t]++
  }
  return counts
})

function typeTag(type) {
  const tags = { choice: '选择', truefalse: '判断', fill: '填空', short: '简答' }
  return tags[type] || '选择'
}

function isAnswerCorrect(q, userAnswer) {
  if (userAnswer === undefined || userAnswer === null) return false
  if (q.type === 'fill' || q.type === 'short') {
    return String(userAnswer).trim().toLowerCase() === String(q.answer || '').trim().toLowerCase()
  }
  return userAnswer === q.answer
}

function sendMessage() {
  if (!chatInput.value.trim() || chatLoading.value) return
  const text = chatInput.value.trim()

  // Handle /搜索 slash command
  const crawlMatch = text.match(/^\/\u641c\u7d22\s+(.+)/)
  if (crawlMatch) {
    chatInput.value = ''
    chatHistory.value.push({ role: 'user', html: text })
    panelTab.value = 'search'
    searchQuery.value = crawlMatch[1].trim()
    startSearch()
    return
  }

  chatHistory.value.push({ role: 'user', html: text })
  chatInput.value = ''
  chatLoading.value = true

  const aiMsg = { role: 'assistant', html: '', citations: [] }
  chatHistory.value.push(aiMsg)
  nextTick(() => scrollChat())

  let accumulated = ''
  openTutorSSE(
    {
      question: text,
      context: JSON.stringify({ subChapterId: currentSubChapterId.value, lessonName: currentSubChapter.value?.title }),
      answerMode: 'student-qa'
    },
    {
      onDelta(chunk) {
        accumulated += chunk
        aiMsg.html = DOMPurify.sanitize(marked(accumulated))
        nextTick(() => scrollChat())
      },
      onCitations(citations) {
        if (Array.isArray(citations)) aiMsg.citations = citations
      },
      onDone() {
        chatLoading.value = false
        nextTick(() => scrollChat())
      },
      onError() {
        chatLoading.value = false
        aiMsg.html = '抱歉，回答出错了，请稍后重试。'
      },
      onClose() {
        chatLoading.value = false
      }
    }
  )
}

function sendQuick(text) {
  chatInput.value = text
  sendMessage()
}

function scrollChat() {
  if (chatMessagesRef.value) {
    chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
  }
}

function execCmd(cmd) {
  document.execCommand(cmd, false, null)
}

function saveNotes() {
  const el = document.querySelector('.notes-editor')
  if (el) {
    notesContent.value = el.innerHTML
  }
}

function isBilibiliUrl(url) {
  return url && (url.includes('bilibili.com') || url.includes('BV'))
}

async function generatePractice() {
  practiceLoading.value = true
  practiceQuestions.value = []
  practiceAnswers.value = {}
  practiceSubmitted.value = false

  const lessonName = currentSubChapter.value?.title || ''
  const content = currentSubChapter.value?.content || ''

  const parseJson = (text) => {
    if (!text) return null
    text = text.replace(/```json\s*/gi, '').replace(/```\s*/g, '').trim()
    let parsed = null
    try { parsed = JSON.parse(text) } catch {}
    if (!parsed) {
      const m = text.match(/\[\s*\{[\s\S]*\}\s*\]/)
      if (m) try { parsed = JSON.parse(m[0]) } catch {}
    }
    if (!parsed) {
      const m = text.match(/\{[^}]*"(?:questions|exercises|data)"[\s\S]*\}/)
      if (m) try { const o = JSON.parse(m[0]); parsed = o.questions || o.exercises || o.data } catch {}
    }
    if (parsed && !Array.isArray(parsed)) {
      parsed = parsed.questions || parsed.exercises || parsed.data || []
    }
    return Array.isArray(parsed) && parsed.length > 0 ? parsed : null
  }

  try {
    const prompt = `\u6839\u636e\u4ee5\u4e0b\u8bfe\u7a0b\u5185\u5bb9\u51fa20\u9053\u7ec3\u4e60\u9898\uff0c\u5305\u542b\u9009\u62e9\u9898(choice)\u3001\u5224\u65ad\u9898(truefalse)\u3001\u586b\u7a7a\u9898(fill)\u81f3\u5c11\u4e09\u79cd\u3002\u8f93\u51fa\u7eafJSON\u6570\u7ec4\uff1a\n[{"type":"choice","question":"\u9898\u76ee","options":["A","B","C","D"],"answer":0,"explanation":"\u89e3\u6790"},{"type":"truefalse","question":"\u9898\u76ee","answer":0},...]\n\u5224\u65ad\u9898answer:0=\u6b63\u786e1=\u9519\u8bef\u3002\u586b\u7a7aanswer\u4e3a\u7b54\u6848\u6587\u672c\u3002\u76f4\u63a5\u8f93\u51fa[]\u6570\u7ec4\u3002\n\n\u8bfe\u7a0b\uff1a${lessonName}\n\u5185\u5bb9\uff1a${content.substring(0, 3000)}`
    const res = await apiAskTutor({ question: prompt, context: JSON.stringify({ subChapterId: currentSubChapterId.value, lessonName: currentSubChapter.value?.title }) })
    const text = res.data?.markdown || res.data?.answer || res.data || ''
    const parsed = parseJson(text)
    if (parsed) { practiceQuestions.value = parsed; return }
  } catch (e) {
    console.warn('[CourseView] exercise gen attempt 1 failed:', e.message)
  }

  try {
    const prompt = `\u6839\u636e\u4ee5\u4e0b\u5185\u5bb9\u51fa10\u9053\u7ec3\u4e60\u9898\uff08\u9009\u62e9+\u5224\u65ad+\u586b\u7a7a\u6df7\u5408\uff09\u3002\u8f93\u51faJSON\u6570\u7ec4\uff1a[{"type":"choice","question":"...","options":["A","B","C","D"],"answer":0,"explanation":"..."},...]\n\n\u8bfe\u7a0b\uff1a${lessonName}\n\u5185\u5bb9\uff1a${content.substring(0, 2000)}`
    const res = await apiAskTutor({ question: prompt, context: JSON.stringify({ subChapterId: currentSubChapterId.value, lessonName: currentSubChapter.value?.title }) })
    const text = res.data?.markdown || res.data?.answer || res.data || ''
    const parsed = parseJson(text)
    if (parsed) { practiceQuestions.value = parsed; return }
  } catch (e) {
    console.warn('[CourseView] exercise gen attempt 2 failed:', e.message)
  } finally {
    practiceLoading.value = false
  }

  if (practiceQuestions.value.length === 0) {
    practiceQuestions.value = [
      { type: 'choice', question: '\u672c\u8282\u8bfe\u7684\u6838\u5fc3\u5185\u5bb9\u662f\u4ec0\u4e48\uff1f', options: ['\u8bf7\u56de\u987e\u8bb2\u4e49', '\u8bf7\u56de\u987e\u8bb2\u4e49', '\u8bf7\u56de\u987e\u8bb2\u4e49', '\u8bf7\u56de\u987e\u8bb2\u4e49'], answer: 0, explanation: '\u8bf7\u4ed4\u7ec6\u9605\u8bfb\u8bfe\u7a0b\u8bb2\u4e49\u540e\u91cd\u65b0\u51fa\u9898' },
      { type: 'truefalse', question: '\u52a8\u624b\u5b9e\u8df5\u6709\u52a9\u4e8e\u52a0\u6df1\u5bf9\u672c\u8bfe\u5185\u5bb9\u7684\u7406\u89e3', answer: 0, explanation: '\u7406\u8bba\u4e0e\u5b9e\u8df5\u76f8\u7ed3\u5408' },
      { type: 'fill', question: '\u672c\u8282\u8bfe\u6700\u91cd\u8981\u7684\u4e00\u4e2a\u6982\u5ff5\u662f____', answer: '\u8bf7\u53c2\u8003\u8bb2\u4e49', explanation: '\u56de\u987e\u8bb2\u4e49\u4e2d\u7684\u5173\u952e\u672f\u8bed' }
    ]
  }
}

async function submitPractice() {
  practiceSubmitted.value = true
  for (let i = 0; i < practiceQuestions.value.length; i++) {
    const q = practiceQuestions.value[i]
    const userAnswer = practiceAnswers.value[i]
    const correct = isAnswerCorrect(q, userAnswer) ? 1 : 0
    try {
      await apiExerciseSubmit({
        subChapterId: currentSubChapterId.value,
        exerciseId: q.id || (i + 1),
        difficulty: q.difficulty || 2,
        correct
      })
    } catch {}
  }
}

function bilibiliEmbedUrl(url) {
  if (!url) return ''
  const match = url.match(/BV[a-zA-Z0-9]{10}/)
  const bvid = match ? match[0] : ''
  const pMatch = url.match(/[?&]p=(\d+)/)
  const page = pMatch ? pMatch[1] : '1'
  return `//player.bilibili.com/player.html?bvid=${bvid}&page=${page}&high_quality=1`
}

// ── Citation jumping ──
function handleCitationClick(cite) {
  switch (cite.type) {
    case 'lecture':
      contentTab.value = 'lecture'
      nextTick(() => {
        const el = document.getElementById(cite.anchor)
        if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      })
      break
    case 'video':
      contentTab.value = 'lecture'
      if (currentSubChapter.value?.videoUrl) {
        if (isBilibiliUrl(currentSubChapter.value.videoUrl)) {
          const base = bilibiliEmbedUrl(currentSubChapter.value.videoUrl)
          videoBiliSrc.value = `${base}&t=${cite.timestamp}`
        } else if (videoPlayer.value) {
          videoPlayer.value.currentTime = cite.timestamp
          videoPlayer.value.play().catch(() => {})
        }
      }
      break
    case 'sub_chapter':
      loadSubChapter(cite.subChapterId)
      panelCollapsed.value = false
      break
    case 'resource':
      panelTab.value = 'resources'
      if (cite.resourceIndex >= 0) highlightResource.value = cite.resourceIndex
      break
    case 'external':
      window.open(cite.url, '_blank')
      break
  }
}

function citationLabel(cite) {
  const prefix = { lecture: '\u8bb2\u4e49', video: '\u89c6\u9891', sub_chapter: '\u8df3\u8f6c', resource: '\u8d44\u6e90', external: '\u94fe\u63a5' }
  return (prefix[cite.type] || '') + '\uff1a' + cite.title
}

function copyMsg(html) {
  const text = html.replace(/<[^>]*>/g, '')
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('\u5df2\u590d\u5236\u5230\u526a\u8d34\u677f')
  }).catch(() => {})
}

// ── Panel drag resize ──
function startDrag(e) {
  isDragging.value = true
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  document.addEventListener('touchmove', onDrag, { passive: true })
  document.addEventListener('touchend', stopDrag)
  e.preventDefault()
}

function onDrag(e) {
  if (!isDragging.value) return
  const clientX = e.touches ? e.touches[0].clientX : e.clientX
  const container = document.querySelector('.course-view')
  if (!container) return
  const rect = container.getBoundingClientRect()
  const maxPx = rect.width * DRAG_MAX_PERCENT
  const newWidth = rect.right - clientX
  panelWidth.value = Math.max(DRAG_MIN, Math.min(maxPx, newWidth))
}

function stopDrag() {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', stopDrag)
}

// ── Web search ──
async function startSearch() {
  if (!searchQuery.value.trim() || isSearching.value) return
  isSearching.value = true
  searchResult.value = null
  searchStepIndex.value = 0

  const stepTimer = setInterval(() => {
    if (searchStepIndex.value < searchSteps.length - 1) searchStepIndex.value++
  }, 900)

  try {
    const res = await apiAskTutor({
      question: `\u8bf7\u641c\u7d22\u5e76\u6574\u7406\u4ee5\u4e0b\u77e5\u8bc6\u70b9\u7684\u516c\u5f00\u8d44\u6e90\uff0c\u5305\u542b\u6982\u5ff5\u89e3\u91ca\u3001\u5b66\u4e60\u8d44\u6599\u548c\u76f8\u5173\u94fe\u63a5\uff1a${searchQuery.value}`,
      context: JSON.stringify({ subChapterId: currentSubChapterId.value })
    })
    const text = res.data?.markdown || res.data?.answer || res.data || ''
    searchResult.value = text
    if (!panelCollapsed.value) panelTab.value = 'search'
  } catch {
    searchResult.value = '\u641c\u7d22\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002'
  } finally {
    clearInterval(stepTimer)
    isSearching.value = false
  }
}

function triggerSearchFromChat(keyword) {
  searchQuery.value = keyword
  panelTab.value = 'search'
  startSearch()
}
</script>

<style scoped>
.course-view {
  display: flex;
  height: calc(100vh - 56px);
  overflow: hidden;
}

/* LEFT: Course Tree */
.course-tree {
  width: 240px; min-width: 240px;
  background: rgba(8, 13, 31, 0.6);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex; flex-direction: column;
  transition: width 0.2s, min-width 0.2s;
}
.course-tree.collapsed { width: 40px; min-width: 40px; }
.course-tree.collapsed .tree-title,
.course-tree.collapsed .tree-subject-name, .tree-course-name,
.course-tree.collapsed .tree-unit-name,
.course-tree.collapsed .tree-lesson-name { display: none; }
.tree-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px; border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.tree-title { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255, 255, 255, 0.3); }
.tree-toggle { background: none; border: none; color: rgba(255,255,255,0.3); cursor: pointer; }
.tree-body { flex: 1; overflow-y: auto; padding: 8px; }
.tree-subject { margin-bottom: 8px; }
.tree-subject-name { font-size: 12px; font-weight: 600; padding: 4px 8px; margin-bottom: 2px; color: #f1f5f9; }
.tree-unit { margin-bottom: 4px; margin-left: 4px; }
.tree-unit-name { font-size: 11px; color: rgba(255, 255, 255, 0.35); padding: 4px 8px 1px; }
.tree-lesson {
  display: flex; align-items: center; gap: 6px; padding: 4px 8px 4px 22px;
  border-radius: 4px; cursor: pointer; font-size: 11px; color: rgba(255, 255, 255, 0.5);
}
.tree-lesson:hover { background: rgba(255, 255, 255, 0.04); }
.tree-lesson.active { background: rgba(59, 130, 246, 0.12); color: #60d9fa; font-weight: 600; }
.tree-lesson-dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,0.2); flex-shrink: 0; }
.tree-lesson.active .tree-lesson-dot { background: #60d9fa; }
.tree-lesson-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tree-loading { padding: 20px; text-align: center; }

/* CENTER: Main */
.course-main {
  flex: 1; overflow-y: auto; padding: 20px 24px;
}
.main-breadcrumb { font-size: 10px; color: rgba(255,255,255,0.3); margin-bottom: 16px; }
.crumb-link { cursor: pointer; color: #3b82f6; }
.crumb-link:hover { text-decoration: underline; }
.crumb-sep { margin: 0 6px; }
.crumb-current { color: rgba(255,255,255,0.7); }
.video-section { margin-bottom: 16px; }
.video-wrapper {
  position: relative; width: 100%; padding-bottom: 56.25%; /* 16:9 */
  border-radius: 8px; overflow: hidden; background: rgba(0,0,0,0.3);
}
.lesson-video {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  border: none; border-radius: 8px; background: #000;
}
.lesson-title { font-size: 20px; font-weight: 700; margin-bottom: 12px; }
.content-body { font-size: 13px; line-height: 1.8; color: rgba(255,255,255,0.6); }

/* Content Tabs */
.content-tabs {
  display: flex; gap: 0; margin-bottom: 0;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.content-tab {
  padding: 10px 20px; font-size: 13px; color: rgba(255,255,255,0.35);
  background: none; border: none; border-bottom: 2px solid transparent; cursor: pointer;
  font-family: inherit; transition: all 0.15s;
}
.content-tab:hover { color: rgba(255,255,255,0.6); }
.content-tab.active { color: #60d9fa; border-bottom-color: #60d9fa; font-weight: 600; }

/* Subtitle */
.subtitle-content { font-size: 13px; line-height: 1.8; color: rgba(255,255,255,0.65); white-space: pre-wrap; }
.subtitle-empty { text-align: center; padding: 40px 20px; }
.subtitle-empty p { font-size: 13px; color: rgba(255,255,255,0.3); margin-bottom: 16px; }
.generate-btn {
  padding: 10px 28px; border-radius: 8px; border: 1px solid rgba(96,217,250,0.3);
  background: rgba(96,217,250,0.08); color: #60d9fa; font-size: 13px;
  font-weight: 600; cursor: pointer; font-family: inherit;
}
.generate-btn:hover { background: rgba(96,217,250,0.15); }
.generate-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* Exercises */
.exercise-type-stats {
  font-size: 11px; color: rgba(255,255,255,0.3); margin-bottom: 16px;
  padding-bottom: 12px; border-bottom: 1px solid rgba(255,255,255,0.04);
}
.exercise-item { margin-bottom: 14px; padding-bottom: 14px; border-bottom: 1px solid rgba(255,255,255,0.04); }
.ex-question { font-size: 13px; font-weight: 500; margin-bottom: 8px; color: rgba(255,255,255,0.85); }
.ex-type-tag {
  display: inline-block; font-size: 10px; padding: 1px 6px; border-radius: 4px;
  background: rgba(96,217,250,0.1); color: #60d9fa; margin-right: 6px;
  vertical-align: 2px;
}
.ex-options { display: flex; flex-direction: column; gap: 4px; }
.ex-option {
  display: flex; align-items: center; gap: 6px; font-size: 12px; color: rgba(255,255,255,0.5);
  padding: 6px 10px; border-radius: 6px; cursor: pointer;
}
.ex-option:hover { background: rgba(255,255,255,0.03); }
.ex-option.selected { background: rgba(59,130,246,0.1); color: rgba(255,255,255,0.8); }
.ex-option.correct { background: rgba(34,197,94,0.1); color: #22c55e; }
.ex-option.wrong { background: rgba(239,68,68,0.1); color: #ef4444; }
.ex-option input[type="radio"] { accent-color: #3b82f6; }

.ex-fill { margin-top: 6px; }
.fill-input {
  width: 100%; padding: 8px 12px; border-radius: 6px;
  border: 1px solid rgba(255,255,255,0.08); background: rgba(255,255,255,0.04);
  color: #f1f5f9; font-size: 13px; font-family: inherit; outline: none;
}
.fill-input:focus { border-color: rgba(96,217,250,0.3); }
.fill-textarea {
  width: 100%; padding: 8px 12px; border-radius: 6px; resize: vertical;
  border: 1px solid rgba(255,255,255,0.08); background: rgba(255,255,255,0.04);
  color: #f1f5f9; font-size: 13px; font-family: inherit; outline: none;
}
.fill-textarea:focus { border-color: rgba(96,217,250,0.3); }

.ex-feedback { font-size: 11px; margin-top: 6px; }
.ex-feedback.correct { color: #22c55e; }
.ex-feedback.wrong { color: #ef4444; }
.ex-explain { color: rgba(255,255,255,0.4); }

.exercise-actions { margin-top: 16px; display: flex; gap: 10px; }
.submit-btn {
  padding: 8px 24px; border-radius: 8px; border: none;
  background: linear-gradient(135deg, #3b82f6, #2563eb); color: #fff;
  font-size: 13px; cursor: pointer; font-family: inherit;
}
.submit-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.retry-btn {
  padding: 8px 24px; border-radius: 8px; border: 1px solid rgba(96,217,250,0.3);
  background: rgba(96,217,250,0.08); color: #60d9fa; font-size: 13px;
  cursor: pointer; font-family: inherit;
}
.retry-btn:hover { background: rgba(96,217,250,0.15); }

.exercise-empty { text-align: center; padding: 40px 20px; }
.exercise-empty p { font-size: 13px; color: rgba(255,255,255,0.3); margin-bottom: 16px; }

/* Bottom nav */
.lesson-nav { display: flex; justify-content: space-between; margin-top: 20px; padding-top: 12px; border-top: 1px solid rgba(255,255,255,0.06); }
.nav-btn { background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.06); border-radius: 8px; padding: 8px 16px; color: rgba(255,255,255,0.6); font-size: 12px; cursor: pointer; }
.nav-btn:hover { background: rgba(255,255,255,0.08); }
.nav-btn.next { background: rgba(59,130,246,0.12); border-color: rgba(59,130,246,0.2); color: #60d9fa; }

/* RIGHT: AI Panel */
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
.chat-msg.assistant .msg-content { background: rgba(59,130,246,0.1); border-radius: 8px; padding: 8px; color: rgba(255,255,255,0.7); line-height: 1.5; }
.chat-msg.user { display: flex; justify-content: flex-end; }
.chat-msg.user .msg-content { background: rgba(168,85,247,0.1); border-radius: 8px; padding: 8px; max-width: 80%; color: rgba(255,255,255,0.7); line-height: 1.5; }
.chat-quick-actions { display: flex; gap: 6px; padding: 0 12px 8px; }
.quick-btn { font-size: 9px; padding: 3px 8px; border-radius: 4px; background: rgba(255,255,255,0.04); border: none; color: rgba(255,255,255,0.4); cursor: pointer; font-family: inherit; }
.quick-btn:hover { background: rgba(255,255,255,0.08); }
.chat-input-row { display: flex; gap: 6px; padding: 0 12px 12px; }
.chat-input { flex: 1; padding: 8px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.08); background: rgba(255,255,255,0.04); color: #fff; font-size: 11px; font-family: inherit; outline: none; }
.chat-input::placeholder { color: rgba(255,255,255,0.3); }
.chat-send { padding: 8px 14px; border-radius: 8px; border: none; background: linear-gradient(135deg,#3b82f6,#2563eb); color: #fff; font-size: 11px; cursor: pointer; font-family: inherit; white-space: nowrap; }

/* Notes */
.notes-panel { padding: 12px; height: 100%; display: flex; flex-direction: column; }
.notes-toolbar { display: flex; gap: 4px; margin-bottom: 8px; }
.notes-tb-btn {
  width: 28px; height: 24px; border-radius: 4px; border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04); color: rgba(255,255,255,0.5); font-size: 12px; cursor: pointer; font-family: inherit;
}
.notes-tb-btn:hover { background: rgba(255,255,255,0.08); }
.notes-editor {
  flex: 1; border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 8px;
  font-size: 12px; color: rgba(255,255,255,0.7); outline: none; background: rgba(255,255,255,0.02);
  overflow-y: auto; min-height: 200px;
}
.notes-save {
  margin-top: 8px; padding: 6px 16px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04); color: rgba(255,255,255,0.6); font-size: 11px; cursor: pointer; font-family: inherit;
}

/* Discuss */
.discuss-panel { padding: 12px; }
.discuss-placeholder { font-size: 12px; color: rgba(255,255,255,0.3); text-align: center; padding: 40px 0; }

.master-btn-row { margin-top: 20px; text-align: center; }
.master-btn {
  padding: 10px 36px; border-radius: 10px; border: 1px solid rgba(168,85,247,0.3);
  background: linear-gradient(135deg, rgba(168,85,247,0.12), rgba(139,92,246,0.08));
  color: #c084fc; font-size: 14px; font-weight: 600; cursor: pointer; font-family: inherit;
  transition: all 0.2s;
}
.master-btn:hover { background: linear-gradient(135deg, rgba(168,85,247,0.2), rgba(139,92,246,0.12)); }
.master-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* Deep Explore */
.deep-fab {
  position: fixed; right: 340px; bottom: 24px; width: 48px; height: 48px;
  border-radius: 50%; border: 1px solid rgba(255,255,255,0.1);
  background: rgba(8,13,31,0.8); backdrop-filter: blur(8px);
  color: rgba(255,255,255,0.6); cursor: pointer; display: flex; align-items: center; justify-content: center;
  z-index: 50; transition: transform 0.15s;
}
.deep-fab:hover { transform: scale(1.05); }
.deep-explore-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 200;
  display: flex; align-items: center; justify-content: center;
}
.deep-explore-modal {
  width: 80vw; max-width: 900px; max-height: 80vh; overflow-y: auto; padding: 0; border-radius: 14px;
}
.deep-explore-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid rgba(255,255,255,0.06);
}
.deep-explore-header h3 { font-size: 15px; font-weight: 600; }
.deep-close-btn { background: none; border: none; color: rgba(255,255,255,0.4); font-size: 20px; cursor: pointer; }
.deep-explore-body { padding: 20px; font-size: 12px; color: rgba(255,255,255,0.5); }

/* Shared */
.loading-state { display: flex; align-items: center; justify-content: center; padding: 60px 0; font-size: 12px; color: rgba(255,255,255,0.4); }
.spinner { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.1); border-top-color: #3b82f6; border-radius: 50%; display: inline-block; animation: spin 0.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.typing-indicator { display: flex; gap: 4px; padding: 8px; }
.typing-indicator span { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,0.3); animation: blink 1.4s infinite both; }
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink { 0%, 100% { opacity: 0.2; } 50% { opacity: 1; } }

/* Glass card */
.glass-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  backdrop-filter: blur(12px);
}

/* Panel tabs: announcements, resources, homework, design */
.tab-panel {
  padding: 12px;
}

.tab-panel-title {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.55);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.tab-empty {
  text-align: center;
  padding: 32px 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.3);
}

/* Homework card (inline in homework tab) */
.hw-card {
  padding: 12px;
  margin-bottom: 8px;
}

.hw-card:last-child {
  margin-bottom: 0;
}

.hw-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hw-title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
}

.hw-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
}

/* ── Drag handle ── */
.panel-drag-handle {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 6px;
  cursor: col-resize;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
}
.panel-drag-handle:hover .drag-handle-inner {
  background: rgba(96, 217, 250, 0.3);
}
.drag-handle-inner {
  width: 3px;
  height: 32px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}
.drag-handle-inner span {
  display: block;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
}
.ai-panel { position: relative; }
.ai-panel.dragging { transition: none !important; user-select: none; }
.ai-panel.dragging .panel-drag-handle .drag-handle-inner { background: rgba(96, 217, 250, 0.4); }

/* ── Citations ── */
.citations-wrap {
  margin-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding-top: 6px;
}
.citations-header {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.35);
  cursor: pointer;
  padding: 2px 0;
  user-select: none;
}
.citations-toggle {
  font-size: 12px;
  font-weight: 700;
  width: 14px;
  text-align: center;
}
.citations-list {
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin-top: 4px;
}
.citation-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 6px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
}
.citation-item:hover {
  background: rgba(59, 130, 246, 0.1);
  color: #60d9fa;
}
.citation-badge {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(59, 130, 246, 0.2);
  color: #60d9fa;
  font-size: 9px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.citation-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.citation-arrow {
  opacity: 0;
  transition: opacity 0.15s;
  font-size: 11px;
}
.citation-item:hover .citation-arrow { opacity: 1; }

/* ── Copy button ── */
.msg-copy {
  display: none;
  margin-top: 4px;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: transparent;
  color: rgba(255, 255, 255, 0.3);
  font-size: 10px;
  cursor: pointer;
  font-family: inherit;
  float: right;
}
.chat-msg.assistant:hover .msg-copy { display: inline-block; }
.msg-copy:hover { color: rgba(255, 255, 255, 0.6); border-color: rgba(255, 255, 255, 0.15); }

/* ── Chat textarea ── */
.chat-textarea {
  flex: 1;
  padding: 8px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  color: #fff;
  font-size: 11px;
  font-family: inherit;
  outline: none;
  resize: none;
  line-height: 1.5;
}
.chat-textarea::placeholder { color: rgba(255, 255, 255, 0.3); }
.chat-textarea:focus { border-color: rgba(96, 217, 250, 0.3); }

/* ── Search panel ── */
.search-panel { padding: 12px; }
.search-input-row {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
}
.search-input {
  flex: 1;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  color: #fff;
  font-size: 12px;
  font-family: inherit;
  outline: none;
}
.search-input::placeholder { color: rgba(255, 255, 255, 0.3); }
.search-input:focus { border-color: rgba(96, 217, 250, 0.3); }
.search-btn {
  padding: 8px 14px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
}
.search-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.search-btn:hover:not(:disabled) { background: linear-gradient(135deg, #2563eb, #1d4ed8); }
.search-progress {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 0;
}
.search-step {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
}
.search-step.active { color: #60d9fa; }
.search-step.done { color: rgba(255, 255, 255, 0.5); }
.step-icon { width: 16px; text-align: center; font-size: 12px; }
.search-result-content {
  font-size: 12px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.6);
}

/* Responsive */
@media (max-width: 1280px) {
  .course-tree { width: 40px; min-width: 40px; }
  .course-tree .tree-title,
  .course-tree .tree-subject-name, .tree-course-name,
  .course-tree .tree-unit-name,
  .course-tree .tree-lesson-name { display: none; }
  .deep-fab { right: 310px; }
}
@media (max-width: 1024px) {
  .ai-panel { display: none; }
  .deep-fab { right: 16px; }
}
</style>
