<template>
  <div class="teacher-dashboard-page">
    <!-- Top Stats Strip -->
    <section class="stats-strip">
      <div v-for="(item, idx) in topCards" :key="idx" :class="['stat-card', { highlight: idx === topCards.length - 1 }]">
        <strong>{{ item.value }}</strong>
        <label>{{ item.label }}</label>
      </div>
    </section>

    <!-- Main Content Grid -->
    <div class="main-content-grid">
      <!-- Left: Bar Chart + Radar + Pie -->
      <div class="left-col">
        <!-- 学生知识掌握进度 (Bar Chart) -->
        <div class="panel chart-panel">
          <div class="panel-head">
            <h4>学生知识掌握进度</h4>
            <p>对比知识点掌握率、学习进度、作业完成率和辅导时长</p>
          </div>
          <div ref="barRef" class="chart-box"></div>
        </div>

        <!-- Bottom Row: Resource Usage Radar + Time Ratio Pie -->
        <div class="bottom-row">
          <div class="panel chart-panel half">
            <div class="panel-head"><h4>资源使用情况</h4></div>
            <div ref="radarRef" class="chart-box sm"></div>
          </div>
          <div class="panel chart-panel half">
            <div class="panel-head"><h4>AI拟/传备课耗时</h4></div>
            <div ref="pieRef" class="chart-box sm"></div>
            <div class="time-ratio-text">时间对比<br/><span class="ratio-num">4.76 : 1</span></div>
          </div>
        </div>
      </div>

      <!-- Right: Calendar + AI Line Chart + Duration Trend -->
      <div class="right-col">
        <!-- Calendar -->
        <div class="panel cal-panel">
          <div class="cal-header">
            <button class="cal-nav" @click="prevMonth">&lt;</button>
            <span class="cal-month">{{ currentMonth }}</span>
            <button class="cal-nav" @click="nextMonth">&gt;</button>
          </div>
          <div class="cal-body">
            <div class="cal-weekdays"><span v-for="d in ['日','一','二','三','四','五','六']" :key="d">{{ d }}</span></div>
            <div class="cal-days">
              <span v-for="(day, idx) in calendarDays" :key="idx"
                    :class="{ 'other-month': day.other, active: day.active }"
                    @click="selectDay(day)">
                {{ day.num }}
              </span>
            </div>
          </div>
        </div>

        <!-- AI辅导频次 Line Chart -->
        <div class="panel chart-panel">
          <div class="panel-head"><h4>AI辅导频次（次数）</h4></div>
        </div>
        <div ref="lineRef" class="chart-box sm"></div>

        <!-- AI备课日趋势时长 Trend -->
        <div class="panel chart-panel">
          <div class="panel-head"><h4>AI备课日趋势时长</h4></div>
        </div>
        <div ref="trendRef" class="chart-box sm"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { computed, nextTick, onMounted, ref } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { apiTeacherDashboard, apiTeacherWorkspace } from '../../api'

const auth = useAuthStore()
const barRef = ref(), lineRef = ref(), pieRef = ref(), radarRef = ref(), trendRef = ref()
const data = ref({ summary: {}, bar: [], notices: [] })
const workspace = ref({ pendingTasks: [] })
const currentMonthIdx = ref(new Date().getMonth())
const currentYear = ref(new Date().getFullYear())

const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
const currentMonth = computed(() => `${currentYear.value}年 ${months[currentMonthIdx.value]}`)

// Generate calendar days
const calendarDays = computed(() => {
  const year = currentYear.value
  const month = currentMonthIdx.value
  const firstDay = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const prevDays = new Date(year, month, 0).getDate()
  const days = []
  for (let i = firstDay - 1; i >= 0; i--) {
    days.push({ num: prevDays - i, other: true })
  }
  const today = new Date().getDate()
  const isCurrentMonth = year === new Date().getFullYear() && month === new Date().getMonth()
  for (let d = 1; d <= daysInMonth; d++) {
    days.push({ num: d, other: false, active: isCurrentMonth && d === today })
  }
  return days
})

const prevMonth = () => {
  if (currentMonthIdx.value === 0) {
    currentMonthIdx.value = 11
    currentYear.value--
  } else {
    currentMonthIdx.value--
  }
}
const nextMonth = () => {
  if (currentMonthIdx.value === 11) {
    currentMonthIdx.value = 0
    currentYear.value++
  } else {
    currentMonthIdx.value++
  }
}
const selectDay = (day) => {
  if (!day.other) {
    // Could trigger date-specific data load
  }
}

const topCards = computed(() => [
  { label: '参与的会议', value: data.value.summary.todayCourses ?? 2 },
  { label: '今日上课数', value: data.value.summary.todayClasses ?? 3 },
  { label: '辅导学生', value: data.value.summary.students ?? 60 },
  { label: '作业提交人数', value: data.value.summary.homeworkRate ?? 45 },
  { label: '学生参与度', value: data.value.summary.participation ?? 60 },
  { label: '学生出勤情况', value: data.value.summary.attendance ?? 58 },
  { label: '累积时长', value: data.value.summary.totalHours ?? 226, highlight: true }
])

const axisStyle = {
  axisLine: { lineStyle: { color: '#d9e3f0' } },
  axisLabel: { color: '#7588a3', fontSize: 11 },
  splitLine: { lineStyle: { color: '#edf2f8' } }
}

const renderCharts = () => {
  // Bar chart: 学生知识掌握进度 (Grouped bar)
  echarts.init(barRef.value).setOption({
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e2e8f0', textStyle: { color: '#334155', fontSize: 12 }, extraCssText: 'box-shadow: 0 4px 12px rgba(0,0,0,.1)' },
    legend: { data: ['知识点掌握情况', '学习进度', '作业完成率', '在线答疑课时'], textStyle: { color: '#7b8da5', fontSize: 11 }, bottom: 0, itemWidth: 14, itemHeight: 10 },
    grid: { left: 48, right: 18, top: 40, bottom: 36 },
    xAxis: { type: 'category', data: data.value.bar.map(i => i.name) || ['青岩班','安顺班','南明班','云岩区','花溪区','观山湖','南明区','清镇市'], ...axisStyle },
    yAxis: [{ type: 'value', name: '(分)', min: 0, max: 100, ...axisStyle }, { type: 'value', name: '(小时)', ...axisStyle }],
    series: [
      { name: '知识点掌握情况', type: 'bar', stack: 'a', data: [28,35,22,48,62,38,55,42], itemStyle: { color: '#f0c35a', borderRadius: [3,3,0,0] }, barMaxWidth: 20 },
      { name: '学习进度', type: 'bar', stack: 'a', data: [30,25,38,12,18,32,15,28], itemStyle: { color: '#5ec46d', borderRadius: [3,3,0,0] }, barMaxWidth: 20 },
      { name: '作业完成率', type: 'bar', stack: 'a', data: [24,26,28,16,22,20,27,19], itemStyle: { color: '#5b92cc', borderRadius: [3,3,0,0] }, barMaxWidth: 20 },
      { name: '在线答疑课时', type: 'line', yAxisIndex: 1, data: [23,22,35,32,18,40,28,34], lineStyle: { color: '#8d90ad', width: 2 }, smooth: true, symbol: 'circle', symbolSize: 5 }
    ]
  })

  // Line chart: AI辅导频次
  echarts.init(lineRef.value).setOption({
    grid: { left: 36, right: 10, top: 10, bottom: 24 },
    xAxis: { type: 'category', data: ['1/4','1/5','1/6','1/7','1/8','1/9','1/10'], ...axisStyle, show: false },
    yAxis: { type: 'value', ...axisStyle, show: false, min: 6, max: 17 },
    series: [{
      type: 'line', smooth: true, data: [13,15,10,9,11,10,16],
      lineStyle: { color: '#e5bf69', width: 2.5 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(229,191,105,.25)' }, { offset: 1, color: 'rgba(229,191,105,.02)' }]) },
      itemStyle: { color: '#e5bf69' },
      symbolSize: 5
    }]
  })

  // Trend chart: AI备课日趋势时长
  echarts.init(trendRef.value).setOption({
    grid: { left: 36, right: 10, top: 10, bottom: 24 },
    xAxis: { type: 'category', data: ['04-04','04-05','04-06','04-07','04-08','04-09','04-10','04-11','04-12','04-13','04-14'], ...axisStyle, show: false, axisLabel: { rotate: 45 } },
    yAxis: { type: 'value', name: '', ...axisStyle, show: true, min: 0, max: 120, axisLabel: { formatter: '{value} 分钟' } },
    series: [{
      type: 'line', smooth: true, data: [15,28,45,72,95,110,85,60,42,30,50],
      lineStyle: { color: '#f59e0b', width: 2.5 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(245,158,11,.25)' }, { offset: 1, color: 'rgba(245,158,11,.02)' }]) },
      itemStyle: { color: '#f59e0b' },
      symbolSize: 5,
      markPoint: {
        data: [
          { type: 'max', name: '最高', label: { formatter: '{b}\n{c}分钟', fontSize: 10 } }
        ],
        symbolSize: 40,
        itemStyle: { color: '#fff', borderColor: '#f59e0b', borderWidth: 2 }
      }
    }]
  })

  // Radar: 资源使用情况
  echarts.init(radarRef.value).setOption({
    radar: {
      indicator: [
        { name: 'ppt生成', max: 100 }, { name: 'word生成', max: 100 },
        { name: '试题生成', max: 100 }, { name: '图片生成', max: 100 },
        { name: '视频生成', max: 100 }
      ],
      radius: 58,
      shape: 'polygon',
      axisName: { color: '#7b8da5', fontSize: 11 },
      splitLine: { lineStyle: { color: '#edf2f8' } },
      splitArea: { areaStyle: { color: ['#fffdf4','#fff'] } }
    },
    series: [{ type: 'radar', data: [{ value: [74,62,58,55,70], areaStyle: { color: 'rgba(242,194,85,.2)' }, lineStyle: { color: '#e2bb57', width: 2 } }] }]
  })

  // Pie: 时间占比
  echarts.init(pieRef.value).setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {d}%', show: false },
    series: [{
      type: 'pie', radius: ['52%', '78%'],
      center: ['50%', '50%'],
      startAngle: 90,
      label: { show: false },
      labelLine: { show: false },
      silent: true,
      data: [
        { value: 82.6, itemStyle: { color: '#90de8f' } },
        { value: 17.4, itemStyle: { color: '#93c5fd' } }
      ]
    }]
  })
}

onMounted(async () => {
  try {
    const [dashRes, wsRes] = await Promise.all([apiTeacherDashboard(), apiTeacherWorkspace()])
    data.value = dashRes.data || {}
    workspace.value = wsRes.data || {}
  } catch (e) {
    console.warn('Dashboard API unavailable')
  }
  await nextTick()
  renderCharts()

  window.addEventListener('resize', () => {
    ;[barRef, lineRef, pieRef, radarRef, trendRef].forEach(ref => {
      if (ref.value) {
        const inst = echarts.getInstanceByDom(ref.value)
        if (inst) inst.resize()
      }
    })
  })
})
</script>

<style scoped>
.teacher-dashboard-page { display: grid; gap: 14px; }

/* ========== Top Stats Strip ========== */
.stats-strip {
  display: flex;
  gap: 0;
  background: white;
  border-radius: 14px;
  box-shadow: 0 2px 10px rgba(33,65,108,.05);
  overflow: hidden;
}
.stat-card {
  flex: 1;
  padding: 14px 8px;
  text-align: center;
  border-right: 1px solid #edf1f6;
  display: grid;
  gap: 4px;
  cursor: default;
  transition: all .2s ease;
}
.stat-card:last-child { border-right: none; }
.stat-card:hover { background: #fafbff; transform: translateY(-2px); }
.stat-card.highlight { background: linear-gradient(135deg, #fff7ed, #ffedd5); }
.stat-card.highlight strong { color: #ea580c !important; }
.stat-card strong {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}
.stat-card label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

/* ========== Main Grid ========== */
.main-content-grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 14px;
}
.left-col, .right-col { display: grid; gap: 14px; align-content: start; }

/* Panels */
.panel {
  background: white;
  border-radius: 14px;
  box-shadow: 0 2px 10px rgba(33,65,108,.05);
  padding: 16px;
  transition: transform .25s ease, box-shadow .25s ease;
}
.panel:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(33,68,120,.08); }

.chart-panel { padding: 14px 16px; }
.panel-head h4 { margin: 0 0 4px; font-size: 14px; color: #1e293b; font-weight: 600; }
.panel-head p { margin: 0; font-size: 12px; color: #94a3b8; }
.chart-box { width: 100%; min-height: 240px; }
.chart-box.sm { min-height: 160px; }

.bottom-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.half { position: relative; overflow: hidden; }
.time-ratio-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  pointer-events: none;
  z-index: 2;
  font-size: 12px;
  color: #64748b;
}
.ratio-num {
  display: block;
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
  margin-top: 2px;
}

/* Calendar */
.cal-panel { padding: 12px 16px; }
.cal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.cal-month { font-size: 14px; font-weight: 600; color: #1e293b; }
.cal-nav {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
  color: #64748b;
  display: grid;
  place-items: center;
  transition: all .2s;
}
.cal-nav:hover { background: #f1f5f9; border-color: #cbd5e1; }

.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
  margin-bottom: 4px;
  text-align: center;
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
}
.cal-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 3px;
}
.cal-days span {
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  cursor: pointer;
  transition: all .15s;
}
.cal-days span:hover:not(.other-month) { background: #eff6ff; color: #2563eb; }
.cal-days span.other-month { opacity: 0.3; }
.cal-days span.active {
  background: #2563eb;
  color: white;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(37,99,235,.3);
}

@media (max-width: 1200px) {
  .main-content-grid { grid-template-columns: 1fr; }
  .stats-strip { flex-wrap: wrap; }
  .stat-card { min-width: calc(14.28% - 1px); }
  .bottom-row { grid-template-columns: 1fr; }
}
@media (max-width: 900px) {
  .stats-strip { flex-wrap: wrap; }
  .stat-card { flex: 1 1 calc(33% - 1px); min-width: auto; border-bottom: 1px solid #edf1f6; border-right: none; }
}
@media (max-width: 600px) {
  .stat-card { flex: 1 1 50%; }
}
</style>
