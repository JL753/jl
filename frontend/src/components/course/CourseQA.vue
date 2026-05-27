<template>
  <div class="course-qa">
    <div class="qa-header">
      <h4 class="qa-title">课程问答</h4>
      <button class="qa-ask-btn" @click="showAskForm = true" v-if="!showAskForm">提问</button>
    </div>
    <div v-if="showAskForm" class="qa-ask-form glass-card">
      <input class="qa-input" v-model="newTitle" placeholder="问题标题" />
      <textarea class="qa-textarea" v-model="newContent" placeholder="详细描述你的问题..." rows="3"></textarea>
      <div class="qa-form-actions">
        <button class="qa-cancel" @click="showAskForm = false">取消</button>
        <button class="qa-submit" @click="submitQuestion" :disabled="!newTitle.trim()">提交</button>
      </div>
    </div>
    <div v-if="loading" class="qa-loading"><span class="spinner"></span></div>
    <div v-else-if="questions.length === 0" class="qa-empty">暂无问题，成为第一个提问的人吧！</div>
    <div v-for="q in questions" :key="q.id" class="qa-card glass-card">
      <div class="qa-card-header">
        <span class="qa-user">{{ q.userName }}</span>
        <span class="qa-time">{{ formatTime(q.createdAt) }}</span>
      </div>
      <h5 class="qa-question-title">{{ q.title }}</h5>
      <p class="qa-question-content">{{ q.content }}</p>
      <div class="qa-card-footer">
        <span class="qa-answer-count">{{ q.answerCount || 0 }} 个回答</span>
        <button class="qa-view-btn" @click="toggleAnswers(q)">{{ expandedQuestions.has(q.id) ? '收起' : '查看回答' }}</button>
      </div>
      <div v-if="expandedQuestions.has(q.id)" class="qa-answers">
        <div v-if="answersLoading[q.id]" class="qa-loading-small"><span class="spinner"></span></div>
        <div v-for="a in answers[q.id]" :key="a.id" class="qa-answer" :class="{ 'ai-answer': a.isAi }">
          <div class="qa-answer-header">
            <span class="qa-answer-user">{{ a.isAi ? 'AI 小慧' : a.userName }}</span>
            <span v-if="a.isAi" class="ai-badge">AI</span>
          </div>
          <div class="qa-answer-content" v-html="renderMarkdown(a.content)"></div>
        </div>
        <div v-if="!hasAiAnswer(q.id)" class="qa-ai-pending">
          <button class="qa-ai-btn" @click="requestAiAnswer(q.id)" :disabled="aiLoading[q.id]">
            {{ aiLoading[q.id] ? 'AI 生成中...' : '让 AI 回答' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { apiCourseQuestions, apiAskQuestion, apiQuestionAnswers, apiAiAnswer } from '../../api/index.js'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps({ courseId: { type: [Number, String], required: true } })
const questions = ref([])
const loading = ref(false)
const showAskForm = ref(false)
const newTitle = ref('')
const newContent = ref('')
const expandedQuestions = ref(new Set())
const answers = reactive({})
const answersLoading = reactive({})
const aiLoading = reactive({})

async function loadQuestions() {
  loading.value = true
  try { const res = await apiCourseQuestions(props.courseId); questions.value = res.data || [] }
  finally { loading.value = false }
}

async function submitQuestion() {
  if (!newTitle.value.trim()) return
  try {
    await apiAskQuestion(props.courseId, { title: newTitle.value.trim(), content: newContent.value.trim() })
    newTitle.value = ''; newContent.value = ''; showAskForm.value = false
    await loadQuestions()
  } catch (e) { alert('提问失败') }
}

async function toggleAnswers(q) {
  if (expandedQuestions.value.has(q.id)) { expandedQuestions.value.delete(q.id); expandedQuestions.value = new Set(expandedQuestions.value); return }
  expandedQuestions.value.add(q.id); expandedQuestions.value = new Set(expandedQuestions.value)
  if (!answers[q.id]) {
    answersLoading[q.id] = true
    try { const res = await apiQuestionAnswers(q.id); answers[q.id] = res.data || [] }
    finally { answersLoading[q.id] = false }
  }
}

async function requestAiAnswer(questionId) {
  aiLoading[questionId] = true
  try { await apiAiAnswer(questionId); const res = await apiQuestionAnswers(questionId); answers[questionId] = res.data || [] }
  catch (e) { alert('AI回答失败') }
  finally { aiLoading[questionId] = false }
}

function hasAiAnswer(questionId) { return (answers[questionId] || []).some(a => a.isAi) }
function renderMarkdown(text) { return text ? DOMPurify.sanitize(marked(text)) : '' }
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }

onMounted(() => loadQuestions())
</script>

<style scoped>
.course-qa { display: flex; flex-direction: column; gap: 12px; }
.qa-header { display: flex; justify-content: space-between; align-items: center; }
.qa-title { font-size: 16px; font-weight: 600; color: #f1f5f9; margin: 0; }
.qa-ask-btn { padding: 6px 16px; border-radius: 6px; border: 1px solid rgba(59,130,246,0.3); background: rgba(59,130,246,0.1); color: #60d9fa; cursor: pointer; font-size: 12px; font-family: inherit; }
.qa-ask-form { padding: 14px; display: flex; flex-direction: column; gap: 8px; }
.qa-input, .qa-textarea { padding: 8px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.08); background: rgba(255,255,255,0.04); color: #f1f5f9; font-size: 12px; font-family: inherit; outline: none; width: 100%; box-sizing: border-box; }
.qa-input::placeholder, .qa-textarea::placeholder { color: rgba(255,255,255,0.3); }
.qa-textarea { resize: vertical; }
.qa-form-actions { display: flex; justify-content: flex-end; gap: 8px; }
.qa-cancel { padding: 4px 12px; border-radius: 4px; border: 1px solid rgba(255,255,255,0.1); background: none; color: rgba(255,255,255,0.4); cursor: pointer; font-size: 11px; font-family: inherit; }
.qa-submit { padding: 4px 16px; border-radius: 4px; border: none; background: linear-gradient(135deg,#3b82f6,#2563eb); color: #fff; cursor: pointer; font-size: 11px; font-family: inherit; }
.qa-submit:disabled { opacity: 0.4; cursor: not-allowed; }
.qa-card { padding: 14px; }
.qa-card-header { display: flex; justify-content: space-between; margin-bottom: 6px; }
.qa-user { font-size: 11px; color: #60d9fa; }
.qa-time { font-size: 10px; color: rgba(255,255,255,0.3); }
.qa-question-title { font-size: 14px; font-weight: 600; color: #f1f5f9; margin: 0 0 4px; }
.qa-question-content { font-size: 12px; color: rgba(255,255,255,0.6); line-height: 1.6; margin: 0; }
.qa-card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.qa-answer-count { font-size: 10px; color: rgba(255,255,255,0.3); }
.qa-view-btn { background: none; border: none; color: #60d9fa; font-size: 11px; cursor: pointer; font-family: inherit; }
.qa-answers { margin-top: 10px; padding-top: 10px; border-top: 1px solid rgba(255,255,255,0.06); display: flex; flex-direction: column; gap: 8px; }
.qa-answer { padding: 10px; background: rgba(255,255,255,0.03); border-radius: 6px; }
.qa-answer.ai-answer { background: rgba(59,130,246,0.06); border: 1px solid rgba(59,130,246,0.1); }
.qa-answer-header { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.qa-answer-user { font-size: 11px; color: rgba(255,255,255,0.6); }
.ai-badge { font-size: 9px; padding: 0 6px; border-radius: 4px; background: rgba(59,130,246,0.2); color: #93c5fd; }
.qa-answer-content { font-size: 12px; color: rgba(255,255,255,0.7); line-height: 1.6; }
.qa-answer-content :deep(p) { margin: 0; }
.qa-ai-pending { text-align: center; padding: 8px; }
.qa-ai-btn { padding: 4px 12px; border-radius: 4px; border: 1px solid rgba(168,85,247,0.3); background: rgba(168,85,247,0.1); color: #c084fc; cursor: pointer; font-size: 11px; font-family: inherit; }
.qa-ai-btn:disabled { opacity: 0.4; }
.qa-loading, .qa-empty { text-align: center; padding: 40px; font-size: 12px; color: rgba(255,255,255,0.3); }
.qa-loading-small { text-align: center; padding: 12px; }
.spinner { width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.1); border-top-color: #3b82f6; border-radius: 50%; display: inline-block; animation: spin 0.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.glass-card { background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; backdrop-filter: blur(12px); }
</style>
