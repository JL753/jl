<template>
  <div class="content-mgmt">
    <h1>内容管理</h1>
    <p class="subtitle">管理课程章节与子章节内容</p>

    <div class="subject-tabs">
      <button v-for="s in subjects" :key="s.id" class="glass-btn"
              :class="{ active: currentSubjectId === s.id }" @click="selectSubject(s.id)">
        {{ s.name }}
      </button>
    </div>

    <div v-if="courses.length > 0" class="course-tabs">
      <button v-for="c in courses" :key="c.id" class="glass-btn"
              :class="{ active: currentCourseId === c.id }" @click="selectCourse(c.id)">
        {{ c.title }}
      </button>
    </div>
    <p v-else-if="currentSubjectId" class="empty-hint">暂无课程</p>

    <div v-if="currentCourseId" class="chapters-section">
      <div class="section-header">
        <h3>章节管理</h3>
        <button class="glass-btn" :disabled="loading" @click="showChapterForm = true">+ 添加章节</button>
      </div>

      <div v-if="showChapterForm" class="glass-card form-card">
        <input v-model="newChapter.title" placeholder="章节标题" class="form-input" />
        <textarea v-model="newChapter.description" placeholder="章节描述" class="form-textarea" rows="2"></textarea>
        <div class="form-actions">
          <button class="glass-btn" @click="showChapterForm = false">取消</button>
          <button class="glass-btn active" :disabled="loading" @click="createChapter">添加</button>
        </div>
      </div>

      <p v-if="chapters.length === 0 && !loading" class="empty-hint">暂无章节，点击上方按钮添加</p>
      <div v-for="ch in chapters" :key="ch.id" class="glass-card chapter-card">
        <div class="chapter-header" @click="toggleExpand(ch.id)">
          <h4>{{ ch.title }}</h4>
          <div class="chapter-actions">
            <button class="glass-btn small" :disabled="loading" @click.stop="showSubFormFor = ch.id">+ 子章节</button>
            <button class="glass-btn small danger" :disabled="loading" @click.stop="deleteChapter(ch.id)">删除</button>
            <span>{{ expanded.has(ch.id) ? '▾' : '▸' }}</span>
          </div>
        </div>

        <div v-if="expanded.has(ch.id)" class="subchapters-list">
          <div v-if="showSubFormFor === ch.id" class="glass-card form-card">
            <input v-model="newSub.title" placeholder="子章节标题" class="form-input" />
            <select v-model="newSub.type" class="form-select">
              <option value="doc">文档</option>
              <option value="video">视频</option>
              <option value="quiz">测验</option>
            </select>
            <div class="form-actions">
              <button class="glass-btn" @click="showSubFormFor = null">取消</button>
              <button class="glass-btn active" :disabled="loading" @click="createSubChapter(ch.id)">添加</button>
            </div>
          </div>

          <div v-for="sc in getSubs(ch.id)" :key="sc.id" class="subchapter-row">
            <span class="sc-name">{{ sc.title }}</span>
            <span class="sc-type">{{ typeName(sc.type) }}</span>
            <span class="sc-status">{{ sc.status || 'published' }}</span>
            <button class="glass-btn small danger" :disabled="loading" @click="deleteSubChapter(sc.id)">删除</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  apiSubjects, apiCoursesBySubject, apiCourseChapters,
  apiCreateChapter, apiDeleteChapter,
  apiCreateSubChapter, apiDeleteSubChapter
} from '../../api/index.js'

const subjects = ref([])
const currentSubjectId = ref(null)
const courses = ref([])
const currentCourseId = ref(null)
const chapters = ref([])
const expanded = ref(new Set())
const loading = ref(false)
const showChapterForm = ref(false)
const showSubFormFor = ref(null)

const newChapter = reactive({ title: '', description: '' })
const newSub = reactive({ title: '', type: 'doc', status: 'published' })

onMounted(async () => {
  try {
    const res = await apiSubjects()
    subjects.value = res.data || []
    if (subjects.value.length) selectSubject(subjects.value[0].id)
  } catch (e) {
    ElMessage.error('获取学科列表失败')
  }
})

async function selectSubject(id) {
  try {
    currentSubjectId.value = id
    currentCourseId.value = null
    chapters.value = []
    const res = await apiCoursesBySubject(id)
    courses.value = res.data || []
  } catch (e) {
    ElMessage.error('获取课程列表失败')
  }
}

async function selectCourse(id) {
  try {
    currentCourseId.value = id
    loading.value = true
    const res = await apiCourseChapters(id)
    chapters.value = res.data || []
    chapters.value.forEach(ch => expanded.value.add(ch.id))
  } catch (e) {
    ElMessage.error('获取章节列表失败')
  } finally {
    loading.value = false
  }
}

function toggleExpand(id) {
  if (expanded.value.has(id)) expanded.value.delete(id)
  else expanded.value.add(id)
  expanded.value = new Set(expanded.value)
}

function getSubs(chapterId) {
  return chapters.value.find(ch => ch.id === chapterId)?.subChapters || []
}

function typeName(t) {
  return { doc: '文档', video: '视频', quiz: '测验', assignment: '作业', discussion: '讨论', exercise: '练习' }[t] || t
}

async function createChapter() {
  if (!currentCourseId.value) return
  try {
    loading.value = true
    await apiCreateChapter({ courseId: currentCourseId.value, title: newChapter.title, description: newChapter.description, sortOrder: chapters.value.length + 1 })
    showChapterForm.value = false
    newChapter.title = ''; newChapter.description = ''
    await selectCourse(currentCourseId.value)
  } catch (e) {
    ElMessage.error('创建章节失败')
  } finally {
    loading.value = false
  }
}

async function deleteChapter(id) {
  try {
    await ElMessageBox.confirm('确认删除此章节？子章节也会被删除。', '提示', { type: 'warning' })
    loading.value = true
    await apiDeleteChapter(id)
    await selectCourse(currentCourseId.value)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除章节失败')
  } finally {
    loading.value = false
  }
}

async function createSubChapter(chapterId) {
  try {
    loading.value = true
    await apiCreateSubChapter({ chapterId, title: newSub.title, type: newSub.type, status: newSub.status, sortOrder: getSubs(chapterId).length + 1 })
    showSubFormFor.value = null
    newSub.title = ''
    await selectCourse(currentCourseId.value)
  } catch (e) {
    ElMessage.error('创建子章节失败')
  } finally {
    loading.value = false
  }
}

async function deleteSubChapter(id) {
  try {
    await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
    loading.value = true
    await apiDeleteSubChapter(id)
    await selectCourse(currentCourseId.value)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除子章节失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.content-mgmt { padding: 24px; }
h1 { color: #f1f5f9; font-size: 24px; margin-bottom: 4px; }
.subtitle { color: #64748b; margin-bottom: 20px; }
.glass-btn { padding: 8px 16px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.15); background: rgba(255,255,255,0.08); backdrop-filter: blur(8px); color: #e2e8f0; cursor: pointer; font-family: inherit; font-size: 13px; }
.glass-btn.active { background: rgba(59,130,246,0.3); border-color: rgba(59,130,246,0.4); }
.glass-btn.small { padding: 4px 10px; font-size: 12px; }
.glass-btn.danger { border-color: rgba(239,68,68,0.3); color: #fca5a5; }
.glass-card { background: rgba(255,255,255,0.06); backdrop-filter: blur(12px); border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 16px; margin-bottom: 12px; }
.subject-tabs, .course-tabs { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.form-card { display: flex; flex-direction: column; gap: 8px; }
.form-input, .form-textarea, .form-select { padding: 8px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.08); background: rgba(255,255,255,0.04); color: #f1f5f9; font-size: 13px; font-family: inherit; outline: none; width: 100%; box-sizing: border-box; }
.form-textarea { resize: vertical; }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-header h3 { color: #e2e8f0; margin: 0; }
.chapter-header { display: flex; justify-content: space-between; align-items: center; cursor: pointer; }
.chapter-header h4 { color: #f1f5f9; margin: 0; }
.chapter-actions { display: flex; gap: 8px; align-items: center; }
.subchapters-list { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.subchapter-row { display: flex; align-items: center; gap: 12px; padding: 8px; background: rgba(255,255,255,0.03); border-radius: 6px; }
.sc-name { flex: 1; color: #cbd5e1; font-size: 14px; }
.sc-type { color: #64748b; font-size: 12px; }
.sc-status { font-size: 11px; padding: 1px 8px; border-radius: 4px; background: rgba(34,197,94,0.15); color: #86efac; }
.empty-hint { color: #64748b; font-size: 14px; padding: 12px 0; text-align: center; }
</style>
