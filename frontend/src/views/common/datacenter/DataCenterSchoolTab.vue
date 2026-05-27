<template>
  <div class="datacenter-shell">
    <div class="screen-header fade-up delay-1">
      <div class="screen-title">智备优教数据中台</div>
      <div class="screen-actions">
        <div class="search-pill"><input placeholder="个人信息查询" /></div>
        <div class="switch-btn" @click="$emit('toggle-chat')">切换页面</div>
      </div>
    </div>

    <div class="top-stat-row fade-up delay-2">
      <div v-for="item in schoolStats" :key="item.label" class="dark-panel stat-box lift-card">
        <span>{{ item.label }}</span><strong>{{ item.value }}</strong>
      </div>
    </div>

    <div class="school-grid fade-up">
      <div class="dark-panel chart large lift-card" ref="schoolMapRef"></div>
      <div class="dark-panel chart lift-card" ref="schoolRingRef"></div>
      <div class="dark-panel chart lift-card" ref="schoolBarRef"></div>
      <div class="dark-panel chart lift-card" ref="schoolLineRef"></div>
      <div class="dark-panel chart lift-card" ref="teacherPieRef"></div>
    </div>
  </div>
</template>

<script setup>
import './datacenter-shared.css'
import echarts from '@/utils/echarts'
import { debounce } from '@/utils/debounce'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  schoolStats: { type: Array, default: () => [] }
})

defineEmits(['toggle-chat'])

const schoolMapRef = ref()
const schoolRingRef = ref()
const schoolBarRef = ref()
const schoolLineRef = ref()
const teacherPieRef = ref()

const chartRefs = [schoolMapRef, schoolRingRef, schoolBarRef, schoolLineRef, teacherPieRef]
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
  const schoolMap = echarts.getInstanceByDom(schoolMapRef.value) || echarts.init(schoolMapRef.value)
  schoolMap.setOption({
    title: { text: '近五年分布情况', subtext: '2023年人数：9702人', left: 'center', textStyle: { color: '#eaf4ff', fontSize: 22, fontWeight: 700 }, subtextStyle: { color: '#7dd3fc', fontSize: 14 } },
    tooltip: { trigger: 'axis', backgroundColor: '#1a2744', borderColor: '#2d5aa0' },
    grid: { left: 50, right: 20, top: 70, bottom: 30 },
    xAxis: { type: 'value', ...axisStyleDark, show: false },
    yAxis: { type: 'category', inverse: true, data: ['黑龙江','吉林','辽宁','河北','山东','江苏','安徽','浙江','福建','广东','广西','云南','四川','河南','湖北','湖南','江西','山西','陕西','甘肃','内蒙古','新疆','西藏','青海','宁夏','北京','天津','上海','重庆','海南','台湾','港澳'], ...axisStyleDark, axisLabel: { fontSize: 10 } },
    series: [{ type: 'bar', data: [20,15,35,45,120,200,80,150,60,280,40,90,180,160,130,70,50,100,30,25,10,15,5,35,220,190,110,55,8,3], itemStyle: { color: (p) => ['#f59e0b','#38bdf8','#34d399'][p.dataIndex % 3], borderRadius: [0, 4, 4, 0] } }]
  })

  const schoolRing = echarts.getInstanceByDom(schoolRingRef.value) || echarts.init(schoolRingRef.value)
  schoolRing.setOption({
    title: { text: '教师总数\n461', left: 'center', top: '42%', textStyle: { color: '#fff', fontSize: 16, fontWeight: 700, lineHeight: 1.4 } },
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)', backgroundColor: '#1a2744' },
    series: [{ type: 'pie', radius: ['40%', '68%'], center: ['50%', '46%'], label: { color: '#d7e8ff', fontSize: 11 }, labelLine: { length: 10, length2: 16 }, data: [
      { name: '理科', value: 135, itemStyle: { color: '#a78bfa' } },
      { name: '工科', value: 165, itemStyle: { color: '#60a5fa' } },
      { name: '农科', value: 36, itemStyle: { color: '#34d399' } },
      { name: '人文社科', value: 79, itemStyle: { color: '#fbbf24' } },
      { name: '艺术与设计', value: 46, itemStyle: { color: '#f472b6' } }
    ] }]
  })

  const schoolBar = echarts.getInstanceByDom(schoolBarRef.value) || echarts.init(schoolBarRef.value)
  schoolBar.setOption({
    title: { text: '总成交/元', textStyle: { color: '#eaf4ff', fontSize: 14 } },
    grid: { left: 50, right: 18, top: 40, bottom: 24 },
    xAxis: { type: 'value', ...axisStyleDark, max: 650, interval: 100 },
    yAxis: { type: 'category', inverse: true, data: ['2023-11','2023-09','2023-07','2023-05','2023-03','2023-01','2022-11'], ...axisStyleDark },
    series: [{ type: 'bar', data: [104000, 87000, 61000, 42000, 35000, 30000, 25000], itemStyle: { color: new echarts.graphic.LinearGradient(0,0,1,0,[{ offset: 0, color: '#38bdf8' }, { offset: 1, color: '#2563eb' }]), borderRadius: [0, 4, 4, 0] } }]
  })

  const schoolLine = echarts.getInstanceByDom(schoolLineRef.value) || echarts.init(schoolLineRef.value)
  schoolLine.setOption({
    title: { text: '近三年通过率', textStyle: { color: '#eaf4ff', fontSize: 14 } },
    grid: { left: 44, right: 18, top: 40, bottom: 24 },
    xAxis: { type: 'category', data: ['2020','2021','2022','2023','2024'], ...axisStyleDark },
    yAxis: { type: 'value', min: 40, max: 65, ...axisStyleDark },
    series: [{ type: 'line', smooth: true, data: [51.67, 51.39, 50.28, 48.61, 47.78], lineStyle: { color: '#67e3ff', width: 2.5 }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(103,227,255,.2)' }, { offset: 1, color: 'rgba(103,227,255,.02)' }]) }, markPoint: { data: [{ coord: ['2024', 47.78], name: '当前', symbolSize: 50 }] } }]
  })

  const teacherPie = echarts.getInstanceByDom(teacherPieRef.value) || echarts.init(teacherPieRef.value)
  teacherPie.setOption({
    title: { text: '教师结构分布', textStyle: { color: '#eaf4ff', fontSize: 14 } },
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: ['42%', '70%'], center: ['50%', '54%'], label: { color: '#d7e8ff' }, data: [
      { value: 40, name: '教授', itemStyle: { color: '#38bdf8' } },
      { value: 32, name: '副教授', itemStyle: { color: '#34d399' } },
      { value: 18, name: '讲师', itemStyle: { color: '#fbbf24' } },
      { value: 10, name: '助教', itemStyle: { color: '#f472b6' } }
    ] }]
  })
}

onMounted(async () => {
  await nextTick()
  renderCharts()
  window.addEventListener('resize', resizeCharts)
})

watch(() => props.schoolStats, async () => {
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
.top-stat-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 10px; margin-bottom: 12px; }
.stat-box { padding: 12px 14px; background: linear-gradient(145deg, rgba(20,40,80,.8), rgba(15,30,60,.6)); border: 1px solid rgba(110,174,255,.15); border-radius: 10px; }
.stat-box span { display: block; color: #8fb8ff; margin-bottom: 6px; font-size: 12px; }
.stat-box strong { font-size: 26px; color: #8be9ff; font-weight: 700; }
.school-grid { display: grid; grid-template-columns: 1.4fr 1fr 1fr; gap: 10px; }
.large { grid-row: span 2; min-height: 500px; }
@media (max-width: 1200px) { .top-stat-row, .school-grid { grid-template-columns: 1fr; } .large { grid-row: auto; min-height: 300px; } }
</style>
