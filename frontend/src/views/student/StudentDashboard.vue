<template>
  <div class="dashboard">
    <!-- ═══ 顶部六维横幅 ═══ -->
    <div class="glass-card ability-banner">
      <!-- 新用户 — 无六维数据 -->
      <div v-if="isNewUser" class="banner-empty">
        <div class="empty-icon">+</div>
        <div class="empty-title">开始你的学习之旅</div>
        <div class="empty-desc">完成课程后，六维能力图将自动生成</div>
        <button class="empty-btn" @click="$router.push('/student/subjects')">前往学习</button>
      </div>

      <!-- 已学习用户 — 雷达图 + 分数条 -->
      <template v-else>
        <div class="banner-left">
          <div ref="radarRef" class="radar-chart"></div>
        </div>
        <div class="banner-right">
          <div class="ability-label">能力六维</div>
          <div class="ability-grid">
            <div v-for="dim in dimensions" :key="dim.key" class="ability-row">
              <span class="dim-name">{{ dim.label }}</span>
              <div class="dim-bar"><div class="dim-fill" :style="{ width: dim.value + '%', background: dim.color }"></div></div>
              <span class="dim-val" :style="{ color: dim.color }">{{ dim.value }}</span>
            </div>
          </div>
          <div v-if="diagnosis" class="diagnosis-box">
            <div class="diagnosis-label">AI 诊断建议</div>
            <div class="diagnosis-text">{{ diagnosis }}</div>
          </div>
        </div>
      </template>
    </div>

    <!-- ═══ 继续学习 / 前往学习 + 周统计 ═══ -->
    <div class="row-two">
      <!-- 老用户：继续上次学习 -->
      <div v-if="!isNewUser && continueCourse" class="continue-card glass-card">
        <div class="cc-label">继续上次学习</div>
        <div class="cc-name">{{ continueCourse.courseName || continueCourse.name || '未命名课程' }}</div>
        <div class="cc-meta">{{ continueCourse.chapterName || continueCourse.unitName || '' }} · 已完成 {{ continueCourse.progress || 0 }}%</div>
        <div class="cc-bar"><div class="cc-bar-fill" :style="{ width: (continueCourse.progress || 0) + '%' }"></div></div>
        <button class="cc-btn" @click="goContinue">继续学习</button>
      </div>
      <!-- 新用户：前往学习 -->
      <div v-else class="go-learn-card glass-card">
        <div class="cc-label">开始学习</div>
        <div class="cc-name">探索知识宇宙</div>
        <div class="cc-meta">选择一门学科，开启你的学习之旅</div>
        <button class="go-learn-btn" @click="$router.push('/student/subjects')">前往学习</button>
      </div>

      <div class="stats-card glass-card">
        <div class="stats-label">本周学习</div>
        <div class="stats-row">
          <div class="stat"><span class="stat-num blue">{{ stats.lessons || 0 }}</span><span class="stat-desc">完成课时</span></div>
          <div class="stat"><span class="stat-num purple">{{ stats.exercises || 0 }}</span><span class="stat-desc">练习题</span></div>
          <div class="stat"><span class="stat-num cyan">{{ stats.accuracy || 0 }}%</span><span class="stat-desc">正确率</span></div>
        </div>
      </div>
    </div>

    <!-- ═══ AI 推荐学习路径 ═══ -->
    <div class="path-card glass-card">
      <div class="path-label">AI 推荐学习路径</div>
      <div v-if="pathSteps.length > 0" class="path-steps">
        <div v-for="(step, i) in pathSteps" :key="i" class="path-step"
          :class="{ done: step.status === 'done', active: step.status === 'active' }"
          @click="goPathStep(step)">
          <div class="step-num">第 {{ i + 1 }} 步</div>
          <div class="step-name">{{ step.name }}</div>
          <div class="step-tag">{{ step.status === 'done' ? '已掌握' : step.status === 'active' ? '进行中' : '待学习' }}</div>
        </div>
      </div>
      <div v-else class="path-empty">完成第一门课程后，AI 将为你规划学习路径</div>
    </div>

    <!-- ═══ 快捷入口 ═══ -->
    <div class="quick-row">
      <div class="quick-card glass-card" @click="$router.push('/student/profile?tab=quiz')">
        <div class="qc-title">自适应测验</div>
        <div class="qc-desc">检测当前掌握度</div>
      </div>
      <div class="quick-card glass-card" @click="$router.push('/student/profile?tab=achievement')">
        <div class="qc-title">今日挑战</div>
        <div class="qc-desc">连续 {{ userInfo.streak }} 天签到</div>
      </div>
      <div class="quick-card glass-card" @click="$router.push('/student/profile?tab=analytics')">
        <div class="qc-title">学习报告</div>
        <div class="qc-desc">查看详细分析</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { apiStudentDashboard, apiAbilityLatest, apiAbilityEvaluate, apiGamificationStreak } from '../../api'

const router = useRouter()
const radarRef = ref(null)

const userInfo = ref({ name: '同学', streak: 0 })
const stats = ref({ lessons: 0, exercises: 0, accuracy: 0 })
const continueCourse = ref(null)
const pathSteps = ref([])
const diagnosis = ref('')
const isNewUser = ref(false)

const dimensions = reactive([
  { key: 'breadth', label: '知识广度', value: 0, color: '#3b82f6' },
  { key: 'depth', label: '知识深度', value: 0, color: '#a855f7' },
  { key: 'problem', label: '解题能力', value: 0, color: '#60d9fa' },
  { key: 'activity', label: '活跃度', value: 0, color: '#f59e0b' },
  { key: 'transfer', label: '知识迁移', value: 0, color: '#22c55e' },
  { key: 'resilience', label: '学习韧性', value: 0, color: '#ef4444' },
])

const dimValues = computed(() => dimensions.map(d => d.value))

onMounted(async () => {
  try {
    // 先触发评估，确保用最新真实数据生成六维图
    const evalRes = await apiAbilityEvaluate().catch((e) => { console.warn('[Dashboard] ability evaluate failed:', e.message) })
    console.log('[Dashboard] ability evaluate result:', evalRes)

    const [dashRes, abilityRes, streakRes] = await Promise.all([
      apiStudentDashboard().catch((e) => { console.error('[Dashboard] studentDashboard API failed:', e.message); return { data: {} } }),
      apiAbilityLatest().catch((e) => { console.warn('[Dashboard] ability latest failed:', e.message); return { data: null } }),
      apiGamificationStreak().catch((e) => { console.warn('[Dashboard] streak failed:', e.message); return { data: {} } })
    ])
    console.log('[Dashboard] dashRes:', dashRes)
    console.log('[Dashboard] abilityRes:', abilityRes)

    const d = dashRes.data || {}
    console.log('[Dashboard] dashboard data:', { continueLearning: d.continueLearning, lastLesson: d.lastLesson, recommendedPath: d.recommendedPath })

    userInfo.value = { name: d.userName || d.name || '同学', streak: streakRes.data?.streak || 0 }
    stats.value = { lessons: d.weeklyLessons || 0, exercises: d.weeklyExercises || 0, accuracy: d.accuracy || 0 }

    // Ability data
    const ability = abilityRes.data
    console.log('[Dashboard] ability data:', ability, 'isAllZero:', ability ? isAllZero(ability) : 'no data')
    if (ability && !isAllZero(ability)) {
      isNewUser.value = false
      dimensions[0].value = ability.breadthScore || 0
      dimensions[1].value = ability.depthScore || 0
      dimensions[2].value = ability.problemScore || 0
      dimensions[3].value = ability.activityScore || 0
      dimensions[4].value = ability.transferScore || 0
      dimensions[5].value = ability.resilienceScore || 0
      diagnosis.value = ability.diagnosis || ''
      await nextTick()
      initRadarChart()
    } else {
      isNewUser.value = true
      console.log('[Dashboard] showing new user state')
    }

    // Continue learning
    if (d.continueLearning) {
      console.log('[Dashboard] setting continueCourse from continueLearning:', d.continueLearning)
      continueCourse.value = d.continueLearning
    } else if (d.lastLesson) {
      console.log('[Dashboard] setting continueCourse from lastLesson:', d.lastLesson)
      continueCourse.value = {
        courseName: d.lastCourseName || d.lastLesson.courseName || '',
        chapterName: d.lastChapterName || d.lastLesson.chapterName || '',
        subChapterTitle: d.lastLesson.name || d.lastLesson.subChapterTitle || '',
        progress: d.lastLesson.progress || 0,
        courseId: d.lastLesson.courseId || d.lastLesson.id,
        subChapterId: d.lastLesson.subChapterId || d.lastLesson.lessonId
      }
    } else {
      console.log('[Dashboard] no continueLearning or lastLesson data')
    }

    if (d.recommendedPath?.length) {
      pathSteps.value = d.recommendedPath
    }
  } catch (e) {
    console.error('[Dashboard] onMounted error:', e)
    isNewUser.value = true
  }
})

function isAllZero(a) {
  return !a.breadthScore && !a.depthScore && !a.problemScore
    && !a.activityScore && !a.transferScore && !a.resilienceScore
}

async function initRadarChart() {
  const el = radarRef.value
  if (!el) return
  try {
    let echarts = window.echarts
    if (!echarts) {
      const mod = await import('echarts')
      echarts = mod.default || mod
    }
    const chart = echarts.init(el)
    chart.setOption({
      radar: {
        center: ['50%', '50%'],
        radius: '70%',
        indicator: dimensions.map(d => ({ name: d.label, max: 100 })),
        axisName: { color: 'rgba(255,255,255,0.35)', fontSize: 10 },
        splitArea: { areaStyle: { color: ['rgba(59,130,246,0.02)', 'rgba(59,130,246,0.04)'] } },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } },
        axisLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } }
      },
      series: [{
        type: 'radar',
        data: [{ value: dimValues.value, name: '能力值', areaStyle: { color: 'rgba(59,130,246,0.15)' } }],
        lineStyle: { color: '#3b82f6', width: 1.5 },
        itemStyle: { color: '#60d9fa' },
        symbol: 'circle', symbolSize: 4
      }]
    })
  } catch (e) { /* echarts not loaded */ }
}

function goContinue() {
  const c = continueCourse.value
  if (c?.courseId && c?.subChapterId) {
    router.push(`/student/courses/${c.courseId}?sc=${c.subChapterId}`)
  } else if (c?.courseId) {
    router.push(`/student/courses/${c.courseId}`)
  } else if (c?.subChapterId) {
    router.push(`/student/courses/0?sc=${c.subChapterId}`)
  } else {
    router.push('/student/subjects')
  }
}

function goPathStep(step) {
  if (step.courseId && step.subChapterId) {
    router.push(`/student/courses/${step.courseId}?sc=${step.subChapterId}`)
  } else if (step.courseId) {
    router.push(`/student/courses/${step.courseId}`)
  }
}
</script>

<style scoped>
.dashboard { padding: 24px; max-width: 960px; margin: 0 auto; }

/* ═══ 六维横幅 ═══ */
.ability-banner { padding: 20px; margin-bottom: 16px; display: flex; gap: 24px; align-items: stretch; }

/* 新用户空态 */
.banner-empty { display: flex; flex-direction: column; align-items: center; text-align: center; width: 100%; padding: 20px 0; }
.empty-icon { font-size: 40px; opacity: 0.15; margin-bottom: 8px; }
.empty-title { font-size: 15px; font-weight: 600; color: rgba(255,255,255,0.5); margin-bottom: 4px; }
.empty-desc { font-size: 11px; color: rgba(255,255,255,0.3); margin-bottom: 12px; }
.empty-btn {
  padding: 8px 24px; border-radius: 8px; border: none; background: linear-gradient(135deg,#3b82f6,#2563eb);
  color: #fff; font-size: 13px; cursor: pointer; font-family: inherit;
}

/* 雷达图 */
.banner-left { width: 280px; min-width: 280px; height: 280px; }
.radar-chart { width: 100%; height: 100%; }

.banner-right { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 8px; }
.ability-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255,255,255,0.3); }
.ability-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 5px 16px; }
.ability-row { display: flex; align-items: center; gap: 8px; }
.dim-name { font-size: 11px; color: rgba(255,255,255,0.5); width: 48px; flex-shrink: 0; }
.dim-bar { flex: 1; height: 4px; background: rgba(255,255,255,0.06); border-radius: 2px; overflow: hidden; }
.dim-fill { height: 100%; border-radius: 2px; transition: width 0.6s ease; }
.dim-val { font-size: 11px; font-weight: 600; width: 24px; text-align: right; }

.diagnosis-box { margin-top: 4px; padding: 10px 12px; background: rgba(59,130,246,0.06); border-radius: 8px; border-left: 2px solid rgba(59,130,246,0.3); }
.diagnosis-label { font-size: 10px; color: rgba(255,255,255,0.3); margin-bottom: 2px; }
.diagnosis-text { font-size: 11px; color: rgba(255,255,255,0.55); line-height: 1.5; }

/* ═══ 继续学习 / 前往学习 ═══ */
.row-two { display: flex; gap: 16px; margin-bottom: 16px; }
.continue-card {
  flex: 1.5; padding: 20px;
  background: linear-gradient(135deg, rgba(59,130,246,0.12), rgba(168,85,247,0.08));
  border-color: rgba(59,130,246,0.2);
}
.go-learn-card {
  flex: 1.5; padding: 20px;
  background: linear-gradient(135deg, rgba(34,197,94,0.08), rgba(16,185,129,0.04));
  border-color: rgba(34,197,94,0.2);
}
.cc-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255,255,255,0.35); margin-bottom: 8px; }
.cc-name { font-size: 18px; font-weight: 700; margin-bottom: 4px; }
.cc-meta { font-size: 12px; color: rgba(255,255,255,0.5); margin-bottom: 12px; }
.cc-bar { height: 4px; background: rgba(255,255,255,0.06); border-radius: 2px; margin-bottom: 12px; }
.cc-bar-fill { height: 100%; background: linear-gradient(90deg, #3b82f6, #60d9fa); border-radius: 2px; transition: width 0.3s; }
.cc-btn {
  padding: 8px 20px; border-radius: 8px; border: none; background: linear-gradient(135deg,#3b82f6,#2563eb);
  color: #fff; font-size: 13px; cursor: pointer; font-family: inherit;
}
.go-learn-btn {
  padding: 8px 20px; border-radius: 8px; border: none; background: linear-gradient(135deg,#22c55e,#16a34a);
  color: #fff; font-size: 13px; cursor: pointer; font-family: inherit;
}

.stats-card { flex: 1; padding: 20px; }
.stats-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255,255,255,0.35); margin-bottom: 12px; }
.stats-row { display: flex; justify-content: space-between; }
.stat { text-align: center; }
.stat-num { display: block; font-size: 24px; font-weight: 800; }
.stat-num.blue { color: #3b82f6; }
.stat-num.purple { color: #a855f7; }
.stat-num.cyan { color: #60d9fa; }
.stat-desc { font-size: 10px; color: rgba(255,255,255,0.4); display: block; margin-top: 2px; }

/* ═══ AI 路径 ═══ */
.path-card { padding: 20px; margin-bottom: 16px; }
.path-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255,255,255,0.35); margin-bottom: 12px; }
.path-steps { display: flex; gap: 12px; overflow-x: auto; }
.path-step { min-width: 160px; padding: 12px; border-radius: 8px; background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.06); cursor: pointer; transition: background 0.15s; }
.path-step:hover { background: rgba(255,255,255,0.05); }
.path-step.done { background: rgba(34,197,94,0.06); border-color: rgba(34,197,94,0.15); }
.path-step.active { background: rgba(59,130,246,0.08); border-color: rgba(59,130,246,0.2); }
.step-num { font-size: 10px; color: rgba(255,255,255,0.3); }
.step-name { font-size: 13px; font-weight: 600; margin: 4px 0; }
.step-tag { font-size: 10px; }
.path-step.done .step-tag { color: #22c55e; }
.path-step.active .step-tag { color: #eab308; }
.path-step:not(.done):not(.active) .step-tag { color: rgba(255,255,255,0.3); }
.path-empty { font-size: 11px; color: rgba(255,255,255,0.25); text-align: center; padding: 16px; }

/* ═══ 快捷入口 ═══ */
.quick-row { display: flex; gap: 16px; }
.quick-card { flex: 1; padding: 16px; text-align: center; cursor: pointer; transition: border-color 0.15s, background 0.15s; }
.quick-card:hover { border-color: rgba(255,255,255,0.12); background: rgba(255,255,255,0.05); }
.qc-title { font-weight: 600; font-size: 13px; }
.qc-desc { font-size: 10px; color: rgba(255,255,255,0.4); margin-top: 4px; }

.glass-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  backdrop-filter: blur(12px);
}
</style>
