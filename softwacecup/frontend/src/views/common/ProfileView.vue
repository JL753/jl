<template>
  <div class="profile-page">
    <!-- User Info Card -->
    <div class="glass-card user-card">
      <div class="user-avatar-wrap">
        <img
          v-if="userAvatar"
          :src="userAvatar"
          alt="avatar"
          class="user-avatar"
        />
        <div v-else class="avatar-placeholder">{{ userInitial }}</div>
      </div>
      <div class="user-info">
        <h2 class="user-name">{{ userName }}</h2>
        <div class="user-meta">
          <span class="meta-tag role-tag">{{ roleLabel }}</span>
          <span class="meta-tag date-tag">加入时间: {{ joinDate }}</span>
        </div>
      </div>
    </div>

    <!-- Tabs -->
    <div class="tab-bar">
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'overview' }"
        @click="activeTab = 'overview'"
      >
        学习概览
      </button>
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'achievement' }"
        @click="activeTab = 'achievement'"
      >
        成就中心
      </button>
    </div>

    <!-- Tab: 学习概览 -->
    <div v-show="activeTab === 'overview'" class="tab-content overview-tab">
      <!-- Radar Chart -->
      <div class="glass-card radar-card">
        <div ref="radarRef" class="radar-chart"></div>
      </div>

      <!-- Ability List -->
      <div class="glass-card ability-card">
        <h3 class="section-title">能力维度</h3>
        <div class="ability-list">
          <div
            v-for="dim in abilityDimensions"
            :key="dim.key"
            class="ability-row"
          >
            <span class="ability-name">{{ dim.label }}</span>
            <div class="ability-bar">
              <div
                class="ability-fill"
                :style="{ width: dim.value + '%', background: dim.color }"
              ></div>
            </div>
            <span class="ability-value">{{ dim.value }}</span>
          </div>
        </div>
      </div>

      <!-- Diagnostic Text -->
      <div class="glass-card diagnostic-card" v-if="diagnosticText">
        <h3 class="section-title">诊断建议</h3>
        <p class="diagnostic-text">{{ diagnosticText }}</p>
      </div>

      <!-- Stats Card -->
      <div class="glass-card stats-card">
        <h3 class="section-title">学习统计</h3>
        <div class="stats-grid">
          <div class="stat-item">
            <span class="stat-value">{{ stats.totalDays || 0 }}</span>
            <span class="stat-label">学习天数</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.completedLessons || 0 }}</span>
            <span class="stat-label">完成课时</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.accuracy || 0 }}%</span>
            <span class="stat-label">练习正确率</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Tab: 成就中心 -->
    <div v-show="activeTab === 'achievement'" class="tab-content achievement-tab">
      <AchievementCenter />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { apiAbilityLatest, apiGamificationProgress } from '../../api/index.js'
import * as echarts from 'echarts'
import AchievementCenter from '../student/AchievementCenter.vue'

const auth = useAuthStore()
const activeTab = ref('overview')
const radarRef = ref(null)
let radarChartInstance = null
const abilities = ref({ breadth: 0, depth: 0, problem: 0, activity: 0, transfer: 0, resilience: 0 })
const stats = ref({ totalDays: 0, completedLessons: 0, accuracy: 0 })

const abilityDimensions = computed(() => [
  { key: 'breadth', label: '知识广度', value: abilities.value.breadth, color: '#3b82f6' },
  { key: 'depth', label: '知识深度', value: abilities.value.depth, color: '#8b5cf6' },
  { key: 'problem', label: '解题能力', value: abilities.value.problem, color: '#10b981' },
  { key: 'activity', label: '活跃度', value: abilities.value.activity, color: '#f59e0b' },
  { key: 'transfer', label: '知识迁移', value: abilities.value.transfer, color: '#ef4444' },
  { key: 'resilience', label: '学习韧性', value: abilities.value.resilience, color: '#ec4899' },
])

const diagnosticText = computed(() => {
  const dims = abilityDimensions.value
  if (dims.every(d => d.value === 0)) return '暂无能力数据，开始学习后将生成诊断建议。'

  const highest = dims.reduce((a, b) => (a.value >= b.value ? a : b))
  const lowest = dims.reduce((a, b) => (a.value <= b.value ? a : b))

  let text = `你在「${highest.label}」方面表现突出`
  if (highest.value >= 80) text += '，继续保持！'
  else if (highest.value >= 60) text += '，基础扎实。'
  else text += '，仍有提升空间。'

  if (lowest.value < 40 && lowest.key !== highest.key) {
    text += ` 建议关注「${lowest.label}」的薄弱环节，针对性加强练习。`
  } else if (lowest.value < 60 && lowest.key !== highest.key) {
    text += ` 「${lowest.label}」方面可以进一步巩固。`
  }

  return text
})

const userName = computed(() => auth.user?.displayName || auth.user?.username || '未设置昵称')
const userInitial = computed(() => (userName.value || '?').slice(0, 1).toUpperCase())
const userAvatar = computed(() => auth.user?.avatarUrl || auth.user?.avatar || '')
const roleLabel = computed(() => {
  const role = auth.user?.role || 'student'
  const map = { student: '学生', teacher: '教师', admin: '管理员' }
  return map[role] || '用户'
})
const joinDate = computed(() => {
  const date = auth.user?.createdAt || auth.user?.created_at
  if (!date) return '未知'
  try {
    return new Date(date).toLocaleDateString('zh-CN')
  } catch {
    return '未知'
  }
})

function initRadarChart() {
  if (!radarRef.value) return

  if (radarChartInstance) {
    radarChartInstance.dispose()
    radarChartInstance = null
  }

  radarChartInstance = echarts.init(radarRef.value)
  const option = {
    backgroundColor: 'transparent',
    radar: {
      center: ['50%', '50%'],
      radius: '65%',
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
        fontSize: 12,
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(59,130,246,0.02)', 'rgba(59,130,246,0.05)'],
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
}

function handleResize() {
  radarChartInstance?.resize()
}

watch(activeTab, async (tab) => {
  if (tab === 'overview') {
    await nextTick()
    if (!radarChartInstance && radarRef.value) {
      initRadarChart()
    } else {
      radarChartInstance?.resize()
    }
  }
})

onMounted(async () => {
  try {
    const [abilityRes, gamifyRes] = await Promise.all([
      apiAbilityLatest(),
      apiGamificationProgress(),
    ])

    const abilityData = abilityRes.data?.data || abilityRes.data || {}
    abilities.value = {
      breadth: abilityData.breadth || 0,
      depth: abilityData.depth || 0,
      problem: abilityData.problem || 0,
      activity: abilityData.activity || 0,
      transfer: abilityData.transfer || 0,
      resilience: abilityData.resilience || 0,
    }

    const gamifyData = gamifyRes.data?.data || gamifyRes.data || {}
    stats.value = {
      totalDays: gamifyData.totalDays || gamifyData.days || gamifyData.studyDays || 0,
      completedLessons: gamifyData.completedLessons || gamifyData.lessons || gamifyData.lessonCount || 0,
      accuracy: gamifyData.accuracy || gamifyData.accuracyRate || 0,
    }
  } catch (e) {
    console.warn('Failed to load profile data', e)
  }

  await nextTick()
  initRadarChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (radarChartInstance) {
    radarChartInstance.dispose()
    radarChartInstance = null
  }
})
</script>

<style scoped>
.profile-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 20px 16px 80px;
  display: grid;
  gap: 14px;
}

/* Glass Card Base */
.glass-card {
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 20px;
}

/* User Card */
.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-avatar-wrap {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  flex-shrink: 0;
  overflow: hidden;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border: 2px solid rgba(255, 255, 255, 0.1);
}

.user-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  font-weight: 700;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 700;
  color: #e6edf3;
}

.user-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.meta-tag {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
}

.role-tag {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}

.date-tag {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.5);
}

/* Tabs */
.tab-bar {
  display: flex;
  gap: 4px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 12px;
  padding: 4px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.tab-btn {
  flex: 1;
  padding: 10px 16px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  background: rgba(59, 130, 246, 0.2);
  color: #60a5fa;
  font-weight: 600;
}

.tab-btn:hover:not(.active) {
  color: rgba(255, 255, 255, 0.7);
}

/* Tab Content */
.tab-content {
  display: grid;
  gap: 14px;
}

/* Radar Card */
.radar-card {
  display: flex;
  justify-content: center;
  padding: 16px;
}

.radar-chart {
  width: 340px;
  height: 340px;
}

/* Ability Card */
.section-title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
}

.ability-list {
  display: grid;
  gap: 10px;
}

.ability-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ability-name {
  width: 64px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  flex-shrink: 0;
  text-align: right;
}

.ability-bar {
  flex: 1;
  height: 8px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 4px;
  overflow: hidden;
}

.ability-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}

.ability-value {
  width: 28px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.7);
  text-align: right;
  flex-shrink: 0;
}

/* Diagnostic Card */
.diagnostic-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.65);
}

/* Stats Card */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: #e6edf3;
  margin-bottom: 4px;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

/* Achievement Tab */
.achievement-tab {
  /* AchievementCenter handles its own styling */
}

/* Responsive */
@media (max-width: 600px) {
  .radar-chart {
    width: 260px;
    height: 260px;
  }

  .stats-grid {
    gap: 8px;
  }

  .stat-value {
    font-size: 22px;
  }

  .ability-name {
    width: 50px;
    font-size: 11px;
  }

  .profile-page {
    padding: 12px 10px 80px;
  }
}
</style>
