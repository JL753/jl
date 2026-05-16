# Unity AI 虚拟人前端移植 + Java 后端适配 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将源项目的 Unity WebGL 虚拟人"小慧"移植到 softwacecup 学生端，新建 AICompanionImmersive 页面，后端新增 chat-stream/STT/TTS 端点。

**Architecture:** 渐进增强——保留现有 AICompanionView，新增 AICompanionImmersive（左 Unity + 右对话面板）。前端复用 LLMClient.chatStream 方法，后端在 AgentController 新增 3 个端点。

**Tech Stack:** Vue 3 + SCSS + Java Spring Boot 3.2 + OkHttp + 讯飞 LLM

---

### Task 1: 复制 Unity Build 文件及录音脚本

**Files:**
- Create: `frontend/public/unity/AI-Assistant(WebGL).data`
- Create: `frontend/public/unity/AI-Assistant(WebGL).framework.js`
- Create: `frontend/public/unity/AI-Assistant(WebGL).loader.js`
- Create: `frontend/public/unity/AI-Assistant(WebGL).wasm`
- Create: `frontend/public/unity/recorder.wav.min.js`
- Create: `frontend/public/unity/unity.recorder.js`

- [ ] **Step 1: 创建目标目录并复制文件**

源文件位置在 `C:\Users\ZWC\Downloads\ai-assistant-teaching-website-main\ai-assistant-teaching-website-main\src\assets\static\Unity\` 下。录音脚本也在同目录的 `JavaScript/` 子目录下。

```bash
mkdir -p "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup\frontend\public\unity"

cp "C:\Users\ZWC\Downloads\ai-assistant-teaching-website-main\ai-assistant-teaching-website-main\src\assets\static\Unity\Build\AI-Assistant(WebGL).data" "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup\frontend\public\unity/"
cp "C:\Users\ZWC\Downloads\ai-assistant-teaching-website-main\ai-assistant-teaching-website-main\src\assets\static\Unity\Build\AI-Assistant(WebGL).framework.js" "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup\frontend\public\unity/"
cp "C:\Users\ZWC\Downloads\ai-assistant-teaching-website-main\ai-assistant-teaching-website-main\src\assets\static\Unity\Build\AI-Assistant(WebGL).loader.js" "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup\frontend\public\unity/"
cp "C:\Users\ZWC\Downloads\ai-assistant-teaching-website-main\ai-assistant-teaching-website-main\src\assets\static\Unity\Build\AI-Assistant(WebGL).wasm" "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup\frontend\public\unity/"
cp "C:\Users\ZWC\Downloads\ai-assistant-teaching-website-main\ai-assistant-teaching-website-main\src\assets\static\Unity\JavaScript\recorder.wav.min.js" "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup\frontend\public\unity/"
cp "C:\Users\ZWC\Downloads\ai-assistant-teaching-website-main\ai-assistant-teaching-website-main\src\assets\static\Unity\JavaScript\unity.recorder.js" "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup\frontend\public\unity/"
```

- [ ] **Step 2: 验证文件完整性**

```bash
ls -la "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup\frontend\public\unity/"
```

预期输出：6 个文件（.data, .framework.js, .loader.js, .wasm, recorder.wav.min.js, unity.recorder.js）

- [ ] **Step 3: Commit**

```bash
cd "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup"
git add frontend/public/unity/
git commit -m "feat: add Unity WebGL build files and recorder scripts"
```

---

### Task 2: 新增路由

**Files:**
- Modify: `frontend/src/router/index.js:29`

- [ ] **Step 1: 在 student children 路由中添加沉浸伴学路由**

编辑 `frontend/src/router/index.js`，在 `companion` 路由下方新增：

```javascript
{ path: 'companion-immersive', name: 'companion-immersive', component: () => import('../views/student/AICompanionImmersive.vue') },
```

具体位置在第 29 行（`companion` 路由）之后，`profile` 路由之前。最终 student children 数组为：

```javascript
children: [
  { path: '', redirect: '/student/dashboard' },
  { path: 'dashboard', name: 'student-dashboard', component: () => import('../views/student/StudentDashboard.vue') },
  { path: 'subjects', name: 'student-subjects', component: () => import('../views/common/SubjectCatalog.vue') },
  { path: 'subjects/:id', name: 'student-subject-detail', component: () => import('../views/common/CourseDetail.vue') },
  { path: 'courses/:id', name: 'student-course', component: () => import('../views/student/CourseView.vue') },
  { path: 'lessons/:id', name: 'student-lesson', component: () => import('../views/student/CourseView.vue') },
  { path: 'knowledge-map', name: 'student-knowledge-map', component: () => import('../views/student/KnowledgeStarMap.vue') },
  { path: 'companion', name: 'companion', component: () => import('../views/student/AICompanionView.vue') },
  { path: 'companion-immersive', name: 'companion-immersive', component: () => import('../views/student/AICompanionImmersive.vue') },
  { path: 'profile', name: 'student-profile', component: () => import('../views/common/ProfileView.vue') },
  { path: 'community', name: 'student-community', component: () => import('../views/student/CommunityView.vue') },
]
```

- [ ] **Step 2: Commit**

```bash
cd "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup"
git add frontend/src/router/index.js
git commit -m "feat: add companion-immersive route"
```

---

### Task 3: 新增前端 API 函数

**Files:**
- Modify: `frontend/src/api/index.js`

- [ ] **Step 1: 在 api/index.js 末尾添加三个 API 函数**

```javascript
/** 流式 AI 对话（Unity 虚拟人专用，SSE） */
export const apiAgentChatStream = ({ question, history }) => {
  const token = localStorage.getItem('sp_token') || ''
  return fetch('/api/agent/chat-stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ question, history: history || [] })
  })
}

/** 语音识别 */
export const apiSTT = (audioBlob) => {
  const formData = new FormData()
  formData.append('file', audioBlob, 'recording.wav')
  return http.post('/agent/stt', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 语音合成 */
export const apiTTS = (text) => {
  const token = localStorage.getItem('sp_token') || ''
  return fetch('/api/agent/tts', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ text })
  })
}
```

注意：`apiSTT` 需要 `multipart/form-data` 上传音频文件，不经过 http 拦截器（直接使用 axios），所以在文件顶部需要 `import axios from 'axios'`：

检查文件顶部，如果没有 `import axios from 'axios'`，则添加。

- [ ] **Step 2: Commit**

```bash
cd "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup"
git add frontend/src/api/index.js
git commit -m "feat: add STT, TTS, and chat-stream API functions"
```

---

### Task 4: 侧边栏新增"沉浸伴学"菜单项

**Files:**
- Modify: `frontend/src/components/LeftSideNav.vue:43-63`

- [ ] **Step 1: 在 navItems 数组中新增菜单项**

编辑 `frontend/src/components/LeftSideNav.vue`，在 `navItems` 数组的 AI 伴学项之后添加新项：

```javascript
{
  label: '沉浸',
  path: '/student/companion-immersive',
  iconSvg: '<circle cx="12" cy="12" r="10"/><circle cx="12" cy="10" r="4"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="M4.93 4.93l1.41 1.41"/><path d="M17.66 17.66l1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/>'
}
```

完整 navItems 数组变为：

```javascript
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
  },
  {
    label: '沉浸',
    path: '/student/companion-immersive',
    iconSvg: '<circle cx="12" cy="12" r="10"/><circle cx="12" cy="10" r="4"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="M4.93 4.93l1.41 1.41"/><path d="M17.66 17.66l1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/>'
  }
]
```

- [ ] **Step 2: Commit**

```bash
cd "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup"
git add frontend/src/components/LeftSideNav.vue
git commit -m "feat: add immersive companion nav item to sidebar"
```

---

### Task 5: 修改 AIFloatingBall 悬浮球，新增沉浸伴学跳转入口

**Files:**
- Modify: `frontend/src/components/AIFloatingBall.vue:82-95`

- [ ] **Step 1: 扩展导航映射，新增"沉浸伴学"关键词**

在 `AIFloatingBall.vue` 的 `navMap` 数组末尾新增一条：

```javascript
{ keywords: ['沉浸', '沉浸伴学', '3d伴学', '虚拟人', '虚拟教师'], path: '/student/companion-immersive' },
```

- [ ] **Step 2: 在对话欢迎语中提及沉浸伴学入口**

修改 `messages` 初始值的欢迎语，在第一行末尾增加提示：

```javascript
const messages = ref([
  {
    role: 'assistant',
    content: '你好！我是知域 AI 智能助手\n\n我可以回答学习问题，也可以帮你导航到对应页面。\n\n试试说：「跳转到课程平台」、「沉浸伴学」或直接提问！',
    html: '你好！我是知域 AI 智能助手<br><br>我可以回答学习问题，也可以帮你导航到对应页面。<br><br>试试说：「跳转到课程平台」、「沉浸伴学」或直接提问！'
  }
])
```

- [ ] **Step 3: Commit**

```bash
cd "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup"
git add frontend/src/components/AIFloatingBall.vue
git commit -m "feat: add immersive companion entry in AIFloatingBall"
```

---

### Task 6: 创建 AICompanionImmersive.vue 主页面

**Files:**
- Create: `frontend/src/views/student/AICompanionImmersive.vue`

这是核心任务，页面包含：左侧 Unity WebGL 容器 + 右侧对话面板 + Unity ↔ Vue 通信桥接。

- [ ] **Step 1: 编写 script setup 部分**

```javascript
<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { apiAgentChatStream, apiSTT, apiTTS } from '../../api/index.js'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const router = useRouter()

// ========== Unity 状态 ==========
const unityLoaded = ref(false)
const unityLoadFailed = ref(false)
const unityCanvasWidth = 360
const unityCanvasHeight = 500
let UnityIns = null
let RecorderIns = null

// ========== 对话状态 ==========
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

// ========== 语音状态 ==========
const isRecording = ref(false)

// ========== Markdown 渲染 ==========
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

// ========== Unity ↔ Vue 通信桥接 ==========
window.handleUnityTransmission = function (str) {
  console.log('[Unity → Vue]', str)

  if (str.startsWith('<网站指令>')) {
    const command = str.substring(6).trim()
    handleNavigation(command)
  } else if (str.startsWith('<用户输入>')) {
    const userText = str.substring(6).trim()
    messages.value.push({ role: 'user', content: userText })
    scrollToBottom()
    sendToLLM(userText)
  } else if (str.startsWith('<LLM回复>')) {
    // LLM 回复已在 sendToLLM 中处理，Unity 仅触发 TTS
    const replyText = str.substring(7).trim()
    sendTTS(replyText)
  } else if (str.startsWith('<聊天完成>')) {
    loading.value = false
  } else if (str.startsWith('<请求失败>')) {
    loading.value = false
    messages.value.push({
      role: 'ai',
      content: '小慧暂时不在线，请稍后重试。',
      html: renderMarkdown('小慧暂时不在线，请稍后重试。')
    })
    scrollToBottom()
  }
}

// ========== 导航指令映射 ==========
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
  if (target) {
    messages.value.push({
      role: 'ai',
      content: `好的，正在跳转到 ${command.replace('打开', '')}...`,
      html: renderMarkdown(`好的，正在跳转到 **${command.replace('打开', '')}**...`)
    })
    scrollToBottom()
    setTimeout(() => router.push(target), 800)
  } else {
    messages.value.push({
      role: 'ai',
      content: `抱歉，我暂时无法执行"${command}"指令。`,
      html: renderMarkdown(`抱歉，我暂时无法执行"${command}"指令。`)
    })
    scrollToBottom()
  }
}

// ========== 解析结构化回复 ==========
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
  // Fallback: 如果没有匹配到结构化字段，整段文字作为回复文本
  if (!parts.text && raw) {
    parts.text = raw
  }
  return parts
}

// ========== 发送消息 ==========
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
      history: messages.value.slice(-10).map(m => ({ role: m.role === 'ai' ? 'assistant' : 'user', content: m.content }))
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
            // 解析结构化回复
            const parts = parseStructuredReply(fullText)
            messages.value.push({
              role: 'ai',
              content: parts.text,
              html: renderMarkdown(parts.text)
            })
            scrollToBottom()

            // 处理指令
            if (parts.command && parts.command !== '无') {
              handleNavigation(parts.command)
            }

            // 通知 Unity 播放表情动作
            if (UnityIns && unityLoaded.value) {
              try {
                UnityIns.SendMessage('ChatManager', 'PlayEmotion', parts.emotion)
                UnityIns.SendMessage('ChatManager', 'PlayAction', parts.action)
              } catch (e) { /* Unity 通信失败，静默降级 */ }
            }

            // TTS 语音合成
            if (parts.text) {
              sendTTS(parts.text)
            }
          }
        } catch (e) { /* 跳过解析失败的 SSE 行 */ }
      }
    }
  } catch (e) {
    console.error('LLM 请求失败:', e)
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

// ========== TTS 语音合成 ==========
async function sendTTS(text) {
  try {
    const response = await apiTTS(text)
    if (response.ok) {
      const audioBlob = await response.blob()
      const audioUrl = URL.createObjectURL(audioBlob)
      const audio = new Audio(audioUrl)
      audio.play().catch(() => { /* 自动播放被浏览器阻止 */ })

      // 同时通知 Unity 播放语音（如果有对应方法）
      if (UnityIns && unityLoaded.value) {
        try {
          UnityIns.SendMessage('ChatManager', 'PlayAudio', audioUrl)
        } catch (e) { /* 静默降级 */ }
      }
    }
  } catch (e) {
    console.log('TTS 合成失败，使用静默模式')
  }
}

// ========== 语音输入 ==========
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
    () => { console.log('麦克风已就绪') },
    (msg, isUserNotAllow) => {
      console.log((isUserNotAllow ? '用户拒绝授权：' : '') + msg)
    }
  )
}

// ========== 清空对话 ==========
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

let unityLoadTimer = null

onMounted(() => {
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

  // 30s 超时
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
      console.error('Unity 加载失败:', message)
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

  scrollToBottom()
})

onBeforeUnmount(() => {
  clearTimeout(unityLoadTimer)
  if (RecorderIns) {
    try { RecorderIns.close() } catch (e) { /* ignore */ }
  }
})
</script>
```

- [ ] **Step 2: 编写 template 部分**

```html
<template>
  <div class="immersive-page">
    <!-- Unity 容器（左侧） -->
    <div class="unity-panel" :class="{ collapsed: unityLoadFailed }">
      <div v-if="unityLoadFailed" class="unity-fallback">
        <div class="fallback-icon">⚠️</div>
        <p class="fallback-text">3D 虚拟人加载失败</p>
        <button class="retry-btn" @click="unityLoadFailed = false; unityLoaded = false; onMounted()">
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
      <!-- Header -->
      <header class="chat-header">
        <div class="header-left">
          <span class="ai-icon">AI</span>
          <div class="header-info">
            <h2 class="header-title">沉浸伴学 · 小慧</h2>
            <span class="header-status" v-if="unityLoaded">在线 · 3D 虚拟人已就绪</span>
            <span class="header-status muted" v-else-if="!unityLoadFailed">3D 虚拟人加载中...</span>
            <span class="header-status muted" v-else>文字对话模式</span>
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

      <!-- Messages -->
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

      <!-- Input area -->
      <div class="input-area">
        <div class="input-wrapper">
          <button
            class="voice-btn"
            :class="{ recording: isRecording }"
            @click="isRecording ? stopRecording() : startRecording()"
            :disabled="loading || unityLoadFailed"
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
```

- [ ] **Step 3: 编写样式部分**

```html
<style scoped lang="scss">
@use '../../styles/variables' as *;

.immersive-page {
  display: flex;
  height: calc(100vh - 56px);
  background: transparent;
  overflow: hidden;
}

/* ========== Unity 面板（左侧） ========== */
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

#unity-canvas {
  background: transparent;
}

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

/* ========== 对话面板（右侧） ========== */
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 400px;

  &.expanded {
    /* Unity 不可用时占满全宽 */
  }
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
  :deep(pre) { background: rgba(0, 0, 0, 0.3); border: 1px solid var(--border-base); border-radius: 10px; padding: 14px 16px; overflow-x: auto; margin: 8px 0;
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
```

- [ ] **Step 4: 在 index.html 中添加录音脚本引用**

检查 `frontend/index.html` 是否已引入 `recorder.wav.min.js`。如果没有，在 `<head>` 中新增：

```html
<script src="/unity/recorder.wav.min.js"></script>
<script src="/unity/unity.recorder.js"></script>
```

- [ ] **Step 5: 验证文件存在**

```bash
ls "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup\frontend\src\views\student\AICompanionImmersive.vue"
```

- [ ] **Step 6: Commit**

```bash
cd "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup"
git add frontend/src/views/student/AICompanionImmersive.vue frontend/index.html
git commit -m "feat: create AICompanionImmersive with Unity WebGL + chat panel"
```

---

### Task 7: 后端新增 chat-stream 端点

**Files:**
- Modify: `backend/src/main/java/com/iflytek/smartprep/controller/AgentController.java`

- [ ] **Step 1: 在 AgentController 中新增 chat-stream SSE 端点**

```java
/**
 * Unity AI 虚拟人专用——流式对话（SSE）
 * 返回结构化回复：表情|动作|回复文本|指令
 */
@PostMapping("/chat-stream")
public SseEmitter chatStream(@RequestBody Map<String, Object> request,
                              @RequestHeader(value = "Authorization") String authHeader) {
    SseEmitter emitter = new SseEmitter(60000L);

    String question = (String) request.getOrDefault("question", "");
    @SuppressWarnings("unchecked")
    List<Map<String, String>> history = (List<Map<String, String>>) request.getOrDefault("history", Collections.emptyList());

    new Thread(() -> {
        try {
            String result = agentService.chatStreamWithCompanion(question, history, chunk -> {
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("delta", chunk);
                    data.put("finish", false);
                    emitter.send(SseEmitter.event().name("message").data(data));
                } catch (IOException e) {
                    // 客户端断开
                }
            });

            Map<String, Object> done = new HashMap<>();
            done.put("delta", "");
            done.put("finish", true);
            emitter.send(SseEmitter.event().name("message").data(done));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }).start();

    return emitter;
}
```

注意：需要在文件顶部新增 import：

```java
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
```

- [ ] **Step 2: 在 AgentService 中新增 chatStreamWithCompanion 方法**

编辑 `backend/src/main/java/com/iflytek/smartprep/service/AgentService.java`，新增方法：

```java
/**
 * Unity AI 虚拟人专用——流式对话 + 结构化 system prompt
 */
public String chatStreamWithCompanion(String question, List<Map<String, String>> history,
                                       java.util.function.Consumer<String> onChunk) {
    List<Map<String, String>> messages = new ArrayList<>();
    messages.add(Map.of("role", "system", "content", COMPANION_SYSTEM_PROMPT));

    // 添加历史对话（最近 6 轮）
    if (history != null && !history.isEmpty()) {
        int start = Math.max(0, history.size() - 6);
        for (int i = start; i < history.size(); i++) {
            messages.add(history.get(i));
        }
    }

    messages.add(Map.of("role", "user", "content", question));

    return llmClient.chatStreamMessages(messages, onChunk);
}
```

同时新增 system prompt 常量：

```java
private static final String COMPANION_SYSTEM_PROMPT = """
        你是知域智能学习平台的虚拟教学助手，名叫小慧。你应该用可爱且口语化的语气回复，
        尽量友善且平易近人，回复长度保持在正常口语交谈的长度。

        你可以做表情和动作来配合回复：
        可用的表情有：生气，困惑，难过，开心，有趣，惊讶。
        可用的动作有：鞠躬，右手放胸前，右手放身前，右手放头上。

        同时你还需要帮助用户操控网站，进行路由导航。目前支持的指令有：
        "打开课程平台"、"打开学习分析"、"打开我的考试"、"打开问答广场"、
        "打开个人资料"、"打开学习首页"、"打开沉浸伴学"。

        回复格式要求（每个字段之间用|隔开）：
        表情：在这里输出你的表情|动作：在这里输出你的动作|回复文本：在这里输出你的回复文本|指令：在这里输出你的指令

        没有指令时指令字段用"无"，没有表情或动作时对应字段用"无"。

        示例对话：
        用户：你好呀。
        小慧：表情：开心|动作：右手放胸前|回复文本：你好！我是虚拟教学助手小慧，请问有什么可以帮你的吗？|指令：无
        用户：帮我打开课程平台
        小慧：表情：开心|动作：右手放身前|回复文本：好的，正在帮你打开课程平台！|指令：打开课程平台
        用户：七乘九是多少？
        小慧：表情：有趣|动作：右手放身前|回复文本：这是很简单的算术问题，七乘九等于六十三，你是想考验我吗？|指令：无
        """;
```

- [ ] **Step 3: 在 LLMClient 中新增 chatStreamMessages 方法**

编辑 `backend/src/main/java/com/iflytek/smartprep/service/LLMClient.java`，新增方法：

```java
/**
 * 流式多轮对话
 */
public String chatStreamMessages(List<Map<String, String>> messages, Consumer<String> onChunk) {
    return chatStreamMessages(messages, defaultModel, defaultTemperature, defaultMaxTokens, onChunk);
}

/**
 * 流式多轮对话 + 完整参数
 */
public String chatStreamMessages(List<Map<String, String>> messages, String model,
                                  double temperature, int maxTokens,
                                  Consumer<String> onChunk) {
    if (apiKey == null || apiKey.isBlank() || baseUrl == null || baseUrl.isBlank()) {
        log.warn("LLM API 未配置，无法流式调用");
        return fallbackCompanionReply();
    }

    try {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("stream", true);

        String json = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(buildUrl())
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(json, MediaType.parse("application/json; charset=utf-8")))
                .build();

        StringBuilder fullText = new StringBuilder();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("LLM 流式调用返回错误 {}", response.code());
                return fallbackCompanionReply();
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) return fallbackCompanionReply();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(responseBody.byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;

                        try {
                            JsonNode root = objectMapper.readTree(data);
                            JsonNode delta = root.path("choices").path(0).path("delta").path("content");
                            if (!delta.isMissingNode()) {
                                String chunk = delta.asText();
                                fullText.append(chunk);
                                if (onChunk != null) onChunk.accept(chunk);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        String result = fullText.toString();
        return result != null && !result.isBlank() ? result : fallbackCompanionReply();
    } catch (Exception e) {
        log.error("LLM 流式调用失败: {}", e.getMessage());
        return fallbackCompanionReply();
    }
}

/**
 * LLM 不可用时的兜底回复
 */
private String fallbackCompanionReply() {
    return "表情：开心|动作：右手放胸前|回复文本：你好！我是小慧，当前服务繁忙，请稍后再试。你仍然可以使用文字对话功能。|指令：无";
}
```

- [ ] **Step 4: Commit**

```bash
cd "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup"
git add backend/src/main/java/com/iflytek/smartprep/controller/AgentController.java backend/src/main/java/com/iflytek/smartprep/service/AgentService.java backend/src/main/java/com/iflytek/smartprep/service/LLMClient.java
git commit -m "feat: add companion chat-stream SSE endpoint with structured system prompt"
```

---

### Task 8: 后端新增 STT 语音识别端点

> **注意：** 讯飞 STT API 的具体接入方式（REST API vs SDK）需在实现时调研确认。当前端点提供完整的请求/响应框架和错误处理，仅核心 API 调用部分标注 TODO。



**Files:**
- Modify: `backend/src/main/java/com/iflytek/smartprep/controller/AgentController.java`

- [ ] **Step 1: 新增 STT 端点**

```java
/**
 * 语音识别（STT）
 * 接收 WAV 音频文件，返回识别文本
 * 注：讯飞 STT API 具体接入方式需调研，当前提供端点框架
 */
@PostMapping("/stt")
public Map<String, Object> speechToText(@RequestParam("file") MultipartFile file) {
    Map<String, Object> result = new HashMap<>();

    if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024) {
        result.put("success", false);
        result.put("message", file.isEmpty() ? "音频文件为空" : "音频文件过大（最大 10MB）");
        return result;
    }

    // TODO: 接入讯飞语音识别 REST API，将 file 的 byte[] 发送给讯飞
    // 当前返回占位文本，STT 功能在确定讯飞 API 后完成接入
    result.put("success", true);
    result.put("text", "语音识别功能接入中...");
    return result;
}
```

注意：需要在文件顶部新增 import：

```java
import org.springframework.web.multipart.MultipartFile;
```

- [ ] **Step 2: Commit**

```bash
cd "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup"
git add backend/src/main/java/com/iflytek/smartprep/controller/AgentController.java
git commit -m "feat: add STT speech-to-text endpoint scaffold"
```

---

### Task 9: 后端新增 TTS 语音合成端点

> **注意：** 讯飞 TTS API 的具体接入方式需在实现时调研确认。当前端点返回静默 WAV 作为兜底，不影响对话功能正常使用。Unity 虚拟人会在收到空音频时只播放表情动作（静默降级）。



**Files:**
- Modify: `backend/src/main/java/com/iflytek/smartprep/controller/AgentController.java`

- [ ] **Step 1: 新增 TTS 端点**

```java
/**
 * 语音合成（TTS）
 * 接收文本，返回 WAV 音频流
 * 注：讯飞 TTS API 具体接入方式需调研，当前返回静默音频
 */
@PostMapping("/tts")
public ResponseEntity<byte[]> textToSpeech(@RequestBody Map<String, Object> request) {
    String text = (String) request.getOrDefault("text", "");

    if (text == null || text.isBlank()) {
        return ResponseEntity.badRequest().build();
    }

    // TODO: 接入讯飞语音合成 REST API
    // 当前返回最小 WAV 文件头
    byte[] silentWav = generateSilentWav();

    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("audio/wav"))
            .body(silentWav);
}

/**
 * 生成最小静默 WAV 文件头（TTS API 就绪前的兜底）
 */
private byte[] generateSilentWav() {
    // 最小 WAV 头 + 0.1s 静默数据 (16kHz, 16bit, mono)
    int sampleRate = 16000;
    int durationMs = 100;
    int dataSize = (sampleRate * 2 * durationMs) / 1000;
    byte[] wav = new byte[44 + dataSize];
    // RIFF header
    wav[0] = 'R'; wav[1] = 'I'; wav[2] = 'F'; wav[3] = 'F';
    int fileSize = 36 + dataSize;
    wav[4] = (byte)(fileSize); wav[5] = (byte)(fileSize >> 8);
    wav[6] = (byte)(fileSize >> 16); wav[7] = (byte)(fileSize >> 24);
    wav[8] = 'W'; wav[9] = 'A'; wav[10] = 'V'; wav[11] = 'E';
    // fmt chunk
    wav[12] = 'f'; wav[13] = 'm'; wav[14] = 't'; wav[15] = ' ';
    wav[16] = 16; wav[17] = 0; wav[18] = 0; wav[19] = 0; // chunk size
    wav[20] = 1; wav[21] = 0; // PCM
    wav[22] = 1; wav[23] = 0; // mono
    wav[24] = (byte)(sampleRate); wav[25] = (byte)(sampleRate >> 8);
    wav[26] = (byte)(sampleRate >> 16); wav[27] = (byte)(sampleRate >> 24);
    int byteRate = sampleRate * 2;
    wav[28] = (byte)(byteRate); wav[29] = (byte)(byteRate >> 8);
    wav[30] = (byte)(byteRate >> 16); wav[31] = (byte)(byteRate >> 24);
    wav[32] = 2; wav[33] = 0; // block align
    wav[34] = 16; wav[35] = 0; // bits per sample
    // data chunk
    wav[36] = 'd'; wav[37] = 'a'; wav[38] = 't'; wav[39] = 'a';
    wav[40] = (byte)(dataSize); wav[41] = (byte)(dataSize >> 8);
    wav[42] = (byte)(dataSize >> 16); wav[43] = (byte)(dataSize >> 24);
    return wav;
}
```

注意：需要在文件顶部新增 import：

```java
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
```

- [ ] **Step 2: Commit**

```bash
cd "C:\Users\ZWC\Desktop\软件杯大赛\softwacecup"
git add backend/src/main/java/com/iflytek/smartprep/controller/AgentController.java
git commit -m "feat: add TTS text-to-speech endpoint with silent WAV fallback"
```

---

### 验收验证

所有任务完成后，执行以下验证：

- [ ] 启动前端 dev server，访问 `/student/companion-immersive`
- [ ] 确认 Unity WebGL 加载进度条出现，最终 3D 虚拟人渲染
- [ ] 侧边栏"沉浸"菜单项可见且点击跳转正常
- [ ] 文字输入 → 发送 → 看到流式 AI 回复（Markdown 渲染）
- [ ] 结构化回复解析正常（表情/动作/文本/指令正确拆分）
- [ ] 说"打开课程平台" → 页面跳转到课程页
- [ ] 语音按钮点击 → 浏览器弹出麦克风权限请求
- [ ] Unity 虚拟人右键全屏按钮可用
- [ ] 故意断开后端 → 显示"小慧暂时不在线"降级提示
- [ ] 现有 AICompanionView 页面（`/student/companion`）不受影响
- [ ] AIFloatingBall 悬浮球原有功能正常
