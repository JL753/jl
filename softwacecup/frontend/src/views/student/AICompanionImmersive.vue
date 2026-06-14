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
            <div v-if="msg.role === 'ai'" class="msg-content" v-html="msg.html || msg.content"></div>
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
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useChatStore } from '../../stores/chat'
import { apiAgentChatStream } from '../../api/index.js'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const router = useRouter()
const chatStore = useChatStore()

const unityLoaded = ref(false)
const unityLoadFailed = ref(false)
const unityCanvasWidth = 360
const unityCanvasHeight = 500
let UnityIns = null

const messagesRef = ref(null)
const messages = computed(() => chatStore.messages.map(m => ({
  ...m,
  role: m.role === 'assistant' ? 'ai' : m.role
})))
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
    chatStore.addAssistantMessage(
      '好的，正在跳转到' + label + '...',
      renderMarkdown('好的，正在跳转到 **' + label + '**...')
    )
    scrollToBottom()
    setTimeout(() => router.push(target), 800)
  } else {
    chatStore.addAssistantMessage(
      '抱歉，我暂时无法执行"' + command + '"指令。',
      renderMarkdown('抱歉，我暂时无法执行"' + command + '"指令。')
    )
    scrollToBottom()
  }
}

function detectEmotion(text) {
  if (!text) return '开心'
  if (/哈哈|嘿嘿|好笑|有趣|笑|😄|😂|🤣|好玩/.test(text)) return '开心'
  if (/难过|伤心|哭|😢|😭|遗憾|抱歉/.test(text)) return '难过'
  if (/惊讶|天啊|居然|没想到|😲|😯|真的吗/.test(text)) return '惊讶'
  if (/生气|愤怒|可恶|讨厌|😡/.test(text)) return '生气'
  if (/困惑|不懂|为什么|什么意思|不明白/.test(text)) return '困惑'
  if (/厉害|优秀|很棒|不错|赞|👍/.test(text)) return '有趣'
  return '开心'
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
  // LLM 未按结构化格式输出时，智能推断表情和动作
  if (parts.emotion === '无') {
    parts.emotion = detectEmotion(parts.text)
  }
  if (parts.action === '无') {
    parts.action = '右手放胸前'
  }
  return parts
}

async function sendMessage() {
  const text = input.value.trim()
  if (!text || loading.value) return

  input.value = ''
  chatStore.addUserMessage(text)
  scrollToBottom()
  await sendToLLM(text)
}

async function sendToLLM(text) {
  loading.value = true

  try {
    const response = await apiAgentChatStream({
      question: text,
      sessionId: chatStore.sessionId,
      history: chatStore.messages.slice(-10).map(m => ({
        role: m.role === 'assistant' ? 'assistant' : 'user',
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
            const text = parts.text || '小慧收到了你的消息，但暂时无法给出回复，请稍后再试。'
            chatStore.addAssistantMessage(text, renderMarkdown(text))
            scrollToBottom()

            console.log('[小慧] 回复解析:', 'emotion=' + parts.emotion, 'action=' + parts.action, 'text长度=' + parts.text.length)

            if (parts.command && parts.command !== '无') {
              handleNavigation(parts.command)
            }

            if (parts.text) {
              speakText(parts.text)
            }
          }
        } catch (e) { /* 跳过解析失败的行 */ }
      }
    }
  } catch (e) {
    chatStore.addAssistantMessage('小慧暂时不在线，请稍后重试。', renderMarkdown('小慧暂时不在线，请稍后重试。'))
    scrollToBottom()
  } finally {
    loading.value = false
  }
}

// ========== TTS 语音合成（Web Speech API 语音 + 模拟音频给 Unity 驱动口型） ==========

/**
 * 生成模拟语音振幅的 WAV 文件
 * Unity 通过 AudioSource.GetOutputData 分析振幅来驱动口型 blend shape，
 * 不需要真实语音内容，只需要振幅随时间变化即可。
 */
function buildLipSyncWav(text) {
  const sampleRate = 16000
  // 估算时长：中文约 4 字/秒，加 500ms 余量
  const charsPerSec = 4.0
  const durationMs = Math.max(2000, (text.length / charsPerSec) * 1000 + 500)
  const totalSamples = Math.floor(sampleRate * durationMs / 1000)
  const dataSize = totalSamples * 2  // 16-bit PCM = 2 bytes per sample
  const fileSize = 44 + dataSize
  const buf = new ArrayBuffer(fileSize)
  const v = new DataView(buf)

  // RIFF header
  const writeStr = (off, s) => { for (let i = 0; i < s.length; i++) v.setUint8(off + i, s.charCodeAt(i)) }
  writeStr(0, 'RIFF'); v.setUint32(4, fileSize - 8, true); writeStr(8, 'WAVE')
  writeStr(12, 'fmt '); v.setUint32(16, 16, true); v.setUint16(20, 1, true)    // PCM
  v.setUint16(22, 1, true)       // mono
  v.setUint32(24, sampleRate, true)
  v.setUint32(28, sampleRate * 2, true)  // byte rate
  v.setUint16(32, 2, true)       // block align
  v.setUint16(34, 16, true)      // bits per sample
  writeStr(36, 'data'); v.setUint32(40, dataSize, true)

  // 生成模拟语音振幅：以词为单位交替高低振幅
  const totalMs = durationMs
  const wordCount = Math.max(6, Math.floor(text.length / 2))
  const words = []
  for (let i = 0; i < wordCount; i++) {
    const startMs = (totalMs / wordCount) * i
    const endMs = startMs + (totalMs / wordCount) * (0.5 + Math.random() * 0.4)
    words.push({ startMs, endMs })
  }

  for (let si = 0; si < totalSamples; si++) {
    const tMs = (si / sampleRate) * 1000
    let amplitude = 0.03  // 静音基线
    for (const w of words) {
      if (tMs >= w.startMs && tMs <= w.endMs) {
        const localT = tMs - w.startMs
        const wordLen = w.endMs - w.startMs
        const envelope = Math.sin((localT / wordLen) * Math.PI)
        // 振幅控制在可检测但较安静的范围，Web Speech API 朗读为主
        amplitude = envelope * 0.35 * (1 + 0.6 * Math.sin(localT * 0.08) + 0.3 * Math.sin(localT * 0.17))
        break
      }
    }
    const sample = Math.floor(Math.max(-1, Math.min(1, amplitude)) * 32767)
    v.setInt16(44 + si * 2, sample, true)
  }

  return new Blob([buf], { type: 'audio/wav' })
}

function speakText(text) {
  if (!text || typeof window === 'undefined') return
  const synth = window.speechSynthesis
  if (!synth) return

  synth.cancel()

  // 口型驱动：从 uLipSync 节点自身获取其 AudioContext（而非我们包装器截获的），
  // 确保 source 与节点属于同一 AudioContext，避免 "cannot connect to a different audio context" 错误
  const uLipSyncNode = window.__uLipSyncNode
  if (uLipSyncNode) {
    const ctx = uLipSyncNode.context
    if (ctx && ctx.state !== 'closed') {
      if (ctx.state === 'suspended') {
        ctx.resume().catch(() => {})
      }
      try {
        const wavBlob = buildLipSyncWav(text)
        wavBlob.arrayBuffer().then(buf => {
          ctx.decodeAudioData(buf, (audioBuffer) => {
            const source = ctx.createBufferSource()
            source.buffer = audioBuffer
            source.connect(uLipSyncNode)
            source.start()
          }, (err) => {
            console.warn('[小慧口型] 音频解码失败:', err)
          })
        }).catch(e => console.warn('[小慧口型] WAV 读取失败:', e))
      } catch (e) {
        console.warn('[小慧口型] 口型注入失败:', e)
      }
    }
  }

  // Web Speech API 朗读（用户听到的声音）
  const utter = new SpeechSynthesisUtterance(text)
  utter.lang = 'zh-CN'
  utter.rate = 1.1
  utter.pitch = 1.05
  utter.volume = 0.9

  const voices = synth.getVoices()
  const preferred = voices.find(v => v.lang === 'zh-CN' && v.name.includes('Tingting'))
    || voices.find(v => v.lang.startsWith('zh'))
    || voices.find(v => v.lang.startsWith('en'))
  if (preferred) utter.voice = preferred

  synth.speak(utter)
}

// 确保 voices 加载完毕（Chrome 需要异步）
if (typeof window !== 'undefined' && window.speechSynthesis) {
  window.speechSynthesis.getVoices()
  window.speechSynthesis.onvoiceschanged = () => { window.speechSynthesis.getVoices() }
}

// ========== STT 语音识别（Web Speech API） ==========
let recognition = null
let preRecordingInput = ''  // 录音前输入框已有内容

function initSpeechRecognition() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) return null

  const rec = new SpeechRecognition()
  rec.lang = 'zh-CN'
  rec.interimResults = true
  rec.maxAlternatives = 1
  rec.continuous = true

  rec.onresult = (event) => {
    let interim = ''
    let finalText = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      const transcript = event.results[i][0].transcript
      if (event.results[i].isFinal) {
        finalText += transcript
      } else {
        interim += transcript
      }
    }
    // 保留录音前的手动输入，追加识别结果。不删除空格
    input.value = preRecordingInput + finalText + interim
  }

  rec.onerror = (event) => {
    console.warn('语音识别错误:', event.error)
    if (event.error === 'no-speech' || event.error === 'aborted') {
      // 正常情况，忽略
    } else {
      ElMessage.error('语音识别出错：' + event.error)
    }
    isRecording.value = false
  }

  rec.onend = () => {
    isRecording.value = false
  }

  return rec
}

function startRecording() {
  if (!recognition) {
    recognition = initSpeechRecognition()
  }
  if (!recognition) {
    ElMessage.warning('当前浏览器不支持语音识别，请使用 Chrome')
    return
  }
  try {
    preRecordingInput = input.value  // 保存录音前用户已输入的内容
    recognition.start()
    isRecording.value = true
  } catch (e) {
    // 可能已经在运行
    isRecording.value = false
  }
}

function stopRecording() {
  if (recognition) {
    try { recognition.stop() } catch (e) {}
  }
  isRecording.value = false
}

function clearConversation() {
  chatStore.newSession()
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

async function initUnity() {
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

  // 策略：标记 uLipSync 的 ScriptProcessorNode → 截获 connect() 捕获该节点 → speakText 时把音频直接注入它
  const OrigAudioContext = window.AudioContext || window.webkitAudioContext
  if (OrigAudioContext) {
    // 1. 拦截 createScriptProcessor：直接捕获 uLipSync 节点（它是在 Unity AudioContext 上创建的第一个 ScriptProcessorNode）
    const origCSP = OrigAudioContext.prototype.createScriptProcessor
    OrigAudioContext.prototype.createScriptProcessor = function () {
      const node = origCSP.apply(this, arguments)
      if (!window.__uLipSyncNode && this === window.__unityAudioCtx) {
        window.__uLipSyncNode = node
      }
      return node
    }

    // 2. 拦截 AudioContext 构造函数，捕获 Unity 的音频上下文
    window.AudioContext = function () {
      const ctx = new OrigAudioContext(...arguments)
      window.__unityAudioCtx = ctx
      return ctx
    }
    window.AudioContext.prototype = OrigAudioContext.prototype
    if (window.webkitAudioContext) {
      window.webkitAudioContext = window.AudioContext
    }
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

onMounted(async () => {
  await chatStore.loadHistory()
  // 无历史记录时使用伴学专用欢迎语
  chatStore.replaceWelcome(
    '你好！我是虚拟教学助手小慧，有什么可以帮你的吗？',
    renderMarkdown('你好！我是虚拟教学助手小慧，有什么可以帮你的吗？')
  )
  initUnity()
  scrollToBottom()
})

onBeforeUnmount(() => {
  clearTimeout(unityLoadTimer)
  if (recognition) {
    try { recognition.abort() } catch (e) { /* ignore */ }
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
