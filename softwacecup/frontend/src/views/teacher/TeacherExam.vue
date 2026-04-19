<template>
  <div class="exam-manage-page">
    <!-- Top Tabs: Create Paper / Manage Papers / Grade Papers -->
    <section class="tab-header panel">
      <div class="tabs">
        <button :class="['tab-btn', { active: currentTab === 'create' }]" @click="currentTab = 'create'">创建试卷</button>
        <button :class="['tab-btn', { active: currentTab === 'manage' }]" @click="currentTab = 'manage'">
          <span class="dot green"></span> 试卷管理
        </button>
        <button :class="['tab-btn', { active: currentTab === 'grade' }]" @click="currentTab = 'grade'">
          <span class="dot blue"></span> 批改试卷
        </button>
      </div>

      <!-- Step indicator for create tab -->
      <div v-if="currentTab === 'create'" class="step-indicator">
        <span :class="['step', { active: createStep >= 1, done: createStep > 1 }]" @click="createStep = 1">
          <span class="num">1</span> 题目设置
          <span class="arrow">&gt;</span>
        </span>
        <span :class="['step', { active: createStep >= 2, done: createStep > 2 }]" @click="createStep >= 2 && (createStep = 2)">
          <span class="num">2</span> 权限设置
          <span class="arrow">&gt;</span>
        </span>
        <span :class="['step', { active: createStep >= 3, done: createStep > 3 }]" @click="createStep >= 3 && (createStep = 3)">
          <span class="num">3</span> 发布试卷
        </span>
      </div>
    </section>

    <!-- ====== Tab 1: CREATE PAPER ====== -->
    <template v-if="currentTab === 'create'">
      <!-- Step 1: Question Settings -->
      <section v-if="createStep === 1" class="panel form-panel">
        <h4>题目设置</h4>
        <el-form label-position="right" label-width="100px" :model="paperForm" size="default">
          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="* 试卷名称"><el-input v-model="paperForm.name" placeholder="线性代数期中考试" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="* 所属题库">
                <el-select v-model="paperForm.bankId" style="width:100%">
                  <el-option label="请选择所属题库" value="" disabled />
                  <el-option v-for="b in banks" :key="b.id" :label="b.name" :value="b.id" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="* 考试说明">
            <el-input v-model="paperForm.description" type="textarea" :rows="3" placeholder="不允许带答案提交文档..." />
          </el-form-item>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="* 单选题个数"><el-input-number v-model="paperForm.singleCount" :min="0" :max="50" style="width:100%" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="* 单选分数"><el-input-number v-model="paperForm.singleScore" :min="0" :max="20" style="width:100%" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="多选题个数"><el-input-number v-model="paperForm.multiCount" :min="0" :max="30" style="width:100%" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="多选分数"><el-input-number v-model="paperForm.multiScore" :min="0" :max="20" style="width:100%" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="判断题个数"><el-input-number v-model="paperForm.judgeCount" :min="0" :max="30" style="width:100%" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="判断分数"><el-input-number v-model="paperForm.judgeScore" :min="0" :max="10" style="width:100%" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="填空题个数"><el-input-number v-model="paperForm.fillCount" :min="0" :max="20" style="width:100%" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="填空题分数"><el-input-number v-model="paperForm.fillScore" :min="0" :max="10" style="width:100%" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="简答题个数"><el-input-number v-model="paperForm.shortCount" :min="0" :max="10" style="width:100%" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="简答题分数"><el-input-number v-model="paperForm.shortScore" :min="0" :max="30" style="width:100%" /></el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="* 开始时间">
                <el-date-picker v-model="paperForm.startTime" type="datetime" placeholder="请选择开始时间" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="* 结束时间">
                <el-date-picker v-model="paperForm.endTime" type="datetime" placeholder="请选择结束时间" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="* 考试时间"><el-input v-model="paperForm.duration" placeholder="请输入考试时间"><template #append>分钟</template></el-input></el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <div class="form-actions">
          <el-button type="primary" size="large" @click="nextStep(2)">下一步</el-button>
          <el-button plain size="large" @click="cancelCreate">取消创建</el-button>
        </div>
      </section>

      <!-- Step 2: Permission Settings -->
      <section v-if="createStep === 2" class="panel form-panel">
        <h4>权限设置</h4>
        <p class="sub-hint">配置可参与考试的学生名单和权限范围</p>
        
        <el-form label-width="90px" size="default">
          <el-form-item label="允许学生">
            <el-transfer
              v-model="allowedStudents"
              :data="allStudents"
              :titles="['可选学生列表', '已授权学生']"
              filterable
              filter-placeholder="搜索学号/姓名"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
        
        <div class="form-actions">
          <el-button @click="createStep = 1">上一步</el-button>
          <el-button type="primary" size="large" @click="nextStep(3)">下一步</el-button>
          <el-button plain size="large" @click="cancelCreate">取消创建</el-button>
        </div>
      </section>

      <!-- Step 3: Publish Exam -->
      <section v-if="createStep === 3" class="panel form-panel">
        <h4>发布试卷</h4>
        <div class="publish-summary">
          <div class="summary-card">
            <h5>试卷信息确认</h5>
            <dl>
              <dt>试卷名称：</dt><dd>{{ paperForm.name || '(未命名)' }}</dd>
              <dt>题目数量：</dt><dd>{{ totalQuestionCount }} 题</dd>
              <dt>总分值：</dt><dd><strong>{{ totalScore }} 分</strong></dd>
              <dt>考试时长：</dt><dd>{{ paperForm.duration || '--' }} 分钟</dd>
              <dt>已授权人数：</dt><dd>{{ allowedStudents.length }} 人</dd>
            </dl>
          </div>
        </div>
        
        <!-- Student list preview -->
        <div class="student-list-preview">
          <h5>参考人员列表</h5>
          <div class="search-students">
            <el-input v-model="studentSearch" placeholder="请输入学号" prefix-icon="Search" size="small" style="width: 180px" clearable />
            <el-input v-model="studentNameSearch" placeholder="请输入学生姓名" size="small" style="width: 180px" clearable />
            <el-button type="primary" size="small">搜索</el-button>
            <el-button size="small" plain>重置</el-button>
          </div>
          <el-table :data="filteredStudentList" stripe size="small" max-height="300">
            <el-table-column prop="userId" label="用户ID" width="80" />
            <el-table-column prop="studentId" label="学号" width="120" />
            <el-table-column prop="name" label="学生姓名" min-width="120" />
          </el-table>
          <div class="list-footer">
            <span>共 {{ filteredStudentList.length }} 条</span>
            <el-pagination small background layout="prev,pager,next,sizes,jumper" :total="filteredStudentList.length" :page-size="5" />
            <el-button type="primary" size="small" @click="confirmPublish">提交发布</el-button>
            <el-button size="small" @click="createStep = 2">上一步</el-button>
          </div>
        </div>
      </section>
    </template>

    <!-- ====== Tab 2: MANAGE PAPERS ====== -->
    <template v-if="currentTab === 'manage'">
      <section class="panel">
        <div class="manage-toolbar">
          <el-radio-group v-model="manageFilter" size="small">
            <el-radio-button label="all">全部试卷</el-radio-button>
            <el-radio-button label="published">已发布</el-radio-button>
            <el-radio-button label="draft">草稿箱</el-radio-button>
          </el-radio-group>
        </div>
        <el-table :data="filteredPapers" stripe>
          <el-table-column prop="name" label="考试标题" min-width="180" />
          <el-table-column prop="type" label="考试类型" width="100" align="center">
            <template #default="{ row }"><el-tag size="small">{{ row.type }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === '进行中' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="考试满分" width="90" align="center" />
          <el-table-column prop="submitted" label="已提交" width="80" align="center" />
          <el-table-column prop="totalStudents" label="及格线" width="80" align="center" />
          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="viewPaperDetail(row)">详情</el-button>
              <el-button link type="primary" size="small" @click="startGrading(row)">开始批改</el-button>
              <el-button link type="danger" size="small" @click="deletePaper(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>

    <!-- ====== Tab 3: GRADE PAPERS ====== -->
    <template v-if="currentTab === 'grade'">
      <section class="panel">
        <div class="grade-toolbar">
          <h4>批改试卷</h4>
          <span class="grading-tip">正在批改：<strong>{{ currentGradingPaper?.name || '未选择' }}</strong></span>
        </div>
        
        <div class="grading-content">
          <!-- Left: Question list with answers -->
          <div class="questions-area">
            <div v-for="(q, idx) in gradingQuestions" :key="idx" class="question-block">
              <div class="q-header" @click="toggleQ(idx)">
                <strong>{{ idx + 1 }}. {{ q.content }}</strong>
                <span :class="['expand-icon', { expanded: expandedQ === idx }]">&#9660;</span>
              </div>
              
              <div v-show="expandedQ === idx" class="q-body">
                <div v-if="q.type !== 'short'" class="options-display">
                  <div v-for="(opt, oi) in q.options" :key="oi" class="opt-item" :class="{ selected: q.studentAnswer?.includes(opt.label), correct: q.correctAnswer?.includes(opt.label) }">
                    <span class="opt-label">{{ opt.label }}</span>
                    <span class="opt-text">{{ opt.text }}</span>
                    <span v-if="q.correctAnswer?.includes(opt.label)" class="correct-mark">✓ 正确答案</span>
                    <span v-else-if="q.studentAnswer?.includes(opt.label)" class="wrong-mark">✗ 学生作答</span>
                  </div>
                </div>
                
                <div v-else class="short-answer-area">
                  <p class="student-answer-text">{{ q.studentAnswer || '(未作答)' }}</p>
                </div>

                <div class="ai-analysis">
                  <h6 @click="showAnalysis[idx] = !showAnalysis[idx]">题目解析 <span class="toggle-arrow">{{ showAnalysis[idx] ? '▼' : '▶' }}</span></h6>
                  <div v-show="showAnalysis[idx]" class="analysis-text">{{ q.analysis || 'AI自动分析中...' }}</div>
                </div>
                
                <!-- Grading area for short answer -->
                <div v-if="q.type === 'short'" class="grading-area">
                  <h6>判断题</h6>
                  <div class="judge-options">
                    <el-radio-group v-model="q.judgeResult">
                      <el-radio value="correct">正确</el-radio>
                      <el-radio value="partial">部分正确</el-radio>
                      <el-radio value="wrong">错误</el-radio>
                    </el-radio-group>
                  </div>
                  <div class="score-input">
                    <label>给分：</label>
                    <el-input-number v-model="q.earnedScore" :min="0" :max="q.maxScore" size="small" />
                    <span>/ {{ q.maxScore }} 分</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Right: Score summary & actions -->
          <div class="score-sidebar">
            <div class="score-card total-card">
              <span>总得分</span>
              <strong class="total-score-num">{{ computedTotalScore }}</strong>
              <span class="full-score">/ {{ gradingTotalMax }} 分</span>
            </div>
            <div class="action-buttons">
              <el-button type="success" @click="submitGrade">提交批改</el-button>
              <el-button @click="resetGrade">重置</el-button>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const currentTab = ref('manage')
const createStep = ref(1)
const studentSearch = ref('')
const studentNameSearch = ref('')
const manageFilter = ref('all')
const expandedQ = ref(null)
const showAnalysis = ref({})
const currentGradingPaper = ref(null)

// Form data
const paperForm = reactive({
  name: '线性代数期中考试',
  bankId: '',
  description: '不允许带答案提交文档。',
  singleCount: 5,
  singleScore: 10,
  multiCount: 0,
  multiScore: 0,
  judgeCount: 2,
  judgeScore: 3,
  fillCount: 3,
  fillScore: 0,
  shortCount: 0,
  shortScore: 0,
  startTime: null,
  endTime: null,
  duration: ''
})

// Mock data
const banks = [
  { id: 1, name: '期中考试题库' },
  { id: 2, name: '机器学习期中测试' }
]

const allStudents = [
  { key: '7', label: '王紫严 (13027575315)', disabled: false },
  { key: '8', label: '雷根灵 (2022105420065)', disabled: false },
  { key: '9', label: '拂志坤 (2022105400173)', disabled: false },
  { key: '103', label: 'fly (17856385258)', disabled: false },
  { key: '105', label: 'vxy (17856385259)', disabled: false }
]
const allowedStudents = ref(['7', '8'])

const papers = reactive([
  { id: 1, name: '预习练习', type: '练习', status: '进行中', score: 36, submitted: 24, totalStudents: 21 },
  { id: 2, name: '课堂练习', type: '考试', status: '进行中', score: 36, submitted: 21, totalStudents: 19 },
  { id: 3, name: '期中考试', type: '考试', status: '草稿', score: 36, submitted: 16, totalStudents: 28 },
  { id: 4, name: '期末考试', type: '考试', status: '草稿', score: 110, submitted: 86, totalStudents: 110 },
])

const filteredPapers = computed(() => {
  if (manageFilter.value === 'all') return [...papers]
  if (manageFilter.value === 'published') return papers.filter(p => p.status === '进行中')
  return papers.filter(p => p.status === '草稿')
})

const filteredStudentList = computed(() => {
  return allStudents.filter(s => {
    const matchId = !studentSearch.value || s.label.includes(studentSearch.value)
    const matchName = !studentNameSearch.value || s.label.includes(studentNameSearch.value)
    return matchId && matchName && allowedStudents.value.includes(s.key)
  }).map(s => {
    const [namePart] = s.label.split(' (')
    const [, idPart] = s.label.match(/\(([^)]+)\)/) || []
    return { userId: s.key, studentId: idPart || '', name: namePart || '' }
  })
})

const totalQuestionCount = computed(() => 
  (paperForm.singleCount||0)+(paperForm.multiCount||0)+(paperForm.judgeCount||0)+(paperForm.fillCount||0)+(paperForm.shortCount||0)
)
const totalScore = computed(() =>
  (paperForm.singleCount||0)*(paperForm.singleScore||0)+
  (paperForm.multiCount||0)*(paperForm.multiScore||0)+
  (paperForm.judgeCount||0)*(paperForm.judgeScore||0)+
  (paperForm.fillCount||0)*(paperForm.fillScore||0)+
  (paperForm.shortCount||0)*(paperForm.shortScore||0)
)

// Grading mock questions
const gradingQuestions = ref([
  {
    content: '有N个样本，一半用于训练，一半用于测试。若增大N值，则训练误差和测试误差之间的差距会如何变化？',
    type: 'single', options: [
      { label: 'A', text: '增大' }, { label: 'B', text: '减小' }
    ], correctAnswer: ['B'], studentAnswer: ['B'], analysis: '正确答案：B\n\n解析：当样本量增加时，模型在更大的数据集上训练，泛化能力增强，训练误差和测试误差的差距会减小。',
    maxScore: 10, earnedScore: 10
  },
  {
    content: '机器学习中L1正则化和L2正则化的区别是？（ ）',
    type: 'single', options: [
      { label: 'A', text: 'L1可以产生稀疏解但可能不可微' },
      { label: 'B', text: 'L2可以产生稀疏解但不可微' },
      { label: 'C', text: 'L1不可产生稀疏解且可能不可微' },
      { label: 'D', text: 'L2正则项对异常值敏感程度较低' }
    ], correctAnswer: ['A'], studentAnswer: ['A'],
    analysis: '正确答案：A\n\nL1正则化（Lasso）倾向于产生稀疏解，即许多系数变为0，但不一定处处可微（绝对值函数在0点不可微）。',
    maxScore: 10, earnedScore: 10
  },
  {
    content: '多重k折结构何以处理该区域的分支情况。（ ）',
    type: 'single', options: [
      { label: 'A', text: '正确' }, { label: 'B', text: '错误' }
    ], correctAnswer: ['A'], studentAnswer: ['A'],
    analysis: '正确答案：A\n\n交叉验证通过多次划分数据集来评估模型的稳定性和泛化能力。',
    maxScore: 10, earnedScore: 10
  },
  {
    content: '简述使用数组的步骤。',
    type: 'short', options: [], correctAnswer: '', studentAnswer: '11',
    analysis: '参考答案应包含：声明数组、初始化、访问元素、遍历操作等步骤。',
    maxScore: 20, earnedScore: 18, judgeResult: 'correct'
  },
  {
    content: 'String类中提供了大量的操作字符串的方法，请写出至少三种方法并说明其作用。',
    type: 'short', options: [], correctAnswer: '', studentAnswer: '11',
    analysis: '参考答案应包含：charAt()、substring()、indexOf()、length()等方法的说明。',
    maxScore: 20, earnedScore: 17, judgeResult: 'partial'
  }
])
const gradingTotalMax = computed(() => gradingQuestions.value.reduce((s, q) => s + (q.maxScore || 0), 0))
const computedTotalScore = computed(() => gradingQuestions.value.reduce((s, q) => s + (q.earnedScore || 0), 0))

function nextStep(step) {
  if (step === 2 && (!paperForm.name || !paperForm.bankId)) {
    ElMessage.warning('请填写完整的试卷基本信息'); return
  }
  if (step === 3 && allowedStudents.value.length === 0) {
    ElMessage.warning('请至少选择一名学生'); return
  }
  createStep.value = step
}
function cancelCreate() {
  ElMessageBox.confirm('确定要取消创建吗？当前填写的信息将丢失。', '提示', { type: 'warning' })
    .then(() => { createStep.value = 1; resetForm(); }).catch(() => {})
}
function resetForm() {
  Object.assign(paperForm, { name: '', bankId: '', description: '', singleCount: 5, singleScore: 10, multiCount: 0, multiScore: 0, judgeCount: 2, judgeScore: 3, fillCount: 3, fillScore: 0, shortCount: 0, shortScore: 0, startTime: null, endTime: null, duration: '' })
}

function confirmPublish() {
  papers.push({
    id: Date.now(),
    name: paperForm.name || '新试卷',
    type: '考试',
    status: '进行中',
    score: totalScore.value,
    submitted: 0,
    totalStudents: allowedStudents.value.length
  })
  ElMessage.success('试卷发布成功！')
  createStep.value = 1
  cancelCreate()
  currentTab.value = 'manage'
}

function viewPaperDetail(row) { ElMessage.info(`查看试卷：${row.name}`) }
function startGrading(row) {
  currentGradingPaper.value = row
  currentTab.value = 'grade'
  ElMessage.info(`开始批改：${row.name}`)
}
function deletePaper(row) {
  ElMessageBox.confirm(`确定删除试卷 "${row.name}"？`, '提示', { type: 'warning' })
    .then(() => {
      const idx = papers.findIndex(p => p.id === row.id)
      if (idx > -1) papers.splice(idx, 1)
      ElMessage.success('已删除')
    }).catch(() => {})
}

function toggleQ(idx) { expandedQ.value = expandedQ.value === idx ? null : idx }

function submitGrade() {
  ElMessage.success(`批改完成！总得分：${computedTotalScore.value} / ${gradingTotalMax.value}`)
}
function resetGrade() {
  gradingQuestions.value.forEach(q => { q.earnedScore = 0; q.judgeResult = ''; })
  ElMessage.info('已重置所有评分')
}
</script>

<style scoped>
.exam-manage-page { display: grid; gap: 14px; }

/* Tab Header */
.tab-header { padding: 14px 18px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.tabs { display: flex; gap: 6px; background: #f1f5f9; border-radius: 10px; padding: 4px; }
.tab-btn {
  padding: 8px 18px; border-radius: 8px; border: none; background: transparent;
  cursor: pointer; font-size: 13.5px; font-weight: 500; color: #64748b;
  transition: all .2s; display: flex; align-items: center; gap: 6px;
}
.tab-btn.active { background: white; color: #2563eb; box-shadow: 0 2px 8px rgba(37,99,235,.15); font-weight: 600; }
.dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.dot.green { background: #22c55e; box-shadow: 0 0 0 3px rgba(34,197,94,.2); }
.dot.blue { background: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,.2); }

/* Step Indicator */
.step-indicator { display: flex; gap: 0; align-items: center; }
.step {
  display: flex; align-items: center; gap: 6px; padding: 6px 14px; cursor: default;
  color: #94a3b8; font-size: 13px; transition: all .2s;
}
.step.active { color: #2563eb; font-weight: 600; }
.step.done { color: #059669; }
.num {
  width: 24px; height: 24px; border-radius: 50%; display: grid; place-items: center;
  font-size: 12px; font-weight: 700; border: 2px solid currentColor;
}
.arrow { margin-left: -4px; opacity: .5; }

/* Form Panels */
.form-panel { padding: 22px 26px; }
.form-panel h4 { margin: 0 0 16px; font-size: 17px; color: #1e293b; font-weight: 700; }
.sub-hint { font-size: 13px; color: #94a3b8; margin: 0 0 18px; }
.form-actions { display: flex; gap: 10px; justify-content: center; margin-top: 24px; padding-top: 18px; border-top: 1px solid #edf2f8; }

/* Publish Summary */
.publish-summary { max-width: 480px; margin-bottom: 18px; }
.summary-card { padding: 18px; border-radius: 12px; background: linear-gradient(135deg, #eff6ff, #fff); border: 1px solid #bfdbfe; }
.summary-card h5 { margin: 0 0 12px; font-size: 14px; color: #2563eb; }
.summary-card dl { display: grid; grid-template-columns: auto 1fr; gap: 6px 16px; font-size: 13.5px; }
.summary-card dt { color: #64748b; }
.summary-card dd { margin: 0; color: #1e293b; font-weight: 500; }

/* Student List Preview */
.student-list-preview { }
.student-list-preview h5 { margin: 0 0 10px; font-size: 14px; }
.search-students { display: flex; gap: 8px; margin-bottom: 10px; flex-wrap: wrap; }
.list-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding-top: 12px; border-top: 1px solid #edf2f8; margin-top: 12px; flex-wrap: wrap; gap: 8px;
}
.list-footer span { font-size: 12.5px; color: #94a3b8; }

/* Manage */
.manage-toolbar { margin-bottom: 14px; }

/* Grading */
.grade-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.grade-toolbar h4 { margin: 0; font-size: 17px; color: #1e293b; }
.grading-tip { font-size: 13px; color: #64748b; }

.grading-content { display: grid; grid-template-columns: 1fr 240px; gap: 16px; }
.questions-area { display: grid; gap: 10px; }
.question-block { border: 1px solid #edf2f8; border-radius: 10px; overflow: hidden; }
.q-header {
  padding: 12px 16px; background: #fafbff; cursor: pointer;
  display: flex; justify-content: space-between; align-items: flex-start;
  user-select: none;
}
.q-header strong { font-size: 14px; color: #334155; line-height: 1.6; }
.expand-icon { color: #94a3b8; font-size: 11px; transition: transform .2s; flex-shrink: 0; margin-top: 3px; }
.expand-icon.expanded { transform: rotate(180deg); }
.q-body { padding: 14px 16px; border-top: 1px solid #edf2f8; }

.options-display { display: grid; gap: 6px; }
.opt-item {
  padding: 8px 12px; border-radius: 8px; border: 1.5px solid #edf2f8; font-size: 13.5px;
  display: grid; grid-template-columns: auto 1fr auto; gap: 8px; align-items: center;
  background: white;
}
.opt-item.selected.wrong { border-color: #fecaca; background: #fef2f2; }
.opt-item.selected.correct { border-color: #bbf7d0; background: #f0fdf4; }
.opt-label { font-weight: 700; color: #64748b; width: 20px; }
.opt-text { color: #334155; }
.correct-mark { color: #16a34a; font-size: 11.5px; font-weight: 600; white-space: nowrap; }
.wrong-mark { color: #dc2626; font-size: 11.5px; font-weight: 600; white-space: nowrap; }

.short-answer-area { }
.student-answer-text { padding: 12px 16px; background: #fafbff; border-radius: 8px; font-size: 13.5px; color: #334155; line-height: 1.8; border: 1px solid #edf2f8; min-height: 60px; }

.ai-analysis { margin-top: 12px; border-top: 1px dashed #e2e8f0; padding-top: 10px; }
.ai-analysis h6 { margin: 0; font-size: 13px; color: #4c5f79; cursor: pointer; display: inline-flex; align-items: center; gap: 4px; }
.toggle-arrow { font-size: 10px; color: #94a3b8; }
.analysis-text { margin-top: 8px; font-size: 12.5px; color: #64748b; line-height: 1.8; white-space: pre-line; background: #fffbeb; border: 1px solid #fef3c7; border-radius: 8px; padding: 10px 14px; }

.grading-area { margin-top: 12px; border-top: 1px dashed #e2e8f0; padding-top: 10px; }
.grading-area h6 { margin: 0 0 8px; font-size: 13px; color: #4c5f79; }
.judge-options { margin-bottom: 8px; }
.score-input { display: flex; align-items: center; gap: 8px; font-size: 13.5px; color: #64748b; }

/* Score Sidebar */
.score-sidebar { display: grid; gap: 12px; position: sticky; top: 20px; align-self: start; }
.score-card { padding: 20px; border-radius: 14px; text-align: center; }
.total-card { background: linear-gradient(135deg, #eff6ff, #dbeafe); border: 1px solid #bfdbfe; }
.total-card span:first-child { font-size: 13px; color: #64748b; display: block; margin-bottom: 4px; }
.total-score-num { font-size: 36px; font-weight: 800; color: #1d4ed8; line-height: 1; }
.full-score { font-size: 14px; color: #94a3b8; }
.action-buttons { display: grid; gap: 8px; }

@media (max-width: 900px) {
  .grading-content { grid-template-columns: 1fr; }
  .score-sidebar { position: static; }
  .tab-header { flex-direction: column; align-items: stretch; }
}
</style>
