<template>
  <div class="subject-catalog">
    <h1 class="page-title">探索知识领域</h1>
    <p class="page-subtitle">选择一门学科，开启学习之旅</p>

    <div class="subject-grid">
      <div v-for="s in subjects" :key="s.id" class="subject-card glass-card" @click="goToSubject(s.id)">
        <div class="subject-icon" :style="{ background: s.color + '22', color: s.color }">{{ s.icon || s.name?.charAt(0) || 'S' }}</div>
        <div class="subject-name">{{ s.name }}</div>
        <div class="subject-desc">{{ s.description }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiSubjects } from '../../api/index.js'

const route = useRoute()
const router = useRouter()
const subjects = ref([])

function goToSubject(id) {
  if (route.path.startsWith('/student')) {
    router.push(`/student/subjects/${id}`)
  } else {
    router.push(`/subjects/${id}`)
  }
}

onMounted(async () => {
  try {
    subjects.value = (await apiSubjects()).data || []
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.subject-catalog { min-height: 100vh; padding: 40px 20px; max-width: 1200px; margin: 0 auto; }
.page-title { color: #f1f5f9; font-size: 28px; margin-bottom: 8px; }
.page-subtitle { color: #64748b; margin-bottom: 32px; }
.subject-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
.subject-card {
  background: rgba(255,255,255,0.06); backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 24px;
  cursor: pointer; transition: all 0.3s;
}
.subject-card:hover { background: rgba(255,255,255,0.1); transform: translateY(-2px); }
.subject-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 24px; margin-bottom: 12px; }
.subject-name { color: #f1f5f9; font-size: 18px; font-weight: 600; margin-bottom: 6px; }
.subject-desc { color: #94a3b8; font-size: 13px; line-height: 1.5; }
</style>
