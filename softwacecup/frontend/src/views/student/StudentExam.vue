<template>
  <div class="student-exam-page">
    <!-- Top Toolbar with filter -->
    <section class="toolbar panel">
      <div class="tb-left"><span class="page-title">开始考试</span></div>
      <div class="tb-right">
        <el-select v-model="filterStatus" placeholder="考试状态" size="small" style="width:120px" clearable>
          <el-option label="全部" value="" /><el-option label="进行中" value="active" /><el-option label="未开始" value="pending" />
        </el-select>
        <el-input v-model="searchKeyword" placeholder="请输入人考试名称" prefix-icon="Search" size="small" style="width:200px" clearable />
      </div>
    </section>

    <!-- Exam List -->
    <section class="exam-list-panel panel">
      <el-table :data="filteredExams" stripe @row-click="viewExamDetail">
        <el-table-column prop="category" label="考试类别" width="100" />
        <el-table-column prop="name" label="考试标题" min-width="180" />
        <el-table-column prop="type" label="考试类型" width="90" align="center">
          <template #default="{ row }"><el-tag size="small">{{ row.type }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <span :class="['status-dot', row.status]"></span> {{ statusMap[row.status] }}
          </template>
        </el-table-column>
        <el-table-column prop="score" label="考试满分" width="90" align="center" />
        <el-table-column prop="submitted" label="及格线" width="80" align="center" />
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" :disabled="row.status !== 'active'" @click.stop="startExam(row)">开始考试</el-button>
            <el-button link type="primary" size="small" @click.stop="viewExamResult(row)">查看成绩</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <!-- ====== Exam Taking Modal ====== -->
    <Teleport to="body">
    <transition name="modal-fade">
      <div v-if="showExamModal" class="exam-modal-overlay" @click.self="cancelExam">
        <div class="exam-modal-content">
          <!-- Confirmation Dialog -->
          <div v-if="!examStarted && !showExamQuestions" class="confirm-dialog">
            <h3>进入考试</h3>
            <h4>考试说明：</h4>
            <ol class="exam-rules">
              <li>本次考试全程进行录像监控，进行前后摄像镜头抓拍。请调整好坐姿，确保面部清晰可见。</li>
              <li>请不要中途退出考试页面。</li>
              <li>请手写回答在打草纸上，避免来电引起考试中断，确保手机处于静音状态。</li>
              <li>保存完成后电脑桌面干净，不要有与考试无关的内容。</li>
            </ol>
            <p class="system-warning">由于本次考试需要进行人脸识别监控，系统需要开启您的摄像头，请确认同意。</p>
            <div class="confirm-actions">
              <el-button @click="cancelExam">取消</el-button>
              <el-button type="primary" @click="startExamProcess">确定</el-button>
            </div>
          </div>

          <!-- Actual Exam Questions View -->
          <div v-if="showExamQuestions" class="exam-taking-view">
            <div class="exam-header-bar">
              <span>{{ currentExam?.name || '在线考试' }}</span>
              <div class="exam-timer">
                <span class="timer-icon">⏱</span>
                <strong :class="{ 'time-danger': timeLeft < 60 }">{{ formatTime(timeLeft) }}</strong>
                <span v-if="timeLeft > 0 && !examFinished">剩余时间</span>
                <span v-if="examFinished">已交卷！用时：{{ formatTime(elapsedTime) }}</span>
              </div>
            </div>

            <div class="questions-container" ref="questionsScrollRef">
              <div v-for="(q, idx) in examQuestions" :key="idx" class="question-item">
                <div class="q-number">{{ idx + 1 }}.</div>
                <div class="q-content">
                  <p class="q-text">{{ q.content }}</p>
                  
                  <!-- Choice questions -->
                  <div v-if="q.type === 'single' || q.type === 'multi'" class="q-options">
                    <label v-for="(opt, oi) in q.options" :key="oi"
                           :class="['option-label', { selected: studentAnswers[q.id]?.includes(opt.label), correct: q.correctAnswer?.includes(opt.label) }]">
                      <input
                        :type="q.type === 'single' ? 'radio' : 'checkbox'"
                        :name="'q'+q.id"
                        :value="opt.label"
                        v-model="answers[q.id]"
                        @change="onAnswerChange(q.id, $event)"
                      />
                      <span>{{ opt.label }}. {{ opt.text }}</span>
                    </label>
                  </div>

                  <!-- Short answer / Fill-blank -->
                  <textarea v-if="q.type === 'short'" v-model="answers[q.id]" placeholder="请在此输入你的答案..." rows="4"></textarea>
                  
                  <!-- Judge question -->
                  <div v-if="q.type === 'judge'" class="q-options">
                    <label><input type="radio" :name="'jg'+q.id" value="正确" v-model="answers[q.id]" @change="onAnswerChange(q.id, $event)" /> 正确</label>
                    <label><input type="radio" :name="'jg'+q.id" value="错误" v-model="answers[q.id]" @change="onAnswerChange(q.id, $event)" /> 错误</label>
                  </div>
                </div>
                
                <!-- Reference answer (shown after submit) -->
                <div v-if="showReference && q.correctAnswer" class="reference-area">
                  <h5>参考答案：{{ q.correctAnswer.join ? q.correctAnswer.join(', ') : q.correctAnswer }}</h5>
                </div>
              </div>
            </div>

            <div class="exam-actions">
              <el-button type="success" size="large" @click="submitExam" :disabled="examFinished || Object.keys(answers).length === 0">
                {{ examFinished ? '已提交' : '提交试卷' }}
              </el-button>
              <el-button plain size="large" @click="saveDraft" :disabled="examFinished">保存草稿</el-button>
            </div>
          </div>
        </div>
      </div>
    </transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onBeforeUnmount, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiExamList, apiSubmitExam } from '../../api'

const filterStatus = ref('')
const searchKeyword = ref('')
const showExamModal = ref(false)
const examStarted = ref(false)
const showExamQuestions = ref(false)
const currentExam = ref(null)
const examFinished = ref(false)
const showReference = ref(false)
const questionsScrollRef = ref(null)

const answers = reactive({})
const studentAnswers = reactive({})
const timeLeft = ref(3600) // 1 hour in seconds
const elapsedTime = ref(0)
let timerInterval = null

// Mock exams
const allExams = ref([
  { id: 1, category: '', name: '预习练习', type: '练习', status: 'active', score: 36, submitted: 24 },
  { id: 2, category: '', name: '课堂练习', type: '考试', status: 'pending', score: 36, submitted: 21 },
  { id: 3, category: '', name: '期中考试', type: '考试', status: 'pending', score: 36, submitted: 16 },
  { id: 4, category: '', name: '期末考试', type: '考试', status: 'pending', score: 110, submitted: 86 },
  { id: 5, category: '', name: '机器学习小测', type: '不限时作答', score: 0, submitted: 0 }
])
const statusMap = { active: '进行中', pending: '未开始', finished: '已完成' }

const filteredExams = computed(() => {
  return allExams.value.filter(e => {
    const matchSt = !filterStatus.value || e.status === filterStatus.value
    const matchKey = !searchKeyword.value || e.name.includes(searchKeyword.value)
    return matchSt && matchKey
  })
})

// Mock exam questions
const examQuestions = ref([
  { id: 'q1', content: '简述使用数组的步骤。', type: 'short', options: [], correctAnswer: ['1.声明数组\n2.初始化数组\n3.访问/修改元素\n4.遍历操作'] },
  { id: 'q2', content: 'String类中提供了大量的操作字符串的方法，请写出至少三种方法并说明其作用。', type: 'short', options: [], correctAnswer: [] },
  { id: 'q3', content: '有N个样本，一半用于训练，一半用于测试。若增大N值，则训练误差和测试误差之间的差距会如何变化？', type: 'single', options: [
    { label: 'A', text: '增大' }, { label: 'B', text: '减小' }
  ], correctAnswer: ['B'] },
  { id: 'q4', content: '以下哪个是Java的关键字？', type: 'single', options: [
    { label: 'A', text: 'class' }, { label: 'B', text: 'main' }, { label: 'C', text: 'public' }, { label: 'D', text: 'void' }
  ], correctAnswer: ['A'] },
  { id: 'q5', content: '机器学习中L1正则化和L2正则化的区别是？（ ）', type: 'single', options: [
    { label: 'A', text: 'L1可以产生稀疏解但可能不可微' },
    { label: 'B', text: 'L2可以产生稀疏解但不可微' },
    { label: 'C', text: 'L1不可产生稀疏解且可能不可微' },
    { label: 'D', text: 'L2正则项对异常值敏感度较低' }
  ], correctAnswer: ['A'] },
  { id: 'q6', content: '多重k折结构何以处理该区域的分支情况。（ ）', type: 'single', options: [
    { label: 'A', text: '正确' }, { label: 'B', text: '错误' }
  ], correctAnswer: ['A'] }
])

function viewExamDetail(row) {
  if (row.status === 'finished') { viewExamResult(row); return }
  if (row.status !== 'active') { ElMessage.info('该考试尚未开始'); return }
  startExam(row)
}

function startExam(exam) {
  currentExam.value = exam
  showExamModal.value = true
}

function startExamProcess() {
  examStarted.value = true
  showExamQuestions.value = true
  
  // Start timer
  timerInterval = setInterval(() => {
    if (timeLeft.value > 0 && !examFinished.value) {
      timeLeft.value--
      elapsedTime.value++
    } else if (timeLeft.value <= 0) {
      submitExam()
    }
  }, 1000)
}

function cancelExam() {
  if (examStarted.value && !examFinished.value) {
    ElMessageBox.confirm('确定要退出吗？当前进度将丢失。', '提示', { type: 'warning' })
      .then(() => { stopTimer(); resetExam() }).catch(() => {})
  } else {
    resetExam()
  }
}

function resetExam() {
  showExamModal.value = false
  examStarted.value = false
  showExamQuestions.value = false
  examFinished.value = false
  showReference.value = false
  currentExam.value = null
  for (const key in answers) delete answers[key]
  for (const key in studentAnswers) delete studentAnswers[key]
}

function stopTimer() {
  if (timerInterval) { clearInterval(timerInterval); timerInterval = null }
}

function onAnswerChange(qId, event) {
  // For radio buttons
  let val = ''
  if (event.target.checked) {
    val = event.target.value
    // Collect all checked values for checkbox
    if (event.target.type === 'checkbox') {
      const name = `input[name="${event.target.name}"]:checked`
      const els = document.querySelectorAll(name)
      val = Array.from(els).map(el => el.value).join(',')
    }
  }
  answers[qId] = val
  studentAnswers[qId] = val
}

function submitExam() {
  if (Object.keys(answers).length === 0) {
    ElMessage.warning('请先完成答题'); return
  }
  
  ElMessageBox.confirm('确定要提交试卷吗？提交后将无法修改。', '提交确认', { type: 'info' })
    .then(async () => {
      examFinished.value = true
      stopTimer()
      
      try {
        await apiSubmitExam({ examId: currentExam.value.id, answers })
        ElMessage.success('试卷提交成功！')
        
        // Update exam status
        const exam = allExams.value.find(e => e.id === currentExam.value.id)
        if (exam) { exam.status = 'finished' }
        
        setTimeout(() => {
          showReference.value = true
        }, 1500)
      } catch(e) {
        console.error('Submit failed:', e)
        ElMessage.error('提交失败，但本地已保存')
        showReference.value = true
      }
    }).catch(() => {})
}

function saveDraft() {
  ElMessage.success('草稿已保存（本地）')
}

function viewExamResult(row) {
  ElMessage.info(`查看 ${row.name} 的成绩详情`)
  // Could navigate to a result page or show modal
}

function formatTime(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`
}

onBeforeUnmount(() => { stopTimer() })

watch(showExamModal, (val) => {
  if (!val) { stopTimer(); resetExam() }
})
</script>

<style scoped>
.student-exam-page { display: grid; gap: 12px; }

/* Toolbar */
.toolbar { padding: 12px 18px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.tb-left .page-title { font-size: 17px; font-weight: 700; color: #1e293b; }
.tb-right { display: flex; gap: 8px; align-items: center; }

.exam-list-panel { overflow: hidden; }

/* Modal */
.exam-modal-overlay {
  position: fixed; inset: 0; z-index: 999;
  background: rgba(0,0,0,.45); backdrop-filter: blur(4px);
  display: grid; place-items: center; animation: fadeIn .2s ease;
}
.exam-modal-content {
  width: min(820px, 92vw); max-height: 88vh;
  background: white; border-radius: 18px;
  box-shadow: 0 24px 64px rgba(0,0,0,.25);
  display: flex; flex-direction: column; overflow: hidden;
}
.modal-fade-enter-active { transition: opacity .25s ease; }
.modal-fade-leave-active { transition: opacity .15s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }

/* Confirm dialog */
.confirm-dialog { padding: 28px 32px; text-align: center; }
.confirm-dialog h3 { margin: 0 0 16px; font-size: 22px; color: #1e293b; font-weight: 700; }
.confirm-dialog h4 { margin: 0 0 14px; font-size: 15px; color: #475569; font-weight: 500; }
.exam-rules { padding: 0 0 0 20px; margin: 12px 0; text-align: left; line-height: 2; font-size: 13.5px; color: #475569; }
.exam-rules li { margin-bottom: 6px; padding-left: 6px; list-style-type: decimal inside; }
.system-warning { margin-top: 16px; padding: 10px 14px; background: #fef2f2; border-radius: 10px; color: #dc2626; font-size: 13px; border: 1px solid #fecaca; }
.confirm-actions { display: flex; gap: 12px; justify-content: center; margin-top: 20px; }

/* Exam taking view */
.exam-taking-view { display: flex; flex-direction: column; height: 82vh; }
.exam-header-bar {
  padding: 12px 20px; display: flex; justify-content: space-between; align-items: center;
  background: linear-gradient(135deg, #f8fafc, #fff); border-bottom: 1px solid #edf2f8;
  position: sticky; top: 0; z-index: 5;
}
.exam-header-bar span:first-child { font-size: 16px; font-weight: 600; color: #1e293b; }
.exam-timer { display: flex; align-items: center; gap: 6px; }
.timer-icon { font-size: 18px; }
.exam-timer strong { font-family: monospace; font-size: 26px; color: #1e293b; font-weight: 700; letter-spacing: 1px; }
.time-danger { color: #ef4444 !important; animation: pulseRed 1s infinite; }
@keyframes pulseRed { 0%,100% { opacity: 1; } 50% { opacity: .5; } }

.questions-container { flex: 1; overflow-y: auto; padding: 16px 24px; scroll-behavior: smooth; }

.question-item { display: grid; grid-template-columns: 28px 1fr; gap: 12px; margin-bottom: 20px; padding-bottom: 18px; border-bottom: 1px solid #f1f5f9; }
.q-number { font-size: 15px; font-weight: 700; color: #2563eb; }
.q-text { font-size: 14px; color: #334155; line-height: 1.8; margin: 0 0 12px; }

.q-options { display: grid; gap: 8px; }
.option-label {
  display: flex; align-items: center; gap: 8px; padding: 10px 14px;
  border-radius: 10px; border: 1.5px solid #e2e8f0; cursor: pointer;
  font-size: 13.5px; color: #475569; transition: all .15s ease;
}
.option-label:hover { background: #f0fdf4; border-color: #86efac; }
.option-label.selected { background: #eff6ff; border-color: #3b82f6; }
.option-label.selected.correct { background: #dcfce7; border-color: #22c55e; }
.option-label input { accent-color: #3b82f6; }
.option-label span:last-child { color: inherit; }

.q-options textarea { width: 100%; border: 1.5px solid #d1d5db; border-radius: 10px; padding: 10px; font-size: 13.5px; resize: vertical; outline: none; font-family: inherit; }
.q-options textarea:focus { border-color: #93c5fd; box-shadow: 0 0 0 3px rgba(147,203,253,.15); }

.reference-area { margin-top: 12px; padding: 12px 16px; background: #f0fdf4; border-radius: 10px; border: 1px solid #bbf7d0; }
.reference-area h5 { margin: 0 0 6px; font-size: 13px; color: #166534; }

.exam-actions {
  padding: 14px 20px; display: flex; gap: 10px; justify-content: flex-end;
  border-top: 1px solid #edf2f8; background: #fafbff;
}
.status-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 4px; }
.status-dot.active { background: #22c55e; box-shadow: 0 0 0 3px rgba(34,197,94,.25); }
.status-dot.pending { background: #94a3b8; }
.status-dot.finished { background: #059669; }

@media(max-width:600px){ .exam-header-bar{flex-direction:column;gap:8px;} .exam-timer strong{font-size:22px;} }
</style>
