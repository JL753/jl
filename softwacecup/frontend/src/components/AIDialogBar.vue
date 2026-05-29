<template>
  <div class="ai-bar-root">
    <div class="ai-bar-panel">
      <!-- 左侧：智能体选择下拉 -->
      <div class="agent-select" @click="showAgentDropdown = !showAgentDropdown">
        <div class="agent-current">
          <span class="agent-name">{{ selectedAgent.label }}</span>
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
        </div>
        <span class="agent-hint">AI意图分析</span>
        <Transition name="drop-fade">
          <div v-if="showAgentDropdown" class="agent-dropdown">
            <div
              v-for="agent in agents" :key="agent.key"
              :class="['agent-option', { active: selectedAgent.key === agent.key }]"
              @click.stop="selectAgent(agent)"
            >
              {{ agent.label }}
            </div>
          </div>
        </Transition>
      </div>

      <!-- 中间：输入框 -->
      <div class="input-area">
        <input
          ref="inputRef"
          v-model="inputText"
          type="text"
          class="ai-input"
          placeholder="输入你想学习的内容或问题，AI智能体将为你分析意图并匹配最佳方案..."
          @keyup.enter="sendMessage"
          @focus="showRouteTag = true"
          @blur="onInputBlur"
        />
        <!-- 路由Agent标签 -->
        <Transition name="tag-fade">
          <span v-if="showRouteTag" class="route-tag" @mousedown.prevent="toggleRouteInfo">
            路由Agent架构
          </span>
        </Transition>
      </div>

      <!-- 右侧：操作按钮 -->
      <div class="action-btns">
        <button class="send-btn" @click="sendMessage" :disabled="!inputText.trim() || loading">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
          </svg>
        </button>
        <button class="voice-btn" title="语音输入">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
            <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
            <line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- 路由Agent架构展开 -->
    <Transition name="expand-fade">
      <div v-if="showRouteInfo" class="route-info-panel">
        <div class="route-flow">
          <div class="flow-step">
            <div class="flow-dot"></div>
            <span>用户输入</span>
          </div>
          <div class="flow-arrow">→</div>
          <div class="flow-step">
            <div class="flow-dot intent"></div>
            <span>意图分析</span>
          </div>
          <div class="flow-arrow">→</div>
          <div class="flow-step">
            <div class="flow-dot sub"></div>
            <span>子智能体</span>
          </div>
          <div class="flow-arrow">→</div>
          <div class="flow-step">
            <div class="flow-dot output"></div>
            <span>输出</span>
          </div>
        </div>
        <div class="flow-labels">
          <span>课程咨询 · 知识讲解 · 学情诊断 · 路径规划</span>
        </div>
      </div>
    </Transition>

    <!-- 响应结果区 -->
    <Transition name="expand-fade">
      <div v-if="aiResponse || loading" class="response-panel">
        <div v-if="loading" class="loading-indicator">
          <span class="dot"></span><span class="dot"></span><span class="dot"></span>
        </div>
        <div v-else class="response-content" v-html="renderedResponse"></div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'

const emit = defineEmits(['update:courses'])

const inputText = ref('')
const loading = ref(false)
const aiResponse = ref('')
const showAgentDropdown = ref(false)
const showRouteTag = ref(false)
const showRouteInfo = ref(false)
const selectedAgent = ref({ key: 'tutor', label: '智能导师' })
const inputRef = ref(null)

const agents = [
  { key: 'tutor', label: '智能导师' },
  { key: 'course', label: '课程匹配' },
  { key: 'knowledge', label: '知识讲解' },
  { key: 'diagnosis', label: '学情诊断' },
  { key: 'path', label: '路径规划' },
]

const selectAgent = (agent) => {
  selectedAgent.value = agent
  showAgentDropdown.value = false
}

const onInputBlur = () => {
  setTimeout(() => { showRouteTag.value = false }, 200)
}

const toggleRouteInfo = () => {
  showRouteInfo.value = !showRouteInfo.value
}

const renderedResponse = computed(() => {
  if (!aiResponse.value) return ''
  // Basic markdown: bold, lists, code, newlines
  return aiResponse.value
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/^## (.+)$/gm, '<h3 style="color:#60a5fa;margin:12px 0 6px;">$1</h3>')
    .replace(/\n- /g, '\n<span class="md-li">• </span>')
    .replace(/\n(\d+)\. /g, '\n<span class="md-li">$1. </span>')
    .replace(/\n/g, '<br>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
})

const isURL = (text) => {
  return /^https?:\/\//i.test(text) || /^www\./i.test(text)
}

const detectIntent = (text) => {
  const lower = text.toLowerCase()
  if (/课程|推荐|学习.*方向|选课|有什么课/.test(lower)) return 'course'
  if (/知识|概念|什么是|怎么理解|解释|原理/.test(lower)) return 'knowledge'
  if (/成绩|进度|诊断|分析|评估|报告/.test(lower)) return 'diagnosis'
  if (/路径|规划|计划|路线|怎么学|学习顺序/.test(lower)) return 'path'
  return 'tutor'
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  // URL detection: open directly
  if (isURL(text)) {
    const url = text.startsWith('www.') ? 'https://' + text : text
    window.open(url, '_blank')
    inputText.value = ''
    return
  }

  inputText.value = ''
  loading.value = true
  aiResponse.value = ''

  const intent = detectIntent(text)
  const agent = agents.find(a => a.key === intent)
  if (agent) {
    selectedAgent.value = agent
  }

  try {
    const endpointMap = {
      tutor: 'course-recommend',
      course: 'course-recommend',
      knowledge: 'knowledge',
      diagnosis: 'diagnosis',
      path: 'path-planning'
    }
    const endpoint = endpointMap[intent] || 'knowledge'
    const response = await fetch(`/api/agent/${endpoint}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: text })
    })
    const result = await response.json()
    aiResponse.value = result.content || result.message || '暂无回复'

    // Sync recommended courses to parent if available
    if (result.courses && result.courses.length > 0) {
      emit('update:courses', result.courses)
    }
  } catch {
    // Fallback to mock responses on network error
    const fallback = {
      tutor: '你好！我是**智能导师**。\n\n根据你的问题，我建议从以下几个方面入手：\n- 明确学习目标和时间安排\n- 选择适合当前水平的课程\n- 制定阶段性的学习计划\n\n你可以切换上方智能体类型，获取更精准的帮助。',
      course: '## 课程推荐分析\n\n根据你的学习背景和兴趣方向，为你推荐以下课程方向：\n- **Web全栈开发**：适合零基础入门，就业前景广阔\n- **数据结构与算法**：夯实编程基础，提升逻辑思维\n- **机器学习基础**：AI时代必备技能\n\n下方已为你展示推荐课程卡片，点击可查看详情。',
      knowledge: '## 知识讲解\n\n这是一个很好的问题！让我来为你详细解释：\n\n该概念的核心要点包括：\n- 基础定义与相关背景\n- 关键原理和运行机制\n- 实际应用场景与案例分析\n\n如果还有疑问，可以继续追问，我会深入讲解。',
      diagnosis: '## 学情诊断报告\n\n根据当前学习数据分析：\n- **知识掌握度**：整体良好，部分概念需巩固\n- **学习效率**：近期呈上升趋势\n- **薄弱环节**：算法复杂度分析、数据库优化\n\n建议针对薄弱环节制定专项练习计划。',
      path: '## 学习路径规划\n\n为你规划以下学习路线：\n1. **第一阶段**（1-2周）：基础知识铺垫\n2. **第二阶段**（2-4周）：核心技能训练\n3. **第三阶段**（4-6周）：项目实战演练\n4. **第四阶段**（持续）：进阶与专项提升\n\n每个阶段都配有对应的推荐课程，请查看下方卡片。',
    }
    aiResponse.value = fallback[intent] || fallback.tutor
  }

  loading.value = false
  await nextTick()
}
</script>

<style scoped>
.ai-bar-root {
  max-width: 1200px; margin: 0 auto;
  display: flex; flex-direction: column; gap: 12px;
}

/* ═══ Main Panel ═══ */
.ai-bar-panel {
  display: flex; align-items: center; gap: 12px;
  padding: 16px 20px;
  background: rgba(255,255,255,0.06);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 16px;
}

/* ═══ Agent Select ═══ */
.agent-select {
  position: relative;
  width: 160px; flex-shrink: 0;
  padding: 10px 14px; border-radius: 10px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  cursor: pointer; user-select: none;
  transition: all 0.2s;
}
.agent-select:hover { border-color: rgba(33,150,243,0.3); }
.agent-current {
  display: flex; align-items: center; justify-content: space-between;
  color: #2196F3; font-size: 13px; font-weight: 600;
}
.agent-hint {
  font-size: 10px; color: rgba(255,255,255,0.3); margin-top: 2px; display: block;
}

.agent-dropdown {
  position: absolute; top: 100%; left: 0; right: 0;
  margin-top: 6px; padding: 6px;
  background: #1a3a5c;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 10px; z-index: 50;
  display: flex; flex-direction: column; gap: 2px;
}
.agent-option {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; border-radius: 6px; height: 30px;
  color: rgba(255,255,255,0.6); font-size: 13px;
  cursor: pointer; transition: all 0.15s;
}
.agent-option:hover { background: rgba(255,255,255,0.1); color: white; }
.agent-option.active { background: rgba(33,150,243,0.2); color: #2196F3; }

.drop-fade-enter-active { transition: all 0.2s ease; }
.drop-fade-leave-active { transition: all 0.15s ease; }
.drop-fade-enter-from, .drop-fade-leave-to { opacity: 0; transform: translateY(-6px); }

/* ═══ Input Area ═══ */
.input-area {
  flex: 1; position: relative;
}
.ai-input {
  width: 100%; height: 50px; padding: 0 70px 0 16px;
  border-radius: 12px; border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.05);
  color: rgba(255,255,255,0.85); font-size: 14px;
  outline: none; transition: border-color 0.2s;
  box-sizing: border-box;
}
.ai-input::placeholder { color: rgba(255,255,255,0.25); font-size: 13px; }
.ai-input:focus { border-color: rgba(33,150,243,0.35); }

.route-tag {
  position: absolute; right: 12px; top: 50%; transform: translateY(-50%);
  padding: 2px 10px; border-radius: 4px;
  background: rgba(255,107,107,0.12);
  color: #FF6B6B; font-size: 10px; cursor: pointer;
  white-space: nowrap; transition: all 0.2s;
}
.route-tag:hover { background: rgba(255,107,107,0.2); }
.tag-fade-enter-active { transition: all 0.2s; }
.tag-fade-leave-active { transition: all 0.15s; }
.tag-fade-enter-from, .tag-fade-leave-to { opacity: 0; }

/* ═══ Action Buttons ═══ */
.action-btns { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.send-btn {
  width: 44px; height: 44px; border-radius: 50%; border: none;
  background: #2196F3;
  color: white; cursor: pointer; display: flex;
  align-items: center; justify-content: center;
  transition: all 0.2s;
  box-shadow: 0 4px 16px rgba(33,150,243,0.3);
}
.send-btn:hover:not(:disabled) { transform: scale(1.05); }
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.voice-btn {
  width: 36px; height: 36px; border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.12);
  background: rgba(255,255,255,0.05);
  color: rgba(255,255,255,0.4);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.voice-btn:hover { background: rgba(255,255,255,0.08); color: rgba(255,255,255,0.6); }

/* ═══ Route Info Panel ═══ */
.route-info-panel {
  padding: 16px 20px;
  background: rgba(255,255,255,0.04);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px;
}
.route-flow {
  display: flex; align-items: center; justify-content: center; gap: 16px;
}
.flow-step {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: rgba(255,255,255,0.5);
}
.flow-dot {
  width: 10px; height: 10px; border-radius: 50%;
  background: rgba(33,150,243,0.4);
}
.flow-dot.intent { background: rgba(245,158,11,0.5); }
.flow-dot.sub { background: rgba(16,185,129,0.5); }
.flow-dot.output { background: rgba(139,92,246,0.5); }
.flow-arrow { color: rgba(255,255,255,0.25); font-size: 16px; }
.flow-labels {
  text-align: center; margin-top: 8px;
  font-size: 11px; color: rgba(255,255,255,0.3);
}

.expand-fade-enter-active { transition: all 0.3s ease; }
.expand-fade-leave-active { transition: all 0.2s ease; }
.expand-fade-enter-from, .expand-fade-leave-to {
  opacity: 0; transform: translateY(-8px); max-height: 0;
}

/* ═══ Response Panel ═══ */
.response-panel {
  padding: 16px 20px;
  background: rgba(255,255,255,0.04);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px;
  min-height: 40px;
}
.response-content {
  font-size: 13px; color: rgba(255,255,255,0.75);
  line-height: 1.8;
}
.response-content :deep(strong) { color: #60a5fa; font-weight: 600; }
.response-content :deep(h3) { color: #60a5fa; margin: 12px 0 6px; font-size: 14px; }
.response-content :deep(code) {
  background: rgba(255,255,255,0.08); padding: 2px 6px;
  border-radius: 4px; font-size: 12px;
}
.response-content :deep(.md-li) { color: rgba(255,255,255,0.3); }

/* Loading dots */
.loading-indicator {
  display: flex; gap: 5px; align-items: center; padding: 4px 0;
}
.loading-indicator .dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: rgba(255,255,255,0.3);
  animation: bounce 1.2s ease-in-out infinite;
}
.loading-indicator .dot:nth-child(2) { animation-delay: 0.2s; }
.loading-indicator .dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-8px); }
}

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .ai-bar-panel { flex-direction: column; gap: 10px; }
  .agent-select { width: 100%; }
  .input-area { width: 100%; }
  .action-btns { align-self: flex-end; }
}
</style>
