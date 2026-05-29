<template>
  <div class="assign-mgmt">
    <h1>作业管理</h1>

    <div class="glass-card create-assign">
      <h3>布置新作业</h3>
      <el-select v-model="selectedClassId" placeholder="选择班级" style="width:100%;margin-bottom:8px">
        <el-option v-for="c in classList" :key="c.classId || c.id" :label="c.className || c.name" :value="c.classId || c.id" />
      </el-select>
      <input v-model="newAssign.title" placeholder="作业标题" class="glass-input" />
      <textarea v-model="newAssign.description" placeholder="作业描述" class="glass-textarea"></textarea>
      <button class="glass-btn primary" :disabled="creating" @click="createAssignment">
        {{ creating ? '布置中...' : '布置作业' }}
      </button>
    </div>

    <div v-if="loading" style="text-align:center;padding:40px;color:#94a3b8">加载中...</div>
    <div v-for="a in assignments" :key="a.id" class="glass-card assign-card">
      <h3>{{ a.title }}</h3>
      <p class="assign-desc">{{ a.description }}</p>
      <span class="assign-meta">截止：{{ a.dueAt || '未设置' }} | 提交：{{ a.submissionCount || 0 }}</span>
    </div>
    <div v-if="!loading && assignments.length === 0" class="empty-state">暂无作业</div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { apiClassAssignments, apiCreateAssignment, apiMyClasses } from '../../api'

const assignments = ref([])
const classList = ref([])
const selectedClassId = ref(null)
const loading = ref(false)
const creating = ref(false)
const newAssign = reactive({ title: '', description: '' })

onMounted(async () => {
  try {
    loading.value = true
    const res = await apiMyClasses()
    classList.value = (res.data?.data || res.data || [])
    if (classList.value.length > 0) {
      selectedClassId.value = classList.value[0].classId || classList.value[0].id
      await loadAssignments()
    }
  } catch (e) {
    ElMessage.error('加载班级列表失败')
  } finally {
    loading.value = false
  }
})

async function loadAssignments() {
  if (!selectedClassId.value) return
  try {
    const res = await apiClassAssignments(selectedClassId.value)
    assignments.value = res.data?.data || []
  } catch (e) {
    ElMessage.error('加载作业列表失败')
  }
}

async function createAssignment() {
  if (!newAssign.title.trim()) { ElMessage.warning('请输入作业标题'); return }
  if (!selectedClassId.value) { ElMessage.warning('请选择班级'); return }
  try {
    creating.value = true
    await apiCreateAssignment({
      classId: selectedClassId.value,
      title: newAssign.title,
      description: newAssign.description,
      lessonIds: '[]'
    })
    newAssign.title = ''
    newAssign.description = ''
    ElMessage.success('作业已布置')
    await loadAssignments()
  } catch (e) {
    ElMessage.error('布置失败，请重试')
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.assign-mgmt { padding: 24px; }
h1 { color: #f1f5f9; font-size: 24px; margin-bottom: 20px; }
.glass-card { background: rgba(255,255,255,0.06); backdrop-filter: blur(12px); border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 16px; margin-bottom: 12px; }
.glass-input, .glass-textarea { width: 100%; padding: 8px 12px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.1); background: rgba(255,255,255,0.05); color: #e2e8f0; margin-bottom: 8px; outline: none; }
.glass-btn { padding: 8px 16px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.15); background: rgba(255,255,255,0.08); backdrop-filter: blur(8px); color: #e2e8f0; cursor: pointer; }
.glass-btn.primary { background: rgba(59,130,246,0.3); border-color: rgba(59,130,246,0.4); }
.glass-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.assign-card h3 { color: #e2e8f0; margin: 0 0 4px; }
.assign-desc { color: #94a3b8; font-size: 13px; margin: 0 0 4px; }
.assign-meta { color: #64748b; font-size: 12px; }
.empty-state { color: #64748b; text-align: center; padding: 40px; }
</style>
