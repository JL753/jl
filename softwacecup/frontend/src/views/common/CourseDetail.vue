<template>
  <div class="course-detail">
    <!-- Header with back button -->
    <div class="detail-header">
      <button class="back-btn" @click="handleBack">
        <span class="back-icon">←</span>
        <span class="back-text">返回</span>
      </button>
    </div>

    <!-- Subject hero -->
    <div class="subject-hero glass-card">
      <div class="subject-icon-wrapper">
        <div
          class="subject-icon"
          :style="{ background: subjectColor + '22', color: subjectColor }"
        >
          {{ subject?.icon || '📚' }}
        </div>
      </div>
      <div class="subject-text">
        <h1 class="subject-name">{{ subject?.name || '加载中...' }}</h1>
        <p v-if="subject?.description" class="subject-desc">{{ subject.description }}</p>
      </div>
    </div>

    <!-- Progress bar -->
    <div class="progress-section glass-card">
      <div class="progress-header">
        <span class="progress-title">学习进度</span>
        <span class="progress-stat">{{ progress.completed }}/{{ progress.total }} 课时</span>
      </div>
      <div class="progress-track">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
      <div class="progress-percent">{{ Math.round(progressPercent) }}%</div>
    </div>

    <!-- Units accordion -->
    <div class="units-section">
      <div
        v-for="unit in units"
        :key="unit.id"
        class="unit-card glass-card"
        :class="{ expanded: expandedUnits.has(unit.id) }"
      >
        <div class="unit-header" @click="toggleUnit(unit.id)">
          <div class="unit-header-left">
            <span class="unit-toggle-icon" :class="{ rotated: expandedUnits.has(unit.id) }">▸</span>
            <div class="unit-info">
              <h3 class="unit-name">{{ unit.name }}</h3>
              <p v-if="unit.description" class="unit-desc">{{ unit.description }}</p>
            </div>
          </div>
          <div class="unit-meta">
            <span class="unit-lesson-count">{{ lessonsByUnit[unit.id]?.length || 0 }} 课时</span>
          </div>
        </div>

        <Transition name="expand">
          <div v-if="expandedUnits.has(unit.id)" class="unit-body">
            <div class="unit-lessons">
              <div
                v-for="lesson in lessonsByUnit[unit.id] || []"
                :key="lesson.id"
                class="lesson-row"
                @click="$router.push('/lessons/' + lesson.id)"
              >
                <div class="lesson-left">
                  <ProgressDot :status="getLessonDotStatus(lesson.id)" />
                  <span class="lesson-name">{{ lesson.name }}</span>
                  <span v-if="lesson.type" class="lesson-type-badge">
                    {{ lesson.type === 'video' ? '🎬' : '📝' }}
                  </span>
                </div>
                <div class="lesson-right">
                  <span class="lesson-mastery" :class="getMasteryClass(lesson.id)">
                    {{ getMasteryText(lesson.id) }}
                  </span>
                  <span v-if="lesson.duration" class="lesson-duration">
                    {{ Math.floor(lesson.duration / 60) }}分钟
                  </span>
                </div>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="loading && units.length === 0" class="empty-state">
      <div class="empty-icon">📖</div>
      <p>加载中...</p>
    </div>
    <div v-if="!loading && units.length === 0" class="empty-state">
      <div class="empty-icon">📖</div>
      <p>暂无课程内容</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  apiSubjects,
  apiSubjectUnits,
  apiUnitLessons,
  apiCourseProgress,
  apiLessonProgress,
} from '../../api/index.js'
import ProgressDot from '../../components/ProgressDot.vue'

const route = useRoute()
const router = useRouter()
const subjectId = computed(() => route.params.id)

const subject = ref({})
const units = ref([])
const lessonsByUnit = reactive({})
const progress = ref({ total: 0, completed: 0, percent: 0 })
const lessonProgressMap = ref({})
const expandedUnits = ref(new Set())
const loading = ref(true)

const subjectColor = computed(() => subject.value?.color || '#3b82f6')

const progressPercent = computed(() => {
  if (progress.value.total === 0) return 0
  return (progress.value.completed / progress.value.total) * 100
})

function toggleUnit(id) {
  const s = new Set(expandedUnits.value)
  if (s.has(id)) {
    s.delete(id)
  } else {
    s.add(id)
  }
  expandedUnits.value = s
}

function getLessonDotStatus(lessonId) {
  const lp = lessonProgressMap.value[lessonId]
  if (!lp) return 'pending'
  if (lp.status === 'completed' || lp.mastery >= 100) return 'completed'
  if (lp.status === 'in_progress' || (lp.mastery > 0)) return 'in_progress'
  return 'pending'
}

function getMasteryText(lessonId) {
  const lp = lessonProgressMap.value[lessonId]
  if (!lp) return '未开始'
  if (lp.mastery >= 100) return '已掌握'
  if (lp.mastery > 0) return `掌握${Math.round(lp.mastery)}%`
  if (lp.status === 'completed') return '已完成'
  if (lp.status === 'in_progress') return '进行中'
  return '未开始'
}

function getMasteryClass(lessonId) {
  const lp = lessonProgressMap.value[lessonId]
  if (!lp) return ''
  if (lp.mastery >= 100) return 'mastery-done'
  if (lp.mastery > 0) return 'mastery-progress'
  if (lp.status === 'completed') return 'mastery-done'
  return ''
}

function handleBack() {
  router.back()
}

onMounted(async () => {
  const id = subjectId.value
  try {
    // 1. Fetch subject info
    const sRes = await apiSubjects()
    subject.value = sRes.data.find((s) => String(s.id) === String(id)) || {}

    // 2. Fetch units
    const uRes = await apiSubjectUnits(id)
    units.value = uRes.data || []

    // 3. Fetch lessons for each unit + auto-open first unit
    let allLessons = []
    for (const u of units.value) {
      const lRes = await apiUnitLessons(u.id)
      const ls = lRes.data || []
      lessonsByUnit[u.id] = ls
      allLessons = allLessons.concat(ls)
    }

    if (units.value.length > 0) {
      expandedUnits.value = new Set([units.value[0].id])
    }

    // 4. Fetch course-level progress
    try {
      const cRes = await apiCourseProgress(id)
      progress.value = cRes.data || { total: 0, completed: 0, percent: 0 }
    } catch (e) {
      // Use lesson count as total
      progress.value.total = allLessons.length
    }

    // 5. Fetch per-lesson progress in parallel
    if (allLessons.length > 0) {
      const results = await Promise.allSettled(
        allLessons.map((l) => apiLessonProgress(l.id))
      )
      const map = {}
      results.forEach((r, idx) => {
        if (r.status === 'fulfilled' && r.value?.data) {
          map[allLessons[idx].id] = r.value.data
        }
      })
      lessonProgressMap.value = map
    }
  } catch (e) {
    console.error('Failed to load course detail:', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.course-detail {
  max-width: 860px;
  margin: 0 auto;
  padding: 24px 20px 40px;
  animation: fadeIn 0.3s ease;
}

/* Header */
.detail-header {
  margin-bottom: 16px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  color: var(--text-sub);
  cursor: pointer;
  transition: all var(--transition-base);
  font-size: var(--font-sm);
  line-height: 1;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-main);
}

.back-icon {
  font-size: 16px;
}

/* Subject hero */
.subject-hero {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 24px;
  margin-bottom: 16px;
}

.subject-icon-wrapper {
  flex-shrink: 0;
}

.subject-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  flex-shrink: 0;
}

.subject-text {
  flex: 1;
  min-width: 0;
}

.subject-name {
  font-size: var(--font-3xl);
  font-weight: 700;
  color: var(--text-main);
  margin: 0 0 6px;
}

.subject-desc {
  color: var(--text-sub);
  font-size: var(--font-base);
  margin: 0;
  line-height: 1.6;
}

/* Progress section */
.progress-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  margin-bottom: 20px;
}

.progress-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.progress-title {
  font-size: var(--font-sm);
  color: var(--text-sub);
}

.progress-stat {
  font-size: var(--font-xs);
  color: var(--text-sub);
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 999px;
}

.progress-track {
  flex: 1;
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  overflow: hidden;
  min-width: 80px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary), var(--accent-cyan));
  border-radius: 999px;
  transition: width 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.progress-percent {
  font-size: var(--font-sm);
  font-weight: 600;
  color: var(--primary-light);
  flex-shrink: 0;
  min-width: 40px;
  text-align: right;
}

/* Units section */
.units-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.unit-card {
  overflow: hidden;
  transition: all var(--transition-base);
}

.unit-card.expanded {
  border-color: rgba(59, 130, 246, 0.25);
}

.unit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
  padding: 2px 0;
}

.unit-header-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.unit-toggle-icon {
  color: var(--text-faint);
  font-size: 14px;
  transition: transform var(--transition-base);
  flex-shrink: 0;
  margin-top: 3px;
}

.unit-toggle-icon.rotated {
  transform: rotate(90deg);
}

.unit-info {
  flex: 1;
  min-width: 0;
}

.unit-name {
  font-size: var(--font-lg);
  font-weight: 600;
  color: var(--text-main);
  margin: 0;
}

.unit-desc {
  font-size: var(--font-xs);
  color: var(--text-faint);
  margin: 4px 0 0;
  line-height: 1.5;
}

.unit-meta {
  flex-shrink: 0;
  margin-left: 12px;
}

.unit-lesson-count {
  font-size: var(--font-xs);
  color: var(--text-faint);
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 999px;
}

/* Unit body */
.unit-body {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--glass-border);
}

.unit-lessons {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.lesson-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-radius: var(--radius-xs);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.lesson-row:hover {
  background: rgba(255, 255, 255, 0.06);
}

.lesson-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.lesson-name {
  font-size: var(--font-base);
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.lesson-type-badge {
  font-size: 12px;
  flex-shrink: 0;
}

.lesson-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.lesson-mastery {
  font-size: var(--font-xs);
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-faint);
}

.lesson-mastery.mastery-done {
  background: rgba(16, 185, 129, 0.15);
  color: var(--success);
}

.lesson-mastery.mastery-progress {
  background: rgba(59, 130, 246, 0.12);
  color: var(--primary-light);
}

.lesson-duration {
  font-size: var(--font-xs);
  color: var(--text-faint);
}

/* Empty state */
.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  color: var(--text-faint);
  font-size: var(--font-base);
}

/* Expand animation */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
  margin-top: 0;
  border-top-width: 0;
}

/* Fade in animation */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
