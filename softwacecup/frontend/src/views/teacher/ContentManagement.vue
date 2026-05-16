<template>
  <div class="content-mgmt">
    <h1>内容管理</h1>
    <p class="subtitle">管理课程、单元和课时内容</p>
    <button class="import-btn" @click="showBiliImport = true">B站导入</button>

    <div class="subject-tabs">
      <button v-for="s in subjects" :key="s.id" class="glass-btn"
              :class="{ active: currentSubject === s.id }" @click="currentSubject = s.id">
        {{ s.name }}
      </button>
    </div>

    <div v-for="u in filteredUnits" :key="u.id" class="glass-card unit-admin-card">
      <div class="unit-header" @click="u.expanded = !u.expanded">
        <h3>{{ u.name }}</h3>
        <span>{{ u.expanded ? '▾' : '▸' }}</span>
      </div>
      <div v-if="u.expanded" class="lesson-list">
        <div v-for="l in (lessonsByUnit[u.id] || [])" :key="l.id" class="lesson-row">
          <span class="lesson-name">{{ l.name }}</span>
          <span class="lesson-type">{{ l.type }}</span>
          <span class="lesson-status" :class="l.status">{{ l.status || 'published' }}</span>
          <button class="glass-btn small" @click="aiGenerate(l.id)">AI 生成</button>
        </div>
      </div>
    </div>
    <BilibiliImportModal :visible="showBiliImport" @close="showBiliImport = false; refreshData()" />
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { apiSubjects, apiSubjectUnits, apiUnitLessons, apiAiGenerateContent } from '../../api/index.js'
import BilibiliImportModal from '../../components/BilibiliImportModal.vue'

const subjects = ref([])
const units = ref([])
const lessonsByUnit = reactive({})
const currentSubject = ref(null)
const showBiliImport = ref(false)

const filteredUnits = computed(() => units.value.filter(u => u.subjectId === currentSubject.value))

async function refreshData() {
  try {
    subjects.value = (await apiSubjects()).data || []
    if (subjects.value.length) currentSubject.value = subjects.value[0].id
    units.value = []
    Object.keys(lessonsByUnit).forEach(k => delete lessonsByUnit[k])
    for (const s of subjects.value) {
      const us = (await apiSubjectUnits(s.id)).data || []
      units.value.push(...us)
      for (const u of us) {
        const ls = (await apiUnitLessons(u.id)).data || []
        lessonsByUnit[u.id] = ls
      }
    }
  } catch(e) { /* ignore */ }
}

onMounted(() => { refreshData() })

async function aiGenerate(lessonId) {
  try {
    await apiAiGenerateContent(lessonId)
    alert('AI 内容已生成，请到审核页面查看')
  } catch(e) { alert('生成失败') }
}
</script>

<style scoped>
.content-mgmt { padding: 24px; }
h1 { color: #f1f5f9; font-size: 24px; margin-bottom: 4px; }
.subtitle { color: #64748b; margin-bottom: 20px; }
.glass-btn { padding: 8px 16px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.15); background: rgba(255,255,255,0.08); backdrop-filter: blur(8px); color: #e2e8f0; cursor: pointer; }
.glass-btn.active { background: rgba(59,130,246,0.3); border-color: rgba(59,130,246,0.4); }
.glass-btn.small { padding: 4px 10px; font-size: 12px; }
.glass-card { background: rgba(255,255,255,0.06); backdrop-filter: blur(12px); border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 16px; margin-bottom: 12px; }
.subject-tabs { display: flex; gap: 8px; margin-bottom: 20px; flex-wrap: wrap; }
.unit-header { display: flex; justify-content: space-between; align-items: center; cursor: pointer; }
.unit-header h3 { color: #e2e8f0; margin: 0; }
.lesson-list { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.lesson-row { display: flex; align-items: center; gap: 12px; padding: 8px; background: rgba(255,255,255,0.03); border-radius: 6px; }
.lesson-name { flex: 1; color: #cbd5e1; font-size: 14px; }
.lesson-type { color: #64748b; font-size: 12px; }
.lesson-status { font-size: 11px; padding: 1px 8px; border-radius: 4px; }
.lesson-status.published { background: rgba(34,197,94,0.15); color: #86efac; }
.lesson-status.draft { background: rgba(234,179,8,0.15); color: #fde68a; }
.import-btn {
  padding: 8px 16px; border-radius: 8px; border: 1px solid rgba(251, 114, 153, 0.3);
  background: rgba(251, 114, 153, 0.1); color: #fda4af; cursor: pointer;
  font-size: 13px; font-family: inherit; margin-bottom: 16px;
}
.import-btn:hover { background: rgba(251, 114, 153, 0.15); }
</style>
