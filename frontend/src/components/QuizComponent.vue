<template>
  <div class="quiz-container">
    <!-- 测验未开始 -->
    <div v-if="quizState === 'idle'" class="quiz-idle">
      <div class="idle-content">
        <div class="icon">📝</div>
        <h3>随堂测验</h3>
        <p>基于当前学习内容，AI将为您生成个性化测验</p>
        <div class="quiz-settings">
          <div class="setting-item">
            <label>单选题数量：</label>
            <el-input-number v-model="settings.choiceCount" :min="3" :max="10" size="small" />
          </div>
          <div class="setting-item">
            <label>简答题数量：</label>
            <el-input-number v-model="settings.essayCount" :min="1" :max="3" size="small" />
          </div>
          <div class="setting-item">
            <label>难度级别：</label>
            <el-rate v-model="settings.difficulty" :max="5" size="large" />
          </div>
        </div>
        <el-button
          type="primary"
          size="large"
          :loading="isGenerating"
          @click="generateQuiz"
        >
          {{ isGenerating ? '生成中...' : '开始测验' }}
        </el-button>
        <p class="hint">请先上传文档并完成向量化</p>
      </div>
    </div>

    <!-- 答题中 -->
    <div v-else-if="quizState === 'answering'" class="quiz-answering">
      <div class="quiz-header">
        <div class="header-left">
          <h3>📝 随堂测验</h3>
          <span class="question-count">共 {{ questions.length }} 题 | 总分 {{ totalScore }} 分</span>
        </div>
        <div class="header-right">
          <el-button size="small" @click="cancelQuiz">取消</el-button>
          <el-button type="primary" size="small" @click="submitQuiz">提交答卷</el-button>
        </div>
      </div>

      <div class="quiz-content">
        <div v-for="(question, index) in questions" :key="question.id" class="question-card">
          <div class="question-header">
            <span class="question-number">第 {{ index + 1 }} 题</span>
            <span class="question-type">{{ question.type === 'choice' ? '单选题' : '简答题' }}</span>
            <span class="question-score">{{ question.score }} 分</span>
            <span class="question-difficulty">难度：{{ '★'.repeat(question.difficulty) }}</span>
          </div>

          <div class="question-body">
            <p class="question-text">{{ question.question }}</p>

            <!-- 单选题选项 -->
            <div v-if="question.type === 'choice'" class="choice-options">
              <div
                v-for="(option, optIndex) in question.options"
                :key="optIndex"
                class="option-item"
                :class="{ selected: answers[question.id] === String(optIndex) }"
                @click="selectOption(question.id, String(optIndex))"
              >
                <span class="option-label">{{ String.fromCharCode(65 + optIndex) }}</span>
                <span class="option-text">{{ option }}</span>
              </div>
            </div>

            <!-- 简答题输入框 -->
            <div v-else class="essay-input">
              <el-input
                v-model="answers[question.id]"
                type="textarea"
                :rows="6"
                placeholder="请输入您的答案..."
              />
            </div>
          </div>

          <div class="question-footer">
            <span class="knowledge-tag">💡 {{ question.knowledgePoint }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 批改结果 -->
    <div v-else-if="quizState === 'graded'" class="quiz-result">
      <div class="result-header">
        <div class="score-display">
          <div class="score-circle">
            <div class="score-number">{{ result.earnedScore }}</div>
            <div class="score-total">/ {{ result.totalScore }}</div>
          </div>
          <div class="score-info">
            <h3>测验完成！</h3>
            <p class="accuracy">正确率：{{ result.accuracy.toFixed(1) }}%</p>
            <p class="comment">{{ result.overallComment }}</p>
          </div>
        </div>
      </div>

      <div class="result-content">
        <!-- 题目详解 -->
        <div class="grading-details">
          <h4>📋 详细解析</h4>
          <div v-for="(question, index) in questions" :key="question.id" class="grading-card">
            <div class="grading-header">
              <span class="question-number">第 {{ index + 1 }} 题</span>
              <span
                class="grading-status"
                :class="{ correct: result.gradingDetails[question.id].correct }"
              >
                {{ result.gradingDetails[question.id].correct ? '✓ 正确' : '✗ 错误' }}
              </span>
              <span class="grading-score">
                {{ result.gradingDetails[question.id].score }} / {{ question.score }} 分
              </span>
            </div>

            <div class="grading-body">
              <p class="question-text">{{ question.question }}</p>
              <div class="answer-comparison">
                <div class="answer-item student-answer">
                  <label>你的答案：</label>
                  <span>{{ formatAnswer(question, result.gradingDetails[question.id].studentAnswer) }}</span>
                </div>
                <div class="answer-item correct-answer">
                  <label>正确答案：</label>
                  <span>{{ formatAnswer(question, result.gradingDetails[question.id].correctAnswer) }}</span>
                </div>
              </div>
              <div class="explanation">
                <label>💡 详细解析：</label>
                <p>{{ result.gradingDetails[question.id].explanation }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 能力变化 -->
        <div v-if="result.abilityChanges && Object.keys(result.abilityChanges).length > 0" class="ability-changes">
          <h4>📊 能力提升</h4>
          <div v-for="(change, dimension) in result.abilityChanges" :key="dimension" class="ability-card">
            <div class="ability-header">
              <span class="dimension-name">{{ change.dimension }}</span>
              <span class="ability-delta" :class="{ positive: change.delta > 0, negative: change.delta < 0 }">
                {{ change.delta > 0 ? '+' : '' }}{{ change.delta }}
              </span>
            </div>
            <div class="ability-progress">
              <div class="progress-bar">
                <div class="progress-before" :style="{ width: change.before + '%' }"></div>
                <div class="progress-after" :style="{ width: change.after + '%' }"></div>
              </div>
              <div class="progress-labels">
                <span>{{ change.before }}</span>
                <span>{{ change.after }}</span>
              </div>
            </div>
            <p class="ability-reason">{{ change.reason }}</p>
          </div>
        </div>
      </div>

      <div class="result-actions">
        <el-button size="large" @click="resetQuiz">再来一次</el-button>
        <el-button type="primary" size="large" @click="viewRadarChart">查看能力雷达图</el-button>
      </div>
    </div>
  </div>

  <!-- 能力变化弹窗 -->
  <AbilityChangeDialog
    v-model="showAbilityDialog"
    :ability-changes="abilityDialogData.abilityChanges"
    :before-data="abilityDialogData.beforeData"
    :after-data="abilityDialogData.afterData"
    @view-report="handleViewReport"
  />
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import AbilityChangeDialog from './AbilityChangeDialog.vue'

const props = defineProps({
  documentId: {
    type: Number,
    default: null
  },
  studentId: {
    type: Number,
    default: 1 // 默认学生ID，实际应从登录状态获取
  }
})

const emit = defineEmits(['viewRadar'])

// 测验状态：idle（未开始）、answering（答题中）、graded（已批改）
const quizState = ref('idle')
const isGenerating = ref(false)

// 能力变化弹窗
const showAbilityDialog = ref(false)
const abilityDialogData = ref({
  abilityChanges: {},
  beforeData: {},
  afterData: {}
})

// 测验设置
const settings = ref({
  choiceCount: 5,
  essayCount: 1,
  difficulty: 3
})

// 测验数据
const quizId = ref(null)
const questions = ref([])
const totalScore = ref(0)
const answers = ref({})

// 批改结果
const result = ref(null)

// 生成测验
const generateQuiz = async () => {
  if (!props.documentId) {
    ElMessage.warning('请先上传文档')
    return
  }

  isGenerating.value = true

  try {
    const response = await axios.post('/api/quiz/generate', {
      documentId: props.documentId,
      choiceCount: settings.value.choiceCount,
      essayCount: settings.value.essayCount,
      difficulty: settings.value.difficulty
    })

    if (response.data.code === 200) {
      quizId.value = response.data.data.quizId
      questions.value = response.data.data.questions
      totalScore.value = response.data.data.totalScore

      // 初始化答案对象
      answers.value = {}
      questions.value.forEach(q => {
        answers.value[q.id] = ''
      })

      quizState.value = 'answering'
      ElMessage.success('测验生成成功！')
    } else {
      ElMessage.error(response.data.message || '生成失败')
    }
  } catch (error) {
    console.error('生成测验失败:', error)
    ElMessage.error('生成测验失败：' + (error.response?.data?.message || error.message))
  } finally {
    isGenerating.value = false
  }
}

// 选择选项
const selectOption = (questionId, optionIndex) => {
  answers.value[questionId] = optionIndex
}

// 取消测验
const cancelQuiz = () => {
  quizState.value = 'idle'
  questions.value = []
  answers.value = {}
}

// 提交测验
const submitQuiz = async () => {
  // 检查是否所有题目都已作答
  const unanswered = questions.value.filter(q => !answers.value[q.id] || answers.value[q.id].trim() === '')
  if (unanswered.length > 0) {
    ElMessage.warning(`还有 ${unanswered.length} 道题未作答`)
    return
  }

  try {
    const response = await axios.post(`/api/quiz/submit/${quizId.value}`, {
      studentId: props.studentId,
      documentId: props.documentId,
      answers: answers.value
    })

    if (response.data.code === 200) {
      result.value = response.data.data
      quizState.value = 'graded'
      ElMessage.success('批改完成！')

      // 如果有能力变化，显示弹窗
      if (result.value.abilityChanges && Object.keys(result.value.abilityChanges).length > 0) {
        setTimeout(() => {
          abilityDialogData.value = {
            abilityChanges: result.value.abilityChanges,
            beforeData: {},
            afterData: {}
          }
          showAbilityDialog.value = true
        }, 500)
      }
    } else {
      ElMessage.error(response.data.message || '提交失败')
    }
  } catch (error) {
    console.error('提交测验失败:', error)
    ElMessage.error('提交测验失败：' + (error.response?.data?.message || error.message))
  }
}

// 格式化答案显示
const formatAnswer = (question, answer) => {
  if (question.type === 'choice') {
    const index = parseInt(answer)
    return `${String.fromCharCode(65 + index)}. ${question.options[index]}`
  }
  return answer
}

// 重置测验
const resetQuiz = () => {
  quizState.value = 'idle'
  questions.value = []
  answers.value = {}
  result.value = null
}

// 查看雷达图
const viewRadarChart = () => {
  emit('viewRadar', result.value.abilityChanges)
}

// 查看完整报告（从弹窗触发）
const handleViewReport = () => {
  // 可以跳转到报告页面或展开详细信息
  ElMessage.info('完整报告功能开发中...')
}
</script>

<style scoped>
.quiz-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
}

/* ==================== 未开始状态 ==================== */
.quiz-idle {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.idle-content {
  text-align: center;
  max-width: 500px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.idle-content .icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.idle-content h3 {
  font-size: 24px;
  margin-bottom: 12px;
  color: #1f2937;
}

.idle-content > p {
  color: #6b7280;
  margin-bottom: 30px;
}

.quiz-settings {
  margin-bottom: 30px;
  text-align: left;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.setting-item label {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.hint {
  margin-top: 16px;
  font-size: 12px;
  color: #9ca3af;
}

/* ==================== 答题中 ==================== */
.quiz-answering {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.quiz-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: white;
  border-bottom: 1px solid #e5e7eb;
}

.header-left h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #1f2937;
}

.question-count {
  font-size: 14px;
  color: #6b7280;
}

.header-right {
  display: flex;
  gap: 12px;
}

.quiz-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.question-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.question-number {
  font-weight: 600;
  color: #3b82f6;
}

.question-type {
  padding: 2px 8px;
  background: #dbeafe;
  color: #1e40af;
  border-radius: 4px;
  font-size: 12px;
}

.question-score {
  padding: 2px 8px;
  background: #fef3c7;
  color: #92400e;
  border-radius: 4px;
  font-size: 12px;
}

.question-difficulty {
  font-size: 12px;
  color: #f59e0b;
}

.question-body {
  margin-bottom: 16px;
}

.question-text {
  font-size: 16px;
  line-height: 1.6;
  color: #1f2937;
  margin-bottom: 20px;
}

.choice-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.option-item:hover {
  border-color: #3b82f6;
  background: #eff6ff;
}

.option-item.selected {
  border-color: #3b82f6;
  background: #dbeafe;
}

.option-label {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border-radius: 50%;
  font-weight: 600;
  color: #374151;
}

.option-item.selected .option-label {
  background: #3b82f6;
  color: white;
}

.option-text {
  flex: 1;
  font-size: 14px;
  color: #374151;
}

.essay-input {
  margin-top: 12px;
}

.question-footer {
  display: flex;
  align-items: center;
  gap: 8px;
}

.knowledge-tag {
  font-size: 12px;
  color: #6b7280;
  padding: 4px 12px;
  background: #f3f4f6;
  border-radius: 12px;
}

/* ==================== 批改结果 ==================== */
.quiz-result {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.result-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 40px 20px;
}

.score-display {
  display: flex;
  align-items: center;
  gap: 30px;
  max-width: 800px;
  margin: 0 auto;
}

.score-circle {
  flex-shrink: 0;
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.score-number {
  font-size: 48px;
  font-weight: bold;
}

.score-total {
  font-size: 18px;
  opacity: 0.9;
}

.score-info h3 {
  font-size: 28px;
  margin: 0 0 12px 0;
}

.accuracy {
  font-size: 18px;
  margin-bottom: 8px;
  opacity: 0.95;
}

.comment {
  font-size: 14px;
  opacity: 0.85;
}

.result-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.grading-details,
.ability-changes {
  max-width: 800px;
  margin: 0 auto 30px;
}

.grading-details h4,
.ability-changes h4 {
  font-size: 18px;
  margin-bottom: 16px;
  color: #1f2937;
}

.grading-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.grading-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.grading-status {
  padding: 2px 8px;
  background: #fee2e2;
  color: #991b1b;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.grading-status.correct {
  background: #d1fae5;
  color: #065f46;
}

.grading-score {
  margin-left: auto;
  font-weight: 600;
  color: #3b82f6;
}

.grading-body .question-text {
  margin-bottom: 16px;
}

.answer-comparison {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.answer-item {
  display: flex;
  gap: 8px;
}

.answer-item label {
  flex-shrink: 0;
  font-weight: 600;
  color: #374151;
}

.student-answer span {
  color: #dc2626;
}

.correct-answer span {
  color: #059669;
}

.explanation {
  padding: 12px;
  background: #f0f9ff;
  border-left: 3px solid #3b82f6;
  border-radius: 4px;
}

.explanation label {
  display: block;
  font-weight: 600;
  color: #1e40af;
  margin-bottom: 8px;
}

.explanation p {
  margin: 0;
  color: #374151;
  line-height: 1.6;
}

.ability-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.ability-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.dimension-name {
  font-weight: 600;
  color: #1f2937;
}

.ability-delta {
  font-size: 20px;
  font-weight: bold;
}

.ability-delta.positive {
  color: #059669;
}

.ability-delta.negative {
  color: #dc2626;
}

.ability-progress {
  margin-bottom: 12px;
}

.progress-bar {
  position: relative;
  height: 24px;
  background: #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-before {
  position: absolute;
  height: 100%;
  background: #9ca3af;
  transition: width 0.5s ease;
}

.progress-after {
  position: absolute;
  height: 100%;
  background: linear-gradient(90deg, #3b82f6 0%, #8b5cf6 100%);
  transition: width 0.5s ease;
}

.progress-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #6b7280;
}

.ability-reason {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

.result-actions {
  padding: 20px;
  background: white;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: center;
  gap: 16px;
}
</style>
