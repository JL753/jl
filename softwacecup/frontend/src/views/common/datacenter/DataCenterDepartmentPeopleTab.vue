<template>
  <div class="datacenter-shell">
    <div class="screen-header fade-up delay-1">
      <div class="screen-title">智备优教数据中台</div>
      <div class="screen-actions">
        <div class="search-pill"><input placeholder="个人信息查询" /></div>
        <div class="switch-btn" @click="$emit('toggle-chat')">切换页面</div>
      </div>
    </div>

    <div class="people-top-row fade-up delay-2">
      <div class="dark-panel people-stats lift-card">
        <div class="stats-2row">
          <div class="ps-row">
            <span v-for="s in peopleTopStats.slice(0,5)" :key="s.label" class="ps-item"><b>{{ s.label }}</b><em>{{ s.value }}</em></span>
          </div>
          <div class="ps-row">
            <span v-for="s in peopleTopStats.slice(5)" :key="s.label" class="ps-item"><b>{{ s.label }}</b><em>{{ s.value }}</em></span>
          </div>
        </div>
        <div ref="peopleRadarRef" class="chart-inline sm"></div>
      </div>

      <div class="dark-panel warn-levels lift-card">
        <h4>学情预警</h4>
        <div v-for="(w,i) in warningLevels" :key="i" class="warn-level-item">
          <span :class="['wl-badge', w.level]">{{ w.levelLabel }}级预警：</span>
          <span class="wl-text">{{ w.text }}</span>
        </div>
      </div>

      <div class="dark-panel rank-table lift-card">
        <div class="rank-header">
          <b>任课教师</b><strong>{{ teacherCount }}名</strong>
          <b>班级委员</b><strong>{{ monitorCount }}名</strong>
          <b>社团指导教师</b><strong>{{ advisorCount }}名</strong>
        </div>
        <table class="rank-tbl">
          <thead><tr><th>学号</th><th>姓名</th><th>进度</th></tr></thead>
          <tbody>
            <tr v-for="r in studentRanking" :key="r.id">
              <td>{{ r.id }}</td>
              <td>{{ r.name }}</td>
              <td><div class="progress-cell"><div class="progress-bar" :style="{ width: r.progress + '%' }"></div><span>{{ r.progress }}%</span></div></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="people-bottom-row fade-up">
      <div class="dark-panel chart lift-card third" ref="peopleScoreTrendRef">
        <h4 class="chart-title-sm">《成员成绩对比》</h4>
      </div>
      <div class="dark-panel chart lift-chart third" ref="peopleCompletionRef">
        <h4 class="chart-title-sm">《站点完成情况》</h4>
      </div>
      <div class="dark-panel chart lift-chart third" ref="peopleExamRef">
        <h4 class="chart-title-sm">《测验完成情况》</h4>
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
  peopleTopStats: { type: Array, default: () => [] },
  warningLevels: { type: Array, default: () => [] },
  studentRanking: { type: Array, default: () => [] },
  teacherCount: { type: [String, Number], default: '0' },
  monitorCount: { type: [String, Number], default: '0' },
  advisorCount: { type: [String, Number], default: '0' }
})

const peopleRadarRef = ref()
const peopleScoreTrendRef = ref()
const peopleCompletionRef = ref()
const peopleExamRef = ref()

const chartRefs = [peopleRadarRef, peopleScoreTrendRef, peopleCompletionRef, peopleExamRef]
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
  const peopleRadar = echarts.getInstanceByDom(peopleRadarRef.value) || echarts.init(peopleRadarRef.value)
  peopleRadar.setOption({
    radar: {
      indicator: [{ name: '优优率', max: 100 }, { name: '及格率', max: 100 }, { name: '粘强度', max: 100 }, { name: '创新度', max: 100 }, { name: '活跃度', max: 100 }],
      radius: 58, shape: 'polygon', axisName: { color: '#bfe0ff', fontSize: 11 },
      splitArea: { areaStyle: { color: ['rgba(9,19,38,.3)','rgba(9,19,38,.15)'] } }
    },
    series: [{ type: 'radar', data: [{ value: [85,92,78,81,88], areaStyle: { color: 'rgba(99,214,255,.18)' }, lineStyle: { color: '#63d6ff', width: 2 } }] }]
  })

  const peopleScoreTrend = echarts.getInstanceByDom(peopleScoreTrendRef.value) || echarts.init(peopleScoreTrendRef.value)
  peopleScoreTrend.setOption({
    grid: { left: 36, right: 16, top: 30, bottom: 22 },
    xAxis: { type: 'category', data: Array.from({ length: 12 }, (_, i) => `第${i + 1}周`), ...axisStyleDark },
    yAxis: { type: 'value', min: 0, max: 5, ...axisStyleDark },
    series: [{ type: 'scatter', data: [[1,3.2],[2,3.5],[3,3.1],[4,3.8],[5,4.0],[6,3.9],[7,4.2],[8,4.1],[9,4.3],[10,4.4],[11,4.5],[12,4.6]], symbolSize: 8, itemStyle: { color: '#67e3ff' } }]
  })

  const peopleCompletion = echarts.getInstanceByDom(peopleCompletionRef.value) || echarts.init(peopleCompletionRef.value)
  peopleCompletion.setOption({
    polar: { radius: ['25%','72%'] },
    angleAxis: { data: ['第1章','第2章','第3章','第4章','第5章','第6章','第7章','第8章','第9章','第10章','第11章','第12章'], ...axisStyleDark },
    radiusAxis: { ...axisStyleDark, max: 100 },
    series: [{ type: 'bar', coordinateSystem: 'polar', data: [95,88,92,76,84,90,78,86,94,82,88,91], itemStyle: { color: '#ef4444', borderRadius: [4,4,0,0] } }]
  })

  const peopleExam = echarts.getInstanceByDom(peopleExamRef.value) || echarts.init(peopleExamRef.value)
  peopleExam.setOption({
    grid: { left: 36, right: 16, top: 30, bottom: 22 },
    xAxis: { type: 'category', data: ['第一次测验','第二次测验','第三次测验','第四次测验','第五次测验'], ...axisStyleDark },
    yAxis: { type: 'value', min: 0, max: 100, ...axisStyleDark },
    series: [{ type: 'bar', data: [85,78,92,88,95], itemStyle: { color: '#ef4444', borderRadius: [4,4,0,0] } }]
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
.people-top-row { display: grid; grid-template-columns: 320px 340px 360px; gap: 10px; margin-bottom: 10px; }
.people-stats { padding: 14px; }
.stats-2row { display: grid; gap: 6px; }
.ps-row { display: grid; grid-template-columns: repeat(5,1fr); gap: 4px; }
.ps-item { padding: 8px 6px; text-align: center; background: rgba(20,40,80,.4); border-radius: 8px; }
.ps-item b { display: block; color: #8fb8ff; font-size: 11px; }
.ps-item em { display: block; font-size: 18px; color: #8be9ff; font-weight: 700; font-style: normal; }
.chart-inline { min-height: 140px; margin-top: 8px; }
.warn-levels { padding: 14px; }
.warn-levels h4 { margin: 0 0 10px; color: #fbbf24; font-size: 14px; }
.warn-level-item { display: flex; align-items: center; gap: 8px; padding: 8px 10px; margin-bottom: 6px; background: rgba(251,191,36,.08); border-radius: 8px; border-left: 3px solid #f59e0b; }
.wl-badge { padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }
.wl-badge.one { background: #fef3c7; color: #b45309; }
.wl-badge.two { background: #fee2e2; color: #dc2626; }
.wl-badge.three { background: #fecaca; color: #b91c1c; }
.wl-badge.four { background: #fecaca; color: #991b1b; }
.wl-text { color: #d1d5db; font-size: 12.5px; flex: 1; }
.rank-table { padding: 14px; }
.rank-header { display: grid; grid-template-columns: repeat(3,auto); gap: 12px; margin-bottom: 10px; padding-bottom: 10px; border-bottom: 1px solid rgba(110,174,255,.1); }
.rank-header b { color: #9cccff; font-size: 12px; }
.rank-header strong { color: #8be9ff; font-size: 16px; font-weight: 700; }
.rank-tbl { width: 100%; border-collapse: collapse; font-size: 12px; }
.rank-tbl th { background: rgba(20,40,80,.5); color: #9cd3ff; padding: 6px 8px; text-align: left; }
.rank-tbl td { padding: 6px 8px; border-bottom: 1px solid rgba(110,174,255,.06); color: #d1d5db; }
.rank-tbl tr:hover td { background: rgba(37,99,235,.08); }
.progress-cell { display: flex; align-items: center; gap: 6px; }
.progress-bar { height: 6px; background: rgba(20,40,80,.4); border-radius: 3px; min-width: 60px; }
.people-bottom-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
.third { min-height: 200px; }
.chart-title-sm { margin: 0 0 6px; color: #9cd3ff; font-size: 13px; }
@media (max-width: 1200px) { .people-top-row, .people-bottom-row { grid-template-columns: 1fr; } }
</style>
