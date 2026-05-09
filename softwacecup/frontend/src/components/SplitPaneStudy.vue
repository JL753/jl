<template>
  <div class="split-pane-study">
    <!-- 左侧：资源预览区 -->
    <div class="left-pane" :style="{ width: leftWidth + '%' }">
      <div class="pane-header">
        <div class="header-left">
          <span class="icon">📚</span>
          <h4>学习资源</h4>
        </div>
        <div class="header-actions">
          <el-button size="small" @click="switchResourceType('pdf')" :type="resourceType === 'pdf' ? 'primary' : ''">PDF</el-button>
          <el-button size="small" @click="switchResourceType('video')" :type="resourceType === 'video' ? 'primary' : ''">视频</el-button>
          <el-button size="small" @click="switchResourceType('doc')" :type="resourceType === 'doc' ? 'primary' : ''">文档</el-button>
          <el-button size="small" @click="switchResourceType('mindmap')" :type="resourceType === 'mindmap' ? 'primary' : ''">知识图谱</el-button>
          <el-button size="small" @click="switchResourceType('quiz')" :type="resourceType === 'quiz' ? 'primary' : ''">随堂测验</el-button>
        </div>
      </div>

      <div class="pane-content">
        <!-- PDF 预览 -->
        <div v-if="resourceType === 'pdf'" class="pdf-viewer">
          <div class="pdf-toolbar">
            <el-button size="small" @click="prevPage" :disabled="currentPage <= 1">上一页</el-button>
            <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
            <el-button size="small" @click="nextPage" :disabled="currentPage >= totalPages">下一页</el-button>
          </div>
          <div class="pdf-content">
            <iframe v-if="pdfUrl" :src="pdfUrl + '#page=' + currentPage" class="pdf-frame"></iframe>
            <div v-else class="empty-state">
              <p>📄</p>
              <p>暂无PDF资源</p>
              <el-button type="primary" size="small" @click="uploadPdf">上传PDF</el-button>
            </div>
          </div>
        </div>

        <!-- 视频播放器 -->
        <div v-else-if="resourceType === 'video'" class="video-player">
          <video v-if="videoUrl" :src="videoUrl" controls class="video-element"></video>
          <div v-else class="empty-state">
            <p>🎬</p>
            <p>暂无视频资源</p>
            <el-button type="primary" size="small" @click="uploadVideo">上传视频</el-button>
          </div>
        </div>

        <!-- 文档预览 -->
        <div v-else-if="resourceType === 'doc'" class="doc-viewer">
          <div v-if="docContent" class="doc-content" v-html="docContent"></div>
          <div v-else class="empty-state">
            <p>📝</p>
            <p>暂无文档资源</p>
            <el-button type="primary" size="small" @click="uploadDoc">上传文档</el-button>
          </div>
        </div>

        <!-- 知识图谱 -->
        <div v-else-if="resourceType === 'mindmap'" class="mindmap-viewer">
          <div v-if="knowledgeMapData" class="mindmap-content">
            <KnowledgeMap :data="knowledgeMapData" :title="knowledgeMapData.name" />
          </div>
          <div v-else class="empty-state">
            <p>🧠</p>
            <p>暂无知识图谱</p>
            <el-button
              type="primary"
              size="small"
              :loading="isGeneratingMap"
              @click="generateKnowledgeMap"
            >
              {{ isGeneratingMap ? '生成中...' : '生成知识图谱' }}
            </el-button>
            <p class="hint-text">请先上传文档并完成向量化</p>
          </div>
        </div>

        <!-- 随堂测验 -->
        <div v-else-if="resourceType === 'quiz'" class="quiz-viewer">
          <QuizComponent :documentId="currentDocumentId" :studentId="currentStudentId" />
        </div>
      </div>
    </div>

    <!-- 拖拽分隔条 -->
    <div
      class="divider"
      @mousedown="startDrag"
      @touchstart="startDrag"
    >
      <div class="divider-handle">
        <span></span>
        <span></span>
        <span></span>
      </div>
    </div>

    <!-- 右侧：AI助手对话区 -->
    <div class="right-pane" :style="{ width: (100 - leftWidth) + '%' }">
      <div class="pane-header">
        <div class="header-left">
          <span class="icon">🤖</span>
          <h4>AI学习助手</h4>
        </div>
        <div class="header-actions">
          <el-button size="small" @click="clearChat">清空对话</el-button>
          <el-button size="small" type="primary" @click="newChat">新对话</el-button>
        </div>
      </div>

      <div class="chat-container">
        <!-- 消息列表 -->
        <div class="chat-messages" ref="messagesContainer">
          <div v-for="(msg, index) in messages" :key="index" :class="['message-item', msg.role]">
            <!-- AI 消息 -->
            <div v-if="msg.role === 'assistant'" class="message-wrapper">
              <div class="message-avatar">
                <img src="https://dummyimage.com/40x40/4c8dff/ffffff&text=AI" alt="AI" />
              </div>
              <div class="message-bubble ai-bubble">
                <div class="message-content" v-html="msg.html"></div>

                <!-- RAG 引用来源 -->
                <div v-if="msg.citations && msg.citations.length > 0" class="citations-section">
                  <div class="citations-header">
                    <span class="citations-icon">📚</span>
                    <span class="citations-title">参考来源</span>
                  </div>
                  <div class="citations-list">
                    <div
                      v-for="(citation, idx) in msg.citations"
                      :key="idx"
                      class="citation-item"
                      @click="handleCitationClick(citation)"
                    >
                      <span class="citation-badge">{{ idx + 1 }}</span>
                      <span class="citation-text">{{ citation }}</span>
                    </div>
                  </div>
                </div>

                <div class="message-meta">
                  <span class="timestamp">{{ msg.timestamp }}</span>
                  <el-button size="small" text @click="copyMessage(msg.text)">复制</el-button>
                </div>
              </div>
            </div>

            <!-- 用户消息 -->
            <div v-else class="message-wrapper user-wrapper">
              <div class="message-bubble user-bubble">
                <div class="message-content">{{ msg.text }}</div>
                <div class="message-meta">
                  <span class="timestamp">{{ msg.timestamp }}</span>
                </div>
              </div>
              <div class="message-avatar">
                <img src="https://dummyimage.com/40x40/10b981/ffffff&text=ME" alt="User" />
              </div>
            </div>
          </div>

          <!-- 加载中提示 -->
          <div v-if="isLoading" class="message-item assistant">
            <div class="message-wrapper">
              <div class="message-avatar">
                <img src="https://dummyimage.com/40x40/4c8dff/ffffff&text=AI" alt="AI" />
              </div>
              <div class="message-bubble ai-bubble">
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="chat-input-area">
          <div class="input-toolbar">
            <el-button size="small" text @click="insertTemplate('解释')">💡 解释概念</el-button>
            <el-button size="small" text @click="insertTemplate('总结')">📋 总结要点</el-button>
            <el-button size="small" text @click="insertTemplate('举例')">🌰 举例说明</el-button>
            <el-button size="small" text @click="insertTemplate('练习')">✍️ 生成练习</el-button>
          </div>
          <div class="input-row">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="输入您的问题，支持 Markdown 和 LaTeX 公式..."
              @keydown.enter.ctrl="sendMessage"
              @keydown.enter.meta="sendMessage"
            />
            <el-button
              type="primary"
              :loading="isLoading"
              :disabled="!inputMessage.trim()"
              @click="sendMessage"
              class="send-button"
            >
              发送
            </el-button>
          </div>
          <div class="input-hint">
            <span>💡 提示：Ctrl+Enter 快速发送 | 支持 LaTeX: $E=mc^2$ | 支持代码块</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { ElMessage } from 'element-plus'
import { openTutorSSE } from '../api/sse'
import KnowledgeMap from './KnowledgeMap.vue'
import QuizComponent from './QuizComponent.vue'
import axios from 'axios'

// ==================== 分屏拖拽逻辑 ====================
const leftWidth = ref(50) // 左侧宽度百分比
const isDragging = ref(false)

const startDrag = (e) => {
  isDragging.value = true
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  document.addEventListener('touchmove', onDrag)
  document.addEventListener('touchend', stopDrag)
  e.preventDefault()
}

const onDrag = (e) => {
  if (!isDragging.value) return
  const clientX = e.touches ? e.touches[0].clientX : e.clientX
  const container = document.querySelector('.split-pane-study')
  if (!container) return

  const rect = container.getBoundingClientRect()
  const newWidth = ((clientX - rect.left) / rect.width) * 100

  // 限制宽度范围 30% - 70%
  if (newWidth >= 30 && newWidth <= 70) {
    leftWidth.value = newWidth
  }
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', stopDrag)
}

// ==================== 资源预览逻辑 ====================
const resourceType = ref('pdf') // pdf | video | doc | mindmap | quiz
const pdfUrl = ref('')
const videoUrl = ref('')
const docContent = ref('')
const currentPage = ref(1)
const totalPages = ref(10)
const knowledgeMapData = ref(null)
const isGeneratingMap = ref(false)
const currentDocumentId = ref(null) // 当前文档ID（用于生成知识图谱和测验）
const currentStudentId = ref(1) // 当前学生ID（实际应从登录状态获取）

const switchResourceType = (type) => {
  resourceType.value = type
}

const prevPage = () => {
  if (currentPage.value > 1) currentPage.value--
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) currentPage.value++
}

const uploadPdf = () => {
  ElMessage.info('PDF上传功能开发中...')
}

const uploadVideo = () => {
  ElMessage.info('视频上传功能开发中...')
}

const uploadDoc = () => {
  ElMessage.info('文档上传功能开发中...')
}

// 生成知识图谱
const generateKnowledgeMap = async () => {
  if (!currentDocumentId.value) {
    ElMessage.warning('请先上传文档')
    return
  }

  isGeneratingMap.value = true

  try {
    const response = await axios.post('/api/knowledge-map/generate', {
      documentId: currentDocumentId.value,
      maxDepth: 3,
      includeDetails: true
    })

    if (response.data.code === 200) {
      knowledgeMapData.value = response.data.data
      ElMessage.success('知识图谱生成成功！')
    } else {
      ElMessage.error(response.data.message || '生成失败')
    }
  } catch (error) {
    console.error('生成知识图谱失败:', error)
    ElMessage.error('生成知识图谱失败：' + (error.response?.data?.message || error.message))
  } finally {
    isGeneratingMap.value = false
  }
}

// ==================== AI对话逻辑 ====================
// 获取当前时间
const getCurrentTime = () => {
  const now = new Date()
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
}

const messages = ref([
  {
    role: 'assistant',
    text: '你好！我是你的AI学习助手。我可以帮你：\n\n1. 📖 解释左侧资源中的知识点\n2. 💡 回答学习中的疑问\n3. ✍️ 生成练习题和测试\n4. 📊 总结学习要点\n\n有什么我可以帮助你的吗？',
    html: '',
    timestamp: getCurrentTime()
  }
])
const inputMessage = ref('')
const isLoading = ref(false)
const messagesContainer = ref(null)

// 配置 marked 支持代码高亮和 LaTeX
marked.setOptions({
  breaks: true,
  gfm: true,
  highlight: function(code, lang) {
    // 简单的代码高亮（后续可以集成 highlight.js）
    return `<pre class="code-block"><code class="language-${lang}">${escapeHtml(code)}</code></pre>`
  }
})

// 转义 HTML
const escapeHtml = (text) => {
  const map = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  }
  return text.replace(/[&<>"']/g, m => map[m])
}

// 处理 LaTeX 公式（简单版本，后续可以集成 KaTeX）
const processLatex = (text) => {
  // 行内公式 $...$
  text = text.replace(/\$([^\$]+)\$/g, '<span class="latex-inline">$1</span>')
  // 块级公式 $$...$$
  text = text.replace(/\$\$([^\$]+)\$\$/g, '<div class="latex-block">$1</div>')
  return text
}

// 渲染 Markdown
const renderMarkdown = (text) => {
  let html = marked.parse(text)
  html = processLatex(html)
  return DOMPurify.sanitize(html)
}

// 初始化第一条消息的 HTML
onMounted(() => {
  messages.value[0].html = renderMarkdown(messages.value[0].text)
})

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value) return

  const userMessage = inputMessage.value.trim()

  // 添加用户消息
  messages.value.push({
    role: 'user',
    text: userMessage,
    timestamp: getCurrentTime()
  })

  // 清空输入框
  inputMessage.value = ''
  isLoading.value = true

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  // 创建 AI 消息占位符
  const aiMessage = {
    role: 'assistant',
    text: '',
    html: '',
    citations: [],
    timestamp: getCurrentTime()
  }
  messages.value.push(aiMessage)

  // 调用 SSE 流式接口
  let accumulatedText = ''

  openTutorSSE(
    {
      question: userMessage,
      context: resourceType.value === 'doc' ? docContent.value : '',
      answerMode: 'student-qa'
    },
    {
      onDelta(chunk) {
        accumulatedText += chunk
        aiMessage.text = accumulatedText
        aiMessage.html = renderMarkdown(accumulatedText)
        scrollToBottom()
      },
      onMeta(data) {
        // 处理元数据（如检索进度提示）
        console.log('Meta:', data)
      },
      onCitations(citations) {
        // 接收引用来源
        if (citations && Array.isArray(citations)) {
          aiMessage.citations = citations
        }
      },
      onDone() {
        isLoading.value = false
        scrollToBottom()
      },
      onError(error) {
        isLoading.value = false
        ElMessage.error('AI 回复失败：' + error.message)
        aiMessage.text = '抱歉，我遇到了一些问题，请稍后再试。'
        aiMessage.html = renderMarkdown(aiMessage.text)
      },
      onClose() {
        isLoading.value = false
      }
    }
  )
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 插入模板
const insertTemplate = (type) => {
  const templates = {
    '解释': '请详细解释一下：',
    '总结': '请总结以下内容的要点：',
    '举例': '请举例说明：',
    '练习': '请根据以下内容生成练习题：'
  }
  inputMessage.value = templates[type] || ''
}

// 复制消息
const copyMessage = (text) => {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 清空对话
const clearChat = () => {
  messages.value = [messages.value[0]] // 保留欢迎消息
  ElMessage.success('对话已清空')
}

// 新对话
const newChat = () => {
  clearChat()
}

// 监听消息变化，自动滚动
watch(() => messages.value.length, () => {
  scrollToBottom()
})

// ==================== 双屏联动：点击引用跳转PDF ====================
const handleCitationClick = (citation) => {
  // 从引用文本中提取页码，支持多种格式：
  // "[人工智能导论 - 第一章 第3页]"
  // "[Page 12]"
  // "[第5页]"
  const pageMatch = citation.match(/第?(\d+)页|Page\s*(\d+)/i)

  if (pageMatch) {
    const pageNumber = parseInt(pageMatch[1] || pageMatch[2])

    if (pageNumber && pageNumber > 0 && pageNumber <= totalPages.value) {
      // 切换到PDF视图
      resourceType.value = 'pdf'

      // 平滑跳转到目标页
      currentPage.value = pageNumber

      // 视觉反馈
      ElMessage.success(`已跳转至第 ${pageNumber} 页`)

      // 高亮效果（可选）
      nextTick(() => {
        const pdfContent = document.querySelector('.pdf-content')
        if (pdfContent) {
          pdfContent.classList.add('highlight-flash')
          setTimeout(() => {
            pdfContent.classList.remove('highlight-flash')
          }, 1000)
        }
      })
    } else {
      ElMessage.warning('页码超出范围')
    }
  } else {
    ElMessage.info('该引用未包含页码信息')
  }
}
</script>

<style scoped lang="scss">

.split-pane-study {
  display: flex;
  height: 100%;
  min-height: 600px;
  background: $bg-page;
  border-radius: $radius-large;
  overflow: hidden;
  box-shadow: $shadow-card;
}

/* ==================== 左侧面板 ==================== */
.left-pane {
  display: flex;
  flex-direction: column;
  background: $bg-white;
  border-right: 1px solid $border-light;
}

.pane-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md $spacing-lg;
  border-bottom: 1px solid $border-light;
  background: $gradient-primary;
  color: $bg-white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: $spacing-sm;

  .icon {
    font-size: $font-size-xxxl;
  }

  h4 {
    margin: 0;
    font-size: $font-size-xl;
    font-weight: $font-weight-semibold;
  }
}

.header-actions {
  display: flex;
  gap: $spacing-xs;
}

.pane-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* PDF 查看器 */
.pdf-viewer {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.pdf-toolbar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm;
  background: $bg-lighter;
  border-bottom: 1px solid $border-light;
}

.page-info {
  font-size: $font-size-base;
  color: $text-secondary;
  min-width: 80px;
  text-align: center;
  font-weight: $font-weight-medium;
}

.pdf-content {
  flex: 1;
  overflow: hidden;
  position: relative;
  background: $bg-lighter;
}

.pdf-frame {
  width: 100%;
  height: 100%;
  border: none;
}

/* 视频播放器 */
.video-player {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
}

.video-element {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* 文档查看器 */
.doc-viewer {
  height: 100%;
  overflow-y: auto;
  padding: $spacing-xl;
}

.doc-content {
  line-height: 1.8;
  color: $text-regular;

  :deep(h1) {
    font-size: $font-size-xxxl;
    margin-top: $spacing-xl;
    margin-bottom: $spacing-sm;
    color: $text-primary;
    font-weight: $font-weight-bold;
  }

  :deep(h2) {
    font-size: $font-size-xxl;
    margin-top: $spacing-lg;
    margin-bottom: $spacing-sm;
    color: $text-primary;
    font-weight: $font-weight-semibold;
  }

  :deep(p) {
    margin-bottom: $spacing-sm;
  }

  :deep(code) {
    background: $bg-lighter;
    padding: 2px 6px;
    border-radius: $radius-small;
    font-size: $font-size-sm;
    color: $danger-color;
    font-family: $font-family-code;
  }
}

/* 知识图谱查看器 */
.mindmap-viewer {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.mindmap-content {
  flex: 1;
  overflow: hidden;
}

/* 测验查看器 */
.quiz-viewer {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.hint-text {
  font-size: $font-size-xs;
  color: $text-secondary;
  margin-top: $spacing-xs;
}

/* 空状态 */
.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $spacing-md;
  color: $text-secondary;

  p:first-child {
    font-size: 64px;
    margin: 0;
    opacity: 0.5;
  }

  p:nth-child(2) {
    font-size: $font-size-md;
    margin: 0;
  }
}

/* ==================== 拖拽分隔条 ==================== */
.divider {
  width: 8px;
  background: $border-light;
  cursor: col-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: background $transition-base;

  &:hover {
    background: $border-base;
  }
}

.divider-handle {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 4px;

  span {
    width: 2px;
    height: 20px;
    background: $text-secondary;
    border-radius: 1px;
  }
}

/* ==================== 右侧面板 - AI 对话区 ==================== */
.right-pane {
  display: flex;
  flex-direction: column;
  background: $bg-white;

  .pane-header {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  }
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 消息列表 - 居中显示 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-xl;
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
  background: linear-gradient(180deg, $bg-lighter 0%, $bg-white 100%);
}

.message-item {
  display: flex;
  justify-content: center;
  animation: fadeIn 0.3s ease-in;
}

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

/* 消息气泡 - 居中且限制最大宽度 */
.message-wrapper {
  display: flex;
  gap: $spacing-sm;
  max-width: 800px;
  width: 100%;
}

.user-wrapper {
  margin-left: auto;
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;

  img {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
    box-shadow: $shadow-light;
  }
}

.message-bubble {
  padding: $spacing-md $spacing-lg;
  border-radius: $radius-medium;
  box-shadow: $shadow-light;
  transition: all $transition-base;

  &:hover {
    box-shadow: $shadow-card;
  }
}

.ai-bubble {
  background: $bg-white;
  border: 1px solid $border-light;
  border-bottom-left-radius: $radius-small;
}

.user-bubble {
  background: $gradient-primary;
  color: $bg-white;
  border-bottom-right-radius: $radius-small;
}

.message-content {
  line-height: 1.7;
  word-wrap: break-word;
  font-size: $font-size-base;

  :deep(p) {
    margin: 0 0 $spacing-xs 0;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(h1),
  :deep(h2),
  :deep(h3) {
    margin: $spacing-sm 0 $spacing-xs 0;
    font-weight: $font-weight-semibold;
  }

  :deep(ul),
  :deep(ol) {
    margin: $spacing-xs 0;
    padding-left: $spacing-xl;
  }

  :deep(li) {
    margin: 4px 0;
  }

  :deep(code) {
    background: rgba(0, 0, 0, 0.05);
    padding: 2px 6px;
    border-radius: $radius-small;
    font-size: $font-size-sm;
    font-family: $font-family-code;
  }

  :deep(.code-block) {
    background: #1e293b;
    color: #e2e8f0;
    padding: $spacing-sm;
    border-radius: $radius-base;
    overflow-x: auto;
    margin: $spacing-xs 0;
  }

  :deep(.code-block code) {
    background: none;
    padding: 0;
    color: inherit;
  }

  :deep(.latex-inline) {
    font-style: italic;
    color: #7c3aed;
    background: #f3e8ff;
    padding: 2px 6px;
    border-radius: $radius-small;
  }

  :deep(.latex-block) {
    font-style: italic;
    color: #7c3aed;
    background: #f3e8ff;
    padding: $spacing-sm;
    border-radius: $radius-base;
    margin: $spacing-sm 0;
    text-align: center;
    font-size: $font-size-md;
  }
}

.message-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: $spacing-xs;
  font-size: $font-size-xs;
  color: $text-secondary;
}

.user-bubble .message-meta {
  color: rgba(255, 255, 255, 0.8);
}

.timestamp {
  font-size: 11px;
}

/* 打字机效果 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: $spacing-xs 0;

  span {
    width: 8px;
    height: 8px;
    background: $text-secondary;
    border-radius: 50%;
    animation: typing 1.4s infinite;

    &:nth-child(2) {
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.7;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

/* ==================== 输入区 - 卷轴风格 ==================== */
.chat-input-area {
  border-top: 2px solid $border-light;
  background: linear-gradient(180deg, $bg-lighter 0%, $bg-white 100%);
  padding: $spacing-lg;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.03);
}

.input-toolbar {
  display: flex;
  gap: $spacing-xs;
  margin-bottom: $spacing-sm;
  flex-wrap: wrap;
}

.input-row {
  display: flex;
  gap: $spacing-sm;
  align-items: flex-end;
  background: $bg-white;
  border: 2px solid $border-light;
  border-radius: $radius-large;
  padding: $spacing-sm;
  transition: all $transition-base;
  box-shadow: $shadow-light;

  &:focus-within {
    border-color: $primary-color;
    box-shadow: $glow-primary;
  }

  :deep(.el-textarea) {
    flex: 1;

    .el-textarea__inner {
      border: none;
      box-shadow: none;
      padding: $spacing-xs;
      font-size: $font-size-base;
      line-height: 1.6;
      resize: none;

      &:focus {
        box-shadow: none;
      }
    }
  }
}

.send-button {
  height: 76px;
  padding: 0 $spacing-xl;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  border-radius: $radius-medium;
  background: $gradient-primary;
  border: none;
  box-shadow: $shadow-card;
  transition: all $transition-base;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-card-hover, $glow-primary;
  }

  &:active {
    transform: translateY(0);
  }
}

.input-hint {
  margin-top: $spacing-xs;
  font-size: $font-size-xs;
  color: $text-secondary;
  text-align: center;
}

/* ==================== RAG 引用来源 - 彩色标签 ==================== */
.citations-section {
  margin-top: $spacing-md;
  padding: $spacing-sm $spacing-md;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.05) 0%, rgba(147, 197, 253, 0.05) 100%);
  border-left: 3px solid $primary-color;
  border-radius: $radius-base;
  box-shadow: $shadow-light;
}

.citations-header {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  margin-bottom: $spacing-sm;
  padding-bottom: $spacing-xs;
  border-bottom: 1px solid $border-light;
}

.citations-icon {
  font-size: $font-size-md;
  line-height: 1;
}

.citations-title {
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $text-primary;
  letter-spacing: 0.3px;
}

.citations-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.citation-item {
  display: flex;
  align-items: flex-start;
  gap: $spacing-sm;
  padding: $spacing-xs $spacing-sm;
  background: $bg-white;
  border-radius: $radius-base;
  transition: all $transition-base;
  cursor: pointer;
  border: 1px solid $border-extra-light;

  &:hover {
    background: $bg-lighter;
    transform: translateX(2px);
    box-shadow: $shadow-light;
    border-color: $primary-color;
  }
}

.citation-badge {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  background: $gradient-primary;
  color: $bg-white;
  font-size: 11px;
  font-weight: $font-weight-bold;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.3);
}

.citation-text {
  flex: 1;
  font-size: $font-size-sm;
  line-height: 1.6;
  color: $text-regular;
  word-break: break-word;
}

/* PDF 跳转高亮动画 */
.pdf-content.highlight-flash {
  animation: flashHighlight 1s ease-in-out;
}

@keyframes flashHighlight {
  0%, 100% {
    box-shadow: 0 0 0 rgba(64, 158, 255, 0);
  }
  50% {
    box-shadow: 0 0 20px rgba(64, 158, 255, 0.6);
  }
}

/* ==================== 响应式 ==================== */
@media (max-width: 1024px) {
  .split-pane-study {
    flex-direction: column;
  }

  .left-pane,
  .right-pane {
    width: 100% !important;
    height: 50%;
  }

  .divider {
    width: 100%;
    height: 8px;
    cursor: row-resize;

    .divider-handle {
      flex-direction: row;

      span {
        width: 20px;
        height: 2px;
      }
    }
  }

  .message-wrapper {
    max-width: 90%;
  }

  .citations-section {
    padding: $spacing-sm;
  }

  .citation-item {
    padding: 6px $spacing-sm;
  }

  .citation-text {
    font-size: $font-size-xs;
  }
}
</style>
