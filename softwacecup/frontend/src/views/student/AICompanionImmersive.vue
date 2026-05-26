<template>
  <div class="immersive-page">
    <!-- Unity 容器（左侧） -->
    <div class="unity-panel" :class="{ collapsed: unityLoadFailed }">
      <div v-if="unityLoadFailed" class="unity-fallback">
        <div class="fallback-icon">&#9888;&#65039;</div>
        <p class="fallback-text">3D 虚拟人加载失败</p>
        <button class="retry-btn" @click="retryUnity">
          重新加载
        </button>
        <p class="fallback-hint">文字对话功能仍然可用</p>
      </div>
      <div v-else id="unity-container" class="unity-desktop">
        <canvas id="unity-canvas" tabindex="-1"></canvas>
        <div id="unity-loading-bar">
          <div id="unity-logo"></div>
          <div id="unity-progress-bar-empty">
            <div id="unity-progress-bar-full"></div>
          </div>
        </div>
        <div id="unity-warning"></div>
        <div id="unity-footer">
          <div id="unity-webgl-logo"></div>
          <div id="unity-fullscreen-button"></div>
          <div id="unity-build-title">AI助教-小慧</div>
        </div>
      </div>
    </div>

    <!-- 对话面板（右侧） -->
    <div class="chat-panel" :class="{ expanded: unityLoadFailed }">
      <header class="chat-header">
        <div class="header-left">
          <span class="ai-icon">AI</span>
          <div class="header-info">
            <h2 class="header-title">AI 学习助手 · 小慧</h2>
            <span class="header-status" v-if="unityLoaded">在线 · 3D 虚拟人已就绪</span>
            <span class="header-status muted" v-else-if="!unityLoadFailed">3D 虚拟人加载中...</span>
            <span class="header-status muted" v-else>随时为你解答</span>
          </div>
        </div>
        <button class="clear-btn" @click="clearConversation">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6" />
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
          </svg>
          清空对话
        </button>
      </header>

      <div class="messages-area" ref="messagesRef">
        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          :class="['msg-row', msg.role]"
        >
          <div v-if="msg.role === 'ai'" class="msg-avatar ai-avatar">AI</div>
          <div :class="['msg-bubble', msg.role]">
            <div v-if="msg.role === 'ai'" class="msg-content" v-html="msg.html"></div>
            <div v-else class="msg-content">{{ msg.content }}</div>
          </div>
          <div v-if="msg.role === 'user'" class="msg-avatar user-avatar">我</div>
        </div>

        <div v-if="loading" class="msg-row ai">
          <div class="msg-avatar ai-avatar">AI</div>
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

      <div class="input-area">
        <div class="input-wrapper">
          <button
            class="voice-btn"
            :class="{ recording: isRecording }"
            @click="isRecording ? stopRecording() : startRecording()"
            :disabled="loading"
            :title="isRecording ? '停止录音' : '语音输入'"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
              <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
              <line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/>
            </svg>
          </button>
          <input
            v-model="input"
            class="chat-input"
            placeholder="输入你的问题或指令..."
            @keydown.enter.prevent="sendMessage"
            :disabled="loading"
            maxlength="2000"
          />
          <button
            class="send-btn"
            :class="{ active: input.trim() }"
            :disabled="!input.trim() || loading"
            @click="sendMessage"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="22" y1="2" x2="11" y2="13" />
              <polygon points="22 2 15 22 11 13 2 9 22 2" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { apiAgentChatStream, apiTTS } from '../../api/index.js'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const router = useRouter()

const unityLoaded = ref(false)
const unityLoadFailed = ref(false)
const unityCanvasWidth = 360
const unityCanvasHeight = 500
let UnityIns = null
let RecorderIns = null

const messagesRef = ref(null)
const messages = ref([
  {
    role: 'ai',
    content: '你好！我是虚拟教学助手小慧，有什么可以帮你的吗？',
    html: DOMPurify.sanitize(marked.parse('你好！我是虚拟教学助手小慧，有什么可以帮你的吗？'))
  }
])
const input = ref('')
const loading = ref(false)
const isRecording = ref(false)

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

const navCommands = {
  '打开课程平台': '/student/courses',
  '打开学习分析': '/student/analytics',
  '打开我的考试': '/student/exam',
  '打开问答广场': '/student/community',
  '打开个人资料': '/student/profile',
  '打开学习首页': '/student/dashboard',
  '打开沉浸伴学': '/student/companion-immersive',
}

function handleNavigation(command) {
  const target = navCommands[command]
  const label = command.replace('打开', '')
  if (target) {
    messages.value.push({
      role: 'ai',
      content: '好的，正在跳转到' + label + '...',
      html: renderMarkdown('好的，正在跳转到 **' + label + '**...')
    })
    scrollToBottom()
    setTimeout(() => router.push(target), 800)
  } else {
    messages.value.push({
      role: 'ai',
      content: '抱歉，我暂时无法执行"' + command + '"指令。',
      html: renderMarkdown('抱歉，我暂时无法执行"' + command + '"指令。')
    })
    scrollToBottom()
  }
}

function parseStructuredReply(raw) {
  const parts = { emotion: '无', action: '无', text: '', command: '无' }
  const fields = raw.split('|')
  for (const field of fields) {
    const trimmed = field.trim()
    if (trimmed.startsWith('表情：') || trimmed.startsWith('表情:')) {
      parts.emotion = trimmed.replace(/^表情[：:]\s*/, '')
    } else if (trimmed.startsWith('动作：') || trimmed.startsWith('动作:')) {
      parts.action = trimmed.replace(/^动作[：:]\s*/, '')
    } else if (trimmed.startsWith('回复文本：') || trimmed.startsWith('回复文本:')) {
      parts.text = trimmed.replace(/^回复文本[：:]\s*/, '')
    } else if (trimmed.startsWith('指令：') || trimmed.startsWith('指令:')) {
      parts.command = trimmed.replace(/^指令[：:]\s*/, '')
    }
  }
  if (!parts.text && raw) {
    parts.text = raw
  }
  return parts
}

async function sendMessage() {
  const text = input.value.trim()
  if (!text || loading.value) return

  input.value = ''
  messages.value.push({ role: 'user', content: text })
  scrollToBottom()
  await sendToLLM(text)
}

async function sendToLLM(text) {
  loading.value = true

  try {
    const response = await apiAgentChatStream({
      question: text,
      history: messages.value.slice(-10).map(m => ({
        role: m.role === 'ai' ? 'assistant' : 'user',
        content: m.content
      }))
    })

    if (!response.ok) throw new Error('SSE connection failed')

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let fullText = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''

      for (const raw of events) {
        const lines = raw.split('\n')
        const dataLines = []
        for (const line of lines) {
          if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
        }
        const payload = dataLines.join('\n')
        if (!payload) continue

        try {
          const parsed = JSON.parse(payload)
          if (parsed.delta) fullText += parsed.delta
          if (parsed.finish) {
            const parts = parseStructuredReply(fullText)
            messages.value.push({
              role: 'ai',
              content: parts.text,
              html: renderMarkdown(parts.text)
            })
            scrollToBottom()

            if (parts.command && parts.command !== '无') {
              handleNavigation(parts.command)
            }

            if (UnityIns && unityLoaded.value) {
              try {
                UnityIns.SendMessage('ChatManager', 'PlayEmotion', parts.emotion)
                UnityIns.SendMessage('ChatManager', 'PlayAction', parts.action)
              } catch (e) { /* Unity 通信失败，静默降级 */ }
            }

            if (parts.text) {
              sendTTS(parts.text)
            }
          }
        } catch (e) { /* 跳过解析失败的行 */ }
      }
    }
  } catch (e) {
    messages.value.push({
      role: 'ai',
      content: '小慧暂时不在线，请稍后重试。',
      html: renderMarkdown('小慧暂时不在线，请稍后重试。')
    })
    scrollToBottom()
  } finally {
    loading.value = false
  }
}

async function sendTTS(text) {
  try {
    const response = await apiTTS(text)
    if (response.ok) {
      const audioBlob = await response.blob()
      const audioUrl = URL.createObjectURL(audioBlob)
      const audio = new Audio(audioUrl)
      audio.play().catch(() => { /* 自动播放被阻止 */ })
      if (UnityIns && unityLoaded.value) {
        try {
          UnityIns.SendMessage('ChatManager', 'PlayAudio', audioUrl)
        } catch (e) { /* 静默降级 */ }
      }
    }
  } catch (e) {
    /* TTS 合成失败，静默模式 */
  }
}

function startRecording() {
  if (!RecorderIns) {
    initRecorder()
  }
  try {
    RecorderIns.recStart()
    isRecording.value = true
  } catch (e) {
    console.error('录音启动失败:', e)
  }
}

function stopRecording() {
  isRecording.value = false
  RecorderIns.recStop()
}

function initRecorder() {
  const defaultOpt = {
    serviceCode: 'asr_aword',
    audioFormat: 'wav',
    sampleRate: 16000,
    sampleBit: 16,
    audioChannels: 1,
    bitRate: 96000,
    audioData: null,
    punctuation: 'true',
    model: null,
    intermediateResult: null,
    maxStartSilence: null,
    maxEndSilence: null,
  }

  RecorderIns = Recorder({
    type: 'wav',
    sampleRate: defaultOpt.sampleRate,
    bitRate: parseInt(defaultOpt.bitRate / 1000) || 16,
    onProcess(buffers, powerLevel, bufferDuration, bufferSampleRate) {
      const LEN = 59 * 1000
      if (bufferDuration > LEN) {
        RecorderIns.recStop()
      }
    },
  })

  RecorderIns.open(
    () => { /* 麦克风就绪 */ },
    (msg, isUserNotAllow) => {
      console.log((isUserNotAllow ? '用户拒绝授权：' : '') + msg)
    }
  )
}

function clearConversation() {
  messages.value = [
    {
      role: 'ai',
      content: '你好！我是虚拟教学助手小慧，有什么可以帮你的吗？',
      html: renderMarkdown('你好！我是虚拟教学助手小慧，有什么可以帮你的吗？')
    }
  ]
}

// ========== Unity WebGL 初始化 ==========
let container = null
let canvas = null
let loadingBar = null
let progressBarFull = null
let fullscreenButton = null
let warningBanner = null
let unityLoadTimer = null

const buildUrl = '/unity'
const loaderUrl = buildUrl + '/AI-Assistant(WebGL).loader.js'
const config = {
  dataUrl: buildUrl + '/AI-Assistant(WebGL).data',
  frameworkUrl: buildUrl + '/AI-Assistant(WebGL).framework.js',
  codeUrl: buildUrl + '/AI-Assistant(WebGL).wasm',
  streamingAssetsUrl: 'StreamingAssets',
  companyName: 'DefaultCompany',
  productName: 'AI-Assistant(WebGL)',
  productVersion: '0.1.0',
}

function retryUnity() {
  unityLoadFailed.value = false
  unityLoaded.value = false
  initUnity()
}

function initUnity() {
  container = document.querySelector('#unity-container')
  canvas = document.querySelector('#unity-canvas')
  loadingBar = document.querySelector('#unity-loading-bar')
  progressBarFull = document.querySelector('#unity-progress-bar-full')
  fullscreenButton = document.querySelector('#unity-fullscreen-button')
  warningBanner = document.querySelector('#unity-warning')

  if (!container || !canvas) {
    unityLoadFailed.value = true
    return
  }

  loadingBar.style.display = 'block'

  if (/iPhone|iPad|iPod|Android/i.test(navigator.userAgent)) {
    const meta = document.createElement('meta')
    meta.name = 'viewport'
    meta.content = 'width=device-width, height=device-height, initial-scale=1.0, user-scalable=no, shrink-to-fit=yes'
    document.getElementsByTagName('head')[0].appendChild(meta)
    container.className = 'unity-mobile'
    canvas.className = 'unity-mobile'
  } else {
    canvas.style.width = unityCanvasWidth + 'px'
    canvas.style.height = unityCanvasHeight + 'px'
  }

  unityLoadTimer = setTimeout(() => {
    if (!unityLoaded.value) {
      unityLoadFailed.value = true
      loadingBar.style.display = 'none'
    }
  }, 30000)

  const script = document.createElement('script')
  script.src = loaderUrl
  script.onload = () => {
    createUnityInstance(canvas, config, (progress) => {
      progressBarFull.style.width = 100 * progress + '%'
    }).then((unityInstance) => {
      clearTimeout(unityLoadTimer)
      loadingBar.style.display = 'none'
      fullscreenButton.onclick = () => {
        unityInstance.SetFullscreen(1)
      }
      UnityIns = unityInstance
      unityLoaded.value = true
      initRecorder()
    }).catch((message) => {
      clearTimeout(unityLoadTimer)
      unityLoadFailed.value = true
      loadingBar.style.display = 'none'
    })
  }
  script.onerror = () => {
    clearTimeout(unityLoadTimer)
    unityLoadFailed.value = true
    loadingBar.style.display = 'none'
  }
  document.body.appendChild(script)
}

onMounted(() => {
  initUnity()
  scrollToBottom()
})

onBeforeUnmount(() => {
  clearTimeout(unityLoadTimer)
  if (RecorderIns) {
    try { RecorderIns.close() } catch (e) { /* ignore */ }
  }
})
</script>

<style scoped lang="scss">
@use '../../styles/variables' as *;

.immersive-page {
  display: flex;
  height: calc(100vh - 56px);
  background: transparent;
  overflow: hidden;
}

/* ── Unity Panel ── */
.unity-panel {
  width: 400px;
  flex-shrink: 0;
  border-right: 1px solid var(--border-base);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;

  &.collapsed {
    width: 0;
    border-right: none;
    overflow: hidden;
  }
}

.unity-desktop {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

#unity-canvas { background: transparent; }

#unity-loading-bar {
  position: absolute;
  bottom: 60px;
  left: 50%;
  transform: translateX(-50%);
  width: 200px;
}

#unity-progress-bar-empty {
  width: 100%;
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
}

#unity-progress-bar-full {
  height: 100%;
  width: 0%;
  background: linear-gradient(90deg, #3b82f6, #06b6d4);
  border-radius: 2px;
  transition: width 0.3s;
}

#unity-footer {
  position: absolute;
  bottom: 12px;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

#unity-build-title {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.3);
}

#unity-fullscreen-button {
  cursor: pointer;
  width: 24px;
  height: 24px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

.unity-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px;
  text-align: center;
}

.fallback-icon { font-size: 40px; }
.fallback-text { color: var(--text-sub); font-size: 14px; }
.fallback-hint { color: var(--text-faint); font-size: 12px; }

.retry-btn {
  padding: 8px 20px;
  border-radius: 8px;
  border: 1px solid var(--border-base);
  background: rgba(59, 130, 246, 0.15);
  color: var(--text-main);
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
  &:hover { background: rgba(59, 130, 246, 0.25); }
}

/* ── Chat Panel ── */
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 400px;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-base);
  flex-shrink: 0;
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

.header-info .header-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
  line-height: 1.3;
}

.header-status {
  font-size: 12px;
  color: #10b981;
  &.muted { color: var(--text-faint); }
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
  &:hover { border-color: rgba(239, 68, 68, 0.3); color: #ef4444; background: rgba(239, 68, 68, 0.06); }
}

/* Messages */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: var(--border-base); border-radius: 2px; }
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 80%;

  &.ai { align-self: flex-start; }
  &.user { align-self: flex-end; flex-direction: row-reverse; }
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

  &.ai-avatar { background: rgba(59, 130, 246, 0.15); border: 1px solid rgba(59, 130, 246, 0.2); color: #3b82f6; }
  &.user-avatar { background: rgba(16, 185, 129, 0.15); border: 1px solid rgba(16, 185, 129, 0.2); color: #10b981; }
}

.msg-bubble {
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;

  &.ai { background: rgba(255, 255, 255, 0.04); border: 1px solid var(--border-base); border-bottom-left-radius: 4px; color: var(--text-main); }
  &.user { background: rgba(59, 130, 246, 0.15); border: 1px solid rgba(59, 130, 246, 0.2); border-bottom-right-radius: 4px; color: var(--text-main); }
}

.msg-content {
  :deep(p) { margin: 0 0 8px; &:last-child { margin-bottom: 0; } }
  :deep(code) { background: rgba(255, 255, 255, 0.08); padding: 2px 6px; border-radius: 4px; font-size: 13px; }
  :deep(pre) {
    background: rgba(0, 0, 0, 0.3); border: 1px solid var(--border-base); border-radius: 10px; padding: 14px 16px; overflow-x: auto; margin: 8px 0;
    code { background: none; padding: 0; }
  }
  :deep(ul), :deep(ol) { padding-left: 20px; margin: 6px 0; }
  :deep(strong) { font-weight: 600; }
  :deep(a) { color: var(--primary-light); }
  :deep(blockquote) { border-left: 3px solid var(--primary); padding-left: 12px; margin: 8px 0; color: var(--text-sub); }
}

.loading-bubble { min-width: 80px; }

.thinking-indicator {
  display: flex; align-items: center; gap: 2px;
  color: var(--text-sub); font-size: 14px;
}

.dots { display: inline-flex; }
.dot {
  animation: pulse-dot 1.4s infinite; font-size: 18px; font-weight: 700;
  &:nth-child(2) { animation-delay: 0.2s; }
  &:nth-child(3) { animation-delay: 0.4s; }
}

@keyframes pulse-dot {
  0%, 60%, 100% { opacity: 0.3; }
  30% { opacity: 1; }
}

/* Input */
.input-area {
  flex-shrink: 0;
  padding: 16px 24px;
  border-top: 1px solid var(--border-base);
}

.input-wrapper {
  display: flex;
  gap: 10px;
  align-items: center;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-base);
  border-radius: 14px;
  padding: 4px;
  &:focus-within { border-color: rgba(59, 130, 246, 0.4); }
}

.chat-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 10px 6px;
  font-size: 14px;
  color: var(--text-main);
  outline: none;
  font-family: inherit;
  &::placeholder { color: var(--text-faint); }
}

.voice-btn {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  border: 1px solid var(--border-base);
  background: transparent;
  color: var(--text-sub);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;

  &:hover:not(:disabled) { background: rgba(255, 255, 255, 0.05); color: var(--text-main); }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
  &.recording { background: rgba(239, 68, 68, 0.2); border-color: rgba(239, 68, 68, 0.4); color: #ef4444; animation: pulse-rec 1.5s ease-in-out infinite; }
}

@keyframes pulse-rec {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(239, 68, 68, 0); }
}

.send-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  border: none;
  background: rgba(59, 130, 246, 0.15);
  color: var(--text-sub);
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;

  &.active { background: var(--primary); color: #fff; box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3); &:hover { background: var(--primary-deep); } }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}

@media (max-width: 768px) {
  .immersive-page { flex-direction: column; }
  .unity-panel { width: 100%; height: 300px; }
  .chat-panel { min-width: 0; }
}
</style>
