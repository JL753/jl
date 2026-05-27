<template>
  <div class="ai-ball-wrap">
    <!-- 悬浮球 -->
    <button class="ai-ball" @click="toggleChat" :class="{ active: isOpen }" title="AI 智能助手">
      <span class="ball-icon">
        <svg v-if="!isOpen" width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
          <path d="M12 2a8 8 0 0 0-8 8c0 3.4 2.1 6.3 5 7.5V20h6v-2.5c2.9-1.2 5-4.1 5-7.5a8 8 0 0 0-8-8z"/>
          <line x1="10" y1="22" x2="14" y2="22"/>
          <path d="M9 12h.01M15 12h.01"/>
        </svg>
        <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5">
          <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
        </svg>
      </span>
      <span class="ball-ripple"></span>
    </button>

    <!-- 对话框 -->
    <Transition name="chat-slide">
      <div v-if="isOpen" class="chat-panel">
        <div class="chat-header">
          <div class="chat-title">
            <span class="chat-dot"></span>
            <strong>AI 智能助手</strong>
          </div>
          <span class="chat-hint">可回答问题 · 帮助导航</span>
        </div>

        <div class="chat-messages" ref="messagesEl">
          <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
            <div class="msg-avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
            <div class="msg-bubble" v-html="msg.html || msg.content"></div>
          </div>
          <div v-if="thinking" class="msg assistant">
            <div class="msg-avatar">AI</div>
            <div class="msg-bubble thinking">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>

        <div class="chat-input-row">
          <input
            v-model="inputText"
            type="text"
            placeholder="问我任何问题，或说「跳转到学生仪表盘」..."
            class="chat-input"
            @keyup.enter="sendMessage"
          />
          <button class="send-btn" @click="sendMessage" :disabled="!inputText.trim() || thinking">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { apiAskTutor } from '../api/index'

const router = useRouter()
const auth = useAuthStore()
const isOpen = ref(false)
const inputText = ref('')
const thinking = ref(false)
const messagesEl = ref(null)
const messages = ref([
  {
    role: 'assistant',
    content: '你好！我是知域 AI 智能助手\n\n我可以回答学习问题，也可以帮你导航到对应页面。\n\n试试说：「跳转到课程平台」、「沉浸伴学」或直接提问！',
    html: '你好！我是知域 AI 智能助手<br><br>我可以回答学习问题，也可以帮你导航到对应页面。<br><br>试试说：「跳转到课程平台」、「沉浸伴学」或直接提问！'
  }
])

// 导航关键词映射
const navMap = [
  { keywords: ['仪表盘', '首页', 'dashboard', '学习首页'], student: '/student/dashboard', teacher: '/teacher/dashboard', admin: '/admin/dashboard' },
  { keywords: ['学习工坊', '工作台', '研习', 'workspace'], path: '/student/workspace' },
  { keywords: ['课程', '课程平台'], path: '/student/courses' },
  { keywords: ['学习分析', '分析报告'], path: '/student/analytics' },
  { keywords: ['深度研习', '沉浸学习', '沉浸', '3d模型'], path: '/student/companion' },
  { keywords: ['问答广场', '社区', '问答'], path: '/student/community' },
  { keywords: ['考试', '开始考试'], path: '/student/exam' },
  { keywords: ['教学助手', '备课'], path: '/teacher/assistant' },
  { keywords: ['题库', '题库管理'], path: '/teacher/manage' },
  { keywords: ['资源', '资源管理'], path: '/teacher/resources' },
  { keywords: ['用户管理', '账号管理'], path: '/admin/users' },
  { keywords: ['系统设置', '设置'], path: '/admin/settings' },
  { keywords: ['个人信息', '个人资料', '个人中心'], student: '/student/profile', teacher: '/teacher/profile', admin: '/admin/profile' },
  { keywords: ['沉浸伴学', '3d伴学', '虚拟人', '虚拟教师'], path: '/student/companion' },
]

const tryNavigate = (text) => {
  const lower = text.toLowerCase()
  const isNav = /跳转|前往|打开|去|进入|导航/.test(text)
  if (!isNav) return null

  for (const item of navMap) {
    if (item.keywords.some(k => lower.includes(k))) {
      if (item.path) return item.path
      const role = auth.userRole || 'student'
      return item[role] || item.student
    }
  }
  return null
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || thinking.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  await scrollToBottom()

  // 尝试导航
  const navPath = tryNavigate(text)
  if (navPath) {
    thinking.value = true
    await new Promise(r => setTimeout(r, 600))
    thinking.value = false
    messages.value.push({
      role: 'assistant',
      content: `好的，正在跳转到 ${navPath}...`,
      html: `好的，正在跳转到 <code>${navPath}</code>...`
    })
    await scrollToBottom()
    setTimeout(() => router.push(navPath), 800)
    return
  }

  // 调用 AI API
  thinking.value = true
  await scrollToBottom()
  try {
    const history = messages.value.slice(-6).map(m => ({ role: m.role, content: m.content }))
    const res = await apiAskTutor({ question: text, history })
    const answer = res?.data?.markdown || res?.data?.answer || res?.message || '抱歉，我暂时无法回答这个问题。'
    messages.value.push({
      role: 'assistant',
      content: answer,
      html: answer.replace(/\n/g, '<br>')
    })
  } catch (e) {
    messages.value.push({
      role: 'assistant',
      content: '网络异常，请稍后再试。',
      html: '网络异常，请稍后再试。'
    })
  } finally {
    thinking.value = false
    await scrollToBottom()
  }
}

const toggleChat = () => { isOpen.value = !isOpen.value }
</script>

<style scoped>
.ai-ball-wrap {
  position: fixed; right: 28px; bottom: 36px;
  z-index: var(--z-fab, 999);
  display: flex; flex-direction: column; align-items: flex-end; gap: 12px;
}

.ai-ball {
  width: 58px; height: 58px; border-radius: 50%; border: none;
  background: linear-gradient(135deg, #18b48f, #4c8dff);
  box-shadow: 0 8px 28px rgba(76, 141, 255, 0.45);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: hidden; position: relative;
  &:hover { transform: scale(1.1) translateY(-3px); box-shadow: 0 14px 36px rgba(76, 141, 255, 0.6); }
  &.active { background: linear-gradient(135deg, #ef6b6b, #f4c15d); }
}
.ball-icon { position: relative; z-index: 1; line-height: 1; }
.ball-ripple {
  position: absolute; width: 100%; height: 100%; border-radius: 50%;
  background: rgba(255,255,255,0.15);
  animation: ripple 2.4s ease-in-out infinite;
}
@keyframes ripple { 0%, 100% { transform: scale(1); opacity: 0.5; } 50% { transform: scale(1.25); opacity: 0; } }

.chat-panel {
  width: min(360px, 90vw);
  background: rgba(8, 12, 24, 0.88);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 20px;
  box-shadow: 0 24px 64px rgba(0,0,0,0.6);
  display: flex; flex-direction: column;
  overflow: hidden; max-height: 480px;
}

.chat-header {
  padding: 14px 16px 10px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  display: flex; flex-direction: column; gap: 2px;
}
.chat-title { display: flex; align-items: center; gap: 8px; }
.chat-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #17c3a0; box-shadow: 0 0 8px rgba(23,195,160,0.6);
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.chat-title strong { color: #e6edf3; font-size: 14px; }
.chat-hint { font-size: 11px; color: rgba(255,255,255,0.35); padding-left: 16px; }

.chat-messages {
  flex: 1; overflow-y: auto; padding: 12px;
  display: flex; flex-direction: column; gap: 10px;
  scrollbar-width: thin; scrollbar-color: rgba(255,255,255,0.1) transparent;
}

.msg { display: flex; gap: 8px; align-items: flex-start; }
.msg.user { flex-direction: row-reverse; }
.msg-avatar {
  width: 28px; height: 28px; border-radius: 50%; flex-shrink: 0;
  display: grid; place-items: center; font-size: 11px; font-weight: 700;
  background: rgba(76, 141, 255, 0.3); color: #7cb3ff;
}
.msg.user .msg-avatar { background: rgba(23, 195, 160, 0.3); color: #4fd1b5; }
.msg-bubble {
  max-width: 80%; padding: 8px 12px; border-radius: 14px;
  font-size: 13px; line-height: 1.6; color: rgba(255,255,255,0.88);
  background: rgba(255,255,255,0.07); border: 1px solid rgba(255,255,255,0.08);
}
.msg.user .msg-bubble { background: rgba(76, 141, 255, 0.2); border-color: rgba(76, 141, 255, 0.25); }
.msg-bubble.thinking {
  display: flex; gap: 4px; align-items: center; padding: 10px 14px;
  span { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,0.5); animation: bounce 1.2s ease-in-out infinite; }
  span:nth-child(2) { animation-delay: 0.2s; }
  span:nth-child(3) { animation-delay: 0.4s; }
}
@keyframes bounce { 0%, 80%, 100% { transform: translateY(0); } 40% { transform: translateY(-6px); } }

.chat-input-row {
  padding: 10px 12px;
  border-top: 1px solid rgba(255,255,255,0.08);
  display: flex; gap: 8px; align-items: center;
}
.chat-input {
  flex: 1; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1);
  border-radius: 20px; padding: 8px 14px; color: rgba(255,255,255,0.9);
  font-size: 13px; outline: none; transition: border-color 0.2s;
  &::placeholder { color: rgba(255,255,255,0.3); }
  &:focus { border-color: rgba(76, 141, 255, 0.4); }
}
.send-btn {
  width: 34px; height: 34px; border-radius: 50%; border: none;
  background: linear-gradient(135deg, #48b5ff, #4d7cff);
  color: white; cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s; flex-shrink: 0;
  &:hover:not(:disabled) { transform: scale(1.1); }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}

.chat-slide-enter-active { transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.chat-slide-leave-active { transition: all 0.2s ease-in; }
.chat-slide-enter-from, .chat-slide-leave-to { opacity: 0; transform: scale(0.85) translateY(20px); transform-origin: bottom right; }
</style>
