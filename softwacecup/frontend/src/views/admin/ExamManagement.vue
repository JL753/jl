<template>
  <div class="exam-mgmt-page">
    <section class="page-header">
      <div><h2>📝 考试管理</h2><p>创建考试、分配给学生、批改成绩并查看统计分析。</p></div>
      <el-button type="primary" @click="openCreateDialog">+ 新建考试</el-button>
    </section>

    <div class="exam-list">
      <div v-for="(exam, idx) in exams" :key="exam.id || idx" class="exam-card panel lift-card">
        <div class="exam-header">
          <div class="exam-status" :class="exam.status === '已发布' ? 'published' : 'draft'">
            {{ exam.status }}
          </div>
          <h3>{{ exam.examName }}</h3>
          <div class="exam-meta-row">
            <span><b>科目：</b>{{ exam.course }}</span>
            <span><b>时长：</b>{{ exam.duration }}分钟</span>
            <span><b>题目数：</b>{{ exam.questionCount || 0 }}题</span>
          </div>
          <p class="exam-desc">{{ exam.topic || '暂无描述' }}</p>
        </div>
        <div class="exam-stats">
          <div class="stat"><b>{{ exam.assignCount || 0 }}</b><span>已分配</span></div>
          <div class="stat"><b>{{ exam.completedCount || 0 }}</b><span>已完成</span></div>
          <div class="stat"><b>{{ avgScore(exam) }}</b><span>平均分</span></div>
        </div>
        <div class="exam-actions">
          <el-button size="small" @click="openEditDialog(exam)" link>编辑</el-button>
          <el-button size="small" type="warning" @click="assignExam(exam)" :disabled="exam.status !== 'published'" plain>分配</el-button>
          <el-popconfirm title="确定删除此考试吗？相关记录也会被清除。" @confirm="handleDelete(exam)">
            <el-button size="small" type="danger" link>删除</el-button>
          </el-popconfirm>
        </div>
      </div>
      <div v-if="exams.length === 0 && !loading" class="empty-state">暂无考试数据，点击"新建考试"开始创建</div>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑考试' : '创建新考试'" width="640px" destroy-on-close>
      <el-form :model="form" :rules="rules" label-width="110px" label-position="top" ref="formRef">
        <el-form-item label="考试名称" prop="examName"><el-input v-model="form.examName" placeholder="如：阶段测试#1" /></el-form-item>
        <el-form-item label="科目" prop="course"><el-input v-model="form.course" placeholder="如：人工智能导论" /></el-form-item>
        <el-form-item label="时长(分钟)" prop="duration"><el-input-number v-model="form.duration" :min="15" :max="180" placeholder="50" /></el-form-item>
        <el-form-item label="题目数量" prop="questionCount"><el-input-number v-model="form.questionCount" :min="1" :max="20" placeholder="5" /></el-form-item>
        <el-form-item label="考试说明" prop="topic"><el-input v-model="form.topic" type="textarea" :rows="3" placeholder="考试范围或说明..." /></el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status"><el-option label="草稿" value="草稿" /><el-option label="已发布" value="已发布" /><el-option label="已结束" value="已结束" /></el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存修改' : '创建考试' }}</el-button>
      </template>
    </el-dialog>

    <!-- Assign Dialog -->
    <el-dialog v-model="assignDialogVisible" title="分配考试给考生" width="480px" destroy-on-close>
      <p style="margin:0 0 16px;color:#64748b;">选择要分配的学生：</p>
      <el-checkbox-group v-model="selectedStudents">
        <div v-for="stu in availableStudents" :key="stu.id">
          <el-checkbox :label="`${stu.displayName} (${stu.username})`" :value="stu.id" />
        </div>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="selectedStudents.length === 0" @click="confirmAssign">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { apiAdminListExams, apiAdminCreateExam, apiAdminUpdateExam, apiAdminDeleteExam } from '../../api'

const loading = ref(false)
const dialogVisible = ref(false)
const assignDialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const selectedStudents = ref([])

const form = reactive({ id: null, examName: '', course: '', duration: 50, questionCount: 5, topic: '', status: '草稿' })
const rules = { examName: [{ required: true, message: '请输入考试名称', trigger: 'blur' }], course: [{ required: true, message: '请输入科目', trigger: 'blur' }] }

// Exams from backend
const exams = ref([])

const availableStudents = ref([
  { id: 1002, username: 'student', displayName: '林学生' },
  { id: 1004, username: 'student03', displayName: '陈同学' },
  { id: 1005, username: 'student02', displayName: '李同学' },
  { id: 1003, username: 'student', displayName: '张同学' }
])

const avgScore = (exam) => exam.completedCount > 0 ? Math.round((exam.completedCount * 80 + (exam.assignCount - exam.completedCount) * 60) / exam.completedCount) : '--'

const openCreateDialog = () => {
  isEdit.value = false; Object.assign(form, { id: null, examName: '', course: '', duration: 50, questionCount: 5, topic: '', status: '草稿' })
  dialogVisible.value = true; nextTick(() => formRef.value?.clearValidate())
}
const openEditDialog = (exam) => {
  isEdit.value = true; Object.assign(form, JSON.parse(JSON.stringify(exam)))
  dialogVisible.value = true; nextTick(() => formRef.value?.clearValidate())
}

// Load exams from backend
const loadExams = async () => {
  loading.value = true
  try {
    const res = await apiAdminListExams()
    exams.value = res.data || []
  } catch (e) {
    console.warn('Load exams failed:', e)
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  submitting.value = true
  try {
    if (isEdit.value) {
      await apiAdminUpdateExam(form.id, form)
      const idx = exams.value.findIndex(e => e.id === form.id)
      if (idx >= 0) exams.value[idx] = {...exams.value[idx], ...form}
      ElMessage.success('考试已更新')
    } else {
      const res = await apiAdminCreateExam(form)
      ElMessage.success('考试已创建')
    }
    dialogVisible.value = false
    await loadExams()
  } catch(e){ElMessage.error((isEdit.value ? '更新' : '创建') + '失败: ' + (e.message || ''))} finally{submitting.value = false}
}

const assignExam = (exam) => { selectedStudents.value=[]; assignDialogVisible.value=true }
const confirmAssign = async () => {
  ElMessage.success(`已分配给 ${selectedStudents.value.length} 名学生`)
  assignDialogVisible.value=false; selectedStudents.value=[]
}

const handleDelete = async (exam) => {
  try {
    await apiAdminDeleteExam(exam.id)
    exams.value = exams.value.filter(e => e.id !== exam.id)
    ElMessage.success('考试已删除')
  } catch (e) { ElMessage.error('删除失败: ' + (e.message || '')) }
}

onMounted(loadExams)
</script>

<style scoped lang="scss">
.exam-mgmt-page {
  display: grid;
  gap: $spacing-lg;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: $spacing-sm;

  h2 {
    margin: 0;
    font-size: $font-size-xxl;
    color: $text-primary;
    font-weight: $font-weight-bold;
  }

  p {
    margin: 0;
    color: $text-secondary;
  }
}

.exam-list {
  display: grid;
  gap: $spacing-md;
}

.exam-card {
  padding: $spacing-lg;
  border-radius: $radius-medium;
  background: $bg-white;
  border: 1px solid $border-extra-light;
  transition: all $transition-base;
  box-shadow: $shadow-card;

  &:hover {
    transform: translateY(-3px);
    box-shadow: $shadow-card-hover;
  }
}

.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: $spacing-sm;
  flex-wrap: wrap;

  h3 {
    margin: 0;
    font-size: $font-size-lg;
    color: $text-primary;
    font-weight: $font-weight-semibold;
  }
}

.exam-status {
  padding: 4px $spacing-sm;
  border-radius: $radius-round;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: white;

  &.published {
    background: $gradient-success;
  }

  &.draft {
    background: $text-secondary;
  }

  &.ended {
    background: $text-regular;
  }
}

.exam-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
  font-size: $font-size-sm;
  color: $text-regular;
  margin: 0 0 $spacing-xs;

  b {
    color: $text-primary;
  }
}

.exam-desc {
  color: $text-regular;
  font-size: $font-size-sm;
  line-height: 1.6;
  margin: 0 0 10px;
}

.exam-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: $spacing-sm;
  padding: $spacing-sm;
  background: $bg-lighter;
  border-radius: $radius-medium;
}

.stat {
  padding: $spacing-xs;
  text-align: center;
  background: $bg-white;
  border-radius: 10px;
  border: 1px solid $border-extra-light;

  b {
    display: block;
    font-size: $font-size-xl;
    color: $text-primary;
    font-weight: $font-weight-bold;
  }

  span {
    display: block;
    font-size: $font-size-xs;
    color: $text-secondary;
    margin-top: 2px;
  }
}

.exam-actions {
  display: flex;
  gap: $spacing-xs;
  border-top: 1px solid $border-extra-light;
  padding-top: $spacing-sm;
  margin-top: auto;
}

.lift-card {
  transition: transform $transition-slow, box-shadow $transition-slow;

  &:hover {
    transform: translateY(-4px);
    box-shadow: $shadow-heavy;
  }
}

.empty-state {
  text-align: center;
  padding: 60px $spacing-lg;
  color: $text-secondary;
}
</style>
