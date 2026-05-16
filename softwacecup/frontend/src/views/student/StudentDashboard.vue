<template>
  <div class="dashboard">
    <!-- Greeting -->
    <div class="greeting">
      <h2 class="greeting-title">欢迎回来，{{ userInfo.name }}</h2>
      <p class="greeting-sub">
        已连续学习 <strong class="hl-cyan">{{ userInfo.streak }} 天</strong> ·
        掌握 <strong class="hl-purple">{{ abilities.masteredCount || 0 }} 个</strong> 知识点 ·
        本周学习 <strong class="hl-blue">{{ stats.weeklyHours || 0 }} 小时</strong>
      </p>
    </div>

    <!-- Row 1: Continue Learning + Weekly Stats -->
    <div class="row-two">
      <div class="continue-card glass-card" v-if="continueCourse">
        <div class="cc-label">继续上次学习</div>
        <div class="cc-name">{{ continueCourse.courseName || continueCourse.name || '未命名课程' }}</div>
        <div class="cc-meta">{{ continueCourse.unitName || '' }} · 已完成 {{ continueCourse.progress || 0 }}%</div>
        <div class="cc-bar"><div class="cc-bar-fill" :style="{ width: (continueCourse.progress || 0) + '%' }"></div></div>
        <button class="cc-btn" @click="goContinue">继续学习</button>
      </div>

      <div class="stats-card glass-card">
        <div class="stats-label">本周学习</div>
        <div class="stats-row">
          <div class="stat"><span class="stat-num blue">{{ stats.lessons || 0 }}</span><span class="stat-label">完成课时</span></div>
          <div class="stat"><span class="stat-num purple">{{ stats.exercises || 0 }}</span><span class="stat-label">练习题</span></div>
          <div class="stat"><span class="stat-num cyan">{{ stats.accuracy || 0 }}%</span><span class="stat-label">正确率</span></div>
        </div>
      </div>
    </div>

    <!-- Row 2: AI Recommended Path -->
    <div class="path-card glass-card" v-if="pathSteps.length > 0">
      <div class="path-label">AI 推荐学习路径</div>
      <div class="path-steps">
        <div v-for="(step, i) in pathSteps" :key="i" class="path-step"
          :class="{ done: step.status === 'done', active: step.status === 'active' }">
          <div class="step-num">第 {{ i + 1 }} 步</div>
          <div class="step-name">{{ step.name }}</div>
          <div class="step-tag">{{ step.status === 'done' ? '已掌握' : step.status === 'active' ? '进行中' : '待学习' }}</div>
        </div>
      </div>
    </div>

    <!-- Row 3: Quick Entries -->
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { apiStudentDashboard, apiAbilityLatest, apiGamificationStreak } from '../../api'

const router = useRouter()

const userInfo = ref({ name: '同学', streak: 0 })
const abilities = ref({ masteredCount: 0 })
const stats = ref({ lessons: 0, exercises: 0, accuracy: 0 })
const continueCourse = ref(null)
const pathSteps = ref([])

onMounted(async () => {
  try {
    const [dashRes, abilityRes, streakRes] = await Promise.all([
      apiStudentDashboard(),
      apiAbilityLatest().catch(() => ({ data: {} })),
      apiGamificationStreak().catch(() => ({ data: {} }))
    ])
    const d = dashRes.data || {}
    userInfo.value = { name: d.userName || d.name || '同学', streak: streakRes.data?.streak || 0 }
    abilities.value = { masteredCount: abilityRes.data?.masteredCount || d.masteredCount || 0 }
    stats.value = { lessons: d.weeklyLessons || 0, exercises: d.weeklyExercises || 0, accuracy: d.accuracy || 0, weeklyHours: d.weeklyHours || 0 }

    if (d.continueLearning) {
      continueCourse.value = d.continueLearning
    } else if (d.lastLesson) {
      continueCourse.value = {
        courseName: d.lastCourseName || d.lastLesson.courseName,
        unitName: d.lastUnitName,
        lessonName: d.lastLesson.name,
        progress: d.lastLesson.progress || 0,
        lessonId: d.lastLesson.id
      }
    }

    if (d.recommendedPath?.length) {
      pathSteps.value = d.recommendedPath
    }
  } catch (e) {
    // Silent degradation
  }
})

function goContinue() {
  const c = continueCourse.value
  if (c?.lessonId) router.push(`/student/lessons/${c.lessonId}`)
  else if (c?.id) router.push(`/student/courses/${c.id}`)
  else router.push('/student/subjects')
}
</script>

<style scoped>
.dashboard { padding: 24px; max-width: 960px; margin: 0 auto; }

.greeting { margin-bottom: 20px; }
.greeting-title { font-size: 22px; font-weight: 700; }
.greeting-sub { font-size: 12px; color: rgba(255,255,255,0.45); margin-top: 4px; }
.hl-cyan { color: #60d9fa; font-weight: 600; }
.hl-purple { color: #a855f7; font-weight: 600; }
.hl-blue { color: #3b82f6; font-weight: 600; }

.row-two { display: flex; gap: 16px; margin-bottom: 16px; }

.continue-card { flex: 1.5; padding: 20px; background: linear-gradient(135deg, rgba(59,130,246,0.12), rgba(168,85,247,0.08)); border-color: rgba(59,130,246,0.2); }
.cc-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255,255,255,0.35); margin-bottom: 8px; }
.cc-name { font-size: 18px; font-weight: 700; margin-bottom: 4px; }
.cc-meta { font-size: 12px; color: rgba(255,255,255,0.5); margin-bottom: 12px; }
.cc-bar { height: 4px; background: rgba(255,255,255,0.06); border-radius: 2px; margin-bottom: 12px; }
.cc-bar-fill { height: 100%; background: linear-gradient(90deg, #3b82f6, #60d9fa); border-radius: 2px; transition: width 0.3s; }
.cc-btn { padding: 8px 20px; border-radius: 8px; border: none; background: linear-gradient(135deg,#3b82f6,#2563eb); color: #fff; font-size: 13px; cursor: pointer; font-family: inherit; }

.stats-card { flex: 1; padding: 20px; }
.stats-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255,255,255,0.35); margin-bottom: 12px; }
.stats-row { display: flex; justify-content: space-between; }
.stat { text-align: center; }
.stat-num { display: block; font-size: 24px; font-weight: 800; }
.stat-num.blue { color: #3b82f6; }
.stat-num.purple { color: #a855f7; }
.stat-num.cyan { color: #60d9fa; }
.stat-label { font-size: 10px; color: rgba(255,255,255,0.4); display: block; margin-top: 2px; }

.path-card { padding: 20px; margin-bottom: 16px; }
.path-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255,255,255,0.35); margin-bottom: 12px; }
.path-steps { display: flex; gap: 12px; overflow-x: auto; }
.path-step {
  min-width: 160px; padding: 12px; border-radius: 8px;
  background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.06);
}
.path-step.done { background: rgba(34,197,94,0.06); border-color: rgba(34,197,94,0.15); }
.path-step.active { background: rgba(59,130,246,0.08); border-color: rgba(59,130,246,0.2); }
.step-num { font-size: 10px; color: rgba(255,255,255,0.3); }
.step-name { font-size: 13px; font-weight: 600; margin: 4px 0; }
.step-tag { font-size: 10px; }
.path-step.done .step-tag { color: #22c55e; }
.path-step.active .step-tag { color: #eab308; }
.path-step:not(.done):not(.active) .step-tag { color: rgba(255,255,255,0.3); }

.quick-row { display: flex; gap: 16px; }
.quick-card {
  flex: 1; padding: 16px; text-align: center; cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
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
