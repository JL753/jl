<template>
  <div class="course-detail">
    <div class="detail-header">
      <button class="back-btn" @click="handleBack">
        <span class="back-icon">←</span>
        <span class="back-text">返回</span>
      </button>
    </div>

    <!-- Subject hero -->
    <div class="subject-hero glass-card">
      <div class="subject-icon-wrapper">
        <div class="subject-icon" :style="{ background: subjectColor + '22', color: subjectColor }">
          {{ subject?.icon || (subject?.name ? subject.name.charAt(0) : 'S') }}
        </div>
      </div>
      <div class="subject-text">
        <h1 class="subject-name">{{ subject?.name || '加载中...' }}</h1>
        <p v-if="subject?.description" class="subject-desc">{{ subject.description }}</p>
      </div>
    </div>

    <!-- Courses list -->
    <div v-if="loading && courses.length === 0" class="empty-state"><p>加载中...</p></div>
    <div v-if="!loading && courses.length === 0" class="empty-state"><p>暂无课程内容</p></div>

    <div class="courses-section">
      <div v-for="course in courses" :key="course.id" class="course-card glass-card"
        :class="{ expanded: expandedCourses.has(course.id) }">
        <div class="course-header" @click="toggleCourse(course.id)">
          <div class="course-header-left">
            <span class="course-toggle" :class="{ rotated: expandedCourses.has(course.id) }">▸</span>
            <div class="course-info">
              <h3 class="course-name">{{ course.title }}</h3>
              <p v-if="course.description" class="course-desc">{{ course.description }}</p>
            </div>
          </div>
          <div class="course-meta">
            <span class="course-progress-badge" v-if="courseProgress[course.id]?.percent">
              {{ Math.round(courseProgress[course.id].percent) }}%
            </span>
            <span class="course-sc-count" v-else-if="flatSubChapters(course.id).length">
              {{ flatSubChapters(course.id).length }} 课时
            </span>
          </div>
        </div>

        <Transition name="expand">
          <div v-if="expandedCourses.has(course.id)" class="course-body">
            <div v-if="treeLoading[course.id]" class="tree-loading">加载中...</div>

            <!-- 子章节扁平列表 -->
            <div v-else-if="flatSubChapters(course.id).length" class="sub-chapters-flat">
              <div v-for="sc in flatSubChapters(course.id)" :key="sc.id" class="sub-chapter-row"
                @click="goToLesson(course.id, sc.id)">
                <div class="sc-left">
                  <ProgressDot :status="getScProgressStatus(sc.id)" />
                  <span class="sc-title">{{ sc.title }}</span>
                  <span v-if="sc.chapterName" class="sc-chapter-tag">{{ sc.chapterName }}</span>
                </div>
                <div class="sc-right">
                  <span v-if="sc.type" class="sc-type-badge">
                    {{ sc.type === 'video' ? '视频' : sc.type === 'exercise' ? '练习' : sc.type }}
                  </span>
                  <span v-if="sc.duration" class="sc-duration">{{ Math.floor(sc.duration / 60) }}分钟</span>
                </div>
              </div>
            </div>
            <div v-else class="no-sub">暂无课时</div>
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  apiSubjects, apiCoursesBySubject, apiSubjectTree, apiCourseProgress, apiSubChapterProgress,
} from '../../api/index.js'
import ProgressDot from '../../components/ProgressDot.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const subjectId = computed(() => route.params.id)

const subject = ref({})
const courses = ref([])
const expandedCourses = ref(new Set())
const chapterTrees = reactive({})
const treeLoading = reactive({})
const courseProgress = reactive({})
const scProgressMap = ref({})
const loading = ref(true)

const subjectColor = computed(() => subject.value?.color || '#3b82f6')

function toggleCourse(id) {
  const s = new Set(expandedCourses.value)
  if (s.has(id)) { s.delete(id) } else { s.add(id); loadCourseTree(id) }
  expandedCourses.value = s
}

async function loadCourseTree(courseId) {
  if (chapterTrees[courseId]) return
  treeLoading[courseId] = true
  try {
    const res = await apiSubjectTree(courseId)
    // API 返回 [{name, chapters: [{title, subChapters}]}]
    const treeData = res.data || []
    const chapters = treeData.length > 0 ? (treeData[0].chapters || []) : []
    chapterTrees[courseId] = chapters

    // 并行加载每个子章节的进度
    const scIds = []
    for (const ch of chapters) {
      for (const sc of (ch.subChapters || [])) { scIds.push(sc.id) }
    }
    if (scIds.length > 0) {
      const results = await Promise.allSettled(
        scIds.map((scId) => apiSubChapterProgress(scId))
      )
      const map = { ...scProgressMap.value }
      results.forEach((r, idx) => {
        if (r.status === 'fulfilled' && r.value?.data) {
          map[scIds[idx]] = r.value.data
        }
      })
      scProgressMap.value = map
    }
  } catch { chapterTrees[courseId] = [] }
  finally { treeLoading[courseId] = false }
}

function flatSubChapters(courseId) {
  const chapters = chapterTrees[courseId] || []
  const result = []
  for (const ch of chapters) {
    for (const sc of (ch.subChapters || [])) {
      result.push({ ...sc, chapterName: ch.title })
    }
  }
  return result
}

function getScProgressStatus(scId) {
  const lp = scProgressMap.value[scId]
  if (!lp) return 'pending'
  if (lp.status === 'completed' || lp.mastery >= 100) return 'completed'
  if (lp.status === 'in_progress' || lp.mastery > 0) return 'in_progress'
  return 'pending'
}

function handleBack() { router.back() }

function goToLesson(courseId, scId) {
  router.push(`/student/courses/${courseId}?sc=${scId}`)
}

onMounted(async () => {
  const id = subjectId.value
  try {
    // 1. Subject info
    const sRes = await apiSubjects()
    subject.value = sRes.data.find((s) => String(s.id) === String(id)) || {}

    // 2. Courses under subject
    const cRes = await apiCoursesBySubject(id)
    courses.value = cRes.data || []

    // 3. Course progress
    for (const c of courses.value) {
      try {
        const pRes = await apiCourseProgress(c.id)
        courseProgress[c.id] = pRes.data || {}
      } catch { /* ignore */ }
    }

    // 4. Auto-expand first course
    if (courses.value.length > 0) {
      const firstId = courses.value[0].id
      expandedCourses.value = new Set([firstId])
      await loadCourseTree(firstId)
    }
  } catch (e) {
    ElMessage.error('课程详情加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.course-detail { max-width: 860px; margin: 0 auto; padding: 24px 20px 40px; animation: fadeIn 0.3s ease; }

.detail-header { margin-bottom: 16px; }
.back-btn {
  display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px;
  border: 1px solid var(--glass-border); border-radius: var(--radius-sm);
  background: var(--glass-bg); backdrop-filter: blur(var(--glass-blur));
  color: var(--text-sub); cursor: pointer; transition: all var(--transition-base);
  font-size: var(--font-sm); line-height: 1;
}
.back-btn:hover { background: rgba(255, 255, 255, 0.1); color: var(--text-main); }
.back-icon { font-size: 16px; }

.subject-hero { display: flex; align-items: flex-start; gap: 20px; padding: 24px; margin-bottom: 16px; }
.subject-icon { width: 64px; height: 64px; border-radius: 16px; display: flex; align-items: center; justify-content: center; font-size: 32px; flex-shrink: 0; }
.subject-name { font-size: var(--font-3xl); font-weight: 700; color: var(--text-main); margin: 0 0 6px; }
.subject-desc { color: var(--text-sub); font-size: var(--font-base); margin: 0; line-height: 1.6; }

/* Courses */
.courses-section { display: flex; flex-direction: column; gap: 12px; }
.course-card { overflow: hidden; transition: all var(--transition-base); }
.course-card.expanded { border-color: rgba(59, 130, 246, 0.25); }

.course-header {
  display: flex; justify-content: space-between; align-items: center;
  cursor: pointer; user-select: none; padding: 2px 0;
}
.course-header-left { display: flex; align-items: flex-start; gap: 12px; flex: 1; min-width: 0; }
.course-toggle {
  color: var(--text-faint); font-size: 14px; transition: transform var(--transition-base);
  flex-shrink: 0; margin-top: 3px;
}
.course-toggle.rotated { transform: rotate(90deg); }
.course-info { flex: 1; min-width: 0; }
.course-name { font-size: var(--font-lg); font-weight: 600; color: var(--text-main); margin: 0; }
.course-desc { font-size: var(--font-xs); color: var(--text-faint); margin: 4px 0 0; line-height: 1.5; }
.course-meta { flex-shrink: 0; margin-left: 12px; }
.course-progress-badge {
  font-size: var(--font-xs); color: var(--primary-light); padding: 2px 8px;
  background: rgba(59, 130, 246, 0.12); border-radius: 999px;
}

.course-body { margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--glass-border); }
.tree-loading { text-align: center; padding: 16px; color: var(--text-faint); font-size: 13px; }

.sub-chapters-flat { display: flex; flex-direction: column; gap: 2px; }
.sub-chapter-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 12px; border-radius: var(--radius-xs); cursor: pointer;
  transition: background var(--transition-fast);
}
.sub-chapter-row:hover { background: rgba(255, 255, 255, 0.06); }
.sc-left { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; }
.sc-title { font-size: var(--font-base); color: var(--text-main); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sc-chapter-tag { font-size: 10px; color: rgba(255,255,255,0.25); padding: 2px 6px; background: rgba(255,255,255,0.03); border-radius: 4px; flex-shrink: 0; margin-left: 4px; }
.sc-type-badge { font-size: 10px; color: rgba(255,255,255,0.3); padding: 2px 6px; background: rgba(255,255,255,0.04); border-radius: 4px; }
.sc-right { flex-shrink: 0; display: flex; align-items: center; gap: 8px; }
.sc-duration { font-size: var(--font-xs); color: var(--text-faint); }

.course-sc-count {
  font-size: var(--font-xs); color: var(--text-faint);
  padding: 2px 8px; background: rgba(255,255,255,0.04); border-radius: 999px;
}

.no-sub { font-size: 13px; color: var(--text-faint); padding: 12px; text-align: center; }

.empty-state { text-align: center; padding: 60px 20px; }
.empty-state p { color: var(--text-faint); font-size: var(--font-base); }

/* Expand animation */
.expand-enter-active, .expand-leave-active { transition: all 0.25s ease; overflow: hidden; }
.expand-enter-from, .expand-leave-to { opacity: 0; max-height: 0; }

.glass-card {
  background: rgba(255,255,255,0.06); backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 20px;
}

@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>
