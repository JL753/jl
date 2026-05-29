<template>
  <div class="teacher-assistant-page">
    <!-- Left Sidebar: Tool Menu -->
    <aside class="tool-sidebar panel">
      <div class="menu-head">
        <p class="eyebrow">TEACHING STUDIO</p>
        <h3>智能备课助手</h3>
      </div>
      <nav class="tool-nav">
        <div v-for="item in menuItems" :key="item.key"
             :class="['tool-item', { active: activeTool === item.key }]"
             @click="switchTool(item.key)">
          <span class="tool-icon">{{ item.icon }}</span>
          <em>{{ item.label }}</em>
        </div>
      </nav>
    </aside>

    <!-- Middle: Dynamic Form Area (changes per tool) -->
    <section class="form-area panel">
      <!-- Header: Breadcrumb-like tabs -->
      <div class="form-breadcrumb">
        <span>智备优教</span>
        <span>/</span>
        <span>教学助手</span>
        <span>/</span>
        <strong>{{ currentMenu.label }}</strong>
      </div>

      <!-- ====== Tool: 智能问答 (QA) - Simple Chat ====== -->
      <template v-if="activeTool === 'qa'">
        <div class="qa-welcome">
          <img src="https://dummyimage.com/48x48/4c8dff/ffffff&text=AI" alt="ai" class="qa-avatar" />
          <div>
            <strong>张老师</strong>
            <p>可以开始聊天了~</p>
          </div>
        </div>
      </template>

      <!-- ====== Tool: 三纲一案 (Plan) - Form with fields ====== -->
      <template v-if="activeTool === 'plan'">
        <el-form label-position="top" :model="planForm" size="default" class="tool-form">
          <el-form-item label="* 三纲一案">
            <el-select v-model="planForm.type" placeholder="请选择" style="width: 100%">
              <el-option label="教学大纲" value="syllabus" />
              <el-option label="授课计划" value="plan" />
              <el-option label="考试大纲" value="exam" />
              <el-option label="教案设计" value="design" />
            </el-select>
          </el-form-item>
          <el-form-item label="* 课程名称"><el-input v-model="planForm.courseName" placeholder="计算机导论" /></el-form-item>
          <el-form-item label="* 教学时长"><el-input-number v-model="planForm.hours" :min="1" :max="100" style="width:100%" /></el-form-item>
          <el-form-item label="* 教学目标">
            <el-input v-model="planForm.goal" type="textarea" :rows="3" placeholder="掌握计算机系统基本概念和原理..." />
          </el-form-item>
          <el-form-item label="* 学 分">
            <el-input-number v-model="planForm.credit" :min="1" :max="10" style="width:100%" />
          </el-form-item>
          <el-form-item label="* 考核方式">
            <el-select v-model="planForm.assessment" multiple placeholder="包括平时考核、期中考核和期末考核" style="width: 100%">
              <el-option label="平时考勤" value="attendance" />
              <el-option label="课堂表现" value="classroom" />
              <el-option label="期中考试" value="midterm" />
              <el-option label="期末考试" value="final" />
              <el-option label="实验/实践" value="lab" />
              <el-option label="项目作业" value="project" />
            </el-select>
          </el-form-item>
          <el-form-item label="* 教学方式">
            <el-checkbox-group v-model="planForm.methods">
              <el-checkbox label="课堂讲授">课堂讲授</el-checkbox>
              <el-checkbox label="讨论">讨论</el-checkbox>
              <el-checkbox label="网络教学">网络教学</el-checkbox>
              <el-checkbox label="演示">演示</el-checkbox>
              <el-checkbox label="实践">实践</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="runAssistant('生成')">生成</el-button>

          <!-- Export button for plan results -->
          <div v-if="planResult" class="export-section">
            <el-button type="success" plain @click="exportToWord('plan')">
              📄 导出为 Word 文档
            </el-button>
            <el-button type="info" plain @click="printResult('plan')">
              🖨️ 打印预览
            </el-button>
          </div>
        </el-form>
      </template>

      <!-- ====== Tool: 智慧备课 (Prepare) ====== -->
      <template v-if="activeTool === 'prepare'">
        <el-form label-position="top" :model="prepareForm" class="tool-form">
          <el-form-item label="课程名称"><el-input v-model="prepareForm.course" /></el-form-item>
          <el-form-item label="章节主题"><el-input v-model="prepareForm.chapter" /></el-form-item>
          <el-form-item label="教学目标"><el-input v-model="prepareForm.goal" type="textarea" :rows="3" /></el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="runAssistant('开始备课')">开始备课</el-button>
        </el-form>
      </template>

      <!-- ====== Tool: 试题生成 (Paper) ====== -->
      <template v-if="activeTool === 'paper'">
        <el-form label-position="top" :model="paperForm" class="tool-form">
          <el-form-item label="题库类型"><el-select v-model="paperForm.bankType" style="width:100%"><el-option label="选择题" value="choice" /><el-option label="填空题" value="fill" /><el-option label="判断题" value="judge" /><el-option label="简答题" value="short" /></el-select></el-form-item>
          <el-form-item label="知识点"><el-input v-model="paperForm.topic" /></el-form-item>
          <el-form-item label="难度"><el-slider v-model="paperForm.difficulty" :marks="{1:'简单',2:'中等',3:'困难'}" :max="3" :min="1" show-stops /></el-form-item>
          <el-form-item label="数量"><el-input-number v-model="paperForm.count" :min="1" :max="50" style="width:100%" /></el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="runAssistant('生成题目')">生成题目</el-button>
        </el-form>
      </template>

      <!-- ====== Tool: 教学资源 (Resource) ====== -->
      <template v-if="activeTool === 'resource'">
        <div class="resource-preview">
          <div class="res-card img-card">
            <div class="res-img-placeholder">
              <span>📷</span>
              <p>教学图片资源</p>
            </div>
          </div>
          <div class="res-card video-card">
            <div class="video-placeholder">
              <span>▶</span>
              <p>0:00 / 2:16</p>
            </div>
          </div>
          <div class="resource-links">
            <h5>相关论坛资源:</h5>
            <a href="#" target="_blank">https://blog.csdn.net</a>
            <a href="#" target="_blank">https://zhuanlan.zhihu.com</a>
            <a href="#" target="_blank">https://www.cnblogs.com</a>
          </div>
          <el-button type="warning" plain class="save-res-btn" @click="saveResource">保存资源</el-button>
        </div>
      </template>

      <!-- ====== Tool: 图片生成 (Image Gen) ====== -->
      <template v-if="activeTool === 'image'">
        <el-form label-position="top" :model="imageForm" class="tool-form">
          <el-form-item label="描述提示词"><el-input v-model="imageForm.prompt" type="textarea" :rows="4" placeholder="请描述您想要生成的图片内容..." /></el-form-item>
          <el-form-item label="风格"><el-select v-model="imageForm.style" style="width:100%"><el-option label="卡通风格" value="cartoon" /><el-option label="写实风格" value="realistic" /><el-option label="扁平化" value="flat" /></el-select></el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="runAssistant('生成图片')">生成图片</el-button>
        </el-form>
      </template>

      <!-- ====== Tool: 图片理解 (Image Understand) ====== -->
      <template v-if="activeTool === 'explain'">
        <el-form label-position="top" :model="explainForm" class="tool-form">
          <el-form-item label="上传图片">
            <el-upload action="#" :auto-upload="false" :limit="1" list-type="picture" accept="image/*">
              <el-button>选择图片</el-button>
            </el-upload>
          </el-form-item>
          <el-form-item label="提问"><el-input v-model="explainForm.question" placeholder="请输入关于图片的问题..." /></el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="runAssistant('分析图片')">分析图片</el-button>
        </el-form>
      </template>

      <!-- ====== Tool: 创作PPT (PPT Creator) ====== -->
      <template v-if="activeTool === 'ppt'">
        <div class="ppt-form-area">
          <el-form label-position="top" :model="pptForm" class="tool-form compact">
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="教材名称"><el-input v-model="pptForm.textbook" placeholder="机器学习" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="教学目标"><el-input v-model="pptForm.target" placeholder="学生理解..." /></el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="专业学科"><el-input v-model="pptForm.major" placeholder="人工智能" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="单元主题"><el-input v-model="pptForm.unitTheme" value="线性回归" /></el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="年 级"><el-select v-model="pptForm.grade" style="width:100%"><el-option label="大一" value="1" /><el-option label="大二" value="2" /><el-option label="大三" value="3" /><el-option label="大四" value="4" /></el-select></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="每章知识点">
                  <el-select v-model="pptForm.points" multiple collapse-tags collapse-tags-tooltip style="width:100%">
                    <el-option label="1. 基本概念" value="basic" />
                    <el-option label="2. 模型训练" value="train" />
                    <el-option label="3. 特征工程" value="feature" />
                    <el-option label="4. 模型评估" value="eval" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="教学目标"><el-select v-model="pptForm.teachingGoal" style="width:100%"><el-option label="学生理解" value="understand" /><el-option label="掌握应用" value="apply" /></el-select></el-form-item>
              </el-col>
              <el-col :span="12"></el-col>
            </el-row>
            
            <!-- File Upload Area -->
            <div class="upload-zone">
              <el-icon :size="32" color="#93c5fd"><UploadFilled /></el-icon>
              <p>拖拽上传图片 / 点击上传</p>
              <small>支持 jpg/png 格式，最大 10MB</small>
            </div>
            <p class="upload-hint">为PPT封面提供素材(可选)，支持 word 及其他</p>

            <el-button type="primary" class="submit-btn" :loading="loading" @click="generatePpt">提交</el-button>
          </el-form>

          <!-- PPT Template Gallery (shown after submit) -->
          <div v-if="showPptGallery" class="ppt-gallery">
            <h4>PPT模板选择</h4>
            <div class="gallery-grid">
              <div v-for="(tmpl, idx) in pptTemplates" :key="idx" 
                   :class="['ppt-tmpl-card', { selected: selectedTemplate === idx }]"
                   @click="selectedTemplate = idx">
                <div class="tmpl-preview" :style="{ background: tmpl.bg }">
                  <span class="tmpl-title">{{ tmpl.name }}</span>
                  <p class="tmpl-desc">{{ tmpl.desc }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </section>

    <!-- Right: Chat Panel (SSE Streaming) -->
    <section class="chat-panel">
      <div class="chat-header">
        <div class="chat-user-info">
          <img src="https://dummyimage.com/40x40/4c8dff/ffffff&text=T" alt="user" />
          <div>
            <span class="chat-time">{{ currentTime }}</span>
            <strong class="chat-name">{{ auth.user?.displayName || '教师' }}</strong>
            <p>{{ lastMessage || '可以开始聊天了~' }}</p>
          </div>
        </div>
        <img src="https://dummyimage.com/60x60/f0f0f0/999&text=👩‍🏫" alt="avatar" class="chat-avatar-right" />
      </div>

      <!-- Chat Messages -->
      <div class="chat-messages" ref="chatScrollRef">
        <div v-for="(item, index) in messages" :key="index" :class="['msg-row', item.role]">
          <!-- AI Message -->
          <div v-if="item.role === 'assistant'" class="msg-bubble ai-bubble">
            <img src="https://dummyimage.com/36x36/eaf4ff/4c8dff.png&text=AI" alt="ai" class="msg-avatar" />
            <div class="bubble-content answer-content" v-html="item.html"></div>
          </div>
          <!-- User Message -->
          <div v-else class="msg-bubble user-bubble">
            <div class="bubble-content user-content">{{ item.text }}</div>
          </div>
        </div>
        
        <!-- Plan result special display -->
        <div v-if="planResult && activeTool === 'plan'" class="msg-row assistant">
          <div class="msg-bubble ai-bubble">
            <img src="https://dummyimage.com/36x36/eaf4ff/4c8dff.png&text=AI" alt="ai" class="msg-avatar" />
            <div class="bubble-content plan-result-box">
              <div class="plan-result-header">
                <span>三纲一案生成结果</span>
                <span class="result-time">{{ new Date().toLocaleString() }}</span>
              </div>
              <div class="plan-result-body" v-html="planResultHtml"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Input Area with Sticker -->
      <div class="chat-input-bar">
        <div class="sticker-area">
          <div class="sticker-character">
            <span class="char-face">🐶</span>
            <div class="scroll-banner">
              <span>v5m</span><span>什么?</span><span>2sm</span><span>3说明</span><span>4.O!o?!</span><span>5.</span>
            </div>
          </div>
        </div>
        <div class="input-row">
          <el-input v-model="question" placeholder="问我在问shift+enter来换行输入"
                   @keyup.enter.ctrl="sendQuestion"
                   clearable>
            <template #prefix>✏️</template>
          </el-input>
          <el-button circle type="primary" :loading="loading" @click="sendQuestion" :disabled="!question.trim()">➤</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, computed, nextTick, onUnmounted } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { openTutorSSE } from '../../api/sse'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const question = ref('')
const messages = ref([{ role: 'assistant', html: '<p>可以开始聊天了~</p>' }])
const activeTool = ref('qa')
const chatScrollRef = ref(null)
const planResult = ref('')
const planResultHtml = ref('')
const showPptGallery = ref(false)
const selectedTemplate = ref(0)
const sseConn = ref(null)

// Forms
const planForm = reactive({ type: '', courseName: '计算机导论', hours: 1, goal: '掌握计算机系统基本概念和原理，理解数据表示与运算的基本方法。教学时长达1个学时，4学分，考核方式包括平时考核、期中考核和期末考核。数学方式是课堂授课、讨论、网络教学、演示、实践。', credit: 4, assessment: [], methods: ['课堂讲授'] })
const prepareForm = reactive({ course: '', chapter: '', goal: '' })
const paperForm = reactive({ bankType: 'choice', topic: '', difficulty: 2, count: 5 })
const imageForm = reactive({ prompt: '', style: 'cartoon' })
const explainForm = reactive({ question: '' })
const pptForm = reactive({ textbook: '机器学习', target: '学生理解', major: '人工智能', unitTheme: '线性回归', grade: '2', points: ['basic','train'], teachingGoal: 'understand' })

const menuItems = [
  { key: 'qa', label: '智能问答', icon: '💬' },
  { key: 'plan', label: '三纲一案', icon: '📋' },
  { key: 'prepare', label: '智慧备课', icon: '📝' },
  { key: 'paper', label: '试题生成', icon: '📄' },
  { key: 'resource', label: '教学资源', icon: '🎬' },
  { key: 'image', label: '图片生成', icon: '🖼️' },
  { key: 'explain', label: '图片理解', icon: '🔍' },
  { key: 'ppt', label: '创作PPT', icon: '📊' }
]

const pptTemplates = [
  { name: '初生不凡·梦想起航', desc: '新学期 新目标', bg: 'linear-gradient(135deg, #a78bfa, #7c3aed)' },
  { name: '校园心理主题教育知识', desc: '心理健康从心开始', bg: 'linear-gradient(135deg, #fb923c, #ea580c)' },
  { name: '以心迎"新" 同心同行', desc: '24届新生班会', bg: 'linear-gradient(135deg, #374151, #111827)' },
  { name: '环球旅行的时间', desc: '划方案讨论', bg: 'linear-gradient(135deg, #38bdf8, #0284c7)' },
  { name: '商务数据分析工作流程汇总', desc: '', bg: 'linear-gradient(135deg, #60a5fa, #2563eb)' },
  { name: '素质教育落地', desc: '', bg: 'linear-gradient(135deg, #fbbf24, #d97706)' },
  { name: '摩滴汗水竞技体育的魅力', desc: '', bg: 'linear-gradient(135deg, #34d399, #059669)' },
  { name: '世界读书日读书点亮生活', desc: '', bg: 'linear-gradient(135deg, #f472b6, #db2777)' }
]

const currentMenu = computed(() => menuItems.find(i => i.key === activeTool.value) || menuItems[0])
const currentTime = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
})
const lastMessage = computed(() => {
  const userMsgs = messages.value.filter(m => m.role === 'user')
  return userMsgs.length > 0 ? userMsgs[userMsgs.length - 1].text?.slice(0, 20) + '...' : ''
})

const switchTool = (key) => {
  if (sseConn.value) {
    sseConn.value.close()
    sseConn.value = null
  }
  activeTool.value = key
}

const sendQuestion = () => {
  if (!question.value.trim()) return
  runAssistant(question.value.trim())
}

const runAssistant = async (customText) => {
  const askText = customText || question.value
  if (!askText) return
  
  // For specific tools, build context from forms
  let contextStr = ''
  if (activeTool.value === 'plan') {
    contextStr = JSON.stringify(planForm)
  } else if (activeTool.value === 'ppt') {
    contextStr = JSON.stringify(pptForm)
  }

  loading.value = true
  const userText = typeof customText === 'string' && customText !== question.value ? customText : question.value
  if (typeof customText === 'string' && customText !== question.value) {
    question.value = customText
  }
  
  messages.value.push({ role: 'user', text: userText })
  const assistantMsg = { role: 'assistant', html: '' }
  messages.value.push(assistantMsg)
  
  let markdown = ''

  sseConn.value = openTutorSSE({ question: askText, context: contextStr || JSON.stringify({}), answerMode: activeTool.value }, {
    onDelta(chunk) {
      markdown += chunk
      assistantMsg.html = DOMPurify.sanitize(marked.parse(markdown))
      
      // Auto-scroll
      nextTick(() => {
        if (chatScrollRef.value) {
          chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
        }
      })

      // Store plan result for export
      if (activeTool.value === 'plan') {
        planResult.value = markdown
        planResultHtml.value = DOMPurify.sanitize(marked.parse(markdown))
      }
    },
    onDone() {
      loading.value = false
      question.value = ''
      
      // Show PPT gallery after generation
      if (activeTool.value === 'ppt') {
        showPptGallery.value = true
      }
    },
    onError() {
      loading.value = false
      const errMsg = '<p style="color:#e53e3e;font-weight:600;">[连接错误] 后端服务不可用，请稍后重试。</p>'
      assistantMsg.html = markdown
        ? DOMPurify.sanitize(marked.parse(markdown)) + errMsg
        : DOMPurify.sanitize(marked.parse(getMockResponse(activeTool.value, askText)))

      if (activeTool.value === 'plan') {
        planResult.value = markdown || getMockResponse(activeTool.value, askText)
        planResultHtml.value = DOMPurify.sanitize(marked.parse(planResult.value))
      }
      if (activeTool.value === 'ppt') {
        showPptGallery.value = true
      }
      question.value = ''
    },
    onClose() {
      loading.value = false
    }
  })
}

const generatePpt = () => {
  if (!Array.isArray(pptForm.points)) return
  runAssistant(`请根据以下信息创作PPT：教材名称=${pptForm.textbook}，教学目标=${pptForm.target}，专业学科=${pptForm.major}，单元主题=${pptForm.unitTheme}，年级=${pptForm.grade}，知识点=${pptForm.points.join(',')}，模板=${pptTemplates[selectedTemplate.value]?.name || '默认模板'}`)
}

const saveResource = () => {
  ElMessage.success('资源已保存到我的资源库')
}

const exportToWord = (type) => {
  // Create a downloadable HTML file as Word document
  const content = `
    <!DOCTYPE html>
    <html>
    <head><meta charset="utf-8"><title>${currentMenu.value.label}</title></head>
    <body style="font-family: SimSun, serif; padding: 40px; line-height: 1.8;">
      <h1 style="text-align:center; border-bottom:2px solid #333; padding-bottom:15px;">
        ${planForm.courseName || currentMenu.value.label}
      </h1>
      <h2>一、基本信息</h2>
      <p>课程名称：${planForm.courseName}</p>
      <p>教学时长：${planForm.hours} 学时</p>
      <p>学分：${planForm.credit}</p>
      <h2>二、教学目标</h2>
      <p>${planForm.goal}</p>
      <h2>三、考核方式</h2>
      <p>${Array.isArray(planForm.assessment) ? planForm.assessment.join('、') : planForm.assessment}</p>
      <h2>四、教学内容（AI生成）</h2>
      <div>${planResultHtml || '<p style="color:#999;">请先生成三纲一案内容</p>'}</div>
    </body>
    </html>
  `
  const blob = new Blob(['\ufeff' + content], { type: 'application/msword' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${planForm.courseName || currentMenu.value.label}_三纲一案.doc`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('Word文档已下载')
}

const printResult = (type) => {
  const printWindow = window.open('', '_blank')
  printWindow.document.write(`
    <html>
    <head><title>打印预览 - ${currentMenu.value.label}</title>
    <style>body{font-family:SimSun,serif;padding:30px;line-height:1.8} h1{text-align:center;border-bottom:2px solid #333;padding-bottom:15px;}</style>
    </head>
    <body>
      <h1>${planForm.courseName || currentMenu.value.label} - 打印预览</h1>
      ${planResultHtml || '<p>暂无内容</p>'}
      <script>window.print();<\/script>
    </body>
    </html>
  `)
  printWindow.document.close()
}

// Mock responses for demo when backend is unavailable
onUnmounted(() => {
  if (sseConn.value) {
    sseConn.value.close()
    sseConn.value = null
  }
})

function getMockResponse(tool, text) {
  switch (tool) {
    case 'plan':
      return `## ${planForm.courseName || '计算机导论'} - 三纲一案

### 一、教学大纲
本课程面向**大学本科**学生开设，属于**${planForm.credit || 4}学分**必修课。
教学总时长 **${planForm.hours || 1}** 学时。

### 二、授课计划
| 周次 | 内容 | 学时 |
|------|------|------|
| 第1周 | 课程概述与历史 | 2 |
| 第2周 | 数据表示方法 | 4 |
| 第3周 | 运算器与CPU结构 | 6 |
| ... | ... | ... |

### 三、考试大纲
**考核方式：** ${Array.isArray(planForm.assessment) && planForm.assessment.length > 0 ? planForm.assessment.join('、') : '包含平时考勤、课堂表现、期中考核和期末考核'}

**评分比例：**
- 平时成绩：30%
- 期中考试：30%
- 期末考试：40%

### 四、教案设计
#### 进阶
培养学生用辩证思维处理复杂问题的能力。

#### 理论教学与实践相结合
增强学生对专业的学习兴趣。

---

> ⚠️ *以上内容由AI辅助生成，建议结合实际教学需求进行调整*
`
    case 'ppt':
      return `## PPT创作完成！

已为您生成 **${pptForm.textbook}** 相关的 **${pptForm.unitTheme}** 主题PPT。

**包含以下幻灯片：**
1. 封面页：标题 + 副标题
2. 目录页：章节导航
3. 内容页：核心知识点展示
4. 总结页：要点回顾
5. 致谢页

请在右侧模板区域选择喜欢的风格，或直接使用默认模板导出。`
    case 'qa':
      return `好的！关于"${text}"的问题，我来为您解答：

这是一个很好的问题。基于我的知识库，我为您提供以下分析和建议：

1. **核心观点**：首先我们需要明确问题的本质
2. **具体方案**：针对您的需求，建议采取分步骤的方式
3. **注意事项**：在实施过程中需要注意以下几点...

希望这个回答对您有帮助！如有更多问题，欢迎继续提问。`
    default:
      return `收到您的请求："${text}"。正在为您处理中...\n\n由于当前未连接到后端服务，这是模拟回复。在实际部署环境中，这里将显示真实的AI流式输出结果。`
  }
}
</script>

<style scoped>
.teacher-assistant-page {
  display: grid;
  grid-template-columns: 180px 360px 1fr;
  gap: 14px;
  min-height: calc(100vh - 140px);
}

/* ========== Tool Sidebar ========== */
.tool-sidebar { padding: 16px; overflow-y: auto; max-height: calc(100vh - 160px); }
.menu-head h3 { margin: 8px 0 14px; font-size: 18px; font-weight: 700; color: #1e293b; }
.eyebrow { font-size: 10px; letter-spacing: 1.5px; color: #94a3b8; margin: 0; font-weight: 600; text-transform: uppercase; }
.tool-nav { display: grid; gap: 4px; }
.tool-item {
  display: flex; align-items: center; gap: 9px; padding: 10px 12px;
  border-radius: 10px; cursor: pointer; color: #52667f; transition: all .2s ease;
  font-size: 13.5px; font-weight: 500;
}
.tool-item:hover { background: #f3f8ff; color: #4d8fff; }
.tool-item.active { background: linear-gradient(90deg, #eef4ff, #fff); color: #2563eb; box-shadow: inset 2px 0 0 #4d8fff; font-weight: 600; }
.tool-icon { font-size: 16px; width: 22px; text-align: center; }
.tool-item em { font-style: normal; }

/* ========== Form Area ========== */
.form-area { padding: 18px; overflow-y: auto; max-height: calc(100vh - 160px); }
.form-breadcrumb { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #94a3b8; margin-bottom: 14px; flex-wrap: wrap; }
.form-breadcrumb strong { color: #1e293b; font-weight: 600; }
.tool-form { display: grid; gap: 4px; }
.submit-btn { width: 100%; height: 38px; border-radius: 10px; margin-top: 8px; letter-spacing: 2px; font-weight: 600; }
.export-section { display: flex; gap: 10px; margin-top: 14px; justify-content: center; }

/* QA Welcome */
.qa-welcome { display: flex; align-items: center; gap: 12px; padding: 24px 16px; }
.qa-avatar { width: 48px; height: 48px; border-radius: 50%; }
.qa-welcome strong { display: block; font-size: 15px; color: #1e293b; }
.qa-welcome p { margin: 4px 0 0; color: #94a3b8; font-size: 13px; }

/* Resource Preview */
.resource-preview { display: grid; gap: 14px; }
.res-card { border-radius: 12px; overflow: hidden; border: 1px solid #edf2f8; }
.res-img-placeholder, .video-placeholder {
  height: 120px; display: flex; flex-direction: column; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1e3a5f, #0f172a); color: white; gap: 6px;
}
.video-placeholder { background: #000; position: relative; }
.video-placeholder span { font-size: 32px; }
.video-placeholder p { font-size: 12px; opacity: .7; }
.res-img-placeholder span { font-size: 36px; }
.resource-links h5 { margin: 6px 0 8px; font-size: 13px; color: #334155; }
.resource-links a { display: block; color: #2563eb; font-size: 12.5px; text-decoration: none; margin-bottom: 4px; }
.save-res-btn { width: 100%; margin-top: 4px; }

/* Upload Zone */
.upload-zone {
  border: 2px dashed #d0d7de; border-radius: 14px; padding: 28px; text-align: center;
  background: #fafbff; cursor: pointer; margin-top: 8px; transition: all .2s;
}
.upload-zone:hover { border-color: #93c5fd; background: #f0f5ff; }
.upload-zone p { margin: 8px 0 2px; color: #475569; font-size: 13px; }
.upload-zone small { color: #94a3b8; font-size: 11.5px; }
.upload-hint { font-size: 11.5px; color: #94a3b8; text-align: center; margin: 6px 0 0; }
.compact .el-form-item { margin-bottom: 10px; }

/* PPT Gallery */
.ppt-gallery { margin-top: 18px; }
.ppt-gallery h4 { font-size: 14px; color: #1e293b; margin: 0 0 10px; }
.gallery-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.ppt-tmpl-card {
  border-radius: 10px; overflow: hidden; cursor: pointer;
  border: 2px solid transparent; transition: all .2s; aspect-ratio: 16/9;
}
.ppt-tmpl-card.selected { border-color: #3b82f6; transform: scale(1.02); box-shadow: 0 4px 16px rgba(59,130,246,.25); }
.tmpl-preview {
  height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: white; padding: 10px; text-align: center;
}
.tmpl-title { font-size: 13px; font-weight: 700; text-shadow: 0 1px 3px rgba(0,0,0,.3); }
.tmpl-desc { font-size: 11px; opacity: .85; margin-top: 2px; }

/* ========== Chat Panel ========== */
.chat-panel {
  display: grid; grid-template-rows: auto 1fr auto; gap: 0;
  background: linear-gradient(180deg, #fffefc, #fffbe9);
  border-radius: 14px;
  overflow: hidden;
  min-height: calc(100vh - 160px);
}
.chat-header {
  padding: 14px 16px; display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #eef3f8; background: rgba(255,255,255,.7);
}
.chat-user-info { display: flex; align-items: center; gap: 10px; }
.chat-user-info img { width: 40px; height: 40px; border-radius: 50%; }
.chat-name { font-size: 14px; color: #2563eb; font-weight: 600; }
.chat-time { font-size: 11px; color: #94a3b8; }
.chat-user-info p { margin: 2px 0 0; font-size: 12px; color: #64748b; }
.chat-avatar-right { width: 52px; height: 52px; border-radius: 50%; object-fit: cover; }

/* Messages */
.chat-messages { overflow-y: auto; padding: 14px 12px; display: grid; gap: 14px; }
.msg-row { display: flex; }
.msg-row.user { justify-content: flex-end; }
.msg-row.assistant { justify-content: flex-start; }
.msg-bubble { display: flex; align-items: flex-start; gap: 8px; max-width: 82%; }
.msg-avatar { width: 34px; height: 34px; border-radius: 50%; flex-shrink: 0; }
.bubble-content {
  padding: 10px 14px; border-radius: 16px; font-size: 13.5px; line-height: 1.75;
  word-break: break-word;
}
.ai-bubble .answer-content { background: #fff; border: 1px solid #eff3f8; color: #34475f; border-top-left-radius: 4px; }
.user-bubble .user-content { background: #ffc83a; border-color: #f2be38; color: #333; border-top-right-radius: 4px; }
.answer-content :deep(p), .answer-content :deep(h1), .answer-content :deep(h2),
.answer-content :deep(h3), .answer-content :deep(ul), .answer-content :deep(ol),
.answer-content :deep(table), .answer-content :deep(blockquote) { margin: 8px 0; }
.answer-content :deep(table) { border-collapse: collapse; width: 100%; }
.answer-content :deep(th), .answer-content :deep(td) { border: 1px solid #ddd; padding: 6px 10px; text-align: left; font-size: 12.5px; }
.answer-content :deep(th) { background: #f8fafc; font-weight: 600; }
.answer-content :deep(code) { background: #f1f5f9; padding: 2px 5px; border-radius: 4px; font-size: 12px; }

/* Plan Result Box */
.plan-result-box { background: #fffbeb; border: 1px solid #fef3c7; border-radius: 14px; overflow: hidden; }
.plan-result-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 14px; background: #fef3c7; font-size: 13px; font-weight: 600; color: #92400e;
}
.result-time { font-size: 11px; color: #b45309; font-weight: 400; }
.plan-result-body { padding: 14px; }

/* Chat Input */
.chat-input-bar { border-top: 1px solid #eef3f8; padding: 10px 12px; background: #fffefa; }
.sticker-area { margin-bottom: 8px; }
.sticker-character {
  display: inline-flex; align-items: center; gap: 6px;
  background: linear-gradient(90deg, #fef3c7, #fde68a); border: 1px solid #fcd34d;
  border-radius: 20px; padding: 4px 12px 4px 6px;
}
.char-face { font-size: 22px; }
.scroll-banner {
  display: flex; gap: 8px; font-size: 11px; color: #92400e; font-weight: 500;
  animation: scrollBanner 8s linear infinite; white-space: nowrap;
}
@keyframes scrollBanner { to { transform: translateX(-50%); } }
.input-row { display: grid; grid-template-columns: 1fr 40px; gap: 8px; align-items: center; }

@media (max-width: 1200px) {
  .teacher-assistant-page { grid-template-columns: 1fr; }
  .tool-sidebar { max-height: none; order: 0; }
  .form-area { max-height: none; order: 2; }
  .chat-panel { min-height: 500px; order: 1; }
}
</style>
