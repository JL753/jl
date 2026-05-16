<template>
  <div class="dashboard">
    <!-- ═══ Top Banner: Radar + User Info ═══ -->
    <section class="top-banner glass-card">
      <div class="radar-area">
        <div ref="radarChartRef" class="radar-chart"></div>
      </div>
      <div class="user-area">
        <h2 class="user-name">{{ userInfo.name }}</h2>
        <div class="user-metrics">
          <div class="metric-item">
            <span class="metric-label">连续学习</span>
            <span class="metric-value streak">{{ userInfo.streak }} 天</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">知识广度</span>
            <span class="metric-value">{{ abilities.breadth }}</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">知识深度</span>
            <span class="metric-value">{{ abilities.depth }}</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">解题能力</span>
            <span class="metric-value">{{ abilities.problem }}</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">活跃度</span>
            <span class="metric-value">{{ abilities.activity }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ Content Flow ═══ -->

    <!-- 继续学习 -->
    <section class="content-section glass-card" v-if="continueCourse">
      <div class="section-header">
        <h3 class="section-title">继续学习</h3>
      </div>
      <div class="continue-course">
        <div class="cc-info">
          <span class="cc-name">{{ continueCourse.courseName || continueCourse.name || continueCourse.title || '未命名课程' }}</span>
          <span class="cc-desc" v-if="continueCourse.description">{{ continueCourse.description }}</span>
        </div>
        <button class="action-btn" @click="goToCourse(continueCourse.id || continueCourse.subjectId)">继续</button>
      </div>
    </section>

    <!-- AI推荐资源 -->
    <section class="content-section glass-card" v-if="aiResources.length > 0">
      <div class="section-header">
        <h3 class="section-title">AI推荐资源</h3>
      </div>
      <div class="resource-list">
        <div class="resource-item" v-for="res in aiResources" :key="res.id">
          <div class="ri-left">
            <span class="ri-title">{{ res.title || res.name || '资源' }}</span>
            <span class="ri-platform">{{ res.platform || res.source || '' }}</span>
          </div>
          <p class="ri-desc">{{ res.description || '' }}</p>
        </div>
      </div>
    </section>

    <!-- 今日任务 -->
    <section class="content-section glass-card">
      <div class="section-header">
        <h3 class="section-title">今日任务</h3>
      </div>
      <div class="task-list">
        <div class="task-item" v-for="(task, i) in todayTasks" :key="i">
          <span class="task-dot"></span>
          <span class="task-text">{{ task }}</span>
        </div>
        <p v-if="todayTasks.length === 0" class="empty-text">暂无待办任务</p>
      </div>
    </section>

    <!-- 快捷入口 -->
    <section class="quick-entry">
      <button class="quick-btn glass-card" @click="goToAllCourses">
        <span class="qb-icon">📚</span>
        <span class="qb-label">全部课程</span>
      </button>
      <button class="quick-btn glass-card" @click="goToKnowledgeMap">
        <span class="qb-icon">🗺️</span>
        <span class="qb-label">知识星图</span>
      </button>
      <button class="quick-btn glass-card" @click="goToCompanion">
        <span class="qb-icon">🤖</span>
        <span class="qb-label">AI辅导</span>
      </button>
      <button class="quick-btn glass-card" @click="goToAchievements">
        <span class="qb-icon">🏆</span>
        <span class="qb-label">成就</span>
      </button>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import {
  apiAbilityLatest,
  apiStudentDashboard,
  apiGamificationStreak
} from '../../api/index.js'
import * as echarts from 'echarts'

const auth = useAuthStore()
const router = useRouter()
const radarChartRef = ref(null)

const abilities = ref({
  breadth: 0,
  depth: 0,
  problem: 0,
  activity: 0,
  transfer: 0,
  resilience: 0
})
const userInfo = ref({ name: '', streak: 0 })
const continueCourse = ref(null)
const aiResources = ref([])
const todayTasks = ref([])

let radarChartInstance = null

onMounted(async () => {
  // Fetch ability data
  try {
    const res = await apiAbilityLatest()
    const data = res.data?.data || res.data || {}
    abilities.value = {
      breadth: data.breadth || 0,
      depth: data.depth || 0,
      problem: data.problem || 0,
      activity: data.activity || 0,
      transfer: data.transfer || 0,
      resilience: data.resilience || 0,
    }
  } catch (e) {
    console.warn('Failed to fetch ability data', e)
  }

  // Fetch streak
  try {
    const res = await apiGamificationStreak()
    userInfo.value.streak = res.data?.data?.currentStreak || 0
  } catch (e) {
    console.warn('Failed to fetch streak', e)
  }

  userInfo.value.name = auth.user?.username || '学生'

  // Fetch dashboard data for current course info
  try {
    const res = await apiStudentDashboard()
    const data = res.data?.data || {}
    if (data.summary && data.summary.title) {
      continueCourse.value = {
        title: data.summary.title
      }
    }
  } catch (e) {
    console.warn('Failed to fetch dashboard data', e)
  }

  // Init radar chart after data is ready
  await nextTick()
  initRadarChart()

  // Set some default tasks
  todayTasks.value = ['完成今日推荐课程', '复习已学知识点', '完成课后练习']
})

function initRadarChart() {
  if (!radarChartRef.value) return

  radarChartInstance = echarts.init(radarChartRef.value)
  const option = {
    backgroundColor: 'transparent',
    radar: {
      center: ['50%', '50%'],
      radius: '70%',
      indicator: [
        { name: '知识广度', max: 100 },
        { name: '知识深度', max: 100 },
        { name: '解题能力', max: 100 },
        { name: '活跃度', max: 100 },
        { name: '知识迁移', max: 100 },
        { name: '学习韧性', max: 100 },
      ],
      axisName: {
        color: 'rgba(255,255,255,0.5)',
        fontSize: 11,
      },
      splitArea: {
        areaStyle: {
          color: [
            'rgba(59,130,246,0.02)',
            'rgba(59,130,246,0.05)',
          ],
        },
      },
      splitLine: {
        lineStyle: { color: 'rgba(255,255,255,0.08)' },
      },
      axisLine: {
        lineStyle: { color: 'rgba(255,255,255,0.08)' },
      },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: [
              abilities.value.breadth,
              abilities.value.depth,
              abilities.value.problem,
              abilities.value.activity,
              abilities.value.transfer,
              abilities.value.resilience,
            ],
          },
        ],
        areaStyle: { color: 'rgba(59,130,246,0.2)' },
        lineStyle: { color: '#60a5fa', width: 2 },
        itemStyle: { color: '#3b82f6' },
      },
    ],
  }
  radarChartInstance.setOption(option)

  // Resize handler
  window.addEventListener('resize', handleResize)
}

function handleResize() {
  if (radarChartInstance) {
    radarChartInstance.resize()
  }
}

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (radarChartInstance) {
    radarChartInstance.dispose()
    radarChartInstance = null
  }
})

function goToCourse(id) {
  if (id) {
    router.push(`/student/subjects/${id}`)
  } else {
    router.push('/student/courses')
  }
}
function goToAllCourses() {
  router.push('/student/courses')
}
function goToKnowledgeMap() {
  router.push('/student/knowledge-map')
}
function goToCompanion() {
  router.push('/student/companion')
}
function goToAchievements() {
  router.push('/student/achievements')
}
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
}

/* ── Glass card base ── */
.glass-card {
  background: rgba(0, 0, 0, 0.42);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  padding: 18px 20px;
  transition: border-color 0.2s, transform 0.2s;
}
.glass-card:hover {
  border-color: rgba(88, 166, 255, 0.25);
  transform: translateY(-1px);
}

/* ── Top Banner ── */
.top-banner {
  display: flex;
  gap: 20px;
  align-items: center;
}
.radar-area {
  width: 220px;
  height: 220px;
  flex-shrink: 0;
}
.radar-chart {
  width: 100%;
  height: 100%;
}
.user-area {
  flex: 1;
  min-width: 0;
}
.user-name {
  font-size: 22px;
  font-weight: 700;
  color: #e6edf3;
  margin: 0 0 16px;
}
.user-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}
.metric-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
}
.metric-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}
.metric-value {
  font-size: 20px;
  font-weight: 700;
  color: #e6edf3;
}
.metric-value.streak {
  color: #d29922;
}

/* ── Content Sections ── */
.content-section {
  margin-bottom: 0;
}
.section-header {
  margin-bottom: 12px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(230, 237, 243, 0.8);
  margin: 0;
}

/* Continue course */
.continue-course {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.15);
}
.cc-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.cc-name {
  font-size: 14px;
  font-weight: 600;
  color: #e6edf3;
}
.cc-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.action-btn {
  padding: 8px 20px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  color: white;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.action-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

/* Resource list */
.resource-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.resource-item {
  padding: 10px 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.ri-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ri-title {
  font-size: 13px;
  font-weight: 600;
  color: #e6edf3;
}
.ri-platform {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  padding: 1px 8px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
}
.ri-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  margin: 0;
  line-height: 1.5;
}

/* Task list */
.task-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.task-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
}
.task-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #3b82f6;
  flex-shrink: 0;
}
.task-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.65);
}
.empty-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.3);
  text-align: center;
  padding: 16px 0;
}

/* ── Quick Entry ── */
.quick-entry {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.quick-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 12px;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(0, 0, 0, 0.35);
  border-radius: 14px;
  transition: all 0.2s;
}
.quick-btn:hover {
  border-color: rgba(88, 166, 255, 0.3);
  background: rgba(88, 166, 255, 0.08);
  transform: translateY(-2px);
}
.qb-icon {
  font-size: 28px;
}
.qb-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
}

/* ═══ Responsive ═══ */
@media (max-width: 1000px) {
  .top-banner {
    flex-direction: column;
    align-items: flex-start;
  }
  .radar-area {
    width: 200px;
    height: 200px;
    align-self: center;
  }
  .user-metrics {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 640px) {
  .dashboard {
    padding: 12px;
  }
  .quick-entry {
    grid-template-columns: repeat(2, 1fr);
  }
  .user-metrics {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
