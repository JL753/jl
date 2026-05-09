<template>
  <div class="datacenter-shell">
    <div class="screen-header fade-up delay-1">
      <div class="screen-title">智备优教数据中台</div>
      <div class="screen-actions"><div class="search-pill">返回</div></div>
    </div>

    <div class="student-data-grid fade-up">
      <div class="dark-panel profile-section lift-card">
        <h4>学生画像</h4>
        <div class="sp-info">
          <p><b>姓名：</b>{{ studentName }}</p>
          <p><b>专业：</b>{{ studentProfile.major || '-' }}</p>
          <p><b>课程：</b>{{ studentProfile.course || '-' }}</p>
          <p><b>知识基础：</b>{{ studentProfile.knowledgeBase || '-' }}</p>
        </div>
      </div>

      <div class="dark-panel calendar-section lift-card" ref="studentCalendarRef"></div>
      <div class="dark-panel radar-section lift-card" ref="studentRadarRef"></div>

      <div class="dark-panel honors-section lift-card">
        <h4>荣誉成就</h4>
        <div v-for="h in honors" :key="h.name" class="honor-item"><strong>{{ h.name }}</strong><span>{{ h.level }}</span></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import './datacenter-shared.css'
import echarts from '@/utils/echarts'
import { debounce } from '@/utils/debounce'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  studentName: { type: String, default: '' },
  studentProfile: { type: Object, default: () => ({}) },
  honors: { type: Array, default: () => [] }
})

const studentCalendarRef = ref()
const studentRadarRef = ref()
const chartRefs = [studentCalendarRef, studentRadarRef]
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
  const studentCalendar = echarts.getInstanceByDom(studentCalendarRef.value) || echarts.init(studentCalendarRef.value)
  studentCalendar.setOption({
    grid: { left: 46, right: 20, top: 60, bottom: 26 },
    xAxis: { type: 'category', data: Array.from({ length: 31 }, (_, i) => String(i + 1)), ...axisStyleDark },
    yAxis: { type: 'category', data: ['四月'], ...axisStyleDark },
    series: [{ type: 'heatmap', data: Array.from({ length: 31 }, (_, i) => [i, 0, (i % 5) + 1]), itemStyle: { color: (params) => params.value[2] > 3 ? '#22c55e' : params.value[2] > 1 ? '#facc15' : '#fef3c7' }, borderRadius: 3 }]
  })

  const studentRadar = echarts.getInstanceByDom(studentRadarRef.value) || echarts.init(studentRadarRef.value)
  studentRadar.setOption({
    radar: { indicator: [{ name: '学习', max: 100 }, { name: '劳动', max: 100 }, { name: '纪律', max: 100 }, { name: '心理', max: 100 }, { name: '创新', max: 100 }, { name: '协作', max: 100 }], radius: 58, axisName: { color: '#bfe0ff' }, splitArea: { areaStyle: { color: ['rgba(9,19,38,.35)','rgba(9,19,38,.18)'] } } },
    series: [{ type: 'radar', data: [{ value: [88,82,84,79,91,85], areaStyle: { color: 'rgba(99,214,255,.18)' }, lineStyle: { color: '#63d6ff' } }] }]
  })
}

onMounted(async () => {
  await nextTick()
  renderCharts()
  window.addEventListener('resize', resizeCharts)
})

watch(() => [props.studentName, props.studentProfile, props.honors], async () => {
  await nextTick()
  renderCharts()
}, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  chartRefs.forEach((chartRef) => {
    if (!chartRef.value) return
    echarts.getInstanceByDom(chartRef.value)?.dispose()
  })
})
</script>

<style scoped>
.student-data-grid { display: grid; grid-template-columns: 280px 1fr 1fr 1fr; gap: 10px; }
.profile-section,.calendar-section,.radar-section,.honors-section { padding: 14px; }
.profile-section h4,.honors-section h4 { margin: 0 0 10px; color: #9cd3ff; font-size: 14px; }
.sp-info { color: #d9e9ff; line-height: 2; font-size: 13px; }
.sp-info p { margin: 0; }
.sp-info b { color: #8be9ff; }
.honor-item { padding: 10px 12px; border-radius: 8px; background: rgba(124,178,255,.08); border: 1px solid rgba(124,178,255,.12); margin-bottom: 8px; }
.honor-item strong { display: block; color: #8be9ff; font-size: 13px; }
.honor-item span { color: #b7d2f4; font-size: 12px; }
@media (max-width: 1200px) { .student-data-grid { grid-template-columns: 1fr; } }
</style>
