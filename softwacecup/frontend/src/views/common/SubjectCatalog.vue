<template>
  <div class="subject-catalog">
    <div class="page-header">
      <div>
        <h1 class="page-title">探索知识领域</h1>
        <p class="page-subtitle">选择一门学科，开启学习之旅</p>
      </div>
      <button v-if="isStudent" class="bili-btn" @click="showBiliImport = true">B站导入</button>
    </div>

    <div class="subject-grid">
      <div v-for="s in subjects" :key="s.id" class="subject-card glass-card" @click="goToSubject(s.id)">
        <div class="subject-icon" :style="{ background: s.color + '22', color: s.color }">{{ s.icon || s.name?.charAt(0) || 'S' }}</div>
        <div class="subject-name">{{ s.name }}</div>
        <div class="subject-desc">{{ s.description }}</div>
      </div>
    </div>

    <!-- 我的导入视频 -->
    <div v-if="isStudent && myImports.length > 0" class="my-imports">
      <h2 class="section-title">我的导入视频</h2>
      <div class="imports-row">
        <div v-for="imp in myImports" :key="imp.bvid" class="import-card glass-card" @click="$router.push('/student/courses/' + imp.courseId + '?sc=' + imp.firstSubChapterId)">
          <div class="import-cover">
            <img v-if="imp.coverUrl" :src="imp.coverUrl" class="import-cover-img" @error="e => e.target.style.display='none'" />
            <div v-if="!imp.coverUrl" class="import-cover-placeholder">{{ imp.title?.charAt(0) || 'V' }}</div>
          </div>
          <div class="import-info">
            <div class="import-title">{{ imp.title }}</div>
            <div class="import-meta">{{ imp.subjectName }} / {{ imp.unitName }}</div>
            <div class="import-progress">{{ imp.lessonCount }} 课时</div>
          </div>
        </div>
      </div>
    </div>

    <BilibiliImportModal :visible="showBiliImport" @close="showBiliImport = false; loadMyImports()" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiSubjects, apiMyImports } from '../../api/index.js'
import BilibiliImportModal from '../../components/BilibiliImportModal.vue'

const route = useRoute()
const router = useRouter()
const subjects = ref([])
const myImports = ref([])
const showBiliImport = ref(false)
const isStudent = computed(() => route.path.startsWith('/student'))

function goToSubject(id) {
  if (isStudent.value) {
    router.push(`/student/subjects/${id}`)
  } else {
    router.push(`/subjects/${id}`)
  }
}

async function loadMyImports() {
  if (!isStudent.value) return
  try {
    const res = await apiMyImports()
    myImports.value = res.data || []
  } catch (e) { /* ignore */ }
}

onMounted(async () => {
  try {
    subjects.value = (await apiSubjects()).data || []
  } catch (e) { /* ignore */ }
  loadMyImports()
})
</script>

<style scoped>
.subject-catalog { min-height: 100vh; padding: 40px 20px; max-width: 1200px; margin: 0 auto; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; }
.page-title { color: #f1f5f9; font-size: 28px; margin-bottom: 8px; }
.page-subtitle { color: #64748b; margin-bottom: 32px; }

.bili-btn {
  padding: 8px 16px; border-radius: 8px; border: 1px solid rgba(251, 114, 153, 0.3);
  background: rgba(251, 114, 153, 0.1); color: #fda4af; cursor: pointer;
  font-size: 13px; font-family: inherit; white-space: nowrap; margin-top: 4px;
}
.bili-btn:hover { background: rgba(251, 114, 153, 0.15); }

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

/* 我的导入视频 */
.my-imports { margin-top: 40px; }
.section-title { color: #f1f5f9; font-size: 20px; font-weight: 600; margin-bottom: 16px; }
.imports-row { display: flex; gap: 14px; overflow-x: auto; padding-bottom: 8px; }
.import-card {
  min-width: 200px; max-width: 220px; padding: 12px; cursor: pointer;
  background: rgba(255,255,255,0.04); backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.06); border-radius: 10px;
  transition: all 0.2s; flex-shrink: 0;
}
.import-card:hover { background: rgba(255,255,255,0.08); transform: translateY(-2px); }
.import-cover { margin-bottom: 10px; }
.import-cover-img {
  width: 100%; height: 100px; border-radius: 6px; object-fit: cover; display: block;
}
.import-cover-placeholder {
  width: 100%; height: 100px; border-radius: 6px;
  background: linear-gradient(135deg, #3b82f6, #a855f7);
  display: flex; align-items: center; justify-content: center;
  font-size: 36px; font-weight: 800; color: #fff; opacity: 0.8;
}
.import-info { min-width: 0; }
.import-title { font-size: 13px; font-weight: 600; color: #f1f5f9; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 4px; }
.import-meta { font-size: 10px; color: rgba(255,255,255,0.4); margin-bottom: 2px; }
.import-progress { font-size: 10px; color: #60d9fa; }
</style>
