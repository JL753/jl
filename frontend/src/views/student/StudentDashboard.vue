<template>
  <div class="student-home-page">
    <!-- Top Hero -->
    <section class="page-hero panel lift-card fade-up">
      <div>
        <p class="eyebrow">STUDENT OVERVIEW</p>
        <h2>我的学业</h2>
        <p>统一展示个人信息、学业表现、行为数据与近期学习任务</p>
      </div>
      <span class="soft-tag date-badge">{{ todayText }}</span>
    </section>

    <!-- Overview Grid: Compact 3-column layout like screenshot -->
    <section class="overview-grid">
      <!-- Left: Profile Card (Personal Center) -->
      <div class="panel profile-card lift-card fade-up delay-1">
        <div class="tab-row">
          <span class="tab-active">个人中心</span><span>个人档案</span><span>我的学业</span>
        </div>
        <div class="student-info">
          <div class="avatar-circle">👨‍🎓</div>
          <div class="info-text">
            <strong>{{ auth.user?.displayName || '邢国曦' }}</strong>
            <p>{{ auth.user?.username || '2023105444845' }}</p>
          </div>
        </div>
        <ul class="info-details">
          <li><span>所属学院：</span>计算机与软件工程学院</li>
          <li><span>所属专业：</span>{{ data.summary.riskLevel || '软件工程' }}</li>
          <li><span>当前课程：</span>人工智能导论</li>
          <li><span>带班导师：</span>李老师</li>
        </ul>

        <!-- Teacher info -->
        <div class="teacher-info-box">
          <small>教师信息</small>
          <strong>{{ workspace.courseSchedule?.[0]?.name || '李老师' }}</strong>
          <p>2025年04月10日 | 星期四</p>
        </div>

        <!-- Today's Schedule -->
        <div class="schedule-section">
          <div class="sched-header"><strong>今日课表</strong> <span style="color:#3b82f6;cursor:pointer">明日课表</span></div>
          <div class="sched-list">
            <div v-for="(item, idx) in defaultSchedule" :key="'s'+idx" class="sched-item">
              <span class="sched-time">{{ item.time }}</span>
              <span class="sched-name">{{ item.name }}</span>
              <el-tag size="small" type="primary">{{ idx === 0 ? '进行中' : '未开始' }}</el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- Center: Study Board with SIX-DIMENSION RADAR + Metrics -->
      <div class="panel study-board lift-card fade-up delay-2">
        <div class="board-header">
          <h3>我的学业</h3>
          <span class="soft-tag">{{ todayText }}</span>
        </div>
        
        <div class="metrics-cards">
          <div class="metric primary">
            <span>本大综测评分</span>
            <strong>{{ data.summary.ranking || 9 }}</strong>
            <small>较上月提升 5.79%</small>
          </div>
          <div class="metric cyan">
            <span>AI认知学习准确率</span>
            <strong>{{ data.summary.accuracy || '91' }}</strong>
            <small>QPA 3.7</small>
          </div>
          <div class="metric violet">
            <span>学业预警占比</span>
            <strong>0%</strong>
            <small>暂无预警风险</small>
          </div>
        </div>

        <!-- Course completion ring -> replaced with SIX DIMENSION RADAR -->
        <div class="content-split">
          <div class="chart-wrap" ref="radarRef"></div>
          
          <!-- Right side: Behavior comparison table -->
          <div class="behavior-list">
            <h5 style="margin:0 0 8px;font-size:13px;color:#1e293b;">行为对比</h5>
            <table class="behavior-table">
              <thead><tr><th></th><th>我</th><th>平均</th></tr></thead>
              <tbody>
                <tr v-for="(row, idx) in behaviorRows" :key="idx">
                  <td class="label-cell">{{ row.label }}</td>
                  <td class="mine-cell">{{ row.mine }}</td>
                  <td class="avg-cell">{{ row.avg }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Right: Today's Progress & Timeline -->
      <div class="panel side-summary lift-card fade-up delay-3">
        <!-- Course info tabs -->
        <div class="course-tabs">
          <span :class="{ active: courseTab === 'today' }" @click="courseTab = 'today'" style="color:#2563eb;text-decoration:underline;">今日课表</span>
          <span :class="{ active: courseTab === 'tmr' }" @click="courseTab = 'tmr'">明日课表</span>
        </div>

        <div class="today-progress-card">
          <span>今日课表完成度</span>
          <strong>{{ data.summary.todayGoal || '3/4' }}</strong>
          <small>{{ data.summary.riskLevel || '低风险' }}</small>
        </div>

        <!-- Timeline -->
        <div class="timeline-small">
          <div v-for="(item, idx) in defaultSchedule" :key="'t'+idx" class="tl-item">
            <span class="tl-dot"></span>
            <div><strong>{{ item.time }}</strong><p>{{ item.name }}</p></div>
          </div>
        </div>

        <!-- Bottom stats cards -->
        <div class="bottom-stats-grid">
          <div class="mini-stat"><span class="ms-icon">📅</span><span>已做数</span></div>
          <div class="mini-stat"><span class="ms-icon">🎯</span><span>C已分</span></div>
          <div class="mini-stat"><span class="ms-icon">⏰</span><span>已耗时</span></div>
        </div>

        <!-- Exam info -->
        <div class="exam-info-bar">
          <span>考试信息</span>
          <div class="exam-stats-row">
            <div><b>全通过人数</b><em>{{ examStats.passCount || 6 }}</em><em class="rate green">{{ examStats.passRate || '25.57%' }}</em></div>
            <div><b>通过率</b><em>{{ examStats.totalRate || '28.57%' }}</em></div>
            <div><b>英语六级</b><em>{{ examStats.cet6 || '36/1049' }}</em><em class="rate red">{{ examStats.cet6Rate || '3.43%' }}</em></div>
            <div><b>智无成绩</b><em>{{ examStats.aiScore || '2.49' }}</em><em class="rate red">{{ examStats.aiRate || '4.08%' }}</em></div>
          </div>
        </div>
      </div>
    </section>

    <!-- Bottom Charts Section -->
    <section class="bottom-grid">
      <div class="panel chart-card lift-card fade-up delay-4" ref="lineRef"></div>
      <div class="panel chart-card lift-card fade-up delay-5" ref="barRef"></div>
    </section>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { computed, nextTick, onMounted, ref } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { apiStudentDashboard, apiStudentWorkspace } from '../../api'

const auth = useAuthStore()
const data = ref({ summary: {}, pie: [], line: [], bar: [], radar: [] })
const workspace = ref({ courseSchedule: [] })
const radarRef = ref(), lineRef = ref(), barRef = ref()
const todayText = computed(() => new Date().toLocaleDateString('zh-CN'))
const courseTab = ref('today')

// Default schedule - compact
const defaultSchedule = [
  { time: '上午', name: '08:00~08:45 Java EE企业级...' },
  { time: '', name: '08:55~09:40 Java EE企业级...' },
  { time: '下午', name: '13:00~13:45 非关系型数据...' },
  { time: '', name: '13:55~14:40 非关系型数据...' }
]

// Behavior rows
const behaviorRows = [
  { label: '行为', mine: '', avg: '' },
  { label: '本周上課时长', mine: '28小时', avg: '30小时' },
  { label: '试卷完成次数', mine: '3次', avg: '2次' },
  { label: '课程回看次数', mine: '02次', avg: '01次' },
  { label: '学期行为预警', mine: '0次', avg: '0次' },
  { label: '近7天图书借阅', mine: '3本', avg: '5本' },
  { label: '近30天视频观看总时长', mine: '0%', avg: '0%' },
  { label: '近30天视频观看总金额', mine: '0¥', avg: '15¥' }
]

// Exam stats
const examStats = ref({ passCount: 6, passRate: '25.57%', totalRate: '28.57%', cet6: '36/1049', cet6Rate: '3.43%', aiScore: '2.49', aiRate: '4.08%' })

const axisStyle = {
  axisLine: { lineStyle: { color: '#d9e3f0' } },
  axisLabel: { color: '#7588a3', fontSize: 11 },
  splitLine: { lineStyle: { color: '#edf2f8' } }
}

const renderCharts = () => {
  // Six-dimension radar chart (replaces the original donut)
  const radarData = data.value.radar && data.value.radar.length > 0 ? data.value.radar : [
    { name: '知识掌握度', value: 82, max: 100 },
    { name: '理解应用力', value: 75, max: 100 },
    { name: '分析推理力', value: 68, max: 100 },
    { name: '创新思维力', value: 71, max: 100 },
    { name: '协作沟通力', value: 85, max: 100 },
    { name: '自主学习力', value: 78, max: 100 }
  ]

  echarts.init(radarRef.value).setOption({
    title: { text: '课程完成情况(6)', left: 'center', top: 6, textStyle: { color: '#4c5f79', fontSize: 14, fontWeight: 700 }, subtextStyle: { color: '#94a3b8', fontSize: 10 } },
    tooltip: { trigger: 'item' },
    radar: {
      indicator: radarData.map(r => ({ name: r.name, max: r.max })),
      radius: 72, center: ['50%', '56%'], shape: 'circle',
      splitNumber: 5,
      axisName: { color: '#5a6d86', fontSize: 11, fontWeight: 500,
        formatter(v) { return v.length > 5 ? v.slice(0,5)+'..' : v } },
      splitLine: { lineStyle: { color: '#e8edf3' } },
      splitArea: { areaStyle: { color: ['rgba(59,130,246,.03)', 'transparent'] } },
      axisLine: { lineStyle: { color: '#dde6ef' } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: radarData.map(r => r.value), name: auth.user?.displayName || '学生',
        symbol: 'circle', symbolSize: 6,
        lineStyle: { color: '#3b82f6', width: 2.5 },
        areaStyle: {
          color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
            { offset: 0, color: 'rgba(59,130,246,.35)' },
            { offset: 1, color: 'rgba(59,130,246,.02)' }
          ])
        },
        itemStyle: { color: '#3b82f6', borderColor: '#fff', borderWidth: 2 }
      }]
    }]
  })

  // Line: 近7日活跃趋势
  echarts.init(lineRef.value).setOption({
    title: { text: '近 7 日学习活跃趋势', textStyle: { color: '#4c5f79', fontSize: 15, fontWeight: 600 } },
    grid: { left: 44, right: 18, top: 50, bottom: 26 },
    xAxis: { type: 'category', data: data.value.line.map(i => i.name) || ['周一','周二','周三','周四','周五','周六','周日'], ...axisStyle },
    yAxis: { type: 'value', name: '活跃值', ...axisStyle },
    series: [{
      type: 'line', smooth: true,
      data: data.value.line.map(i => i.value) || [23,38,31,45,29,18,12],
      lineStyle: { color: '#10b981', width: 2.5 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(16,185,129,.22)' }, { offset: 1, color: 'rgba(16,185,129,.02)' }]) },
      itemStyle: { color: '#10b981' }
    }]
  })

  // Bar: 周任务完成度
  echarts.init(barRef.value).setOption({
    title: { text: '周学习任务完成度', textStyle: { color: '#4c5f79', fontSize: 15, fontWeight: 600 } },
    grid: { left: 44, right: 18, top: 50, bottom: 26 },
    xAxis: { type: 'category', data: data.value.bar.map(i => i.name) || ['阅读','练习','视频','测验','项目','讨论'], ...axisStyle },
    yAxis: { type: 'value', name: '完成率 %', min: 0, max: 100, ...axisStyle },
    series: [{
      type: 'bar',
      data: data.value.bar.map(i => i.value) || [85,72,90,68,78,82],
      itemStyle: {
        color: function(params) {
          const colors = ['#3b82f6','#10b981','#f59e0b','#ef4444','#8b5cf6','#ec4899']
          return new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:colors[params.dataIndex%colors.length]},{offset:1,color:colors[params.dataIndex%colors.length]+'66'}])
        },
        borderRadius: [8,8,0,0], barMaxWidth: 36
      }
    }]
  })
}

onMounted(async () => {
  try {
    const [dashRes, wsRes] = await Promise.all([apiStudentDashboard(), apiStudentWorkspace()])
    data.value = dashRes.data || {}
    workspace.value = wsRes.data || {}
  } catch(e) { console.warn('Student dashboard API unavailable') }
  await nextTick()
  renderCharts()

  window.addEventListener('resize', () => {
    ;[radarRef, lineRef, barRef].forEach(ref => {
      if (ref.value) { const inst = echarts.getInstanceByDom(ref.value); if(inst) inst.resize() }
    })
  })
})
</script>

<style scoped>
.student-home-page { display: grid; gap: 12px; }

.page-hero { padding: 18px 22px; display: flex; justify-content: space-between; align-items: center; background: linear-gradient(135deg,#fff,#f0fdf4); border: 1px solid #dcfce7; }
.page-hero h2 { margin: 6px 0 6px; font-size: 26px; font-weight: 700; color: #1e293b; }
.page-hero p { margin: 0; color: #64748b; font-size: 13px; line-height: 1.7; }

.overview-grid { display: grid; grid-template-columns: 280px 1fr 260px; gap: 12px; }

/* Profile card - compact */
.profile-card, .study-board, .side-summary { padding: 14px 16px; background: white; border-radius: 14px; box-shadow: 0 2px 10px rgba(33,65,108,.05); }
.tab-bar { display: flex; gap: 14px; margin-bottom: 12px; font-size: 13px; color: #94a3b8; }
.tab-bar .tab-active { color: #166534; font-weight: 700; border-bottom: 2px solid #22c55e; padding-bottom: 2px; }
.student-info { display: flex; gap: 12px; align-items: center; margin-bottom: 10px; }
.avatar-circle { width: 52px; height: 52px; border-radius: 50%; display: grid; place-items: center; background: linear-gradient(135deg,#ffca85,#ff8e6f); font-size: 26px; box-shadow: 0 4px 14px rgba(255,140,112,.2); }
.info-text strong { display: block; font-size: 15px; color: #1e293b; font-weight: 600; }
.info-text p { margin: 2px 0 0; font-size: 12px; color: #94a3b8; }
.info-details { margin: 0 0 10px; padding-left: 18px; color: #64748b; line-height: 1.85; font-size: 12.5px; list-style-type: none; }
.info-details li::before { content: "•"; margin-right: 6px; color: #94a3b8; }

.teacher-info-box { padding: 10px 12px; border-radius: 10px; background: linear-gradient(135deg,#f0fdf4,#dcfce7); border: 1px solid #bbf7d0; margin-bottom: 10px; }
.teacher-info-box small { display: block; font-size: 11px; color: #166534; font-weight: 500; }
.teacher-info-box strong { display: block; font-size: 13.5px; color: #166534; margin-top: 2px; }
.teacher-info-box p { margin: 2px 0 0; font-size: 11.5px; color: #059669; }

.schedule-section { } .sched-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-size: 13px; font-weight: 600; color: #1e293b; } .sched-header strong { color: #1e293b; } .sched-list { display: grid; gap: 6px; } .sched-item { padding: 8px 12px; border-radius: 10px; background: linear-gradient(135deg,#f8fbff,#fff); border: 1px solid #edf2f8; display: grid; grid-template-columns: auto 1fr auto; gap: 6px; align-items: center; transition: all .18s ease; font-size: 12.5px; } .sched-item:hover { transform: translateX(3px); border-color: #dbeafe; } .sched-time { color: #2563eb; font-weight: 600; white-space: nowrap; } .sched-name { color: #64748b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* Study board */
.board-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; } .board-header h3 { margin: 0; font-size: 17px; color: #1e293b; font-weight: 700; } .board-header span.active { color: #2563eb; font-weight: 600; }
.metrics-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 12px; }
.metric { padding: 10px 12px; border-radius: 12px; display: grid; gap: 3px; transition: transform .18s ease; } .metric:hover { transform: translateY(-2px); }
.metric.primary { background: linear-gradient(135deg, #eff6ff, #dbeafe); border: 1px solid #bfdbfe; } .metric.cyan { background: linear-gradient(135deg, #ecfeff, #cffafe); border: 1px solid #a5f3fc; } .metric.violet { background: linear-gradient(135deg, #f5f3ff, #ede9fe); border: 1px solid #ddd6fe; }
.metric span { font-size: 11.5px; color: #64748b; } .metric strong { font-size: 22px; color: #1e293b; font-weight: 700; } .metric small { font-size: 10.5px; color: #94a3b8; }
.content-split { display: grid; grid-template-columns: 300px 1fr; gap: 12px; margin-top: 12px; }
.chart-wrap { min-height: 300px; width: 100%; }

/* Behavior table */
.behavior-list { overflow-x: auto; }
.behavior-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.behavior-table th { background: #f8fafc; color: #475569; padding: 6px 10px; text-align: left; font-weight: 600; border-bottom: 1px solid #e2e8f0; }
.behavior-table td { padding: 5px 10px; border-bottom: 1px solid #f1f5f9; color: #657994; }
.label-cell { color: #475569; font-weight: 500; white-space: nowrap; }
.mine-cell { color: #1e293b; font-weight: 600; }
.avg-cell { color: #94a3b8; }

/* Side summary */
.side-summary { padding: 14px 16px; }
.course-tabs { display: flex; gap: 16px; margin-bottom: 12px; font-size: 13px; cursor: pointer; color: #94a3b8; }
.course-tabs .active { color: #2563eb; font-weight: 700; text-decoration: underline; }
.today-progress-card { padding: 12px 14px; border-radius: 12px; background: linear-gradient(135deg,#eff5ff,#fbfdff); border: 1px solid #e0edfa; display: grid; gap: 3px; margin-bottom: 12px; } .today-progress-card strong { font-size: 28px; color: #1e293b; font-weight: 700; }
.timeline-small { display: grid; gap: 6px; margin-bottom: 12px; } .tl-item { display: flex; gap: 8px; padding: 4px 0; } .tl-dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 8px; background: #f97316; flex-shrink: 0; } .tl-item p { margin: 3px 0 0; color: #94a3b8; font-size: 12px; } .tl-item strong { color: #2563eb; font-size: 12.5px; font-weight: 600; }
.bottom-stats-grid { display: flex; gap: 6px; margin-bottom: 10px; }
.mini-stat { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 3px; padding: 8px; border-radius: 8px; background: #f8fafc; border: 1px solid #edf2f8; font-size: 11px; color: #64748b; } .ms-icon { font-size: 16px; }
.exam-info-bar { border-top: 1px solid #edf2f8; padding-top: 10px; } .exam-info-bar > span { display: block; font-size: 12px; font-weight: 600; color: #475569; margin-bottom: 6px; }
.exam-stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; } .exam-stats-row div { padding: 6px 8px; border-radius: 8px; background: #f8fafc; border: 1px solid #edf2f8; font-size: 11px; } .exam-stats-row b { display: block; color: #475569; font-size: 11px; margin-bottom: 2px; } .exam-stats-row em { font-size: 12px; font-weight: 600; margin-right: 4px; } em.rate.green { color: #16a34a; } em.rate.red { color: #ef4444; }

.bottom-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.chart-card { min-height: 240px; padding: 12px; background: white; border-radius: 14px; box-shadow: 0 2px 10px rgba(33,65,108,.05); }

.lift-card { transition: transform .22s ease, box-shadow .22s ease; }
.lift-card:hover { transform: translateY(-3px); box-shadow: 0 14px 32px rgba(33,68,120,.1); }
.soft-tag { padding: 4px 12px; border-radius: 999px; font-size: 11.5px; font-weight: 600; background: #dcfce7; color: #166534; } .date-badge { background: #eff6ff; color: #2563eb; }
.fade-up { animation: fadeUp .5s ease both; } .delay-1{animation-delay:.05s}.delay-2{animation-delay:.1s}.delay-3{animation-delay:.15s}.delay-4{animation-delay:.2s}.delay-5{animation-delay:.25s}
@keyframes fadeUp{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:translateY(0)}}
.eyebrow{font-size:11px;letter-spacing:1.5px;text-transform:uppercase;color:#94a3b8;font-weight:600;margin:0}

@media(max-width:1100px){.overview-grid,.metrics-cards,.content-split,.bottom-grid{grid-template-columns:1fr}}
@media(max-width:760px){.overview-grid{grid-template-columns:1fr}.chart-wrap{min-height:220px}}
</style>
