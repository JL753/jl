<template>
  <div class="class-mgmt">
    <h1>班级管理</h1>
    <div class="action-bar">
      <button class="glass-btn primary" @click="showCreate = true">+ 创建班级</button>
    </div>

    <div v-if="showCreate" class="glass-card create-form">
      <input v-model="newName" placeholder="班级名称" class="glass-input" />
      <textarea v-model="newDesc" placeholder="班级描述（可选）" class="glass-textarea"></textarea>
      <div class="form-actions">
        <button class="glass-btn" @click="showCreate = false">取消</button>
        <button class="glass-btn primary" @click="createClass">创建</button>
      </div>
    </div>

    <div v-for="c in classes" :key="c.id" class="glass-card class-card">
      <div class="class-header" @click="c.expanded = !c.expanded">
        <div>
          <h3>{{ c.name }}</h3>
          <span class="invite-code">邀请码：{{ c.inviteCode }}</span>
        </div>
        <span>{{ c.expanded ? '▾' : '▸' }}</span>
      </div>
      <div v-if="c.expanded">
        <div class="member-list">
          <div v-for="m in (classMembers[c.id] || [])" :key="m.id" class="member-row">
            <span>{{ m.displayName || m.username }}</span>
            <button class="glass-btn small danger" @click="removeMember(c.id, m.id)">移除</button>
          </div>
          <div v-if="!classMembers[c.id]?.length" class="empty-hint">暂无成员</div>
        </div>
      </div>
    </div>
    <div v-if="classes.length === 0" class="empty-state">暂无班级，点击上方按钮创建</div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { apiMyClasses, apiCreateClass, apiClassMembers, apiRemoveClassMember } from '../../api/index.js'

const classes = ref([])
const classMembers = reactive({})
const showCreate = ref(false)
const newName = ref('')
const newDesc = ref('')

onMounted(async () => {
  try {
    const res = await apiMyClasses()
    classes.value = res.data || []
    const results = await Promise.all(classes.value.map(c =>
      apiClassMembers(c.id).then(r => ({ id: c.id, members: r.data || [] })).catch(() => ({ id: c.id, members: [] }))
    ))
    results.forEach(({ id, members }) => { classMembers[id] = members })
  } catch(e) { ElMessage.error('加载班级失败') }
})

async function createClass() {
  if (!newName.value) return
  try {
    const res = await apiCreateClass({ name: newName.value, description: newDesc.value })
    const newClass = res.data
    classes.value.push(newClass)
    classMembers[newClass.id] = []
    showCreate.value = false
    newName.value = ''
    newDesc.value = ''
    ElMessage.success('班级创建成功')
  } catch(e) { ElMessage.error('创建班级失败') }
}

async function removeMember(classId, studentId) {
  try {
    await apiRemoveClassMember(classId, studentId)
    classMembers[classId] = classMembers[classId].filter(m => m.id !== studentId)
    ElMessage.success('已移除成员')
  } catch(e) { ElMessage.error('移除成员失败') }
}
</script>

<style scoped>
.class-mgmt { padding: 24px; }
h1 { color: #f1f5f9; font-size: 24px; margin-bottom: 20px; }
.action-bar { margin-bottom: 16px; }
.glass-btn { padding: 8px 16px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.15); background: rgba(255,255,255,0.08); backdrop-filter: blur(8px); color: #e2e8f0; cursor: pointer; }
.glass-btn.primary { background: rgba(59,130,246,0.3); border-color: rgba(59,130,246,0.4); }
.glass-btn.small { padding: 4px 10px; font-size: 12px; }
.glass-btn.danger { border-color: rgba(239,68,68,0.4); }
.glass-card { background: rgba(255,255,255,0.06); backdrop-filter: blur(12px); border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 16px; margin-bottom: 12px; }
.glass-input, .glass-textarea { width: 100%; padding: 8px 12px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.1); background: rgba(255,255,255,0.05); color: #e2e8f0; margin-bottom: 8px; outline: none; }
.form-actions { display: flex; gap: 8px; justify-content: flex-end; }
.class-header { display: flex; justify-content: space-between; align-items: center; cursor: pointer; }
.class-header h3 { color: #e2e8f0; font-size: 16px; margin: 0; }
.invite-code { color: #60a5fa; font-size: 12px; }
.member-list { margin-top: 12px; }
.member-row { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; border-bottom: 1px solid rgba(255,255,255,0.05); color: #cbd5e1; font-size: 14px; }
.empty-state, .empty-hint { color: #64748b; text-align: center; padding: 20px; }
</style>
