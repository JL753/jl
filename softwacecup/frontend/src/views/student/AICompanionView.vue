<template>
  <div class="ai-companion-page">
    <!-- Header -->
    <header class="chat-header">
      <div class="header-left">
        <span class="ai-icon">AI</span>
        <div class="header-info">
          <h2 class="header-title">AI 学习助手</h2>
          <p style="font-size:11px;color:rgba(255,255,255,0.4);margin:2px 0 0;">随时提问，AI 辅导你的学习问题</p>
          <span class="header-status">在线 · 随时为你解答</span>
        </div>
      </div>
      <button class="clear-btn" @click="clearConversation">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <polyline points="3 6 5 6 21 6" />
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
        </svg>
        清空对话
      </button>
    </header>

    <!-- Messages -->
    <div class="messages-area" ref="messagesRef" aria-live="polite">
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        :class="['msg-row', msg.role]"
      >
        <div v-if="msg.role === 'ai'" class="msg-avatar ai-avatar" role="img" aria-label="AI 助手">AI</div>
        <div :class="['msg-bubble', msg.role]">
          <div v-if="msg.role === 'ai'" class="msg-content" v-html="msg.html"></div>
          <div v-else class="msg-content">{{ msg.content }}</div>
        </div>
        <div v-if="msg.role === 'user'" class="msg-avatar user-avatar" role="img" aria-label="用户">我</div>
      </div>

      <!-- Loading indicator -->
      <div v-if="loading" class="msg-row ai">
        <div class="msg-avatar ai-avatar" role="img" aria-label="AI 助手">AI</div>
        <div class="msg-bubble ai loading-bubble">
          <div class="thinking-indicator">
            <span>思考中</span>
            <span class="dots">
              <span class="dot">.</span>
              <span class="dot">.</span>
              <span class="dot">.</span>
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Input area -->
    <div class="input-area">
      <div class="input-wrapper">
        <input
          v-model="input"
          class="chat-input"
          placeholder="输入你的问题..."
          @keydown.enter.prevent="send"
          :disabled="loading"
          maxlength="2000"
        />
        <button
          class="send-btn"
          :class="{ active: input.trim() }"
          :disabled="!input.trim() || loading"
          @click="send"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <line x1="22" y1="2" x2="11" y2="13" />
            <polygon points="22 2 15 22 11 13 2 9 22 2" />
          </svg>
          <span>发送</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { apiAskTutor } from '../../api/index.js'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const messagesRef = ref(null)
const messages = ref([
  {
    role: 'ai',
    content: '你好！我是你的AI学伴，有什么学习问题可以问我。',
    html: DOMPurify.sanitize(marked.parse('你好！我是你的AI学伴，有什么学习问题可以问我。'))
  }
])
const input = ref('')
const loading = ref(false)

function renderMarkdown(text) {
  return DOMPurify.sanitize(marked.parse(text))
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

async function send() {
  if (!input.value.trim() || loading.value) return

  const userMsg = input.value.trim()
  input.value = ''

  messages.value.push({ role: 'user', content: userMsg })
  scrollToBottom()

  loading.value = true

  try {
    const res = await apiAskTutor({ question: userMsg })
    const answer = res.data?.answer || res.data?.content || res.data?.markdown || ''
    const reply = answer || '抱歉，我没有理解这个问题。'
    messages.value.push({
      role: 'ai',
      content: reply,
      html: renderMarkdown(reply)
    })
  } catch {
    messages.value.push({
      role: 'ai',
      content: '网络错误，请稍后再试。',
      html: renderMarkdown('网络错误，请稍后再试。')
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function clearConversation() {
  messages.value = [
    {
      role: 'ai',
      content: '你好！我是你的AI学伴，有什么学习问题可以问我。',
      html: renderMarkdown('你好！我是你的AI学伴，有什么学习问题可以问我。')
    }
  ]
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped lang="scss">
@use '../../styles/variables' as *;

.ai-companion-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 24px;
  background: transparent;
}

/* ── Header ── */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-base);
  flex-shrink: 0;
  background: transparent;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.header-info {
  .header-title {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    color: var(--text-main);
    line-height: 1.3;
  }
  .header-status {
    font-size: 12px;
    color: var(--text-sub);
  }
}

.clear-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 10px;
  border: 1px solid var(--border-base);
  background: transparent;
  color: var(--text-sub);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: rgba(239, 68, 68, 0.3);
    color: #ef4444;
    background: rgba(239, 68, 68, 0.06);
  }
}

/* ── Messages ── */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  scroll-behavior: smooth;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: var(--border-base);
    border-radius: 2px;
  }
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 80%;

  &.ai {
    align-self: flex-start;
  }

  &.user {
    align-self: flex-end;
    flex-direction: row-reverse;
  }
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;

  &.ai-avatar {
    background: rgba(59, 130, 246, 0.15);
    border: 1px solid rgba(59, 130, 246, 0.2);
    color: #3b82f6;
  }

  &.user-avatar {
    background: rgba(16, 185, 129, 0.15);
    border: 1px solid rgba(16, 185, 129, 0.2);
    color: #10b981;
  }
}

.msg-bubble {
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;

  &.ai {
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid var(--border-base);
    border-bottom-left-radius: 4px;
    color: var(--text-main);
  }

  &.user {
    background: rgba(59, 130, 246, 0.15);
    border: 1px solid rgba(59, 130, 246, 0.2);
    border-bottom-right-radius: 4px;
    color: var(--text-main);
  }
}

.msg-content {
  :deep(p) {
    margin: 0 0 8px;
    &:last-child { margin-bottom: 0; }
  }

  :deep(code) {
    background: rgba(255, 255, 255, 0.08);
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'JetBrains Mono', 'Fira Code', monospace;
    font-size: 13px;
  }

  :deep(pre) {
    background: rgba(0, 0, 0, 0.3);
    border: 1px solid var(--border-base);
    border-radius: 10px;
    padding: 14px 16px;
    overflow-x: auto;
    margin: 8px 0;

    code {
      background: none;
      padding: 0;
      border-radius: 0;
      font-size: 13px;
      line-height: 1.6;
    }
  }

  :deep(ul), :deep(ol) {
    padding-left: 20px;
    margin: 6px 0;
  }

  :deep(li) {
    margin: 4px 0;
  }

  :deep(strong) {
    font-weight: 600;
  }

  :deep(a) {
    color: var(--primary-light);
    text-decoration: underline;
  }

  :deep(blockquote) {
    border-left: 3px solid var(--primary);
    padding-left: 12px;
    margin: 8px 0;
    color: var(--text-sub);
  }

  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    margin: 12px 0 6px;
    font-weight: 600;
  }

  :deep(h1) { font-size: 18px; }
  :deep(h2) { font-size: 16px; }
  :deep(h3) { font-size: 15px; }
}

/* Loading indicator */
.loading-bubble {
  min-width: 80px;
}

.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 2px;
  color: var(--text-sub);
  font-size: 14px;
}

.dots {
  display: inline-flex;
}

.dot {
  animation: pulse-dot 1.4s infinite;
  font-size: 18px;
  line-height: 1;
  font-weight: 700;

  &:nth-child(2) { animation-delay: 0.2s; }
  &:nth-child(3) { animation-delay: 0.4s; }
}

@keyframes pulse-dot {
  0%, 60%, 100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  30% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

/* ── Input Area ── */
.input-area {
  flex-shrink: 0;
  padding: 16px 24px;
  border-top: 1px solid var(--border-base);
  background: transparent;
}

.input-wrapper {
  display: flex;
  gap: 10px;
  align-items: center;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-base);
  border-radius: 14px;
  padding: 4px;
  transition: border-color 0.2s;

  &:focus-within {
    border-color: rgba(59, 130, 246, 0.4);
  }
}

.chat-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 10px 14px;
  font-size: 14px;
  color: var(--text-main);
  outline: none;
  font-family: inherit;

  &::placeholder {
    color: var(--text-faint);
  }
}

.send-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 10px;
  border: none;
  background: rgba(59, 130, 246, 0.15);
  color: var(--text-sub);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &.active {
    background: var(--primary);
    color: #fff;
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);

    &:hover {
      background: var(--primary-deep);
    }
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

/* ── Responsive ── */
@media (max-width: 768px) {
  .chat-header {
    padding: 12px 16px;
  }

  .messages-area {
    padding: 16px;
  }

  .msg-row {
    max-width: 90%;
  }

  .input-area {
    padding: 12px 16px;
  }
}
</style>
