<template>
  <div class="quiz-container">
    <!-- ===== EXERCISE MODE: exercises passed as prop ===== -->
    <div v-if="parsedExercises && parsedExercises.length > 0" class="exercise-mode">
      <!-- Exercise header -->
      <div class="exercise-header">
        <h3 class="exercise-title">📝 随堂练习</h3>
        <span class="exercise-count">{{ parsedExercises.length }} 题</span>
      </div>

      <!-- Exercise list -->
      <div class="exercise-list">
        <div v-for="(ex, i) in parsedExercises" :key="ex.id" class="ex-card">
          <div class="ex-card-header">
            <span class="ex-number">第 {{ i + 1 }} 题</span>
            <span v-if="showResult" class="ex-result-badge" :class="getExerciseResult(ex.id) ? 'correct' : 'wrong'">
              {{ getExerciseResult(ex.id) ? '✓ 正确' : '✗ 错误' }}
            </span>
          </div>

          <div class="ex-question">{{ ex.parsedContent?.question || ex.questionText }}</div>

          <div v-if="ex.parsedOptions" class="ex-options">
            <div
              v-for="(opt, oi) in ex.parsedOptions"
              :key="oi"
              class="ex-option"
              :class="{
                selected: selectedAnswers[ex.id] === opt,
                correct: showResult && ex.answer === opt,
                wrong: showResult && selectedAnswers[ex.id] === opt && ex.answer !== opt,
              }"
              @click="handleSelect(ex.id, opt)"
            >
              <span class="option-letter">{{ String.fromCharCode(65 + oi) }}</span>
              <span class="option-text">{{ opt }}</span>
              <span v-if="showResult && ex.answer === opt" class="option-icon">✓</span>
              <span v-if="showResult && selectedAnswers[ex.id] === opt && ex.answer !== opt" class="option-icon">✗</span>
            </div>
          </div>

          <!-- Feedback -->
          <div v-if="showResult && selectedAnswers[ex.id]" class="ex-feedback" :class="getExerciseResult(ex.id) ? 'correct' : 'wrong'">
            <div class="feedback-text">
              {{ getExerciseResult(ex.id) ? '回答正确！' : `回答错误。正确答案：${ex.answer}` }}
            </div>
            <div v-if="ex.explanation" class="feedback-explain">{{ ex.explanation }}</div>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="exercise-actions">
        <button
          v-if="!showResult"
          class="action-btn submit-btn"
          @click="$emit('submit-answers')"
        >
          提交答案
        </button>
        <button
          v-if="showResult"
          class="action-btn reset-btn"
          @click="$emit('reset')"
        >
          重新作答
        </button>
      </div>
    </div>

    <!-- ===== QUIZ MODE: standalone quiz generation (original mode) ===== -->
    <template v-else>
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
    </template>

    <!-- 能力变化弹窗 -->
    <AbilityChangeDialog
      v-model="showAbilityDialog"
      :ability-changes="abilityDialogData.abilityChanges"
      :before-data="abilityDialogData.beforeData"
      :after-data="abilityDialogData.afterData"
      @view-report="handleViewReport"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import AbilityChangeDialog from './AbilityChangeDialog.vue'

const parsedExercises = computed(() => {
  return (props.exercises || []).map(ex => ({
    ...ex,
    parsedContent: ex.contentJson
      ? (typeof ex.contentJson === 'string' ? JSON.parse(ex.contentJson) : ex.contentJson)
      : null,
    parsedOptions: ex.optionsJson
      ? (typeof ex.optionsJson === 'string' ? JSON.parse(ex.optionsJson) : ex.optionsJson)
      : null,
  }))
})

const props = defineProps({
  // Exercise mode props
  exercises: {
    type: Array,
    default: null,
  },
  lessonId: {
    type: [Number, String],
    default: null,
  },
  selectedAnswers: {
    type: Object,
    default: () => ({}),
  },
  showResult: {
    type: Boolean,
    default: false,
  },
  // Original quiz mode props
  documentId: {
    type: Number,
    default: null,
  },
  studentId: {
    type: Number,
    default: 1,
  },
})

const emit = defineEmits(['select-answer', 'submit-answers', 'reset', 'viewRadar'])

// ===== Exercise mode handlers =====
function handleSelect(exId, opt) {
  if (props.showResult) return
  emit('select-answer', exId, opt)
}

function getExerciseResult(exId) {
  return props.selectedAnswers[exId] && props.exercises
    ? props.selectedAnswers[exId] === props.exercises.find(e => e.id === exId)?.answer
    : false
}

// ===== Quiz mode state =====
const quizState = ref('idle')
const isGenerating = ref(false)
const showAbilityDialog = ref(false)
const abilityDialogData = ref({
  abilityChanges: {},
  beforeData: {},
  afterData: {},
})

const settings = ref({
  choiceCount: 5,
  essayCount: 1,
  difficulty: 3,
})

const quizId = ref(null)
const questions = ref([])
const totalScore = ref(0)
const answers = ref({})
const result = ref(null)

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
      difficulty: settings.value.difficulty,
    })

    if (response.data.success) {
      quizId.value = response.data.data.quizId
      questions.value = response.data.data.questions
      totalScore.value = response.data.data.totalScore

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
    ElMessage.error('生成测验失败：' + (error.response?.data?.message || error.message))
  } finally {
    isGenerating.value = false
  }
}

const selectOption = (questionId, optionIndex) => {
  answers.value[questionId] = optionIndex
}

const cancelQuiz = () => {
  quizState.value = 'idle'
  questions.value = []
  answers.value = {}
}

const submitQuiz = async () => {
  const unanswered = questions.value.filter(q => !answers.value[q.id] || answers.value[q.id].trim() === '')
  if (unanswered.length > 0) {
    ElMessage.warning(`还有 ${unanswered.length} 道题未作答`)
    return
  }

  try {
    const response = await axios.post(`/api/quiz/submit/${quizId.value}`, {
      studentId: props.studentId,
      documentId: props.documentId,
      answers: answers.value,
    })

    if (response.data.success) {
      result.value = response.data.data
      quizState.value = 'graded'
      ElMessage.success('批改完成！')

      if (result.value.abilityChanges && Object.keys(result.value.abilityChanges).length > 0) {
        setTimeout(() => {
          abilityDialogData.value = {
            abilityChanges: result.value.abilityChanges,
            beforeData: {},
            afterData: {},
          }
          showAbilityDialog.value = true
        }, 500)
      }
    } else {
      ElMessage.error(response.data.message || '提交失败')
    }
  } catch (error) {
    ElMessage.error('提交测验失败：' + (error.response?.data?.message || error.message))
  }
}

const formatAnswer = (question, answer) => {
  if (question.type === 'choice') {
    const index = parseInt(answer)
    return `${String.fromCharCode(65 + index)}. ${question.options[index]}`
  }
  return answer
}

const resetQuiz = () => {
  quizState.value = 'idle'
  questions.value = []
  answers.value = {}
  result.value = null
}

const viewRadarChart = () => {
  emit('viewRadar', result.value.abilityChanges)
}

const handleViewReport = () => {
  ElMessage.info('完整报告功能开发中...')
}
</script>

<style scoped>
.quiz-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* ==================== Exercise Mode ==================== */
.exercise-mode {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.exercise-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 12px;
  border-bottom: 1px solid var(--glass-border);
}

.exercise-title {
  font-size: var(--font-lg);
  font-weight: 600;
  color: var(--text-main);
  margin: 0;
}

.exercise-count {
  font-size: var(--font-xs);
  color: var(--text-faint);
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 999px;
}

.exercise-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ex-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  padding: 16px;
  transition: all var(--transition-fast);
}

.ex-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.ex-number {
  font-size: var(--font-xs);
  font-weight: 600;
  color: var(--primary-light);
}

.ex-result-badge {
  font-size: var(--font-xs);
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 600;
}

.ex-result-badge.correct {
  background: rgba(16, 185, 129, 0.15);
  color: var(--success);
}

.ex-result-badge.wrong {
  background: rgba(239, 68, 68, 0.15);
  color: var(--danger);
}

.ex-question {
  font-size: var(--font-base);
  color: var(--text-main);
  line-height: 1.6;
  margin-bottom: 12px;
}

.ex-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ex-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  cursor: pointer;
  transition: all var(--transition-fast);
  color: var(--text-sub);
  font-size: var(--font-sm);
}

.ex-option:hover:not(.correct):not(.wrong) {
  border-color: rgba(59, 130, 246, 0.3);
  background: rgba(59, 130, 246, 0.08);
}

.ex-option.selected {
  border-color: var(--primary);
  background: rgba(59, 130, 246, 0.15);
  color: var(--primary-light);
}

.ex-option.correct {
  border-color: var(--success);
  background: rgba(16, 185, 129, 0.15);
  color: var(--success);
  box-shadow: 0 0 12px rgba(16, 185, 129, 0.25);
}

.ex-option.wrong {
  border-color: var(--danger);
  background: rgba(239, 68, 68, 0.15);
  color: var(--danger);
  box-shadow: 0 0 12px rgba(239, 68, 68, 0.25);
}

.option-letter {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.06);
  color: var(--text-faint);
}

.ex-option.selected .option-letter {
  background: var(--primary);
  color: #fff;
}

.ex-option.correct .option-letter {
  background: var(--success);
  color: #fff;
}

.ex-option.wrong .option-letter {
  background: var(--danger);
  color: #fff;
}

.option-text {
  flex: 1;
}

.option-icon {
  flex-shrink: 0;
  font-weight: bold;
}

.ex-feedback {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: var(--font-sm);
  line-height: 1.5;
}

.ex-feedback.correct {
  background: rgba(16, 185, 129, 0.1);
  color: var(--success);
}

.ex-feedback.wrong {
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger);
}

.feedback-text {
  font-weight: 500;
}

.feedback-explain {
  margin-top: 6px;
  font-size: var(--font-xs);
  color: var(--text-faint);
  opacity: 0.9;
}

.exercise-actions {
  padding-top: 12px;
  border-top: 1px solid var(--glass-border);
}

.action-btn {
  width: 100%;
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  font-size: var(--font-sm);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
  text-align: center;
}

.submit-btn {
  background: linear-gradient(135deg, var(--primary), var(--accent-cyan));
  color: #fff;
}

.submit-btn:hover {
  opacity: 0.9;
  box-shadow: 0 0 16px rgba(59, 130, 246, 0.3);
}

.reset-btn {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  color: var(--text-sub);
}

.reset-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-main);
}

/* ==================== Quiz Mode: Idle ==================== */
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
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--glass-border);
  border-radius: 16px;
}

.idle-content .icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.idle-content h3 {
  font-size: 24px;
  margin-bottom: 12px;
  color: var(--text-main);
}

.idle-content > p {
  color: var(--text-sub);
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
  color: var(--text-sub);
  font-weight: 500;
}

.hint {
  margin-top: 16px;
  font-size: 12px;
  color: var(--text-faint);
}

/* ==================== Quiz Mode: Answering ==================== */
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
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  margin-bottom: 12px;
}

.header-left h3 {
  margin: 0 0 6px;
  font-size: 18px;
  color: var(--text-main);
}

.question-count {
  font-size: 13px;
  color: var(--text-faint);
}

.header-right {
  display: flex;
  gap: 8px;
}

.quiz-content {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  padding: 20px;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--glass-border);
}

.question-number {
  font-weight: 600;
  color: var(--primary-light);
  font-size: 13px;
}

.question-type {
  padding: 2px 8px;
  background: rgba(59, 130, 246, 0.15);
  color: var(--primary-light);
  border-radius: 4px;
  font-size: 11px;
}

.question-score {
  padding: 2px 8px;
  background: rgba(245, 158, 11, 0.15);
  color: var(--warning);
  border-radius: 4px;
  font-size: 11px;
}

.question-difficulty {
  font-size: 11px;
  color: var(--warning);
}

.question-body {
  margin-bottom: 14px;
}

.question-text {
  font-size: 15px;
  line-height: 1.6;
  color: var(--text-main);
  margin-bottom: 16px;
}

.choice-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.option-item:hover {
  border-color: rgba(59, 130, 246, 0.3);
  background: rgba(59, 130, 246, 0.08);
}

.option-item.selected {
  border-color: var(--primary);
  background: rgba(59, 130, 246, 0.15);
}

.option-label {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 50%;
  font-weight: 600;
  font-size: 12px;
  color: var(--text-faint);
}

.option-item.selected .option-label {
  background: var(--primary);
  color: #fff;
}

.option-text {
  flex: 1;
  font-size: 14px;
  color: var(--text-sub);
}

.option-item.selected .option-text {
  color: var(--text-main);
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
  font-size: 11px;
  color: var(--text-faint);
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 999px;
}

/* ==================== Quiz Mode: Graded ==================== */
.quiz-result {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.result-header {
  background: linear-gradient(135deg, var(--primary-deep), var(--purple-deep));
  color: white;
  padding: 28px 20px;
  border-radius: 12px;
  margin-bottom: 16px;
}

.score-display {
  display: flex;
  align-items: center;
  gap: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.score-circle {
  flex-shrink: 0;
  width: 100px;
  height: 100px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.score-number {
  font-size: 36px;
  font-weight: bold;
  line-height: 1;
}

.score-total {
  font-size: 16px;
  opacity: 0.8;
  margin-top: 2px;
}

.score-info h3 {
  font-size: 22px;
  margin: 0 0 8px;
}

.accuracy {
  font-size: 16px;
  margin-bottom: 6px;
  opacity: 0.95;
}

.comment {
  font-size: 13px;
  opacity: 0.8;
}

.result-content {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.grading-details h4,
.ability-changes h4 {
  font-size: 16px;
  margin-bottom: 12px;
  color: var(--text-main);
}

.grading-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
}

.grading-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--glass-border);
}

.grading-status {
  padding: 2px 8px;
  background: rgba(239, 68, 68, 0.15);
  color: var(--danger);
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.grading-status.correct {
  background: rgba(16, 185, 129, 0.15);
  color: var(--success);
}

.grading-score {
  margin-left: auto;
  font-weight: 600;
  font-size: 13px;
  color: var(--primary-light);
}

.grading-body .question-text {
  margin-bottom: 14px;
}

.answer-comparison {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 14px;
}

.answer-item {
  display: flex;
  gap: 8px;
  font-size: 13px;
}

.answer-item label {
  flex-shrink: 0;
  font-weight: 600;
  color: var(--text-sub);
}

.student-answer span {
  color: var(--danger);
}

.correct-answer span {
  color: var(--success);
}

.explanation {
  padding: 10px 14px;
  background: rgba(59, 130, 246, 0.06);
  border-left: 3px solid var(--primary);
  border-radius: 4px;
}

.explanation label {
  display: block;
  font-weight: 600;
  color: var(--primary-light);
  margin-bottom: 6px;
  font-size: 13px;
}

.explanation p {
  margin: 0;
  color: var(--text-sub);
  line-height: 1.6;
  font-size: 13px;
}

.ability-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
}

.ability-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.dimension-name {
  font-weight: 600;
  color: var(--text-main);
  font-size: 14px;
}

.ability-delta {
  font-size: 18px;
  font-weight: bold;
}

.ability-delta.positive {
  color: var(--success);
}

.ability-delta.negative {
  color: var(--danger);
}

.ability-progress {
  margin-bottom: 10px;
}

.progress-bar {
  position: relative;
  height: 20px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 999px;
  overflow: hidden;
  margin-bottom: 6px;
}

.progress-before {
  position: absolute;
  height: 100%;
  background: rgba(255, 255, 255, 0.15);
  transition: width 0.5s ease;
}

.progress-after {
  position: absolute;
  height: 100%;
  background: linear-gradient(90deg, var(--primary), var(--accent-cyan));
  transition: width 0.5s ease;
}

.progress-labels {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-faint);
}

.ability-reason {
  font-size: 12px;
  color: var(--text-faint);
  margin: 0;
}

.result-actions {
  padding: 16px 0;
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
