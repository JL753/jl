<template>
  <div class="course-view">
    <!-- LEFT: Course Tree -->
    <aside class="course-tree" :class="{ collapsed: treeCollapsed }">
      <div class="tree-header">
        <span class="tree-title">导航</span>
        <button class="tree-toggle" @click="treeCollapsed = !treeCollapsed">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline :points="treeCollapsed ? '15 18 9 12 15 6' : '9 18 15 12 9 6'"/></svg>
        </button>
      </div>
      <div class="tree-body" v-if="subjectTree.length > 0">
        <div v-for="subject in subjectTree" :key="subject.id" class="tree-subject">
          <div class="tree-subject-name">{{ subject.name }}</div>
          <div v-for="unit in subject.units" :key="unit.id" class="tree-unit">
            <div class="tree-unit-name">{{ unit.name }}</div>
            <div
              v-for="lesson in unit.lessons" :key="lesson.id"
              class="tree-lesson"
              :class="{ active: currentLessonId === lesson.id }"
              @click="goToLesson(lesson.id)"
            >
              <span class="tree-lesson-dot"></span>
              <span class="tree-lesson-name">{{ lesson.name }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="tree-loading"><span class="spinner"></span></div>
    </aside>

    <!-- CENTER: Main Content -->
    <div class="course-main">
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
        <div v-if="currentLesson.videoUrl" class="video-section">
          <div class="video-wrapper">
            <iframe
              v-if="isBilibiliUrl(currentLesson.videoUrl)"
              :src="bilibiliEmbedUrl(currentLesson.videoUrl)"
              class="lesson-video"
              frameborder="0"
              allowfullscreen
            ></iframe>
            <video v-else :src="currentLesson.videoUrl" controls class="lesson-video"></video>
          </div>
        </div>

        <div class="content-section glass-card">
          <h2 class="lesson-title">{{ currentLesson.name }}</h2>
          <div class="content-body markdown-body" v-html="renderedContent"></div>
        </div>

        <div v-if="exercises.length > 0" class="exercise-section glass-card">
          <h3 class="section-title">练习</h3>
          <div v-for="(ex, idx) in exercises" :key="idx" class="exercise-item">
            <div class="ex-question">{{ idx + 1 }}. {{ ex.question }}</div>
            <div class="ex-options" v-if="ex.options">
              <label v-for="(opt, oi) in ex.options" :key="oi"
                class="ex-option"
                :class="{ selected: selectedAnswers[idx] === oi, correct: submitted && oi === ex.answer, wrong: submitted && selectedAnswers[idx] === oi && oi !== ex.answer }"
              >
                <input type="radio" :name="'ex-'+idx" :value="oi" v-model="selectedAnswers[idx]" :disabled="submitted" />
                <span>{{ opt }}</span>
              </label>
            </div>
            <div v-if="submitted" class="ex-feedback" :class="{ correct: selectedAnswers[idx] === ex.answer, wrong: selectedAnswers[idx] !== ex.answer }">
              {{ selectedAnswers[idx] === ex.answer ? '正确' : '错误 - 正确答案: ' + ex.options[ex.answer] }}
            </div>
          </div>
          <button class="submit-btn" @click="submitExercises" :disabled="submitted || Object.keys(selectedAnswers).length < exercises.length">提交答案</button>
        </div>

        <div class="lesson-nav">
          <button v-if="prevLessonId" class="nav-btn" @click="goToLesson(prevLessonId)">上一个</button>
          <span v-else></span>
          <button v-if="nextLessonId" class="nav-btn next" @click="goToLesson(nextLessonId)">下一个</button>
        </div>
      </template>
    </div>

    <!-- RIGHT: AI Panel -->
    <aside class="ai-panel" :class="{ collapsed: panelCollapsed }">
      <div class="panel-tabs">
        <button class="panel-tab" :class="{ active: panelTab === 'ai' }" @click="panelTab = 'ai'">AI 辅导</button>
        <button class="panel-tab" :class="{ active: panelTab === 'notes' }" @click="panelTab = 'notes'">笔记</button>
        <button class="panel-tab" :class="{ active: panelTab === 'discuss' }" @click="panelTab = 'discuss'">讨论</button>
        <button class="panel-tab xiaohui-tab" :class="{ active: showXiaohui }" @click="showXiaohui = !showXiaohui">小慧</button>
        <button class="panel-toggle" @click="panelCollapsed = !panelCollapsed">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline :points="panelCollapsed ? '15 18 9 12 15 6' : '9 18 15 12 9 6'"/></svg>
        </button>
      </div>
      <div class="panel-body" v-show="!panelCollapsed">
        <!-- Virtual Character 小慧 -->
        <div v-if="showXiaohui" class="xiaohui-area">
          <div class="xiaohui-container">
            <div class="xiaohui-avatar">
              <div class="xiaohui-face">
                <div class="xiaohui-eyes">
                  <div class="xiaohui-eye left"></div>
                  <div class="xiaohui-eye right"></div>
                </div>
                <div class="xiaohui-mouth"></div>
              </div>
            </div>
            <div class="xiaohui-name">小慧</div>
            <div class="xiaohui-status">AI 学习助手在线</div>
          </div>
        </div>
        <!-- AI Chat tab -->
        <div v-if="panelTab === 'ai'" class="ai-chat">
          <div class="chat-messages">
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

        <!-- Notes tab -->
        <div v-if="panelTab === 'notes'" class="notes-panel">
          <div class="notes-toolbar">
            <button class="notes-tb-btn" @click="execCmd('bold')">B</button>
            <button class="notes-tb-btn" @click="execCmd('italic')">I</button>
            <button class="notes-tb-btn" @click="execCmd('underline')">U</button>
            <button class="notes-tb-btn" @click="execCmd('insertUnorderedList')">-</button>
          </div>
          <div class="notes-editor" contenteditable="true" ref="notesEditor"></div>
          <button class="notes-save" @click="saveNotes">保存笔记</button>
        </div>

        <!-- Discuss tab -->
        <div v-if="panelTab === 'discuss'" class="discuss-panel">
          <div class="discuss-placeholder">讨论功能即将上线</div>
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
const showXiaohui = ref(false)

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
const notesContent = ref('')

// Navigation
const prevLessonId = ref(null)
const nextLessonId = ref(null)

onMounted(() => {
  loadCourse()
})

async function loadCourse() {
  loading.value = true
  try {
    const id = route.params.id
    if (route.path.includes('/lessons/')) {
      // Lesson mode: load lesson first, then find its course
      const lessonRes = await apiLessonDetail(id)
      currentLesson.value = lessonRes.data
      currentLessonId.value = lessonRes.data.id
      renderedContent.value = DOMPurify.sanitize(marked(currentLesson.value.content || ''))
      currentUnitName.value = lessonRes.data.unitName || ''

      // Load course tree
      const treeRes = await apiSubjectTree()
      subjectTree.value = treeRes.data || []
      // Find which subject/unit contains this lesson
      locateLessonInTree(currentLessonId.value)

      // Load exercises
      if (currentLesson.value.knowledgePointId) {
        try {
          const exRes = await apiKpExercises(currentLesson.value.knowledgePointId)
          exercises.value = exRes.data || []
        } catch {}
      }
    } else {
      // Course mode: load course tree, show first lesson
      const treeRes = await apiSubjectTree()
      subjectTree.value = treeRes.data || []
      // Find and load first lesson
      const first = findFirstLesson()
      if (first) await loadLesson(first.id)
      if (currentLessonId.value) locateLessonInTree(currentLessonId.value)
    }

    // Compute prev/next
    computeNav()

    // Init AI chat
    if (currentLesson.value) {
      chatHistory.value = [{
        role: 'assistant',
        html: `你好，可以看到你在学习<strong>${currentLesson.value.name}</strong>，有什么问题可以随时问我。`
      }]
    }
  } finally {
    loading.value = false
  }
}

function findFirstLesson() {
  for (const subject of subjectTree.value) {
    if (subject.units) {
      for (const unit of subject.units) {
        if (unit.lessons?.length > 0) return unit.lessons[0]
      }
    }
  }
  return null
}

function locateLessonInTree(lessonId) {
  for (const subject of subjectTree.value) {
    if (subject.units) {
      for (const unit of subject.units) {
        if (unit.lessons) {
          for (const lesson of unit.lessons) {
            if (lesson.id === lessonId) {
              courseName.value = subject.name
              currentUnitName.value = unit.name
              // 过滤树：只保留当前学科
              subjectTree.value = [subject]
              return
            }
          }
        }
      }
    }
  }
}

async function loadLesson(lessonId) {
  currentLessonId.value = lessonId
  try {
    const res = await apiLessonDetail(lessonId)
    currentLesson.value = res.data
    currentUnitName.value = res.data.unitName || ''
    renderedContent.value = DOMPurify.sanitize(marked(res.data.content || ''))

    exercises.value = []
    selectedAnswers.value = {}
    submitted.value = false

    if (res.data.knowledgePointId) {
      try {
        const exRes = await apiKpExercises(res.data.knowledgePointId)
        exercises.value = exRes.data || []
      } catch {}
    }

    computeNav()

    chatHistory.value = [{
      role: 'assistant',
      html: `你好，可以看到你在学习<strong>${res.data.name}</strong>，有什么问题可以随时问我。`
    }]
  } catch (e) {
    // Handle load error
  }
}

function computeNav() {
  const allLessons = []
  for (const unit of subjectTree.value) {
    if (unit.lessons) {
      allLessons.push(...unit.lessons.map(l => ({ ...l, unitIndex: subjectTree.value.indexOf(unit) })))
    }
  }
  const idx = allLessons.findIndex(l => l.id === currentLessonId.value)
  prevLessonId.value = idx > 0 ? allLessons[idx - 1].id : null
  nextLessonId.value = idx < allLessons.length - 1 ? allLessons[idx + 1].id : null
}

function goToLesson(lessonId) {
  loadLesson(lessonId)
}

async function submitExercises() {
  submitted.value = true
  try {
    await apiSubmitAnswer({ lessonId: currentLessonId.value, answers: selectedAnswers.value })
  } catch {}
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

function execCmd(cmd) {
  document.execCommand(cmd, false, null)
}

function saveNotes() {
  const el = document.querySelector('.notes-editor')
  if (el) {
    notesContent.value = el.innerHTML
    // Future: save to backend
  }
}

function isBilibiliUrl(url) {
  return url && (url.includes('bilibili.com') || url.includes('BV'))
}

function bilibiliEmbedUrl(url) {
  if (!url) return ''
  const match = url.match(/BV[a-zA-Z0-9]{10}/)
  const bvid = match ? match[0] : ''
  return `//player.bilibili.com/player.html?bvid=${bvid}&page=1&high_quality=1`
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

/* Exercises */
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
.ex-option input[type="radio"] { accent-color: #3b82f6; }
.ex-feedback { font-size: 11px; margin-top: 4px; }
.ex-feedback.correct { color: #22c55e; }
.ex-feedback.wrong { color: #ef4444; }
.submit-btn {
  margin-top: 8px; padding: 8px 20px; border-radius: 8px; border: none;
  background: linear-gradient(135deg, #3b82f6, #2563eb); color: #fff;
  font-size: 12px; cursor: pointer;
}
.submit-btn:disabled { opacity: 0.4; cursor: not-allowed; }

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

/* 小慧 Virtual Character */
.xiaohui-tab { color: #f9a8d4 !important; }
.xiaohui-tab.active { color: #f472b6 !important; border-bottom-color: #f472b6 !important; }
.xiaohui-area { padding: 12px; border-bottom: 1px solid rgba(255,255,255,0.06); background: rgba(168,85,247,0.04); }
.xiaohui-container { text-align: center; }
.xiaohui-avatar {
  width: 80px; height: 80px; border-radius: 50%; margin: 0 auto 8px;
  background: linear-gradient(135deg, #a855f7, #ec4899);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 0 20px rgba(168,85,247,0.3);
  animation: xiaohui-pulse 3s ease-in-out infinite;
}
.xiaohui-face { position: relative; width: 50px; height: 50px; }
.xiaohui-eyes { display: flex; justify-content: center; gap: 12px; padding-top: 14px; }
.xiaohui-eye {
  width: 8px; height: 10px; border-radius: 50%; background: #fff;
  animation: xiaohui-blink 4s infinite;
}
.xiaohui-eye.right { animation-delay: 0.2s; }
.xiaohui-mouth {
  width: 12px; height: 6px; border-radius: 0 0 12px 12px; background: #fff;
  margin: 8px auto 0; opacity: 0.8;
}
.xiaohui-name { font-size: 14px; font-weight: 700; color: #f9a8d4; margin-bottom: 2px; }
.xiaohui-status { font-size: 10px; color: rgba(255,255,255,0.4); }
@keyframes xiaohui-pulse {
  0%, 100% { box-shadow: 0 0 20px rgba(168,85,247,0.3); }
  50% { box-shadow: 0 0 30px rgba(168,85,247,0.5); }
}
@keyframes xiaohui-blink {
  0%, 96%, 100% { transform: scaleY(1); }
  98% { transform: scaleY(0.1); }
}

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

/* Responsive */
@media (max-width: 1280px) {
  .course-tree { width: 40px; min-width: 40px; }
  .course-tree .tree-title,
  .course-tree .tree-subject-name, .tree-course-name,
  .course-tree .tree-unit-name,
  .course-tree .tree-lesson-name { display: none; }
  .ai-panel { width: 300px; min-width: 300px; }
  .deep-fab { right: 310px; }
}
@media (max-width: 1024px) {
  .ai-panel { display: none; }
  .deep-fab { right: 16px; }
}
</style>
