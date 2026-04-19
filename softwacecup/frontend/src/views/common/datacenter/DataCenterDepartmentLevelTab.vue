<template>
  <div class="datacenter-shell">
    <div class="screen-header fade-up delay-1">
      <div class="screen-title">智备优教数据中台</div>
      <div class="screen-actions">
        <div class="search-pill"><input placeholder="个人信息查询" /></div>
        <div class="switch-btn" @click="$emit('toggle-chat')">切换页面</div>
      </div>
    </div>

    <div class="dept-top-row fade-up delay-2">
      <div class="dark-panel major-selector lift-card">
        <h3>▶ 计算机与软件工程学院 ◀</h3>
        <select v-model="selectedMajor" class="major-select">
          <option value="csse">2023级</option>
          <option value="ai">人工智能</option>
          <option value="se">软件工程</option>
        </select>
        <nav class="major-nav">
          <a :class="{ active: deptSubPage === 'overview' }" @click="deptSubPage = 'overview'">📊 区块链工程</a>
          <a :class="{ active: deptSubPage === 'sw' }" @click="deptSubPage = 'sw'">🔧 软件工程</a>
          <a :class="{ active: deptSubPage === 'ds' }" @click="deptSubPage = 'ds'">💾 数据科学与大数据技术</a>
          <a :class="{ active: deptSubPage === 'cs' }" @click="deptSubPage = 'cs'">🖥️ 计算机科学与技术</a>
        </nav>
      </div>

      <div class="dark-panel dept-stats lift-card">
        <div class="stat-grid-3col">
          <div v-for="s in deptLevelStats" :key="s.label" class="d-stat"><b>{{ s.label }}</b><strong>{{ s.value }}</strong></div>
        </div>
        <div ref="deptRadarRef" class="chart-inline sm"></div>
      </div>

      <div class="dark-panel class-rates lift-card">
        <h4>各班通过率《</h4>
        <div class="rate-cards">
          <div v-for="(r, i) in classRates" :key="i" class="rate-item">
            <span class="rate-label">{{ r.name }}</span>
            <div class="rate-bar-wrap"><div class="rate-bar" :style="{ width: r.rate + '%' }"></div></div>
            <strong>{{ r.rate }}%</strong>
          </div>
        </div>
      </div>
    </div>

    <div class="dept-bottom-row fade-up">
      <div class="dark-panel chart lift-card half" ref="deptAvgRef">
        <h4 class="chart-title">《两学期班级平均评分》</h4>
      </div>
      <div class="dark-panel chart lift-chart half" ref="deptTrendRef">
        <h4 class="chart-title">《继续深造意向》</h4>
      </div>
    </div>
  </div>
</template>

<script setup>
import './datacenter-shared.css'
import echarts from '@/utils/echarts'
import { debounce } from '@/utils/debounce'
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

defineProps({
  deptLevelStats: { type: Array, default: () => [] },
  classRates: { type: Array, default: () => [] }
})

const selectedMajor = ref('csse')
const deptSubPage = ref('overview')
const deptRadarRef = ref()
const deptAvgRef = ref()
const deptTrendRef = ref()

const chartRefs = [deptRadarRef, deptAvgRef, deptTrendRef]
const axisStyleDark = {
  axisLine: { lineStyle: { color: 'rgba(156,211,255,.25)' } },
  axisLabel: { color: '#9cccff', fontSize: 11 },
  splitLine: { lineStyle: { color: 'rgba(156,211,255,.1)' } }
}

const resizeCharts = debounce(() => {
  chartRefs.forEach((chartRef) => {
    if (!chartRef.value) return
    echarts.getInstanceByDom(chartRef.value)?.resize()
  })
}, 120)

function renderCharts() {
  const deptRadar = echarts.getInstanceByDom(deptRadarRef.value) || echarts.init(deptRadarRef.value)
  deptRadar.setOption({
    radar: {
      indicator: [{ name: '科研产出', max: 100 }, { name: '学科评价', max: 100 }, { name: '社会服务', max: 100 }, { name: '国际合作', max: 100 }, { name: '社会责任', max: 100 }],
      radius: 62, shape: 'polygon', axisName: { color: '#bfe0ff', fontSize: 11 },
      splitArea: { areaStyle: { color: ['rgba(9,19,38,.32)', 'rgba(9,19,38,.16)'] } },
      splitLine: { lineStyle: { color: 'rgba(156,211,255,.12)' } }
    },
    series: [{ type: 'radar', data: [{ value: [82,75,68,71,79], areaStyle: { color: 'rgba(99,214,255,.15)' }, lineStyle: { color: '#63d6ff', width: 2 }, symbolSize: 5, itemStyle: { color: '#63d6ff', borderColor: '#fff', borderWidth: 1 } }] }]
  })

  const deptAvg = echarts.getInstanceByDom(deptAvgRef.value) || echarts.init(deptAvgRef.value)
  deptAvg.setOption({
    grid: { left: 36, right: 16, top: 36, bottom: 24 },
    xAxis: { type: 'category', data: ['一班','二班','三班'], ...axisStyleDark },
    yAxis: { type: 'value', min: 0, max: 5, ...axisStyleDark },
    series: [
      { name: '上学期', type: 'bar', data: [3.2,4.0,3.6], itemStyle: { color: '#60b4ff', borderRadius: [4,4,0,0] } },
      { name: '本学期', type: 'bar', data: [3.8,4.5,4.2], itemStyle: { color: '#7fe1c7', borderRadius: [4,4,0,0] } }
    ]
  })

  const deptTrend = echarts.getInstanceByDom(deptTrendRef.value) || echarts.init(deptTrendRef.value)
  deptTrend.setOption({
    radar: {
      indicator: [{ name: '三班', max: 5 }, { name: '二班', max: 5 }, { name: '一班', max: 5 }],
      radius: 56, shape: 'polygon', axisName: { color: '#9cd3ff', fontSize: 11 },
      splitArea: { areaStyle: { color: ['rgba(9,19,38,.3)','rgba(9,19,38,.15)'] } }
    },
    series: [{ type: 'radar', data: [{ value: [4.2,4.5,3.8], areaStyle: { color: 'rgba(99,214,255,.2)' }, lineStyle: { color: '#63d6ff', width: 2 } }], symbol: 'circle', symbolSize: 5 }]
  })
}

onMounted(async () => {
  await nextTick()
  renderCharts()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  chartRefs.forEach((chartRef) => {
    if (!chartRef.value) return
    echarts.getInstanceByDom(chartRef.value)?.dispose()
  })
})
</script>

<style scoped>
.dept-top-row { display: grid; grid-template-columns: 240px 1fr 260px; gap: 10px; margin-bottom: 10px; }
.major-selector { padding: 14px; }
.major-selector h3 { margin: 0 0 10px; color: #9cd3ff; font-size: 14px; }
.major-select { width: 100%; background: rgba(12,26,52,.6); border: 1px solid rgba(110,174,255,.2); color: #dcecff; border-radius: 6px; padding: 6px 10px; outline: none; }
.major-nav { display: grid; gap: 4px; margin-top: 10px; }
.major-nav a { padding: 8px 12px; border-radius: 8px; color: #9cd3ff; text-decoration: none; font-size: 12.5px; transition: all .15s; background: rgba(20,40,80,.4); }
.major-nav a:hover, .major-nav a.active { background: rgba(37,99,235,.2); color: #8be9ff; }
.dept-stats { padding: 14px; }
.stat-grid-3col { display: grid; grid-template-columns: repeat(4,1fr); gap: 8px; margin-bottom: 12px; }
.d-stat { padding: 8px; text-align: center; background: rgba(20,40,80,.4); border-radius: 8px; }
.d-stat b { display: block; color: #8fb8ff; font-size: 11.5px; }
.d-stat strong { display: block; font-size: 22px; color: #8be9ff; font-weight: 700; }
.chart-inline { min-height: 160px; margin-top: 10px; }
.chart-inline.sm { min-height: 140px; }
.class-rates { padding: 12px; }
.class-rates h4 { margin: 0 0 10px; color: #9cd3ff; font-size: 13px; }
.rate-item { display: grid; grid-template-columns: auto 1fr auto; gap: 8px; align-items: center; margin-bottom: 6px; }
.rate-label { color: #9cccff; font-size: 12px; }
.rate-bar-wrap { height: 8px; background: rgba(20,40,80,.4); border-radius: 4px; overflow: hidden; }
.rate-bar { height: 100%; background: #38bdf8; border-radius: 4px; transition: width .5s; }
.class-rates strong { color: #8be9ff; font-size: 14px; font-weight: 700; min-width: 36px; text-align: right; }
.dept-bottom-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.half { min-height: 220px; }
.chart-title { margin: 0 0 8px; color: #9cd3ff; font-size: 14px; }
@media (max-width: 1200px) { .dept-top-row, .dept-bottom-row { grid-template-columns: 1fr; } }
</style>
