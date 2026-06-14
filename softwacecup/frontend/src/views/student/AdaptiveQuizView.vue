<template>
  <div class="adaptive-quiz-page">
    <header class="page-header">
      <div class="header-info">
        <h2>🎯 自适应练习系统</h2>
        <p>AI 根据你的掌握情况动态出题，千人千面，精准练习</p>
      </div>
    </header>

    <div class="quiz-content">
      <!-- 左侧：练习区 -->
      <div class="quiz-main">
        <!-- 未开始状态 -->
        <div v-if="!quizStarted" class="quiz-start">
          <div class="start-card">
            <div class="start-icon">🎯</div>
            <h3>开始自适应练习</h3>
            <p>系统将根据你的学习数据，自动调整题目难度</p>

            <div class="config-area">
              <div class="config-item">
                <label>选择知识点</label>
                <el-select v-model="selectedKpId" placeholder="请选择知识点" style="width: 100%" filterable>
                  <el-option v-for="kp in knowledgePoints" :key="kp.id" :label="kp.name" :value="kp.id">
                    <span>{{ kp.name }}</span>
                    <span style="float:right;color:#999;font-size:12px;">Lv.{{ kp.difficultyLevel || 1 }}</span>
                  </el-option>
                </el-select>
              </div>
              <div class="config-item">
                <label>题目数量：{{ config.count }} 题</label>
                <el-slider v-model="config.count" :min="5" :max="20" :step="5" show-stops />
              </div>
            </div>

            <el-button type="primary" size="large" @click="startQuiz" :loading="isGenerating">
              {{ isGenerating ? 'AI 出题中...' : '🚀 开始练习' }}
            </el-button>
          </div>
        </div>

        <!-- 答题中 -->
        <div v-else-if="!quizFinished" class="quiz-active">
          <div class="quiz-progress">
            <div class="progress-info">
              <span>第 {{ currentIdx + 1 }} / {{ questions.length }} 题</span>
              <span class="difficulty-badge" :class="difficultyClass(currentQuestion.difficulty)">
                {{ difficultyLabel(currentQuestion.difficulty) }}
              </span>
            </div>
            <el-progress :percentage="progressPercent" :stroke-width="8" :color="progressColor" />
          </div>

          <!-- 出题依据 -->
          <div v-if="quizBasis" class="quiz-basis">
            <span class="basis-icon">💡</span>
            <span>{{ quizBasis }}</span>
          </div>

          <div class="question-card">
            <div class="question-type-badge">{{ currentQuestion.typeLabel }}</div>
            <h3 class="question-text">{{ currentQuestion.question }}</h3>

            <div v-if="currentQuestion.type === 'choice'" class="options-list">
              <div
                v-for="(opt, idx) in currentQuestion.options"
                :key="idx"
                :class="['option-item', { selected: userAnswers[currentIdx] === idx, correct: showAnswer && idx === currentQuestion.answer, wrong: showAnswer && userAnswers[currentIdx] === idx && idx !== currentQuestion.answer }]"
                @click="selectAnswer(idx)"
              >
                <span class="option-letter">{{ String.fromCharCode(65 + idx) }}</span>
                <span class="option-text">{{ opt }}</span>
                <span v-if="showAnswer && idx === currentQuestion.answer" class="option-icon">✓</span>
                <span v-if="showAnswer && userAnswers[currentIdx] === idx && idx !== currentQuestion.answer" class="option-icon wrong">✕</span>
              </div>
            </div>

            <div v-else-if="currentQuestion.type === 'judge'" class="judge-area">
              <el-button :type="userAnswers[currentIdx] === true ? 'primary' : 'default'" size="large" @click="selectAnswer(true)">✓ 正确</el-button>
              <el-button :type="userAnswers[currentIdx] === false ? 'danger' : 'default'" size="large" @click="selectAnswer(false)">✕ 错误</el-button>
            </div>

            <div v-else-if="currentQuestion.type === 'fill'" class="fill-area">
              <el-input v-model="fillAnswer" placeholder="请输入答案" size="large" @keydown.enter="submitFill" />
            </div>

            <!-- 解析区 -->
            <div v-if="showAnswer" class="explanation-area">
              <div class="explanation-header">
                <span :class="['result-badge', isCorrect ? 'correct' : 'wrong']">
                  {{ isCorrect ? '✓ 回答正确' : '✕ 回答错误' }}
                </span>
              </div>
              <div class="explanation-body">
                <p><b>解析：</b>{{ currentQuestion.explanation }}</p>
                <div v-if="currentQuestion.knowledgePoints?.length" class="kp-list">
                  <span class="kp-label">涉及知识点：</span>
                  <el-tag v-for="kp in currentQuestion.knowledgePoints" :key="kp" size="small" type="info">{{ kp }}</el-tag>
                </div>
              </div>
              <el-button type="primary" @click="nextQuestion">
                {{ currentIdx < questions.length - 1 ? '下一题 →' : '查看结果' }}
              </el-button>
            </div>
          </div>

          <!-- 知识点提示 -->
          <div class="hint-bar">
            <span>💡 当前难度自适应调整中</span>
            <span>难度曲线：{{ difficultyCurve }}</span>
          </div>
        </div>

        <!-- 结果页 -->
        <div v-else class="quiz-result">
          <div class="result-card">
            <div class="result-score">
              <div class="score-circle" :style="{ '--score': resultScore }">
                <span class="score-num">{{ resultScore }}</span>
                <span class="score-unit">分</span>
              </div>
            </div>
            <h3>{{ resultScore >= 90 ? '🎉 太棒了！' : resultScore >= 70 ? '👍 不错哦！' : resultScore >= 60 ? '💪 继续加油！' : '📚 需要加强' }}</h3>
            <p>正确率 {{ correctCount }} / {{ questions.length }}</p>

            <div class="result-stats">
              <div class="rs-item"><span class="rs-value">{{ correctCount }}</span><span class="rs-label">正确</span></div>
              <div class="rs-item"><span class="rs-value">{{ questions.length - correctCount }}</span><span class="rs-label">错误</span></div>
              <div class="rs-item"><span class="rs-value">{{ avgDifficulty }}</span><span class="rs-label">平均难度</span></div>
              <div class="rs-item"><span class="rs-value">{{ formatTime(totalTime) }}</span><span class="rs-label">用时</span></div>
            </div>

            <!-- 知识点分析 -->
            <div class="kp-analysis">
              <h4>📊 知识点掌握情况</h4>
              <div v-for="kp in kpAnalysis" :key="kp.name" class="kp-row">
                <span class="kp-name">{{ kp.name }}</span>
                <el-progress :percentage="kp.mastery" :color="kp.mastery >= 80 ? '#10b981' : kp.mastery >= 60 ? '#f59e0b' : '#ef4444'" :stroke-width="10" style="flex:1;" />
                <span class="kp-score">{{ kp.mastery }}%</span>
              </div>
            </div>

            <!-- 错题根因分析 -->
            <div v-if="mistakeAnalysis?.hasIssues" class="mistake-analysis">
              <h4>🔍 错题根因分析</h4>
              <p class="root-cause"><b>根本原因：</b>{{ mistakeAnalysis.rootCause }}</p>
              <div v-if="mistakeAnalysis.weakPoints?.length" class="analysis-section">
                <span class="analysis-label">薄弱环节：</span>
                <el-tag v-for="wp in mistakeAnalysis.weakPoints" :key="wp" size="small" type="danger" style="margin:2px;">{{ wp }}</el-tag>
              </div>
              <div v-if="mistakeAnalysis.suggestions?.length" class="analysis-section">
                <span class="analysis-label">改进建议：</span>
                <ul class="suggestion-list">
                  <li v-for="s in mistakeAnalysis.suggestions" :key="s">{{ s }}</li>
                </ul>
              </div>
            </div>

            <!-- 错题回顾 -->
            <div v-if="wrongQuestions.length" class="wrong-review">
              <h4>❌ 错题回顾</h4>
              <div v-for="(wq, i) in wrongQuestions" :key="i" class="wrong-item">
                <p class="wrong-q">{{ i + 1 }}. {{ wq.question }}</p>
                <p class="wrong-a">正确答案：{{ wq.correctAnswer }}</p>
                <p class="wrong-e">{{ wq.explanation }}</p>
              </div>
            </div>

            <div class="result-actions">
              <el-button type="primary" @click="restartQuiz">🔄 再练一次</el-button>
              <el-button @click="reviewWrong">📋 错题本</el-button>
              <el-button @click="backToDashboard">返回首页</el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：学习状态 -->
      <div class="quiz-sidebar">
        <div class="sidebar-card">
          <h4>📈 知识点掌握度</h4>
          <div class="engine-info">
            <div class="engine-item">
              <span class="engine-label">当前掌握度</span>
              <span class="engine-value">{{ Math.round(userMastery * 100) }}%</span>
            </div>
            <div class="engine-item">
              <span class="engine-label">连续正确</span>
              <span class="engine-value">{{ streakCount }}</span>
            </div>
            <div class="engine-item">
              <span class="engine-label">题目数量</span>
              <span class="engine-value">{{ questions.length || '-' }}</span>
            </div>
          </div>
          <div class="difficulty-meter">
            <div class="meter-bar">
              <div class="meter-fill" :style="{ width: Math.round(userMastery * 100) + '%' }"></div>
            </div>
            <div class="meter-labels">
              <span>0%</span><span>25%</span><span>50%</span><span>75%</span><span>100%</span>
            </div>
          </div>
        </div>

        <div class="sidebar-card">
          <h4>🏆 今日挑战</h4>
          <div class="challenge-item">
            <span class="challenge-icon">🔥</span>
            <div>
              <p class="challenge-name">连续答对5题</p>
              <el-progress :percentage="Math.min(streakCount / 5 * 100, 100)" :stroke-width="6" />
            </div>
          </div>
          <div class="challenge-item">
            <span class="challenge-icon">⭐</span>
            <div>
              <p class="challenge-name">完成10道题</p>
              <el-progress :percentage="Math.min((currentIdx + 1) / 10 * 100, 100)" :stroke-width="6" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  apiKnowledgeGraphFull,
  apiGenerateAdaptiveQuiz,
  apiSubmitAdaptiveQuiz,
  apiAnalyzeMistake
} from '../../api/index.js'

const router = useRouter()

const difficulties = [
  { value: 'basic', label: '基础' },
  { value: 'medium', label: '中等' },
  { value: 'hard', label: '困难' },
  { value: 'challenge', label: '挑战' }
]

// 知识点列表（从知识图谱加载）
const knowledgePoints = ref([])
const selectedKpId = ref(null)
const config = reactive({ count: 10 })
const quizStarted = ref(false)
const quizFinished = ref(false)
const isGenerating = ref(false)
const showAnswer = ref(false)
const currentIdx = ref(0)
const userAnswers = ref([])
const fillAnswer = ref('')
const startTime = ref(null)
const totalTime = ref(0)
const streakCount = ref(0)
const currentDifficulty = ref(2)

// API 返回的数据
const quizBasis = ref('')
const weakPrerequisites = ref([])
const userMastery = ref(0)
const mistakeAnalysis = ref(null)

const questions = ref([])

const difficultyLabel = (d) => {
  if (typeof d === 'number') return { 1: '基础', 2: '中等', 3: '困难', 4: '挑战', 5: '挑战+' }[d] || '中等'
  return { basic: '基础', medium: '中等', hard: '困难', challenge: '挑战' }[d] || d
}

const typeLabel = (t) => ({ choice: '单选题', judge: '判断题', fill: '填空题', code: '编程题' }[t] || t)
const difficultyClass = (d) => {
  if (typeof d === 'number') return { 1: 'basic', 2: 'basic', 3: 'medium', 4: 'hard', 5: 'challenge' }[d] || 'medium'
  return d
}

const difficultyPercent = computed(() => {
  const map = { basic: 20, medium: 50, hard: 75, challenge: 95 }
  return map[currentDifficulty.value] || 50
})

const difficultyCurve = computed(() => {
  const history = questions.value.slice(0, currentIdx.value + 1)
  return history.length > 0 ? history.map(q => difficultyLabel(q.difficulty)).join(' → ') : '-'
})

const currentQuestion = computed(() => questions.value[currentIdx.value] || {})
const progressPercent = computed(() =>
  questions.value.length > 0 ? ((currentIdx.value + 1) / questions.value.length) * 100 : 0
)
const progressColor = computed(() => {
  if (progressPercent.value < 30) return '#f59e0b'
  if (progressPercent.value < 70) return '#3b82f6'
  return '#10b981'
})
const isCorrect = computed(() => {
  const q = currentQuestion.value
  const ua = userAnswers.value[currentIdx.value]
  if (!q || ua == null) return false
  const uas = String(ua).trim().toLowerCase().replace(/\s+/g, '')
  const cas = String(q.correctAnswer || q.answer || '').trim().toLowerCase().replace(/\s+/g, '')
  return uas === cas
})

const correctCount = computed(() => {
  let c = 0
  questions.value.forEach((q, i) => {
    const ua = userAnswers.value[i]
    if (ua == null) return
    const uas = String(ua).trim().toLowerCase().replace(/\s+/g, '')
    const cas = String(q.correctAnswer || q.answer || '').trim().toLowerCase().replace(/\s+/g, '')
    if (uas === cas) c++
  })
  return c
})

const resultScore = computed(() =>
  questions.value.length > 0 ? Math.round((correctCount.value / questions.value.length) * 100) : 0
)

const wrongQuestions = computed(() => questions.value.filter((q, i) => {
  const ua = userAnswers.value[i]
  if (ua == null) return true
  const uas = String(ua).trim().toLowerCase().replace(/\s+/g, '')
  const cas = String(q.correctAnswer || q.answer || '').trim().toLowerCase().replace(/\s+/g, '')
  return uas !== cas
}))

const avgDifficulty = computed(() => {
  const avg = questions.value.reduce((s, q) => s + (q.difficulty || 2), 0) / questions.value.length
  return difficultyLabel(Math.round(avg))
})

const kpAnalysis = computed(() => {
  const kps = {}
  questions.value.forEach((q, i) => {
    const kp = q.knowledgePointHint || '综合'
    if (!kps[kp]) kps[kp] = { total: 0, correct: 0 }
    kps[kp].total++
    const ua = userAnswers.value[i]
    if (ua != null) {
      const uas = String(ua).trim().toLowerCase().replace(/\s+/g, '')
      const cas = String(q.correctAnswer || q.answer || '').trim().toLowerCase().replace(/\s+/g, '')
      if (uas === cas) kps[kp].correct++
    }
  })
  return Object.entries(kps).map(([name, data]) => ({
    name,
    mastery: data.total > 0 ? Math.round((data.correct / data.total) * 100) : 0
  }))
})

// 加载知识点列表
const loadKnowledgePoints = async () => {
  try {
    const res = await apiKnowledgeGraphFull()
    if (res?.data?.nodes) {
      knowledgePoints.value = res.data.nodes
      if (knowledgePoints.value.length > 0) {
        selectedKpId.value = knowledgePoints.value[0].id
      }
    }
  } catch (e) {
    ElMessage.warning('知识点加载失败')
    knowledgePoints.value = []
  }
}

const startQuiz = async () => {
  if (!selectedKpId.value) {
    ElMessage.warning('请选择一个知识点')
    return
  }
  isGenerating.value = true
  try {
    const res = await apiGenerateAdaptiveQuiz({
      knowledgePointId: selectedKpId.value,
      count: config.count
    })
    if (res?.data) {
      questions.value = (res.data.questions || []).map(q => ({
        ...q,
        typeLabel: typeLabel(q.type),
        answer: q.correctAnswer || q.answer,
        knowledgePoints: [q.knowledgePointHint || ''].filter(Boolean)
      }))
      quizBasis.value = res.data.quizBasis || ''
      weakPrerequisites.value = res.data.weakPrerequisites || []
      userMastery.value = res.data.userMastery || 0

      if (questions.value.length === 0) {
        ElMessage.warning('AI 未能生成题目，请重试')
        isGenerating.value = false
        return
      }

      quizStarted.value = true
      startTime.value = Date.now()
      ElMessage.success(`AI 已生成 ${questions.value.length} 道个性化练习题！`)
    }
  } catch (e) {
    ElMessage.error('出题失败，请检查网络后重试')
  } finally {
    isGenerating.value = false
  }
}

const selectAnswer = (val) => {
  if (showAnswer.value) return
  userAnswers.value[currentIdx.value] = val
  showAnswer.value = true

  if (isCorrect.value) {
    streakCount.value++
  } else {
    streakCount.value = 0
  }

  // 自适应难度调整
  const q = currentQuestion.value
  if (q && typeof q.difficulty === 'number') {
    currentDifficulty.value = q.difficulty
  }
}

const submitFill = () => {
  if (!fillAnswer.value.trim()) return
  selectAnswer(fillAnswer.value)
}

const nextQuestion = () => {
  showAnswer.value = false
  fillAnswer.value = ''
  if (currentIdx.value < questions.value.length - 1) {
    currentIdx.value++
  } else {
    finishQuiz()
  }
}

const finishQuiz = async () => {
  quizFinished.value = true
  totalTime.value = Date.now() - startTime.value

  // 提交答案到后端批改
  try {
    const answers = questions.value.map((q, i) => ({
      question: q.question,
      userAnswer: userAnswers.value[i],
      correctAnswer: q.correctAnswer || q.answer,
      type: q.type
    }))
    const res = await apiSubmitAdaptiveQuiz({
      knowledgePointId: selectedKpId.value,
      answers
    })
    if (res?.data) {
      // 更新掌握度
      userMastery.value = res.data.updatedMastery || res.data.accuracy || 0
      // 获取错题分析
      if (res.data.mistakeAnalysis) {
        mistakeAnalysis.value = res.data.mistakeAnalysis
      }
    }
  } catch (e) {
    ElMessage.warning('提交批改失败')
  }

  // 如果有错题，进行根因分析
  if (wrongQuestions.value.length > 0) {
    try {
      const mistakes = wrongQuestions.value.map(q => ({
        question: q.question,
        correctAnswer: q.correctAnswer || q.answer,
        userAnswer: userAnswers.value[questions.value.indexOf(q)]
      }))
      const res = await apiAnalyzeMistake({
        knowledgePointId: selectedKpId.value,
        mistakes
      })
      if (res?.data) {
        mistakeAnalysis.value = res.data
      }
    } catch (e) {
      ElMessage.warning('错题分析失败')
    }
  }
}

const formatTime = (ms) => {
  const s = Math.floor(ms / 1000)
  return `${Math.floor(s / 60)}分${s % 60}秒`
}

const restartQuiz = () => {
  quizStarted.value = false
  quizFinished.value = false
  currentIdx.value = 0
  userAnswers.value = []
  showAnswer.value = false
  streakCount.value = 0
  currentDifficulty.value = 2
  questions.value = []
  mistakeAnalysis.value = null
  quizBasis.value = ''
}

const reviewWrong = () => ElMessage.info('已加入错题本')
const backToDashboard = () => router.push('/student/dashboard')

onMounted(() => loadKnowledgePoints())
</script>

<style scoped lang="scss">
.adaptive-quiz-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
}

.page-header {
  padding: 20px 24px;
  background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);
  border-radius: 16px;
  color: #fff;
  h2 { margin: 0 0 4px; font-size: 22px; }
  p { margin: 0; opacity: 0.85; font-size: 14px; }
}

.quiz-content {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 20px;
  min-height: 0;
}

.quiz-main {
  background: rgba(255, 255, 255, 0.06); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); border: 1px solid rgba(255, 255, 255, 0.1); box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35);
  border-radius: 16px;
  
  overflow-y: auto;
  padding: 24px;
}

.quiz-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-card {
  background: rgba(255, 255, 255, 0.06); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); border: 1px solid rgba(255, 255, 255, 0.1); box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35);
  border-radius: 16px;
  
  padding: 18px;

  h4 { margin: 0 0 14px; font-size: 15px; }
}

/* 开始页 */
.quiz-start {
  display: flex;
  justify-content: center;
  padding-top: 40px;
}

.start-card {
  text-align: center;
  max-width: 480px;
  width: 100%;

  .start-icon { font-size: 72px; margin-bottom: 16px; }
  h3 { font-size: 22px; margin: 0 0 8px; }
  p { color: rgba(255, 255, 255, 0.45); margin: 0 0 24px; }
}

.config-area {
  text-align: left;
  margin-bottom: 24px;
}

.config-item {
  margin-bottom: 16px;

  label { display: block; font-size: 13px; font-weight: 600; color: #555; margin-bottom: 6px; }
}

.difficulty-range { display: flex; flex-wrap: wrap; gap: 4px; }

/* 答题区 */
.quiz-progress {
  margin-bottom: 20px;

  .progress-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 600;
  }
}

.difficulty-badge {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;

  &.basic { background: #e8f5e9; color: #2e7d32; }
  &.medium { background: rgba(245, 158, 11, 0.1); color: #e65100; }
  &.hard { background: #fce4ec; color: #c62828; }
  &.challenge { background: #e8eaf6; color: #283593; }
}

.question-card {
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  background: #fafbfc;
}

.question-type-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 12px;
}

.question-text {
  font-size: 17px;
  line-height: 1.7;
  margin: 0 0 20px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover { border-color: #667eea; background: #f8f7ff; }

  &.selected { border-color: #667eea; background: #eef0ff; }
  &.correct { border-color: #10b981; background: rgba(16, 185, 129, 0.1); }
  &.wrong { border-color: #ef4444; background: rgba(239, 68, 68, 0.1); }

  .option-letter {
    width: 28px; height: 28px;
    border-radius: 50%;
    display: flex; align-items: center; justify-content: center;
    background: #f0f0f0;
    font-weight: 700;
    font-size: 13px;
    flex-shrink: 0;
  }

  &.selected .option-letter { background: #667eea; color: #fff; }
  &.correct .option-letter { background: #10b981; color: #fff; }
  &.wrong .option-letter { background: #ef4444; color: #fff; }

  .option-text { flex: 1; font-size: 14px; }

  .option-icon { font-weight: 700; color: #10b981; font-size: 18px; }
  .option-icon.wrong { color: #ef4444; }
}

.judge-area {
  display: flex;
  gap: 16px;
  justify-content: center;
  padding: 20px 0;
}

.fill-area { padding: 10px 0; }

.explanation-area {
  margin-top: 20px;
  padding: 16px;
  border-radius: 12px;
  background: #f8f9ff;
  border: 1px solid #e0e7ff;

  .explanation-header { margin-bottom: 10px; }

  .result-badge {
    font-weight: 700;
    font-size: 15px;
    &.correct { color: #10b981; }
    &.wrong { color: #ef4444; }
  }

  .explanation-body {
    p { font-size: 13px; line-height: 1.7; color: #555; margin: 4px 0; }
  }

  .kp-list {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 8px;
    .kp-label { font-size: 12px; color: rgba(255, 255, 255, 0.45); }
  }
}

.hint-bar {
  display: flex;
  justify-content: space-between;
  padding: 10px 16px;
  margin-top: 16px;
  border-radius: 8px;
  background: rgba(245, 158, 11, 0.1);
  font-size: 12px;
  color: #92400e;
}

/* 结果页 */
.quiz-result {
  display: flex;
  justify-content: center;
  padding-top: 20px;
}

.result-card {
  text-align: center;
  max-width: 560px;
  width: 100%;

  h3 { font-size: 20px; margin: 12px 0 4px; }
  p { color: rgba(255, 255, 255, 0.45); margin: 0 0 20px; }
}

.score-circle {
  width: 140px;
  height: 140px;
  margin: 0 auto;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: conic-gradient(#10b981 calc(var(--score) * 3.6deg), #f0f0f0 0);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: 8px;
    background: rgba(255, 255, 255, 0.06); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); border: 1px solid rgba(255, 255, 255, 0.1); box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35);
    border-radius: 50%;
  }

  .score-num { font-size: 40px; font-weight: 800; color: #10b981; position: relative; z-index: 1; }
  .score-unit { font-size: 14px; color: rgba(255, 255, 255, 0.45); position: relative; z-index: 1; }
}

.result-stats {
  display: flex;
  gap: 0;
  margin: 20px 0;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.rs-item {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  &:last-child { border-right: none; }
  .rs-value { font-size: 20px; font-weight: 700; color: #e2e8f0; }
  .rs-label { font-size: 12px; color: rgba(255, 255, 255, 0.45); margin-top: 2px; }
}

.kp-analysis, .wrong-review {
  text-align: left;
  margin-top: 20px;
  padding: 16px;
  background: #fafbfc;
  border-radius: 12px;

  h4 { margin: 0 0 12px; font-size: 15px; }
}

.kp-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  .kp-name { width: 100px; font-size: 13px; flex-shrink: 0; }
  .kp-score { width: 40px; font-size: 12px; font-weight: 600; text-align: right; }
}

.wrong-item {
  padding: 10px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  &:last-child { border-bottom: none; }

  .wrong-q { font-size: 13px; font-weight: 600; margin: 0 0 4px; }
  .wrong-a { font-size: 12px; color: #10b981; margin: 2px 0; }
  .wrong-e { font-size: 12px; color: rgba(255, 255, 255, 0.55); margin: 2px 0; }
}

.result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 20px;
}

.quiz-basis {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 16px;
  background: #f0f7ff;
  border-radius: 10px;
  border: 1px solid #d0e3ff;
  font-size: 12px;
  color: #1e40af;
  line-height: 1.5;
  .basis-icon { flex-shrink: 0; }
}

.mistake-analysis {
  text-align: left;
  margin-top: 20px;
  padding: 16px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid #fecaca;
  border-radius: 12px;

  h4 { margin: 0 0 12px; font-size: 15px; color: #991b1b; }

  .root-cause {
    font-size: 13px; color: #7f1d1d; line-height: 1.6;
    margin: 0 0 12px;
  }

  .analysis-section {
    margin-top: 8px;
    .analysis-label {
      font-size: 12px; font-weight: 600; color: #991b1b;
      display: block; margin-bottom: 4px;
    }
    .suggestion-list, .remediation-list {
      margin: 4px 0 0; padding-left: 18px;
      font-size: 12px; color: #7f1d1d;
      li { margin-bottom: 4px; line-height: 1.5; }
    }
  }
}

/* 侧边栏 */
.engine-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.engine-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;

  .engine-label { color: rgba(255, 255, 255, 0.45); }
  .engine-value { font-weight: 600; color: #e2e8f0; }
  .engine-value.basic { color: #10b981; }
  .engine-value.medium { color: #f59e0b; }
  .engine-value.hard { color: #ef4444; }
  .engine-value.challenge { color: #7c3aed; }
}

.difficulty-meter {
  .meter-bar {
    height: 8px;
    border-radius: 4px;
    background: #f0f0f0;
    overflow: hidden;

    .meter-fill {
      height: 100%;
      border-radius: 4px;
      background: linear-gradient(90deg, #10b981, #f59e0b, #ef4444, #7c3aed);
      transition: width 0.5s ease;
    }
  }

  .meter-labels {
    display: flex;
    justify-content: space-between;
    font-size: 10px;
    color: #bbb;
    margin-top: 4px;
  }
}

.challenge-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  margin-bottom: 12px;

  .challenge-icon { font-size: 20px; }
  .challenge-name { font-size: 12px; margin: 0 0 4px; font-weight: 600; }
}

/* Element Plus 深色主题覆盖 */
:deep(.el-select) { --el-fill-color-blank: rgba(255,255,255,0.06); }
:deep(.el-input__wrapper) { background: rgba(255,255,255,0.06) !important; box-shadow: none !important; border: 1px solid rgba(255,255,255,0.1) !important; }
:deep(.el-input__inner) { color: #e2e8f0 !important; }
:deep(.el-select-dropdown) { background: rgba(15,23,42,0.95) !important; border: 1px solid rgba(255,255,255,0.1) !important; backdrop-filter: blur(16px); }
:deep(.el-select-dropdown__item) { color: rgba(255,255,255,0.7) !important; }
:deep(.el-select-dropdown__item.hover) { background: rgba(255,255,255,0.06) !important; }
:deep(.el-progress-bar__outer) { background: rgba(255,255,255,0.08) !important; }
:deep(.el-progress__text) { color: rgba(255,255,255,0.6) !important; }
:deep(.el-tag) { background: rgba(255,255,255,0.06) !important; border-color: rgba(255,255,255,0.1) !important; color: rgba(255,255,255,0.7) !important; }
:deep(.el-empty__description p) { color: rgba(255,255,255,0.4) !important; }

/* 文字颜色修正 */
.meter-labels { color: rgba(255,255,255,0.4); }

@media (max-width: 1024px) {
  .quiz-content { grid-template-columns: 1fr; }
}
</style>
