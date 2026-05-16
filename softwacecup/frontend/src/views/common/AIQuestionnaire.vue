<template>
  <div class="questionnaire-root">
    <!-- Background effects -->
    <div class="bg-glow glow-1"></div>
    <div class="bg-glow glow-2"></div>

    <div class="questionnaire-container">
      <!-- Header -->
      <div class="header">
        <h1 class="title">AI 初始问卷</h1>
        <p class="subtitle">让我了解你的学习情况，为你定制专属学习方案</p>
      </div>

      <!-- In-progress state -->
      <template v-if="!submitted">
        <!-- Progress bar -->
        <div class="progress-section">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
          </div>
          <span class="progress-text">第 {{ currentStep + 1 }} / {{ steps.length }} 题</span>
        </div>

        <!-- Question card with transition -->
        <Transition name="slide" mode="out-in">
          <div class="question-card" :key="currentStep">
            <!-- AI Avatar -->
            <div class="ai-section">
              <div class="ai-avatar">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <rect x="2" y="2" width="20" height="8" rx="2" ry="2" />
                  <rect x="2" y="14" width="20" height="8" rx="2" ry="2" />
                  <line x1="6" y1="6" x2="6.01" y2="6" />
                  <line x1="6" y1="18" x2="6.01" y2="18" />
                </svg>
              </div>
              <div class="question-bubble">
                <p class="question-text">{{ steps[currentStep].question }}</p>
              </div>
            </div>

            <!-- Options -->
            <div class="options-area">
              <button
                v-for="(opt, idx) in steps[currentStep].options"
                :key="idx"
                :class="['option-btn', { selected: selectedOption === opt }]"
                @click="selectOption(opt)"
              >
                <span class="option-icon">{{ steps[currentStep].icons?.[idx] || '○' }}</span>
                <span class="option-label">{{ opt }}</span>
              </button>
            </div>
          </div>
        </Transition>
      </template>

      <!-- Completed state -->
      <div v-else class="result-section">
        <Transition name="fade-up" mode="out-in">
          <div class="result-card" key="result">
            <div class="result-icon">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
            </div>
            <h2 class="result-title">问卷完成</h2>
            <p class="result-desc">AI 已根据你的回答分析了你的学习能力画像</p>

            <!-- Ability chart -->
            <div class="ability-chart">
              <div
                v-for="a in abilities"
                :key="a.key"
                class="ability-bar"
              >
                <div class="ability-header">
                  <span class="ability-label">{{ a.label }}</span>
                  <span class="ability-value">{{ a.value }}%</span>
                </div>
                <div class="ability-track">
                  <div
                    class="ability-fill"
                    :style="{ width: a.value + '%' }"
                    :class="a.color"
                  ></div>
                </div>
              </div>
            </div>

            <button class="start-btn" @click="goToDashboard">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <polygon points="5 3 19 12 5 21 5 3" />
              </svg>
              开始学习
            </button>
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { apiBuildProfile } from '../../api/index.js'

const router = useRouter()

let pendingTimeout = null

onBeforeUnmount(() => {
  if (pendingTimeout) clearTimeout(pendingTimeout)
})

const currentStep = ref(0)
const selectedOption = ref('')
const submitted = ref(false)

const steps = [
  {
    question: '你当前的学习阶段是？',
    options: ['高中', '大学', '在职'],
    icons: ['🎓', '📚', '💼'],
    key: 'stage',
  },
  {
    question: '你已掌握的知识领域？',
    options: ['编程基础', '数学', '无'],
    icons: ['💻', '📐', '🌱'],
    key: 'knowledge',
  },
  {
    question: '你的学习目标是？',
    options: ['考试', '兴趣', '技能提升'],
    icons: ['🎯', '💡', '🚀'],
    key: 'goal',
  },
  {
    question: '每天可用学习时间是？',
    options: ['<30分钟', '1小时', '2小时以上', '不固定'],
    icons: ['⏳', '⌛', '🔥', '🔁'],
    key: 'time',
  },
  {
    question: '你喜欢的学习风格是？',
    options: ['图文阅读', '视频学习', '动手练习', '混合模式'],
    icons: ['📖', '🎬', '✍️', '🎨'],
    key: 'style',
  },
]

const answers = ref({
  stage: '',
  knowledge: '',
  goal: '',
  time: '',
  style: '',
})

const progressPercent = computed(() => {
  if (submitted.value) return 100
  return ((currentStep.value + 1) / steps.length) * 100
})

// Generate estimated ability levels based on answers
const abilities = computed(() => {
  // Default moderate levels
  const base = {
    breadth: { label: '知识广度', value: 50, color: 'blue' },
    depth: { label: '知识深度', value: 40, color: 'purple' },
    problemSolving: { label: '解题能力', value: 45, color: 'cyan' },
    activity: { label: '学习活跃度', value: 55, color: 'emerald' },
    transfer: { label: '知识迁移', value: 40, color: 'amber' },
    resilience: { label: '学习韧性', value: 50, color: 'rose' },
  }

  const a = answers.value

  // Adjust based on stage
  if (a.stage === '大学') {
    base.depth.value += 15
    base.problemSolving.value += 10
    base.transfer.value += 10
  } else if (a.stage === '在职') {
    base.breadth.value += 10
    base.transfer.value += 15
    base.resilience.value += 10
  }

  // Adjust based on knowledge
  if (a.knowledge === '编程基础') {
    base.breadth.value += 15
    base.problemSolving.value += 10
  } else if (a.knowledge === '数学') {
    base.depth.value += 10
    base.problemSolving.value += 15
  }

  // Adjust based on goal
  if (a.goal === '考试') {
    base.depth.value += 10
    base.problemSolving.value += 10
    base.resilience.value += 10
  } else if (a.goal === '技能提升') {
    base.transfer.value += 15
    base.activity.value += 10
  } else if (a.goal === '兴趣') {
    base.breadth.value += 10
    base.activity.value += 10
  }

  // Adjust based on time
  if (a.time === '2小时以上') {
    base.activity.value += 15
    base.resilience.value += 10
  } else if (a.time === '1小时') {
    base.activity.value += 5
  } else if (a.time === '<30分钟') {
    base.resilience.value += 5
  }

  // Adjust based on style
  if (a.style === '动手练习') {
    base.problemSolving.value += 10
    base.transfer.value += 10
  } else if (a.style === '混合模式') {
    base.breadth.value += 10
    base.transfer.value += 5
  } else if (a.style === '图文阅读') {
    base.depth.value += 5
  } else if (a.style === '视频学习') {
    base.breadth.value += 5
  }

  // Clamp to 5-95 range
  for (const k of Object.keys(base)) {
    base[k].value = Math.max(5, Math.min(95, base[k].value))
  }

  return Object.values(base)
})

function selectOption(option) {
  selectedOption.value = option
  const step = steps[currentStep.value]
  answers.value[step.key] = option

  if (currentStep.value < steps.length - 1) {
    // Auto-advance after brief delay for visual feedback
    pendingTimeout = setTimeout(() => {
      currentStep.value++
      selectedOption.value = answers.value[steps[currentStep.value].key] || ''
    }, 300)
  } else {
    // Last step - submit
    submitProfile()
  }
}

async function submitProfile() {
  try {
    await apiBuildProfile({
      major: answers.value.stage,
      course: answers.value.goal,
      knowledgeBase: answers.value.knowledge,
      cognitiveStyle: answers.value.style,
      dailyTime: answers.value.time,
    })
    submitted.value = true
  } catch (e) {
    console.warn('Profile submission failed', e)
    // Still show results even if API fails
    submitted.value = true
  }
}

function goToDashboard() {
  router.push('/student/dashboard')
}
</script>

<style scoped lang="scss">
@use '../../styles/variables' as *;

.questionnaire-root {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  position: relative;
  overflow: hidden;
}

/* ── Background Glow ── */
.bg-glow {
  position: fixed;
  border-radius: 50%;
  pointer-events: none;
  z-index: -1;

  &.glow-1 {
    width: 500px;
    height: 500px;
    top: -10%;
    right: -5%;
    background: radial-gradient(circle, rgba(59, 130, 246, 0.08) 0%, transparent 70%);
  }

  &.glow-2 {
    width: 400px;
    height: 400px;
    bottom: -5%;
    left: -5%;
    background: radial-gradient(circle, rgba(139, 92, 246, 0.06) 0%, transparent 70%);
  }
}

/* ── Container ── */
.questionnaire-container {
  width: 100%;
  max-width: 520px;
}

/* ── Header ── */
.header {
  text-align: center;
  margin-bottom: 32px;

  .title {
    margin: 0 0 8px;
    font-size: 26px;
    font-weight: 700;
    color: var(--text-main);
    background: linear-gradient(135deg, #3b82f6, #8b5cf6);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .subtitle {
    margin: 0;
    font-size: 14px;
    color: var(--text-sub);
    line-height: 1.5;
  }
}

/* ── Progress ── */
.progress-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.progress-bar {
  flex: 1;
  height: 4px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  border-radius: 2px;
  transition: width 0.4s ease;
}

.progress-text {
  font-size: 12px;
  color: var(--text-sub);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

/* ── Question Card ── */
.question-card {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  border-radius: 20px;
  padding: 32px 28px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

/* AI Section */
.ai-section {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 24px;
}

.ai-avatar {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
}

.question-bubble {
  flex: 1;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-base);
  border-radius: 14px;
  border-bottom-left-radius: 4px;
  padding: 14px 18px;
}

.question-text {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
  line-height: 1.5;
}

/* Options */
.options-area {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 18px;
  border-radius: 12px;
  border: 1px solid var(--border-base);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-main);
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;

  .option-icon {
    font-size: 18px;
    width: 28px;
    text-align: center;
    flex-shrink: 0;
  }

  .option-label {
    flex: 1;
  }

  &:hover {
    border-color: rgba(59, 130, 246, 0.3);
    background: rgba(59, 130, 246, 0.06);
    transform: translateX(4px);
  }

  &.selected {
    border-color: rgba(59, 130, 246, 0.5);
    background: rgba(59, 130, 246, 0.12);
    box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.3);
    transform: translateX(4px);
  }
}

/* ── Result Section ── */
.result-section {
  display: flex;
  justify-content: center;
}

.result-card {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  border-radius: 20px;
  padding: 40px 32px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  text-align: center;
  width: 100%;
}

.result-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.15), rgba(59, 130, 246, 0.15));
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: #10b981;
}

.result-title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-main);
}

.result-desc {
  margin: 0 0 28px;
  font-size: 14px;
  color: var(--text-sub);
  line-height: 1.5;
}

/* Ability Chart */
.ability-chart {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 32px;
  text-align: left;
}

.ability-bar {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ability-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ability-label {
  font-size: 13px;
  color: var(--text-sub);
}

.ability-value {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-main);
  font-variant-numeric: tabular-nums;
}

.ability-track {
  height: 8px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 4px;
  overflow: hidden;
}

.ability-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 1s ease 0.2s;

  &.blue {
    background: linear-gradient(90deg, #3b82f6, #60a5fa);
  }

  &.purple {
    background: linear-gradient(90deg, #8b5cf6, #a78bfa);
  }

  &.cyan {
    background: linear-gradient(90deg, #06b6d4, #22d3ee);
  }

  &.emerald {
    background: linear-gradient(90deg, #10b981, #34d399);
  }

  &.amber {
    background: linear-gradient(90deg, #f59e0b, #fbbf24);
  }

  &.rose {
    background: linear-gradient(90deg, #f43f5e, #fb7185);
  }
}

/* Start Button */
.start-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 32px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 24px rgba(59, 130, 246, 0.4);
  }

  &:active {
    transform: translateY(0);
  }
}

/* ── Transitions ── */
.slide-enter-active {
  transition: all 0.35s ease-out;
}
.slide-leave-active {
  transition: all 0.2s ease-in;
}
.slide-enter-from {
  opacity: 0;
  transform: translateX(40px);
}
.slide-leave-to {
  opacity: 0;
  transform: translateX(-40px);
}

.fade-up-enter-active {
  transition: all 0.4s ease-out;
}
.fade-up-leave-active {
  transition: all 0.2s ease-in;
}
.fade-up-enter-from {
  opacity: 0;
  transform: translateY(20px);
}
.fade-up-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* ── Responsive ── */
@media (max-width: 768px) {
  .questionnaire-root {
    padding: 24px 16px;
  }

  .question-card,
  .result-card {
    padding: 24px 20px;
  }

  .header .title {
    font-size: 22px;
  }

  .option-btn {
    padding: 12px 14px;
    font-size: 14px;
  }
}
</style>
