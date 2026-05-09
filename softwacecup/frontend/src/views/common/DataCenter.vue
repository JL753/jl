<template>
  <div class="datacenter-page dark-screen">
    <div class="switch-bar fade-up">
      <el-radio-group v-model="tab" size="small">
        <el-radio-button label="school">学校</el-radio-button>
        <el-radio-button label="department-level">学院(等级)</el-radio-button>
        <el-radio-button label="department-people">学院(人员)</el-radio-button>
        <el-radio-button label="student">个人</el-radio-button>
      </el-radio-group>
    </div>

    <!-- ====== SCHOOL TAB: 近五年分布情况 ====== -->
    <template v-if="tab === 'school'">
      <div class="screen-header fade-up delay-1">
        <div class="screen-title">智备优教数据中台</div>
        <div class="screen-actions">
          <div class="search-pill"><input placeholder="个人信息查询" /></div>
          <div class="switch-btn" @click="showAiChat = !showAiChat">切换页面</div>
        </div>
      </div>

      <!-- Top stat row -->
      <div class="top-stat-row fade-up delay-2">
        <div v-for="item in schoolStats" :key="item.label" class="dark-panel stat-box lift-card">
          <span>{{ item.label }}</span><strong>{{ item.value }}</strong>
        </div>
      </div>

      <div class="school-grid fade-up">
        <!-- Map + Bar chart area -->
        <div class="dark-panel chart large lift-card" ref="schoolMapRef"></div>
        
        <!-- Right side: Ring chart -->
        <div class="dark-panel chart lift-card" ref="schoolRingRef"></div>
        
        <!-- Bottom right: Bar + Line -->
        <div class="dark-panel chart lift-card" ref="schoolBarRef"></div>
        <div class="dark-panel chart lift-card" ref="schoolLineRef"></div>
        
        <!-- Teacher pie chart -->
        <div class="dark-panel chart lift-card" ref="teacherPieRef"></div>
      </div>
    </template>

    <!-- ====== DEPARTMENT LEVEL TAB: 学院等级分析 ====== -->
    <template v-else-if="tab === 'department-level'">
      <div class="screen-header fade-up delay-1">
        <div class="screen-title">智备优教数据中台</div>
        <div class="screen-actions">
          <div class="search-pill"><input placeholder="个人信息查询" /></div>
          <div class="switch-btn" @click="showAiChat = !showAiChat">切换页面</div>
        </div>
      </div>

      <div class="dept-top-row fade-up delay-2">
        <!-- Left: Major selector -->
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
        
        <!-- Center: Stats cards -->
        <div class="dark-panel dept-stats lift-card">
          <div class="stat-grid-3col">
            <div v-for="s in deptLevelStats" :key="s.label" class="d-stat"><b>{{ s.label }}</b><strong>{{ s.value }}</strong></div>
          </div>
          
          <!-- Radar chart for department capabilities -->
          <div ref="deptRadarRef" class="chart-inline sm"></div>
        </div>
        
        <!-- Right: Class pass rates -->
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

      <!-- Bottom section -->
      <div class="dept-bottom-row fade-up">
        <div class="dark-panel chart lift-card half" ref="deptAvgRef">
          <h4 style="margin:0 0 8px;color:#9cd3ff;font-size:14px">《两学期班级平均评分》</h4>
        </div>
        <div class="dark-panel chart lift-chart half" ref="deptTrendRef">
          <h4 style="margin:0 0 8px;color:#9cd3ff;font-size:14px">《继续深造意向》</h4>
        </div>
      </div>
    </template>

    <!-- ====== DEPARTMENT PEOPLE TAB: 学院人员分析 ====== -->
    <template v-else-if="tab === 'department-people'">
      <div class="screen-header fade-up delay-1">
        <div class="screen-title">智备优教数据中台</div>
        <div class="screen-actions">
          <div class="search-pill"><input placeholder="个人信息查询" /></div>
          <div class="switch-btn" @click="showAiChat = !showAiChat">切换页面</div>
        </div>
      </div>

      <div class="people-top-row fade-up delay-2">
        <!-- Left: Stats grid -->
        <div class="dark-panel people-stats lift-card">
          <div class="stats-2row">
            <div class="ps-row">
              <span v-for="s in peopleTopStats.slice(0,5)" :key="s.label" class="ps-item"><b>{{ s.label }}</b><em>{{ s.value }}</em></span>
            </div>
            <div class="ps-row">
              <span v-for="s in peopleTopStats.slice(5)" :key="s.label" class="ps-item"><b>{{ s.label }}</b><em>{{ s.value }}</em></span>
            </div>
          </div>
          
          <!-- Pentagon radar -->
          <div ref="peopleRadarRef" class="chart-inline sm"></div>
        </div>
        
        <!-- Center: Warning levels -->
        <div class="dark-panel warn-levels lift-card">
          <h4>学情预警</h4>
          <div v-for="(w,i) in warningLevels" :key="i" class="warn-level-item">
            <span :class="['wl-badge', w.level]">{{ w.levelLabel }}级预警：</span>
            <span class="wl-text">{{ w.text }}</span>
          </div>
        </div>
        
        <!-- Right: Student ranking table -->
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

      <!-- Bottom charts -->
      <div class="people-bottom-row fade-up">
        <div class="dark-panel chart lift-card third" ref="peopleScoreTrendRef">
          <h4 style="margin:0 0 6px;color:#9cd3ff;font-size:13px">《成员成绩对比》</h4>
        </div>
        <div class="dark-panel chart lift-chart third" ref="peopleCompletionRef">
          <h4 style="margin:0 0 6px;color:#9cd3ff;font-size:13px">《站点完成情况》</h4>
        </div>
        <div class="dark-panel chart lift-chart third" ref="peopleExamRef">
          <h4 style="margin:0 0 6px;color:#9cd3ff;font-size:13px">《测验完成情况》</h4>
        </div>
      </div>
    </template>

    <!-- ====== STUDENT TAB ====== -->
    <template v-else-if="tab === 'student'">
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
    </template>

    <!-- ====== AI Chat Assistant Sidebar ====== -->
    <transition name="slide-right">
      <div v-if="showAiChat" class="ai-chat-sidebar">
        <div class="ai-header">
          <span class="ai-icon">🤖</span>
          <strong>云脑小星</strong>
        </div>
        <div class="ai-messages">
          <div v-for="(m,i) in aiMessages" :key="i" :class="['ai-msg', m.role]">
            <img v-if="m.role==='bot'" src="https://dummyimage.com/28x28/4c8dff/fff&text=AI" alt="" class="ai-avatar-sm" />
            <div class="ai-bubble">{{ m.text }}</div>
          </div>
        </div>
        <div class="ai-input-area">
          <input v-model="aiInputText" placeholder="想聊点什么吗..." @keyup.enter="sendAiMsg" />
          <button @click="sendAiMsg">➜</button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { nextTick, onMounted, ref, watch } from 'vue'
import { apiDatacenter } from '../../api'

const tab = ref('school')
const showAiChat = ref(false)
const selectedMajor = ref('csse')
const deptSubPage = ref('overview')

// Stats data
const schoolStats = ref([
  { label: '两院院士', value: '8名' }, { label: '千人计划', value: '12名' },
  { label: '万人计划', value: '14名' }, { label: '长江学者', value: '58名' },
  { label: '播放量第一', value: 'Java—每天一个知识点' }, { label: '销量第一', value: '两蒙开发' }
])
const departmentStats = ref([
  { label: '博士讲师', value: '7名' }, { label: '硕士讲师', value: '6名' },
  { label: '软件课程', value: '8科' }, { label: '硬件课程', value: '5科' }
])
const deptLevelStats = ref([
  { label: '任务进度风险提示', value: '' }
])
const peopleTopStats = ref([
  { label: '国家级奖项', value: '2个' }, { label: '省级奖项', value: '5个' },
  { label: '校级奖项', value: '9个' }, { label: '院级奖项', value: '17个' },
  { label: '竞赛达人', value: '17人' }, { label: '优秀干部', value: '12人' },
  { label: '集体荣誉', value: '2个' }
])
const teacherCount = ref('2'), monitorCount = ref('6'), advisorCount = ref('38')
const warningLevels = ref([
  { level: 'one', levelLabel: '一', text: '4人课堂表现不佳' },
  { level: 'two', levelLabel: '二', text: '7人学习状态出现轻微问题' },
  { level: 'three', levelLabel: '三', text: '3人学习状态出现严重问题' },
  { level: 'four', levelLabel: '四', text: '1人有极其严重学习问题' }
])
const classRates = ref([
  { name: '一班', rate: 82 }, { name: '二班', rate: 90 }, { name: '三班', rate: 86 }
])
const studentRanking = ref([
  { id: '2023105444845', name: '安安琪', progress: 83 },
  { id: '2023105444846', name: '水壶芝', progress: 67 },
  { id: '2023105444847', name: '袁盖辰', progress: 81 }
])

const studentStats = ref([])
const honors = ref([])
const studentName = ref('邢国曦')
const studentProfile = ref({})

// AI Chat
const aiInputText = ref('')
const aiMessages = ref([
  { role: 'bot', text: '您好，请问有什么可以帮助您的？' }
])

// Refs
const schoolMapRef = ref(), schoolRingRef = ref(), schoolBarRef = ref(), schoolLineRef = ref(), teacherPieRef = ref()
const deptRadarRef = ref(), deptAvgRef = ref(), deptTrendRef = ref()
const peopleRadarRef = ref(), peopleScoreTrendRef = ref(), peopleCompletionRef = ref(), peopleExamRef = ref()
const studentCalendarRef = ref(), studentRadarRef = ref()

const axisStyleDark = {
  axisLine: { lineStyle: { color: 'rgba(156,211,255,.25)' } },
  axisLabel: { color: '#9cccff', fontSize: 11 },
  splitLine: { lineStyle: { color: 'rgba(156,211,255,.1)' } }
}

function renderSchool() {
  // Map-like distribution bar
  echarts.init(schoolMapRef.value).setOption({
    title: { text: '近五年分布情况', subtext: `2023年人数：9702人`, left: 'center', textStyle: { color: '#eaf4ff', fontSize: 22, fontWeight: 700 }, subtextStyle: { color: '#7dd3fc', fontSize: 14 } },
    tooltip: { trigger: 'axis', backgroundColor: '#1a2744', borderColor: '#2d5aa0' },
    grid: { left: 50, right: 20, top: 70, bottom: 30 },
    xAxis: { type: 'value', ...axisStyleDark, show: false },
    yAxis: { type: 'category', inverse: true, data: ['黑龙江','吉林','辽宁','河北','山东','江苏','安徽','浙江','福建','广东','广西','云南','四川','河南','湖北','湖南','江西','山西','陕西','甘肃','内蒙古','新疆','西藏','青海','宁夏','北京','天津','上海','重庆','海南','台湾','港澳'], ...axisStyleDark, axisLabel: { fontSize: 10 } },
    series: [{ type: 'bar', data: [20,15,35,45,120,200,80,150,60,280,40,90,180,160,130,70,50,100,30,25,10,15,5,35,220,190,110,55,8,3], itemStyle: { color: function(p){ return ['#f59e0b','#38bdf8','#34d399'][p.dataIndex%3] }, borderRadius: [0, 4, 4, 0] } }]
  })

  // Teacher composition ring
  echarts.init(schoolRingRef.value).setOption({
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

  // Revenue bar
  echarts.init(schoolBarRef.value).setOption({
    title: { text: '总成交/元', textStyle: { color: '#eaf4ff', fontSize: 14 } },
    grid: { left: 50, right: 18, top: 40, bottom: 24 },
    xAxis: { type: 'value', ...axisStyleDark, max: 650, interval: 100 },
    yAxis: { type: 'category', inverse: true, data: ['2023-11','2023-09','2023-07','2023-05','2023-03','2023-01','2022-11'], ...axisStyleDark },
    series: [{ type: 'bar', data: [104000, 87000, 61000, 42000, 35000, 30000, 25000], itemStyle: { color: new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'#38bdf8'},{offset:1,color:'#2563eb'}]), borderRadius: [0, 4, 4, 0] } }]
  })

  // Trend line
  echarts.init(schoolLineRef.value).setOption({
    title: { text: '近三年通过率', textStyle: { color: '#eaf4ff', fontSize: 14 } },
    grid: { left: 44, right: 18, top: 40, bottom: 24 },
    xAxis: { type: 'category', data: ['2020','2021','2022','2023','2024'], ...axisStyleDark },
    yAxis: { type: 'value', min: 40, max: 65, ...axisStyleDark },
    series: [{ type: 'line', smooth: true, data: [51.67, 51.39, 50.28, 48.61, 47.78], lineStyle: { color: '#67e3ff', width: 2.5 }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(103,227,255,.2)' }, { offset: 1, color: 'rgba(103,227,255,.02)' }]) }, markPoint: { data: [{ coord: ['2024', 47.78], name: '当前', symbolSize: 50 }] } }]
  })
}

function renderDeptLevel() {
  // Dept capability radar (pentagon shape)
  echarts.init(deptRadarRef.value).setOption({
    radar: {
      indicator: [{ name: '科研产出', max: 100 }, { name: '学科评价', max: 100 }, { name: '社会服务', max: 100 }, { name: '国际合作', max: 100 }, { name: '社会责任', max: 100 }],
      radius: 62, shape: 'polygon', axisName: { color: '#bfe0ff', fontSize: 11 },
      splitArea: { areaStyle: { color: ['rgba(9,19,38,.32)', 'rgba(9,19,38,.16)'] } },
      splitLine: { lineStyle: { color: 'rgba(156,211,255,.12)' } }
    },
    series: [{
      type: 'radar', data: [{ value: [82,75,68,71,79],
        areaStyle: { color: 'rgba(99,214,255,.15)' }, lineStyle: { color: '#63d6ff', width: 2 },
        symbolSize: 5, itemStyle: { color: '#63d6ff', borderColor: '#fff', borderWidth: 1 }
      }]
    }]
  })

  // Average score comparison
  echarts.init(deptAvgRef.value).setOption({
    grid: { left: 36, right: 16, top: 36, bottom: 24 },
    xAxis: { type: 'category', data: ['一班','二班','三班'], ...axisStyleDark },
    yAxis: { type: 'value', min: 0, max: 5, ...axisStyleDark },
    series: [{ name: '上学期', type: 'bar', data: [3.2,4.0,3.6], itemStyle: { color: '#60b4ff', borderRadius: [4,4,0,0] } }, { name: '本学期', type: 'bar', data: [3.8,4.5,4.2], itemStyle: { color: '#7fe1c7', borderRadius: [4,4,0,0] } }]
  })

  // Trend triangle
  echarts.init(deptTrendRef.value).setOption({
    radar: {
      indicator: [{ name: '三班', max: 5 }, { name: '二班', max: 5 }, { name: '一班', max: 5 }],
      radius: 56, shape: 'polygon', axisName: { color: '#9cd3ff', fontSize: 11 },
      splitArea: { areaStyle: { color: ['rgba(9,19,38,.3)','rgba(9,19,38,.15)'] } }
    },
    series: [{ type: 'radar', data: [{ value: [4.2,4.5,3.8], areaStyle: { color: 'rgba(99,214,255,.2)' }, lineStyle: { color: '#63d6ff', width: 2 } }], symbol: 'circle', symbolSize: 5 }]
  })
}

function renderDeptPeople() {
  // People pentagon radar
  echarts.init(peopleRadarRef.value).setOption({
    radar: {
      indicator: [{ name: '优优率', max: 100 }, { name: '及格率', max: 100 }, { name: '粘强度', max: 100 }, { name: '创新度', max: 100 }, { name: '活跃度', max: 100 }],
      radius: 58, shape: 'polygon', axisName: { color: '#bfe0ff', fontSize: 11 },
      splitArea: { areaStyle: { color: ['rgba(9,19,38,.3)','rgba(9,19,38,.15)'] } }
    },
    series: [{ type: 'radar', data: [{ value: [85,92,78,81,88], areaStyle: { color: 'rgba(99,214,255,.18)' }, lineStyle: { color: '#63d6ff', width: 2 } }] }]
  })

  // Score trend scatter
  echarts.init(peopleScoreTrendRef.value).setOption({
    grid: { left: 36, right: 16, top: 30, bottom: 22 },
    xAxis: { type: 'category', data: Array.from({length:12},(_,i)=>`第${i+1}周`), ...axisStyleDark },
    yAxis: { type: 'value', min: 0, max: 5, ...axisStyleDark },
    series: [{ type: 'scatter', data: [[1,3.2],[2,3.5],[3,3.1],[4,3.8],[5,4.0],[6,3.9],[7,4.2],[8,4.1],[9,4.3],[10,4.4],[11,4.5],[12,4.6]], symbolSize: 8, itemStyle: { color: '#67e3ff' } }]
  })

  // Completion radar
  echarts.init(peopleCompletionRef.value).setOption({
    polar: { radius: ['25%','72%'] },
    angleAxis: { data: ['第1章','第2章','第3章','第4章','第5章','第6章','第7章','第8章','第9章','第10章','第11章','第12章'], ...axisStyleDark },
    radiusAxis: { ...axisStyleDark, max: 100 },
    series: [{ type: 'bar', coordinateSystem: 'polar', data: [95,88,92,76,84,90,78,86,94,82,88,91], itemStyle: { color: '#ef4444', borderRadius: [4,4,0,0] } }]
  })

  // Exam completion bars
  echarts.init(peopleExamRef.value).setOption({
    grid: { left: 36, right: 16, top: 30, bottom: 22 },
    xAxis: { type: 'category', data: ['第一次测验','第二次测验','第三次测验','第四次测验','第五次测验'], ...axisStyleDark },
    yAxis: { type: 'value', min: 0, max: 100, ...axisStyleDark },
    series: [{ type: 'bar', data: [85,78,92,88,95], itemStyle: { color: '#ef4444', borderRadius: [4,4,0,0] } }]
  })
}

function renderStudent() {
  echarts.init(studentCalendarRef.value).setOption({
    grid: { left: 46, right: 20, top: 60, bottom: 26 },
    xAxis: { type: 'category', data: Array.from({length:31}, (_,i)=>String(i+1)), ...axisStyleDark },
    yAxis: { type: 'category', data: ['四月'], ...axisStyleDark },
    series: [{ type: 'heatmap', data: Array.from({ length: 31 }, (_, i) => [i, 0, Math.floor(Math.random() * 5) + 1]), itemStyle: { color: (params) => params.value[2] > 3 ? '#22c55e' : params.value[2] > 1 ? '#facc15' : '#fef3c7' }, borderRadius: 3 }]
  })
  
  echarts.init(studentRadarRef.value).setOption({
    radar: { indicator: [{name:'学习',max:100},{name:'劳动',max:100},{name:'纪律',max:100},{name:'心理',max:100},{name:'创新',max:100},{name:'协作',max:100}], radius:58, axisName:{color:'#bfe0ff'},splitArea:{areaStyle:{color:['rgba(9,19,38,.35)','rgba(9,19,38,.18)']}} },
    series: [{type:'radar',data:[{value:[88,82,84,79,91,85],areaStyle:{color:'rgba(99,214,255,.18)'},lineStyle:{color:'#63d6ff'}}]}]
  })
}

async function renderCurrent() {
  await nextTick()
  if (tab.value === 'school') renderSchool()
  else if (tab.value === 'department-level') renderDeptLevel()
  else if (tab.value === 'department-people') renderDeptPeople()
  else if (tab.value === 'student') renderStudent()
}
watch(tab, () => renderCurrent())

onMounted(async () => {
  try {
    const res = await apiDatacenter()
    if (res.data.schoolView?.stats) schoolStats.value = res.data.schoolView.stats
    if (res.data.studentView?.studentName) studentName.value = res.data.studentView.studentName
    if (res.data.studentView?.profile) studentProfile.value = res.data.studentView.profile
    if (res.data.studentView?.honors) honors.value = res.data.studentView.honors
    studentStats.value = [
      { label: '当前课程', value: studentProfile.value.course||'-' },
      { label: '知识基础', value: studentProfile.value.knowledgeBase||'-' },
      { label: '认知风格', value: studentProfile.value.cognitiveStyle||'-' },
      { label: '考试目标', value: studentProfile.value.examGoal||'-' }
    ]
  } catch(e) { console.warn('DataCenter API unavailable') }
  await renderCurrent()

  window.addEventListener('resize', () => {
    const refs = [schoolMapRef,schoolRingRef,schoolBarRef,schoolLineRef,teacherPieRef,deptRadarRef,deptAvgRef,deptTrendRef,peopleRadarRef,peopleScoreTrendRef,peopleCompletionRef,peopleExamRef,studentCalendarRef,studentRadarRef]
    refs.forEach(r => { if(r.value) { const inst = echarts.getInstanceByDom(r.value); if(inst) inst.resize() } })
  })
})

function sendAiMsg() {
  if (!aiInputText.value.trim()) return
  aiMessages.push({ role: 'user', text: aiInputText.value })
  setTimeout(() => {
    aiMessages.push({ role: 'bot', text: `关于"${aiInputText.value}"的分析：根据当前数据分析，这是一个很好的问题。建议您从以下几个方面进一步了解...` })
    aiInputText.value = ''
  }, 600)
}
</script>

<style scoped>
.datacenter-page { min-height: calc(100vh - 108px); border-radius: 18px; padding: 14px; background: linear-gradient(135deg, #091227, #111d38); position: relative; overflow: hidden; }

/* Stars background */
.datacenter-page::before { content:''; position:absolute; inset:0; background-image: radial-gradient(2px 2px at 20px 30px, #ffffff08, transparent), radial-gradient(2px 2px at 70% 20%, #ffffff06, transparent), radial-gradient(1px 1px at 40% 70%, #ffffff08, transparent); pointer-events:none; }

.switch-bar { margin-bottom: 12px; display: flex; justify-content: flex-end; }
.screen-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.screen-title { margin: 0 auto; padding: 8px 30px; border-radius: 12px; border: 1px solid rgba(110,174,255,.2); background: rgba(12,24,48,.7); letter-spacing: 2px; font-weight: 700; font-size: 18px; color: #eaf4ff; }
.screen-actions { display: flex; gap: 8px; align-items: center; }
.search-pill input, .search-pill { height: 34px; padding: 0 14px; border-radius: 8px; border: none; background: rgba(255,255,255,.9); color: #475875; font-size: 12.5px; outline: none; }
.switch-btn { height: 34px; padding: 0 14px; border-radius: 8px; background: rgba(255,255,255,.85); color: #475875; cursor: pointer; font-size: 12.5px; border: none; transition: all .15s; }
.switch-btn:hover { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,.1); }

/* Stat boxes */
.top-stat-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 10px; margin-bottom: 12px; }
.stat-box { padding: 12px 14px; background: linear-gradient(145deg, rgba(20,40,80,.8), rgba(15,30,60,.6)); border: 1px solid rgba(110,174,255,.15); border-radius: 10px; }
.stat-box span { display: block; color: #8fb8ff; margin-bottom: 6px; font-size: 12px; }
.stat-box strong { font-size: 26px; color: #8be9ff; font-weight: 700; }

/* School grid */
.school-grid { display: grid; grid-template-columns: 1.4fr 1fr 1fr; gap: 10px; }
.large { grid-row: span 2; min-height: 500px; }
.chart { min-height: 240px; padding: 8px; background: rgba(12,26,52,.6); border: 1px solid rgba(110,174,255,.1); border-radius: 12px; }

/* Department Level */
.dept-top-row { display: grid; grid-template-columns: 240px 1fr 260px; gap: 10px; margin-bottom: 10px; }
.major-selector { padding: 14px; h3 { margin: 0 0 10px; color: #9cd3ff; font-size: 14px; cursor: default; } .major-select { width: 100%; background: rgba(12,26,52,.6); border: 1px solid rgba(110,174,255,.2); color: #dcecff; border-radius: 6px; padding: 6px 10px; outline: none; option { background: #1a2744; color: white; } } .major-nav { display: grid; gap: 4px; margin-top: 10px; } a { padding: 8px 12px; border-radius: 8px; color: #9cd3ff; text-decoration: none; font-size: 12.5px; transition: all .15s; background: rgba(20,40,80,.4); } a:hover, a.active { background: rgba(37,99,235,.2); color: #8be9ff; } }
.dept-stats { padding: 14px; .stat-grid-3col { display: grid; grid-template-columns: repeat(4,1fr); gap: 8px; margin-bottom: 12px; } .d-stat { padding: 8px; text-align: center; background: rgba(20,40,80,.4); border-radius: 8px; b { display: block; color: #8fb8ff; font-size: 11.5px; } strong { display: block; font-size: 22px; color: #8be9ff; font-weight: 700; } } .chart-inline { min-height: 160px; margin-top: 10px; } .chart-inline.sm { min-height: 140px; } }
.class-rates { padding: 12px; h4 { margin: 0 0 10px; color: #9cd3ff; font-size: 13px; } .rate-item { display: grid; grid-template-columns: auto 1fr auto; gap: 8px; align-items: center; margin-bottom: 6px; } .rate-label { color: #9cccff; font-size: 12px; } .rate-bar-wrap { height: 8px; background: rgba(20,40,80,.4); border-radius: 4px; overflow: hidden; } .rate-bar { height: 100%; background: #38bdf8; border-radius: 4px; transition: width .5s; } strong { color: #8be9ff; font-size: 14px; font-weight: 700; min-width: 36px; text-align: right; } }
.dept-bottom-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; } .half { min-height: 220px; } .third { min-height: 200px; }

/* Department People */
.people-top-row { display: grid; grid-template-columns: 320px 340px 360px; gap: 10px; margin-bottom: 10px; }
.people-stats { padding: 14px; .stats-2row { display: grid; gap: 6px; } .ps-row { display: grid; grid-template-columns: repeat(5,1fr); gap: 4px; } .ps-item { padding: 8px 6px; text-align: center; background: rgba(20,40,80,.4); border-radius: 8px; b { display: block; color: #8fb8ff; font-size: 11px; } em { display: block; font-size: 18px; color: #8be9ff; font-weight: 700; } } .chart-inline { min-height: 140px; margin-top: 8px; } }
.warn-levels { padding: 14px; h4 { margin: 0 0 10px; color: #fbbf24; font-size: 14px; } .warn-level-item { display: flex; align-items: center; gap: 8px; padding: 8px 10px; margin-bottom: 6px; background: rgba(251,191,36,.08); border-radius: 8px; border-left: 3px solid #f59e0b; } .wl-badge { padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; } .wl-badge.one { background: #fef3c7; color: #b45309; } .wl-badge.two { background: #fee2e2; color: #dc2626; } .wl-badge.three { background: #fecaca; color: #b91c1c; } .wl-badge.four { background: #fecaca; color: #991b1b; } .wl-text { color: #d1d5db; font-size: 12.5px; flex: 1; } }
.rank-table { padding: 14px; .rank-header { display: grid; grid-template-columns: repeat(3,auto); gap: 12px; margin-bottom: 10px; padding-bottom: 10px; border-bottom: 1px solid rgba(110,174,255,.1); b { color: #9cccff; font-size: 12px; } strong { color: #8be9ff; font-size: 16px; font-weight: 700; } } .rank-tbl { width: 100%; border-collapse: collapse; font-size: 12px; th { background: rgba(20,40,80,.5); color: #9cd3ff; padding: 6px 8px; text-align: left; } td { padding: 6px 8px; border-bottom: 1px solid rgba(110,174,255,.06); color: #d1d5db; } tr:hover td { background: rgba(37,99,235,.08); } } .progress-cell { display: flex; align-items: center; gap: 6px; } .progress-bar { height: 6px; background: rgba(20,40,80,.4); border-radius: 3px; min-width: 60px; } .progress-bar::after { content:''; display:block; height: 100%; background: linear-gradient(90deg, #22c55e, #10b981); border-radius: 3px; } }
.people-bottom-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }

/* Student */
.student-data-grid { display: grid; grid-template-columns: 280px 1fr 1fr 1fr; gap: 10px; }
.profile-section,.calendar-section,.radar-section,.honors-section { padding: 14px; } .profile-section h4,.honors-section h4 { margin: 0 0 10px; color: #9cd3ff; font-size: 14px; } .sp-info { color: #d9e9ff; line-height: 2; font-size: 13px; } .sp-info p { margin: 0; } .sp-info b { color: #8be9ff; } .honor-item { padding: 10px 12px; border-radius: 8px; background: rgba(124,178,255,.08); border: 1px solid rgba(124,178,255,.12); margin-bottom: 8px; } .honor-item strong { display: block; color: #8be9ff; font-size: 13px; } .honor-item span { color: #b7d2f4; font-size: 12px; }

/* Shared */
.lift-card { transition: transform .25s ease, box-shadow .25s ease; background: rgba(12,26,52,.5); border: 1px solid rgba(110,174,255,.1); }
.lift-card:hover { transform: translateY(-3px); box-shadow: 0 16px 40px rgba(0,0,0,.25); border-color: rgba(110,174,255,.25); }
.fade-up { animation: fadeUp .5s ease both; } .delay-1{animation-delay:.05s}.delay-2{animation-delay:.1s}.delay-3{animation-delay:.15s}.delay-4{animation-delay:.2s}.delay-5{animation-delay:.25s}
@keyframes fadeUp{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:translateY(0)}}
.chart.lift-chart { padding: 8px; }

/* Radio buttons override */
::deep(.el-radio-button__inner) { background: rgba(8,18,35,.75); border-color: rgba(110,174,255,.15); color: #dcecff; font-size: 12.5px; }
::deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) { background: #225bca; border-color: #225bca; color: #fff; }

/* AI Chat Sidebar */
.ai-chat-sidebar {
  position: fixed; right: 0; top: 0; bottom: 0;
  width: 340px; z-index: 999;
  background: linear-gradient(180deg, #eff6ff, #dbeafe);
  border-left: 1px solid #93c5fd; border-radius: 0;
  display: flex; flex-direction: column;
  box-shadow: -8px 0 32px rgba(0,0,0,.15);
}
.ai-header { padding: 14px 18px; display: flex; align-items: center; gap: 8px; background: #fff; border-bottom: 1px solid #e5e7eb; }
.ai-icon { font-size: 22px; }
.ai-header strong { font-size: 15px; color: #1e293b; font-weight: 600; }
.ai-messages { flex: 1; overflow-y: auto; padding: 12px; display: grid; gap: 10px; }
.ai-msg { display: flex; gap: 8px; align-items: flex-start; }
.ai-msg.user { justify-content: flex-end; }
.ai-avatar-sm { width: 28px; height: 28px; border-radius: 50%; flex-shrink: 0; }
.ai-bubble { max-width: 82%; padding: 10px 14px; border-radius: 14px; font-size: 13px; line-height: 1.7; word-break: break-word; }
.ai-msg.bot .ai-bubble { background: #fff; border: 1px solid #e5e7eb; color: #334155; border-top-left-radius: 4px; }
.ai-msg.user .ai-bubble { background: #3b82f6; color: #fff; border-top-right-radius: 4px; }
.ai-input-area { display: flex; gap: 8px; padding: 10px 14px; background: #fff; border-top: 1px solid #e5e7eb; }
.ai-input-area input { flex: 1; border: 1px solid #e5e7eb; border-radius: 20px; padding: 8px 14px; outline: none; font-size: 13px; }
.ai-input-area button { width: 36px; height: 36px; border-radius: 50%; border: none; background: #3b82f6; color: #fff; cursor: pointer; display: grid; place-items: center; }

.slide-right-enter-active, .slide-right-leave-active { transition: transform .3s ease; }
.slide-right-enter-from, .slide-right-leave-to { transform: translateX(100%); }

@media (max-width: 1200px) {
  .top-stat-row, .school-grid, .dept-top-row, .dept-bottom-row, .people-top-row, .people-bottom-row, .student-data-grid { grid-template-columns: 1fr; }
  .large { grid-row: auto; min-height: 300px; }
  .ai-chat-sidebar { width: 100%; position: fixed; inset: 0; border-radius: 0; z-index: 1000; }
}
</style>
