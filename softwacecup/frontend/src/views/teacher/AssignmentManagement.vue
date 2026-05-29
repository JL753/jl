<template>
  <div class="assign-mgmt">
    <h1>作业管理</h1>

    <div class="glass-card create-assign">
      <h3>布置新作业</h3>
      <input v-model="newAssign.title" placeholder="作业标题" class="glass-input" />
      <textarea v-model="newAssign.description" placeholder="作业描述" class="glass-textarea"></textarea>
      <button class="glass-btn primary" @click="createAssignment">布置作业</button>
    </div>

    <div v-for="a in assignments" :key="a.id" class="glass-card assign-card">
      <h3>{{ a.title }}</h3>
      <p class="assign-desc">{{ a.description }}</p>
      <span class="assign-meta">截止：{{ a.dueAt || '无' }}</span>
    </div>
    <div v-if="assignments.length === 0" class="empty-state">暂无作业</div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'

const assignments = ref([])
const newAssign = reactive({ title: '', description: '' })

onMounted(async () => {
  try {
    const res = await axios.get('/api/assignments/class/1')
    assignments.value = res.data?.data || []
  } catch(e) { /* ignore */ }
})

async function createAssignment() {
  if (!newAssign.title) return
  try {
    await axios.post('/api/assignments', { classId: 1, title: newAssign.title, description: newAssign.description, lessonIds: '[]' })
    newAssign.title = ''; newAssign.description = ''
  } catch(e) { /* ignore */ }
}
</script>

<style scoped>
.assign-mgmt { padding: 24px; }
h1 { color: #f1f5f9; font-size: 24px; margin-bottom: 20px; }
.glass-card { background: rgba(255,255,255,0.06); backdrop-filter: blur(12px); border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 16px; margin-bottom: 12px; }
.glass-input, .glass-textarea { width: 100%; padding: 8px 12px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.1); background: rgba(255,255,255,0.05); color: #e2e8f0; margin-bottom: 8px; outline: none; }
.glass-btn { padding: 8px 16px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.15); background: rgba(255,255,255,0.08); backdrop-filter: blur(8px); color: #e2e8f0; cursor: pointer; }
.glass-btn.primary { background: rgba(59,130,246,0.3); border-color: rgba(59,130,246,0.4); }
.assign-card h3 { color: #e2e8f0; margin: 0 0 4px; }
.assign-desc { color: #94a3b8; font-size: 13px; margin: 0 0 4px; }
.assign-meta { color: #64748b; font-size: 12px; }
.empty-state { color: #64748b; text-align: center; padding: 40px; }
</style>
