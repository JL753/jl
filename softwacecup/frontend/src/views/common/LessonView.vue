<template>
  <div class="lesson-view">
    <div class="lesson-layout">
      <!-- Left: Main content (70%) -->
      <div class="lesson-main">
        <!-- Top bar -->
        <div class="lesson-topbar">
          <button class="back-btn" @click="handleBack">
            <span class="back-icon">←</span>
            <span>返回</span>
          </button>
          <h2 class="lesson-title">{{ lesson?.name || '加载中...' }}</h2>
        </div>

        <!-- Video player -->
        <div v-if="lesson?.videoUrl" class="video-section glass-card">
          <video
            :src="lesson.videoUrl"
            controls
            class="lesson-video"
            @play="handleVideoPlay"
          ></video>
        </div>

        <!-- Markdown content -->
        <div class="content-section glass-card">
          <div class="content-body markdown-body" v-html="renderedContent"></div>
        </div>

        <!-- Bottom navigation -->
        <div class="lesson-nav">
          <button
            class="nav-btn prev-btn"
            :disabled="!prevLessonId"
            @click="navigateToLesson(prevLessonId)"
          >
            ← 上一个课时
          </button>
          <button
            class="nav-btn complete-btn"
            @click="completeLesson"
            :disabled="completing"
          >
            {{ completing ? '提交中...' : '完成并继续' }}
          </button>
        </div>
      </div>

      <!-- Right: Sidebar (30%) -->
      <div class="lesson-sidebar">
        <!-- Tab bar -->
        <div class="sidebar-tabs">
          <button
            class="sidebar-tab"
            :class="{ active: activeTab === 'exercise' }"
            @click="activeTab = 'exercise'"
          >
            📝 练习
          </button>
          <button
            class="sidebar-tab"
            :class="{ active: activeTab === 'ai' }"
            @click="activeTab = 'ai'"
          >
            🤖 AI
          </button>
        </div>

        <!-- Exercise tab -->
        <div v-if="activeTab === 'exercise'" class="tab-content exercise-tab">
          <QuizComponent
            :lesson-id="lessonId"
            :exercises="exercises"
            :selected-answers="selectedAnswers"
            :show-result="showResult"
            @select-answer="selectAnswer"
            @submit-answers="submitAnswers"
            @reset="resetExercises"
          />
        </div>

        <!-- AI tab -->
        <div v-else class="tab-content ai-tab">
          <div class="ai-messages" ref="aiMessagesRef">
            <div
              v-for="(msg, i) in aiMessages"
              :key="i"
              class="ai-msg"
              :class="msg.role"
            >
              <div class="msg-avatar">
                {{ msg.role === 'user' ? '👤' : '🤖' }}
              </div>
              <div class="msg-content">
                <div class="msg-text" v-html="msg.text"></div>
              </div>
            </div>
            <div v-if="aiLoading" class="ai-msg assistant">
              <div class="msg-avatar">🤖</div>
              <div class="msg-content">
                <div class="msg-text thinking">思考中...</div>
              </div>
            </div>
          </div>
          <div class="ai-input-area">
            <textarea
              v-model="aiInput"
              class="ai-input"
              placeholder="输入问题..."
              rows="2"
              @keydown.enter.exact.prevent="askAI"
              :disabled="aiLoading"
            ></textarea>
            <button
              class="ai-send-btn"
              @click="askAI"
              :disabled="aiLoading || !aiInput.trim()"
            >
              发送
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  apiLessonDetail,
  apiLessonExercises,
  apiCompleteLesson,
  apiLessonProgress,
  apiAskTutor,
} from '../../api/index.js'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import QuizComponent from '../../components/QuizComponent.vue'

const route = useRoute()
const router = useRouter()

const lessonId = computed(() => route.params.id)
const lesson = ref(null)
const exercises = ref([])
const selectedAnswers = ref({})
const showResult = ref(false)
const aiMessages = ref([
  {
    role: 'assistant',
    text: '你好！我是AI学习助手。关于这个课时，有什么想问的吗？',
  },
])
const aiInput = ref('')
const aiLoading = ref(false)
const activeTab = ref('exercise')
const completing = ref(false)
const prevLessonId = ref(null)

const aiMessagesRef = ref(null)

// Scroll AI messages to bottom
watch(
  () => aiMessages.value.length,
  async () => {
    await nextTick()
    if (aiMessagesRef.value) {
      aiMessagesRef.value.scrollTop = aiMessagesRef.value.scrollHeight
    }
  },
)

// Markdown rendering
const renderedContent = computed(() => {
  if (!lesson.value?.content) return '<p>暂无内容</p>'

  // Parse markdown to HTML
  let html
  try {
    html = marked.parse(lesson.value.content)
  } catch (e) {
    html = lesson.value.content
  }

  // AI content labeling: replace markers with styled badges
  html = html.replace(
    /🔴/g,
    '<span class="ai-badge badge-key">🔴 重点</span>',
  )
  html = html.replace(
    /🟡/g,
    '<span class="ai-badge badge-hard">🟡 难点</span>',
  )
  html = html.replace(
    /⚠️/g,
    '<span class="ai-badge badge-mistake">⚠️ 常见误区</span>',
  )

  // Also handle Chinese markers
  html = html.replace(
    /【重点】/g,
    '<span class="ai-badge badge-key">🔴 重点</span>',
  )
  html = html.replace(
    /【难点】/g,
    '<span class="ai-badge badge-hard">🟡 难点</span>',
  )
  html = html.replace(
    /【易错点】/g,
    '<span class="ai-badge badge-mistake">⚠️ 常见误区</span>',
  )

  // Sanitize
  html = DOMPurify.sanitize(html, {
    ADD_TAGS: ['video', 'source', 'iframe'],
    ADD_ATTR: ['controls', 'src', 'frameborder', 'allowfullscreen'],
  })

  return html
})

function handleBack() {
  router.back()
}

function handleVideoPlay() {
  // Auto-mark as in progress when video starts playing
  // (no-op for now, progress is tracked on complete)
}

async function completeLesson() {
  completing.value = true
  try {
    await apiCompleteLesson(lessonId.value)
    router.push('/courses/' + lesson.value?.subjectId)
  } catch (e) {
    console.error('Failed to complete lesson:', e)
    // Still navigate back on error
    router.back()
  } finally {
    completing.value = false
  }
}

function navigateToLesson(id) {
  if (id) {
    router.push('/lessons/' + id)
  }
}

function selectAnswer(exId, opt) {
  if (showResult.value) return
  selectedAnswers.value = { ...selectedAnswers.value, [exId]: opt }
}

function submitAnswers() {
  showResult.value = true
}

function resetExercises() {
  selectedAnswers.value = {}
  showResult.value = false
}

async function askAI() {
  const text = aiInput.value.trim()
  if (!text || aiLoading.value) return

  aiMessages.value.push({ role: 'user', text })
  aiInput.value = ''
  aiLoading.value = true

  try {
    const res = await apiAskTutor({
      question: text,
      lessonId: lessonId.value,
    })
    aiMessages.value.push({
      role: 'assistant',
      text:
        res.data?.answer || res.data?.reply || '好的，让我想想再回答你...',
    })
  } catch (e) {
    aiMessages.value.push({
      role: 'assistant',
      text: '暂时无法回答你的问题，请稍后再试。',
    })
  } finally {
    aiLoading.value = false
  }
}

onMounted(async () => {
  const id = lessonId.value
  try {
    // Fetch lesson detail
    const detailRes = await apiLessonDetail(id)
    lesson.value = detailRes.data || detailRes

    // Fetch exercises
    try {
      const exRes = await apiLessonExercises(id)
      exercises.value = exRes.data || []
    } catch (e) {
      exercises.value = []
    }

    // Fetch progress (to determine if already completed)
    try {
      const progRes = await apiLessonProgress(id)
      if (progRes.data?.status === 'completed' || progRes.data?.mastery >= 100) {
        showResult.value = true
      }
    } catch (e) {
      // ignore
    }
  } catch (e) {
    lesson.value = {
      name: '加载失败',
      content: '# 加载失败\n\n请检查网络连接后刷新页面。',
    }
  }
})
</script>

<style scoped>
.lesson-view {
  min-height: calc(100vh - var(--topbar-height, 64px));
  animation: fadeIn 0.3s ease;
}

.lesson-layout {
  display: flex;
  gap: 20px;
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

/* Left main content (70%) */
.lesson-main {
  flex: 7;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Top bar */
.lesson-topbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 4px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  color: var(--text-sub);
  cursor: pointer;
  transition: all var(--transition-base);
  font-size: var(--font-sm);
  flex-shrink: 0;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-main);
}

.back-icon {
  font-size: 16px;
}

.lesson-title {
  font-size: var(--font-2xl);
  font-weight: 700;
  color: var(--text-main);
  margin: 0;
  flex: 1;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Video section */
.video-section {
  padding: 0;
  overflow: hidden;
}

.lesson-video {
  width: 100%;
  max-height: 480px;
  display: block;
  border-radius: var(--radius-sm);
}

/* Content section */
.content-section {
  padding: 24px;
}

.content-body {
  line-height: 1.8;
  color: var(--text-main);
  font-size: var(--font-md);
}

.content-body :deep(h2) {
  font-size: var(--font-2xl);
  color: var(--text-main);
  margin: 24px 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--glass-border);
}

.content-body :deep(h3) {
  font-size: var(--font-xl);
  color: var(--text-main);
  margin: 20px 0 10px;
}

.content-body :deep(p) {
  margin: 10px 0;
  color: var(--text-sub);
}

.content-body :deep(ul),
.content-body :deep(ol) {
  margin: 10px 0;
  padding-left: 24px;
  color: var(--text-sub);
}

.content-body :deep(li) {
  margin: 4px 0;
}

.content-body :deep(strong) {
  color: var(--text-main);
  font-weight: 600;
}

.content-body :deep(blockquote) {
  margin: 16px 0;
  padding: 12px 20px;
  border-left: 4px solid var(--primary);
  background: rgba(59, 130, 246, 0.06);
  border-radius: 0 var(--radius-xs) var(--radius-xs) 0;
  color: var(--text-sub);
}

.content-body :deep(a) {
  color: var(--primary-light);
  text-decoration: none;
}

.content-body :deep(a:hover) {
  text-decoration: underline;
}

.content-body :deep(img) {
  max-width: 100%;
  border-radius: var(--radius-xs);
  margin: 16px 0;
}

.content-body :deep(hr) {
  margin: 24px 0;
  border: none;
  border-top: 1px solid var(--glass-border);
}

.content-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  border-radius: var(--radius-xs);
  overflow: hidden;
}

.content-body :deep(th) {
  background: rgba(255, 255, 255, 0.06);
  color: var(--text-main);
  font-weight: 600;
  padding: 10px 14px;
  text-align: left;
  border-bottom: 1px solid var(--glass-border);
}

.content-body :deep(td) {
  padding: 10px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  color: var(--text-sub);
}

.content-body :deep(tr:last-child td) {
  border-bottom: none;
}

/* AI badges in content */
.content-body :deep(.ai-badge) {
  display: inline-block;
  font-size: var(--font-xs);
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 999px;
  margin: 0 4px;
}

.content-body :deep(.badge-key) {
  background: rgba(239, 68, 68, 0.15);
  color: #fca5a5;
  border: 1px solid rgba(239, 68, 68, 0.25);
}

.content-body :deep(.badge-hard) {
  background: rgba(234, 179, 8, 0.15);
  color: #fde68a;
  border: 1px solid rgba(234, 179, 8, 0.25);
}

.content-body :deep(.badge-mistake) {
  background: rgba(59, 130, 246, 0.15);
  color: #93c5fd;
  border: 1px solid rgba(59, 130, 246, 0.25);
}

/* Bottom navigation */
.lesson-nav {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 4px 0 20px;
}

.nav-btn {
  flex: 1;
  padding: 12px 24px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--glass-border);
  font-size: var(--font-base);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-base);
  text-align: center;
}

.nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.prev-btn {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  color: var(--text-sub);
}

.prev-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-main);
}

.complete-btn {
  background: linear-gradient(135deg, var(--primary), var(--accent-cyan));
  border-color: transparent;
  color: #fff;
}

.complete-btn:hover:not(:disabled) {
  opacity: 0.9;
  box-shadow: var(--shadow-glow-primary);
}

/* Right sidebar (30%) */
.lesson-sidebar {
  flex: 3;
  min-width: 280px;
  max-width: 400px;
  display: flex;
  flex-direction: column;
  gap: 0;
  position: sticky;
  top: calc(var(--topbar-height, 64px) + 20px);
  height: fit-content;
  max-height: calc(100vh - var(--topbar-height, 64px) - 40px);
}

/* Sidebar tabs */
.sidebar-tabs {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: var(--radius-sm);
  margin-bottom: 12px;
}

.sidebar-tab {
  flex: 1;
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-faint);
  font-size: var(--font-sm);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  text-align: center;
}

.sidebar-tab.active {
  background: rgba(59, 130, 246, 0.2);
  color: var(--primary-light);
}

.sidebar-tab:hover:not(.active) {
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-sub);
}

/* Tab content */
.tab-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* AI chat tab */
.ai-tab {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 400px;
}

.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-msg {
  display: flex;
  gap: 10px;
  max-width: 100%;
}

.msg-avatar {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.04);
}

.ai-msg.user .msg-avatar {
  background: rgba(59, 130, 246, 0.15);
}

.msg-content {
  flex: 1;
  min-width: 0;
}

.msg-text {
  font-size: var(--font-sm);
  line-height: 1.6;
  padding: 8px 12px;
  border-radius: 8px;
  color: var(--text-main);
}

.ai-msg.user .msg-text {
  background: rgba(59, 130, 246, 0.12);
  color: var(--primary-light);
}

.ai-msg.assistant .msg-text {
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-sub);
}

.msg-text.thinking {
  opacity: 0.5;
}

/* AI input area */
.ai-input-area {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid var(--glass-border);
  background: rgba(0, 0, 0, 0.15);
}

.ai-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xs);
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-main);
  font-size: var(--font-sm);
  outline: none;
  resize: none;
  font-family: inherit;
  line-height: 1.5;
}

.ai-input::placeholder {
  color: var(--text-faint);
}

.ai-input:focus {
  border-color: rgba(59, 130, 246, 0.4);
}

.ai-send-btn {
  padding: 8px 16px;
  border: none;
  border-radius: var(--radius-xs);
  background: linear-gradient(135deg, var(--primary), var(--accent-cyan));
  color: #fff;
  font-size: var(--font-sm);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
  flex-shrink: 0;
  align-self: flex-end;
}

.ai-send-btn:hover:not(:disabled) {
  opacity: 0.9;
  box-shadow: var(--shadow-glow-primary);
}

.ai-send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* Exercise tab */
.exercise-tab {
  height: 100%;
  min-height: 400px;
}

/* Fade in animation */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 1024px) {
  .lesson-layout {
    flex-direction: column;
  }

  .lesson-sidebar {
    flex: none;
    max-width: none;
    position: static;
    max-height: none;
  }

  .ai-tab {
    min-height: 300px;
  }

  .exercise-tab {
    min-height: 300px;
  }
}
</style>
