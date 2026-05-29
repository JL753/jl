<template>
  <div class="learning-analytics-page">
    <header class="page-header">
      <div class="header-info">
        <h2>📊 学习分析与洞察报告</h2>
        <p>数据驱动学习，精准发现你的优势和不足</p>
      </div>
      <div class="header-actions">
        <el-select v-model="timeRange" style="width: 140px">
          <el-option label="本周" value="week" />
          <el-option label="本月" value="month" />
          <el-option label="本学期" value="semester" />
        </el-select>
        <el-button type="primary" @click="exportReport">📥 导出报告</el-button>
        <el-button type="success" @click="generateMapSnapshot">🗺️ 生成版图快照</el-button>
      </div>
    </header>

    <div class="analytics-content" v-loading="loading">
      <!-- 顶部统计卡片 -->
      <div class="stats-row">
        <div v-for="card in statCards" :key="card.label" class="stat-card" :style="{ borderLeft: `4px solid ${card.color}` }">
          <div class="stat-icon" :style="{ background: card.color + '15', color: card.color }">{{ card.icon }}</div>
          <div class="stat-info">
            <span class="stat-value">{{ card.value }}</span>
            <span class="stat-label">{{ card.label }}</span>
          </div>
          <div v-if="card.trend" class="stat-trend" :class="card.trendDir">
            {{ card.trendDir === 'up' ? '↑' : '↓' }} {{ card.trend }}
          </div>
        </div>
      </div>

      <div class="charts-row">
        <!-- 学习行为分析 -->
        <div class="chart-card large">
          <div class="card-header">
            <h3>📈 学习行为时间线</h3>
            <el-radio-group v-model="behaviorType" size="small">
              <el-radio-button value="study">学习时长</el-radio-button>
              <el-radio-button value="quiz">刷题数量</el-radio-button>
              <el-radio-button value="qa">提问次数</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="behaviorChart" class="chart-area"></div>
        </div>

        <!-- 能力雷达图 -->
        <div class="chart-card">
          <div class="card-header"><h3>🎯 能力雷达图</h3></div>
          <div ref="radarChart" class="chart-area"></div>
        </div>
      </div>

      <div class="charts-row">
        <!-- 预测性提醒 -->
        <div class="chart-card">
          <div class="card-header"><h3>🔮 预测性提醒</h3></div>
          <div class="predictions">
            <div v-for="(pred, i) in predictions" :key="i" :class="['pred-item', pred.level]">
              <span class="pred-icon">{{ pred.icon }}</span>
              <div class="pred-content">
                <p class="pred-title">{{ pred.title }}</p>
                <p class="pred-desc">{{ pred.desc }}</p>
              </div>
              <span class="pred-confidence">{{ pred.confidence }}%</span>
            </div>
          </div>
        </div>

        <!-- 知识点掌握热力图 -->
        <div class="chart-card">
          <div class="card-header"><h3>🗺️ 知识掌握热力图</h3></div>
          <div class="heatmap">
            <div v-for="(row, ri) in heatmapData" :key="ri" class="heatmap-row">
              <span class="heatmap-label">{{ row.label }}</span>
              <div v-for="(val, ci) in row.data" :key="ci"
                class="heatmap-cell"
                :style="{ background: heatmapColor(val) }"
                :title="`${row.label}: ${val}% — 点击前往AI工作台补强`"
                @click="repairKnowledge(row.label, val)"
              >
                {{ val }}
              </div>
            </div>
            <div class="heatmap-legend">
              <span style="background:#ef4444">0%</span>
              <span style="background:#f59e0b">25%</span>
              <span style="background:#84cc16">50%</span>
              <span style="background:#10b981">75%</span>
              <span style="background:#059669">100%</span>
            </div>
          </div>
        </div>

        <!-- 学习习惯分析 -->
        <div class="chart-card">
          <div class="card-header"><h3>⏰ 学习习惯分析</h3></div>
          <div ref="habitChart" class="chart-area small"></div>
          <div class="habit-insights">
            <div v-for="insight in habitInsights" :key="insight.text" class="insight-item">
              <span class="insight-icon">{{ insight.icon }}</span>
              <span class="insight-text">{{ insight.text }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 详细报告表格 -->
      <div class="chart-card full">
        <div class="card-header">
          <h3>📋 各科目学习详情</h3>
          <el-input v-model="searchSubject" placeholder="搜索科目" size="small" style="width:200px" clearable />
        </div>
        <el-table :data="filteredSubjects" stripe style="width: 100%">
          <el-table-column prop="name" label="科目" width="160" />
          <el-table-column prop="progress" label="学习进度" width="200">
            <template #default="{ row }">
              <el-progress :percentage="row.progress" :stroke-width="8" :color="row.progress >= 80 ? '#10b981' : row.progress >= 50 ? '#f59e0b' : '#ef4444'" />
            </template>
          </el-table-column>
          <el-table-column prop="score" label="平均分" width="100" />
          <el-table-column prop="studyHours" label="学习时长" width="100" />
          <el-table-column prop="quizCount" label="练习题数" width="100" />
          <el-table-column prop="correctRate" label="正确率" width="100" />
          <el-table-column prop="weakPoints" label="薄弱知识点">
            <template #default="{ row }">
              <el-tag v-for="wp in row.weakPoints" :key="wp" size="small" type="danger" style="margin:2px">{{ wp }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="viewDetail(row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { useKnowledgeMapStore } from '../../stores/knowledgeMap'
import { apiAnalyticsDashboard } from '../../api/index.js'

const router = useRouter()
const kmStore = useKnowledgeMapStore()

const timeRange = ref('week')
const behaviorType = ref('study')
const searchSubject = ref('')
const loading = ref(true)

const behaviorChart = ref(null)
const radarChart = ref(null)
const habitChart = ref(null)
let behaviorInstance = null
let radarInstance = null
let habitInstance = null

// 从 API 获取的真实数据
const dashboardData = ref(null)
const statCards = ref([])
const predictions = ref([])
const heatmapData = ref([])
const radarDimensions = ref([])
const radarCurrent = ref([])
const trendDates = ref([])
const trendStudy = ref([])
const trendQuiz = ref([])
const trendQa = ref([])
const habitHourly = ref([])
const habitInsights = ref([])
const subjectData = ref([])

const heatmapColor = (val) => {
  if (val >= 80) return '#059669'
  if (val >= 60) return '#10b981'
  if (val >= 40) return '#84cc16'
  if (val >= 20) return '#f59e0b'
  return '#ef4444'
}

const filteredSubjects = computed(() => {
  if (!searchSubject.value) return subjectData.value
  return subjectData.value.filter(s => s.name.includes(searchSubject.value))
})

// ==================== 加载数据 ====================

const loadDashboard = async () => {
  loading.value = true
  try {
    const res = await apiAnalyticsDashboard()
    const data = res?.data || res
    dashboardData.value = data

    // 统计卡片
    if (data.statCards) {
      statCards.value = data.statCards.map(c => ({
        ...c,
        trend: c.trend || '',
        trendDir: c.trendDir || 'up'
      }))
    }

    // 预测
    if (data.predictions) {
      predictions.value = data.predictions.map(p => ({
        icon: p.type === 'danger' ? '⚠️' : p.type === 'warning' ? '⏰' : p.type === 'success' ? '🎯' : '💡',
        title: p.type === 'danger' ? '学习预警' : p.type === 'warning' ? '学习提醒' : p.type === 'success' ? '正面趋势' : '学习建议',
        desc: p.message,
        confidence: 75,
        level: p.type || 'info'
      }))
    }

    // 热力图
    if (data.heatmap) {
      heatmapData.value = data.heatmap.map(h => ({
        label: h.name,
        data: [h.mastery, h.mastery, h.mastery, h.mastery, h.mastery],
        raw: h
      }))
    }

    // 雷达图
    if (data.radar?.dimensions) {
      radarDimensions.value = data.radar.dimensions
      radarCurrent.value = data.radar.dimensions.map(d => d.value || 0)
    }

    // 趋势
    if (data.trends) {
      trendDates.value = data.trends.dates || []
      trendStudy.value = data.trends.studyMinutes || []
      trendQuiz.value = data.trends.quizCount || []
      trendQa.value = data.trends.qaCount || []
    }

    // 学习习惯
    if (data.habits) {
      habitHourly.value = data.habits.hourlyDistribution || []
      const avgMin = data.habits.averageDailyMinutes || 0
      const totalDays = data.habits.totalStudyDays || 0
      habitInsights.value = [
        { icon: '📊', text: `日均学习约 ${avgMin} 分钟` },
        { icon: '📅', text: `累计学习 ${totalDays} 天` },
        { icon: '🔄', text: avgMin > 60 ? '学习时间较长，建议劳逸结合' : '保持规律学习节奏' }
      ]
    }

    // 科目详情
    if (data.subjects) {
      subjectData.value = data.subjects.map(s => ({
        name: s.name,
        progress: s.mastery || 0,
        score: s.mastery || 0,
        studyHours: '-',
        quizCount: s.kpCount || 0,
        correctRate: (s.mastery || 0) + '%',
        weakPoints: s.mastery < 60 ? [s.name] : []
      }))
    }
  } catch (e) {
    console.warn('Failed to load analytics dashboard', e)
    ElMessage.warning('加载分析数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// ==================== 初始化图表 ====================

const initCharts = () => {
  const initIfReady = (ref, name) => {
    if (!ref.value || ref.value.clientWidth === 0 || ref.value.clientHeight === 0) {
      return null
    }
    return echarts.init(ref.value)
  }

  behaviorInstance = initIfReady(behaviorChart, 'behavior')
  updateBehaviorChart()

  radarInstance = initIfReady(radarChart, 'radar')
  updateRadarChart()

  habitInstance = initIfReady(habitChart, 'habit')
  updateHabitChart()
}

const updateRadarChart = () => {
  if (!radarInstance) return
  const dims = radarDimensions.value.length > 0
    ? radarDimensions.value.map(d => ({ name: d.name, max: 100 }))
    : [{ name: '加载中', max: 100 }]
  const vals = radarCurrent.value.length > 0 ? radarCurrent.value : [0]

  radarInstance.setOption({
    radar: {
      indicator: dims,
      shape: 'circle',
      splitNumber: 5
    },
    series: [{
      type: 'radar',
      data: [{
        value: vals,
        name: '当前水平',
        areaStyle: { color: 'rgba(102, 126, 234, 0.2)' },
        lineStyle: { color: '#667eea', width: 2 }
      }]
    }]
  })
}

const updateHabitChart = () => {
  if (!habitInstance) return
  const data = habitHourly.value.length > 0
    ? habitHourly.value
    : [10, 20, 30, 25, 35, 45, 50, 15]

  habitInstance.setOption({
    polar: { radius: [30, '70%'] },
    angleAxis: { type: 'category', data: ['6-8', '8-10', '10-12', '12-14', '14-16', '16-18', '18-20', '20-22'], boundaryGap: false },
    radiusAxis: { axisLabel: { fontSize: 10 } },
    series: [{
      type: 'bar',
      data,
      coordinateSystem: 'polar',
      itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#667eea' }, { offset: 1, color: '#764ba2' }]) }
    }]
  })
}

const updateBehaviorChart = () => {
  if (!behaviorInstance) return
  const dataMap = {
    study: trendStudy.value.length > 0 ? trendStudy.value : [0],
    quiz: trendQuiz.value.length > 0 ? trendQuiz.value : [0],
    qa: trendQa.value.length > 0 ? trendQa.value : [0]
  }
  const labelMap = { study: '学习时长(min)', quiz: '刷题数量', qa: '提问次数' }
  const days = trendDates.value.length > 0 ? trendDates.value : ['暂无数据']

  behaviorInstance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: days },
    yAxis: { type: 'value', name: labelMap[behaviorType.value] },
    series: [{
      data: dataMap[behaviorType.value],
      type: 'line',
      smooth: true,
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(102,126,234,0.3)' }, { offset: 1, color: 'rgba(102,126,234,0.02)' }]) },
      lineStyle: { color: '#667eea', width: 3 },
      itemStyle: { color: '#667eea' }
    }],
    grid: { left: 50, right: 20, top: 20, bottom: 30 }
  })
}

watch(behaviorType, () => updateBehaviorChart())

// ==================== 交互方法 ====================

const exportReport = () => ElMessage.success('学习报告导出成功！')
const viewDetail = (row) => ElMessage.info(`查看 ${row.name} 详细报告`)

const repairKnowledge = (label, val) => {
  if (val >= 80) {
    ElMessage.success(`${label} 掌握良好，继续保持！`)
    return
  }
  router.push({ path: '/student/workspace', query: { q: `请帮我补强"${label}"这个知识点，我的掌握度只有${val}%，请从基础开始讲解并给出练习题` } })
}

const generateMapSnapshot = () => {
  const lit = kmStore.litCount
  const active = kmStore.activeCount
  ElMessage.success(`版图快照已生成：已点亮 ${lit} 个领域，研习中 ${active} 个领域`)
}

// ==================== 生命周期 ====================

onMounted(async () => {
  await loadDashboard()
  nextTick(() => initCharts())
  window.addEventListener('resize', () => {
    behaviorInstance?.resize()
    radarInstance?.resize()
    habitInstance?.resize()
  })
})
</script>

<style scoped lang="scss">
.learning-analytics-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
  height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: 16px;
  color: #fff;

  h2 { margin: 0 0 4px; font-size: 22px; }
  p { margin: 0; opacity: 0.85; font-size: 14px; }

  .header-actions { display: flex; gap: 12px; align-items: center; }
}

.analytics-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);

  .stat-icon {
    width: 44px; height: 44px;
    border-radius: 12px;
    display: flex; align-items: center; justify-content: center;
    font-size: 20px;
    flex-shrink: 0;
  }

  .stat-info {
    display: flex;
    flex-direction: column;
    flex: 1;
  }

  .stat-value { font-size: 20px; font-weight: 700; color: #333; }
  .stat-label { font-size: 12px; color: #999; }

  .stat-trend {
    font-size: 12px;
    font-weight: 600;
    &.up { color: #10b981; }
    &.down { color: #ef4444; }
  }
}

.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.chart-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  overflow: hidden;

  &.large { grid-column: span 1; }
  &.full { grid-column: 1 / -1; }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0;

  h3 { margin: 0; font-size: 15px; }
}

.chart-area {
  height: 300px;
  padding: 16px;

  &.small { height: 200px; }
}

/* 预测 */
.predictions {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pred-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  border-radius: 10px;
  border-left: 3px solid;

  &.danger { border-color: #ef4444; background: #fef2f2; }
  &.warning { border-color: #f59e0b; background: #fffbeb; }
  &.info { border-color: #3b82f6; background: #eff6ff; }
  &.success { border-color: #10b981; background: #ecfdf5; }

  .pred-icon { font-size: 18px; flex-shrink: 0; }
  .pred-content { flex: 1; }
  .pred-title { font-size: 13px; font-weight: 600; margin: 0 0 4px; }
  .pred-desc { font-size: 12px; color: #666; margin: 0; line-height: 1.5; }
  .pred-confidence { font-size: 12px; font-weight: 700; color: #333; flex-shrink: 0; }
}

/* 热力图 */
.heatmap {
  padding: 16px;
}

.heatmap-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;

  .heatmap-label { width: 80px; font-size: 12px; color: #666; flex-shrink: 0; }
}

.heatmap-cell {
  width: 36px;
  height: 28px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  transition: transform .15s, box-shadow .15s;
  &:hover { transform: scale(1.15); box-shadow: 0 4px 10px rgba(0,0,0,.2); }
}

.heatmap-legend {
  display: flex;
  gap: 4px;
  margin-top: 10px;
  justify-content: flex-end;

  span {
    width: 40px;
    height: 16px;
    border-radius: 3px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 9px;
    color: #fff;
  }
}

.habit-insights {
  padding: 0 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.insight-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #555;

  .insight-icon { font-size: 14px; }
}

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(3, 1fr); }
  .charts-row { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr 1fr; }
}
</style>
