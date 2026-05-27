<template>
  <div class="question-manage-page">
    <!-- Top Toolbar -->
    <section class="toolbar panel">
      <div class="toolbar-left">
        <span class="page-title">题库管理</span>
        <span class="sub-title">共 {{ questionBanks.length }} 个题库</span>
      </div>
      <div class="toolbar-right">
        <el-input v-model="searchKeyword" placeholder="题库名称" prefix-icon="Search" size="default" style="width: 200px" clearable />
        <el-select v-model="filterCategory" placeholder="题库分类" size="default" style="width: 150px" clearable>
          <el-option label="全部" value="" />
          <el-option label="Java开发" value="java" />
          <el-option label="大数据" value="bigdata" />
          <el-option label="人工智能" value="ai" />
          <el-option label="软件工程" value="se" />
        </el-select>
        <el-button type="primary" @click="showAddBankDialog = true">
          <span>+ 新增</span>
        </el-button>
        <el-button plain @click="showImportDialog = true">批量导入</el-button>
      </div>
    </section>

    <!-- Question Bank List Table -->
    <section class="bank-table-panel panel">
      <el-table :data="filteredBanks" stripe style="width: 100%" @row-click="viewQuestions">
        <!-- Checkbox column for bulk actions -->
        <el-table-column type="selection" width="45" />

        <el-table-column prop="name" label="题库名称" min-width="180">
          <template #default="{ row }">
            <div class="bank-name-cell">
              <span class="bank-name">{{ row.name }}</span>
              <el-tag v-if="row.isDefault" type="danger" size="small" effect="plain">默认</el-tag>
              <el-tag v-else-if="row.isNew" type="warning" size="small" effect="plain">新增</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="category" label="题库分类" width="120">
          <template #default="{ row }">
            <span>{{ categoryMap[row.category] || row.category }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="questionCount" label="单选题数量" width="110" align="center">
          <template #default="{ row }"><strong>{{ row.questionCount }}</strong></template>
        </el-table-column>

        <el-table-column prop="multiChoiceCount" label="多选题量" width="100" align="center">
          <template #default="{ row }">{{ row.multiChoiceCount }}</template>
        </el-table-column>

        <el-table-column prop="judgeCount" label="判断题量" width="100" align="center">
          <template #default="{ row }">{{ row.judgeCount }}</template>
        </el-table-column>

        <el-table-column prop="fillBlankCount" label="填空题量" width="100" align="center">
          <template #default="{ row }">{{ row.fillBlankCount }}</template>
        </el-table-column>

        <el-table-column prop="shortAnswerCount" label="简答题量" width="100" align="center">
          <template #default="{ row }">{{ row.shortAnswerCount || 0 }}</template>
        </el-table-column>

        <el-table-column prop="difficulty" label="难度系数" width="90" align="center">
          <template #default="{ row }">
            <el-rate v-model="row.difficulty" disabled :max="5" size="small" show-score score-template="{value}" />
          </template>
        </el-table-column>

        <el-table-column prop="status" label="题库状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'warning'" size="small" effect="light">
              {{ row.status === 'published' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="coverImage" label="题库介图" width="80" align="center">
          <template #default="{ row }">
            <img v-if="row.coverImage" :src="row.coverImage" class="cover-thumb" alt="" />
            <span v-else class="no-cover">-</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="viewQuestions(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click.stop="exportBank(row)">导出</el-button>
            <el-button link type="danger" size="small" @click.stop="deleteBank(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="table-footer">
        <span class="total-info">共 {{ filteredBanks.length }} 条</span>
        <el-pagination small background layout="prev, pager, next, sizes, jumper" :total="filteredBanks.length" :page-size="10" />
      </div>
    </section>

    <!-- ====== Add Bank Dialog ====== -->
    <el-dialog v-model="showAddBankDialog" title="新增题库" width="520px" destroy-on-close>
      <el-form :model="newBank" label-width="100px" size="default">
        <el-form-item label="* 题库名称">
          <el-input v-model="newBank.name" placeholder="例如：期中考试题库" />
        </el-form-item>
        <el-form-item label="* 题库类型">
          <el-select v-model="newBank.type" style="width: 100%">
            <el-option label="选择题库" value="choice" />
            <el-option label="填空/简答" value="fill" />
            <el-option label="混合题库" value="mixed" />
          </el-select>
        </el-form-item>
        <el-form-item label="题库分类">
          <el-select v-model="newBank.category" style="width: 100%">
            <el-option label="Java开发" value="java" />
            <el-option label="大数据" value="bigdata" />
            <el-option label="人工智能" value="ai" />
            <el-option label="软件工程" value="se" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度等级">
          <el-slider v-model="newBank.difficulty" :min="1" :max="5" show-stops :marks="{1:'简单',2:'较易',3:'中等',4:'较难',5:'困难'}" />
        </el-form-item>
        <el-form-item label="描述说明">
          <el-input v-model="newBank.description" type="textarea" :rows="3" placeholder="请输入题库描述..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddBankDialog = false">取消</el-button>
        <el-button type="primary" @click="addBank">确定</el-button>
      </template>
    </el-dialog>

    <!-- ====== Delete Confirmation Dialog ====== -->
    <el-dialog v-model="showDeleteDialog" title="系统提示" width="400px">
      <div class="delete-confirm">
        <el-icon color="#f59e0b" :size="24"><WarningFilled /></el-icon>
        <p>是否确认删除该题库?该操作为不可逆操作!</p>
      </div>
      <template #footer>
        <el-button @click="showDeleteDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmDelete">确定</el-button>
      </template>
    </el-dialog>

    <!-- ====== Import Dialog (Excel batch import) ====== -->
    <el-dialog v-model="showImportDialog" title="批量导入题目" width="500px" destroy-on-close>
      <div class="import-area">
        <p class="import-tip">可单个新增题目也可使用 Excel 进行批量导入</p>
        <el-upload drag action="#" :auto-upload="false" accept=".xlsx,.xls,.csv" :limit="1">
          <el-icon :size="48" color="#93c5fd"><UploadFilled /></el-icon>
          <div style="margin-top:8px;">将Excel文件拖到此处，或<em>点击上传</em></div>
          <template #tip><div class="upload-tip">仅支持 .xlsx / .xls 格式，单次最大 10MB</div></template>
        </el-upload>
        <el-divider>或</el-divider>
        <el-button type="text" @click="downloadTemplate">下载导入模板 (.xlsx)</el-button>
      </div>
      <template #footer>
        <el-button @click="showImportDialog = false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="doImport">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- ====== Questions Sub-page (shown when a bank is selected) ====== -->
    <Teleport to="body">
      <transition name="slide-up">
        <div v-if="selectedBank" class="questions-overlay" @click.self="closeQuestions">
          <div class="questions-drawer">
            <div class="drawer-header">
              <h4>试题管理 - {{ selectedBank.name }}</h4>
              <button class="close-btn" @click="closeQuestions">&times;</button>
            </div>

            <div class="q-toolbar">
              <div class="q-search-row">
                <el-input v-model="qSearchKeyword" placeholder="输入关键词搜索题目" prefix-icon="Search" clearable size="small" style="width: 220px" />
                <el-select v-model="qFilterType" placeholder="题目类型" size="small" style="width: 120px" clearable>
                  <el-option label="单选" value="single" /><el-option label="多选" value="multi" />
                  <el-option label="判断" value="judge" /><el-option label="填空" value="fill" /><el-option label="简答" value="short" />
                </el-select>
                <el-select v-model="qFilterDifficulty" placeholder="难度" size="small" style="width: 100px" clearable>
                  <el-option v-for="n in 5" :key="n" :label="'⭐'.repeat(n)" :value="n" />
                </el-select>
              </div>
              <div class="q-action-row">
                <el-button type="primary" size="small" @click="showQuestionForm = true">+ 新增试题</el-button>
                <el-button size="small" @click="showQuestionForm = true">+ 新增客观题</el-button>
                <el-button size="small" @click="showQuestionForm = true">+ 新增主观题</el-button>
              </div>
            </div>

            <!-- Questions Table -->
            <el-table :data="filteredQuestions" stripe size="small" max-height="calc(70vh - 200px)">
              <el-table-column type="selection" width="40" />
              <el-table-column prop="name" label="目标题库" width="120" />
              <el-table-column prop="type" label="一级分类" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="typeColor(row.type)">{{ typeName(row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="category2" label="二级分类" width="100" />
              <el-table-column prop="category3" label="三级分类" width="100" />
              <el-table-column prop="content" label="题目内容" min-width="250" show-overflow-tooltip />
              <el-table-column prop="difficulty" label="* 难度" width="80" align="center">
                <template #default="{ row }">{{ '⭐'.repeat(row.difficulty || 1) }}</template>
              </el-table-column>
              <el-table-column prop="analysis" label="* 题目答案" width="140" show-overflow-tooltip />
              <el-table-column prop="explanation" label="答案解析" width="130" show-overflow-tooltip />
              <el-table-column label="图片" width="60" align="center">
                <template #default="{ row }">
                  <img v-if="row.image" :src="row.image" class="q-img-thumb" alt="" />
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="A选项" width="100" show-overflow-tooltip>
                <template #default="{ row }"><span v-if="row.optionA">{{ row.optionA }}</span><span v-else class="gray-text">请填入内容</span></template>
              </el-table-column>
              <el-table-column label="B选项" width="100" show-overflow-tooltip>
                <template #default="{ row }"><span v-if="row.optionB">{{ row.optionB }}</span><span v-else class="gray-text">请填入内容</span></template>
              </el-table-column>
              <el-table-column label="C选项" width="100" show-overflow-tooltip>
                <template #default="{ row }"><span v-if="row.optionC">{{ row.optionC }}</span><span v-else class="gray-text">请填入内容</span></template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="editQuestion(row)">编辑</el-button>
                  <el-button link type="danger" size="small" @click="removeQuestion(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="q-pagination">
              <el-pagination small background layout="prev,pager,next,sizes,jumper" :total="currentQuestions.length" :page-size="10" />
            </div>
          </div>
        </div>
      </transition>
    </Teleport>

    <!-- ====== Add/Edit Question Form Dialog ====== -->
    <el-dialog v-model="showQuestionForm" :title="editingQuestion ? '编辑题目' : '新增题目'" width="600px" destroy-on-close>
      <el-form :model="questionForm" label-width="90px" size="default">
        <el-form-item label="* 所属题库">
          <el-select v-model="questionForm.bankId" disabled style="width:100%"><el-option :label="selectedBank?.name" :value="selectedBank?.id" /></el-select>
        </el-form-item>
        <el-form-item label="题库名称"><el-input v-model="questionForm.bankName" /></el-form-item>
        <el-form-item label="题目内容"><el-input v-model="questionForm.content" type="textarea" :rows="3" placeholder="请输入题目完整内容，支持LaTeX公式如 $$x^2$$" /></el-form-item>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="一级分类"><el-select v-model="questionForm.type" style="width:100%"><el-option label="单选题" value="single" /><el-option label="多选题" value="multi" /><el-option label="判断题" value="judge" /><el-option label="填空题" value="fill" /><el-option label="简答题" value="short" /></el-select></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="二级分类"><el-input v-model="questionForm.category2" placeholder="如：二测试1" /></el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="三级分类"><el-input v-model="questionForm.category3" placeholder="如：三测试1" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="* 难度"><el-select v-model="questionForm.difficulty" style="width:100%"><el-option v-for="n in 5" :key="n" :label="['简单','较易','中等','较难','困难'][n-1]" :value="n" /></el-select></el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="* 题目答案">
          <el-input v-model="questionForm.answer" placeholder="请选择填写" />
        </el-form-item>

        <el-form-item label="答案解析">
          <el-input v-model="questionForm.explanation" type="textarea" :rows="2" />
        </el-form-item>

        <!-- Options (for choice questions) -->
        <template v-if="['single','multi'].includes(questionForm.type)">
          <el-form-item label="A选项"><el-input v-model="questionForm.optionA" placeholder="请输入内容" /></el-form-item>
          <el-form-item label="B选项"><el-input v-model="questionForm.optionB" placeholder="请输入内容" /></el-form-item>
          <el-form-item label="C选项"><el-input v-model="questionForm.optionC" placeholder="请输入内容" /></el-form-item>
        </template>

        <el-form-item label="图片">
          <el-upload action="#" :auto-upload="false" :limit="1" list-type="picture" accept="image/*">
            <el-button size="small">选择图片</el-button>
            <template #tip><div class="upload-tip">最大大小不超过2MB，格式支持 png/jpg/png/gif</div></template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showQuestionForm = false">取消</el-button>
        <el-button type="primary" @click="saveQuestion">{{ editingQuestion ? '保存' : '添加' }}</el-button>
      </template>
    </el-dialog>

    <!-- ====== Course Creation Section ====== -->
    <div class="glass-card" style="margin-top: 20px">
      <h2>创建课程</h2>
      <div class="form-grid" style="display: grid; gap: 12px; margin-top: 12px;">
        <select v-model="courseForm.subjectId" class="form-input">
          <option :value="null">选择学科</option>
          <option v-for="s in subjects" :key="s.id" :value="s.id">{{ s.name }}</option>
        </select>
        <input v-model="courseForm.title" placeholder="课程名称" class="form-input" />
        <input v-model="courseForm.coverImage" placeholder="封面图片URL" class="form-input" />
        <textarea v-model="courseForm.description" placeholder="课程描述" class="form-textarea" rows="3"></textarea>
        <textarea v-model="courseForm.background" placeholder="课程背景" class="form-textarea" rows="2"></textarea>
        <textarea v-model="courseForm.target" placeholder="教学目标" class="form-textarea" rows="2"></textarea>
        <textarea v-model="courseForm.principle" placeholder="设计原则" class="form-textarea" rows="2"></textarea>
        <input v-model="courseForm.tag" placeholder="标签" class="form-input" />
        <input v-model="courseForm.price" placeholder="价格" class="form-input" />
        <input v-model.number="courseForm.totalHours" type="number" placeholder="总课时" class="form-input" />
        <button class="glass-btn active" @click="saveCourse" :disabled="!courseForm.title">保存课程</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled, UploadFilled } from '@element-plus/icons-vue'
import { apiSubjects, apiCreateCourse } from '../../api/index.js'

const searchKeyword = ref('')
const filterCategory = ref('')
const showAddBankDialog = ref(false)
const showDeleteDialog = ref(false)
const showImportDialog = ref(false)
const importing = ref(false)
const targetDeleteBank = ref(null)
const selectedBank = ref(null)
const qSearchKeyword = ref('')
const qFilterType = ref('')
const qFilterDifficulty = ref(null)
const showQuestionForm = ref(false)
const editingQuestion = ref(null)

const categoryMap = { java: 'Java开发', bigdata: '大数据', ai: '人工智能', se: '软件工程' }

// Mock data
const questionBanks = ref([
  { id: 1, name: '期中考试题库', category: 'java', questionCount: 20, multiChoiceCount: 0, judgeCount: 10, fillBlankCount: 6, shortAnswerCount: 0, difficulty: 3, status: 'published', isDefault: true, coverImage: '' },
  { id: 2, name: '机器学习期中测试', category: 'bigdata', questionCount: 0, multiChoiceCount: 0, judgeCount: 0, fillBlankCount: 0, shortAnswerCount: 0, difficulty: 4, status: 'draft', isNew: true, coverImage: '' },
  { id: 3, name: '机器学习小测', category: 'ai', questionCount: 0, multiChoiceCount: 0, judgeCount: 0, fillBlankCount: 0, shortAnswerCount: 0, difficulty: 3, status: 'draft', coverImage: '' },
  { id: 4, name: '软件工程小测', category: 'se', questionCount: 0, multiChoiceCount: 0, judgeCount: 0, fillBlankCount: 0, shortAnswerCount: 0, difficulty: 2, status: 'draft', coverImage: '' }
])

// Mock questions
const allQuestions = ref({
  1: [
    { id: 101, bankId: 1, bankName: '期中考试题库', type: 'single', category2: '二测试1', category3: '三测试1', content: '以下哪个是Java的关键字？', difficulty: 2, answer: 'class', explanation: 'class 是 Java 的关键字', optionA: 'Class', optionB: 'class', optionC: 'CLASS' },
    { id: 102, bankId: 1, bankName: '期中考试题库', type: 'single', category2: '二测试1', category3: '三测试1', content: 'Java中 main方法的返回值类型是什么？', difficulty: 1, answer: 'void', explanation: 'main 方法返回 void', optionA: 'int', optionB: 'void', optionC: 'String' },
    { id: 103, bankId: 1, bankName: '期中考试题库', type: 'multi', category2: '二测试1', category3: '', content: '下列哪些是Java的基本数据类型？', difficulty: 3, answer: 'AB', explanation: 'int 和 double 都是基本数据类型', optionA: 'int', optionB: 'double', optionC: 'String' }
  ]
})

const currentQuestions = computed(() => allQuestions.value[selectedBank.value?.id] || [])

const filteredBanks = computed(() => {
  return questionBanks.value.filter(b => {
    const matchKey = !searchKeyword.value || b.name.includes(searchKeyword.value)
    const matchCat = !filterCategory.value || b.category === filterCategory.value
    return matchKey && matchCat
  })
})

const filteredQuestions = computed(() => {
  return currentQuestions.value.filter(q => {
    const matchKey = !qSearchKeyword.value || q.content.includes(qSearchKeyword.value)
    const matchType = !qFilterType.value || q.type === qFilterType.value
    const matchDiff = !qFilterDifficulty.value || q.difficulty === qFilterDifficulty.value
    return matchKey && matchType && matchDiff
  })
})

const newBank = reactive({ name: '', type: 'mixed', category: 'java', difficulty: 3, description: '' })
const questionForm = reactive({ bankId: '', bankName: '', content: '', type: 'single', category2: '', category3: '', difficulty: 3, answer: '', explanation: '', optionA: '', optionB: '', optionC: '' })

// Course creation form
const subjects = ref([])
const courseForm = reactive({
  subjectId: null, title: '', coverImage: '', description: '',
  background: '', target: '', principle: '',
  tag: '', price: '免费', totalHours: 0, status: '已发布'
})

onMounted(async () => {
  const res = await apiSubjects()
  subjects.value = res.data || []
})

async function saveCourse() {
  await apiCreateCourse(courseForm)
  ElMessage.success('课程创建成功')
  courseForm.title = ''; courseForm.description = ''; courseForm.background = ''
  courseForm.target = ''; courseForm.principle = ''
}

const addBank = () => {
  if (!newBank.name) { ElMessage.warning('请输入题库名称'); return }
  questionBanks.value.push({ id: Date.now(), ...newBank, questionCount: 0, multiChoiceCount: 0, judgeCount: 0, fillBlankCount: 0, shortAnswerCount: 0, status: 'draft', isNew: true, coverImage: '' })
  ElMessage.success('题库添加成功')
  showAddBankDialog.value = false
  Object.assign(newBank, { name: '', type: 'mixed', category: 'java', difficulty: 3, description: '' })
}

const deleteBank = (bank) => {
  targetDeleteBank.value = bank
  showDeleteDialog.value = true
}
const confirmDelete = () => {
  questionBanks.value = questionBanks.value.filter(b => b.id !== targetDeleteBank.value.id)
  showDeleteDialog.value = false
  ElMessage.success('题库已删除')
}

const exportBank = (bank) => {
  ElMessage.info(`正在导出题库：${bank.name}`)
}

const viewQuestions = (bank) => {
  selectedBank.value = bank
}
const closeQuestions = () => {
  selectedBank.value = null
}

const editQuestion = (row) => {
  editingQuestion.value = row
  Object.assign(questionForm, row)
  showQuestionForm.value = true
}

const removeQuestion = (row) => {
  ElMessageBox.confirm('确定要删除这道题目吗？', '提示', { type: 'warning' }).then(() => {
    const qs = allQuestions.value[selectedBank.value.id] || []
    allQuestions.value[selectedBank.value.id] = qs.filter(q => q.id !== row.id)
    ElMessage.success('已删除')
  }).catch(() => {})
}

const saveQuestion = () => {
  if (!questionForm.content) { ElMessage.warning('请输入题目内容'); return }
  
  if (editingQuestion.value) {
    // Update existing
    Object.assign(editingQuestion.value, questionForm)
    ElMessage.success('题目已更新')
  } else {
    // Add new
    if (!allQuestions.value[selectedBank.value.id]) {
      allQuestions.value[selectedBank.value.id] = []
    }
    allQuestions.value[selectedBank.value.id].push({ id: Date.now(), bankId: selectedBank.value.id, ...questionForm })
    ElMessage.success('题目已添加')
  }
  showQuestionForm.value = false
  editingQuestion.value = null
  Object.assign(questionForm, { bankId: '', bankName: '', content: '', type: 'single', category2: '', category3: '', difficulty: 3, answer: '', explanation: '', optionA: '', optionB: '', optionC: '' })
}

const doImport = () => {
  importing.value = true
  setTimeout(() => {
    importing.value = false
    showImportDialog.value = false
    ElMessage.success('导入完成！已成功导入 50 道题目')
  }, 1500)
}

const downloadTemplate = () => {
  ElMessage.info('正在下载导入模板...')
}

function typeName(t) { return { single: '单选题', multi: '多选题', judge: '判断题', fill: '填空题', short: '简答题' }[t] || t }
function typeColor(t) { return { single: '', multi: 'warning', judge: 'info', fill: 'success', short: 'danger' }[t] || '' }
</script>

<style scoped>
.question-manage-page { display: grid; gap: 14px; }

/* Toolbar */
.toolbar { padding: 14px 18px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.toolbar-left { display: flex; align-items: baseline; gap: 10px; }
.page-title { font-size: 18px; font-weight: 700; color: #1e293b; }
.sub-title { font-size: 13px; color: #94a3b8; }
.toolbar-right { display: flex; gap: 8px; align-items: center; }

/* Table Panel */
.bank-table-panel { overflow: hidden; }
.table-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 14px; border-top: 1px solid #edf2f8; margin-top: 14px; }
.total-info { font-size: 13px; color: #94a3b8; }

/* Table cells */
.bank-name-cell { display: flex; align-items: center; gap: 6px; }
.bank-name { font-weight: 600; color: #1e293b; font-size: 13.5px; }
.cover-thumb { width: 48px; height: 32px; object-fit: cover; border-radius: 6px; }
.no-cover { color: #d1d5db; }

/* Delete Confirm */
.delete-confirm { display: flex; align-items: center; gap: 12px; padding: 16px 0; }
.delete-confirm p { margin: 0; font-size: 14px; color: #334155; line-height: 1.6; }

/* Import Area */
.import-area { text-align: center; }
.import-tip { font-size: 13.5px; color: #64748b; margin-bottom: 14px; }
.upload-tip { color: #94a3b8; font-size: 11.5px; margin-top: 6px; }

/* ====== Questions Drawer Overlay ====== */
.questions-overlay {
  position: fixed; inset: 0; z-index: 999;
  background: rgba(0,0,0,.35); backdrop-filter: blur(3px);
  display: grid; place-items: center; animation: fadeIn .2s ease;
}
.questions-drawer {
  width: min(95vw, 1280px); height: 85vh; max-height: calc(100vh - 80px);
  background: white; border-radius: 18px; box-shadow: 0 20px 64px rgba(0,0,0,.25);
  display: flex; flex-direction: column; overflow: hidden;
}
.drawer-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 22px; border-bottom: 1px solid #edf2f8; background: #fafbff;
}
.drawer-header h4 { margin: 0; font-size: 17px; color: #1e293b; font-weight: 700; }
.close-btn {
  width: 32px; height: 32px; border-radius: 50%; border: none; background: #f1f5f9;
  cursor: pointer; font-size: 18px; color: #64748b; transition: all .15s;
}
.close-btn:hover { background: #e2e8f0; color: #ef4444; }

.q-toolbar { padding: 12px 18px; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.q-search-row { display: flex; gap: 8px; align-items: center; }
.q-action-row { display: flex; gap: 6px; }
.q-pagination { padding: 12px 18px; border-top: 1px solid #f1f5f9; display: flex; justify-content: flex-end; }

.q-img-thumb { width: 36px; height: 26px; object-fit: cover; border-radius: 4px; }
.gray-text { color: #d1d5db; font-size: 12px; }

.slide-up-enter-active { transition: all .25s ease; }
.slide-up-enter-from { opacity: 0; transform: scale(.96); }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

@media (max-width: 900px) {
  .toolbar { flex-direction: column; align-items: stretch; }
  .questions-drawer { width: 98vw; height: 92vh; }
}

/* Course creation form styles */
.glass-card {
  background: rgba(255,255,255,0.06); backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 20px;
}
.glass-card h2 { color: #1e293b; font-size: 18px; margin: 0 0 4px; }
.glass-btn { padding: 8px 16px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.15); background: rgba(255,255,255,0.08); backdrop-filter: blur(8px); color: #e2e8f0; cursor: pointer; font-family: inherit; font-size: 13px; }
.glass-btn.active { background: rgba(59,130,246,0.3); border-color: rgba(59,130,246,0.4); }
.glass-btn.active:disabled { opacity: 0.4; cursor: not-allowed; }
.form-input, .form-textarea { padding: 8px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.08); background: rgba(255,255,255,0.04); color: #f1f5f9; font-size: 13px; font-family: inherit; outline: none; width: 100%; box-sizing: border-box; }
.form-textarea { resize: vertical; }
</style>
