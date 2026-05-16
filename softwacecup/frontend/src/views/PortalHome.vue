<template>
  <div class="portal-root">
    <TopNavBar />
    <div class="portal-inner">
      <!-- ═══ ① Hero + ② 角色分流 ═══ -->
      <div class="hero-row">
        <!-- Hero 卡片 -->
        <div class="hero-card glass-card" :class="{ 'hero-full': auth.isLoggedIn }">
          <!-- 未登录 -->
          <template v-if="!auth.isLoggedIn">
            <div class="hero-left">
              <h1 class="hero-title">探索知识宇宙</h1>
              <p class="hero-sub">AI驱动的个性化学习平台</p>
              <div class="hero-actions">
                <button class="cta-btn" @click="handleEnter">开始学习</button>
                <button class="cta-btn secondary" @click="scrollToSubjects">了解知域</button>
              </div>
            </div>
            <div class="hero-visual">
              <div class="orbit-ring ring-1"></div>
              <div class="orbit-ring ring-2"></div>
              <div class="orbit-ring ring-3"></div>
              <div class="center-orb">知</div>
            </div>
          </template>
          <!-- 已登录 -->
          <template v-else>
            <div class="hero-left">
              <h1 class="hero-greet">欢迎回来，{{ auth.user?.username }}</h1>
              <p class="hero-stat">
                已连续学习 <strong>{{ streakDays }}</strong> 天 · 掌握 <strong>{{ masteredKps }}</strong> 个知识点
              </p>
              <div class="hero-actions">
                <button class="cta-btn" @click="goContinue">继续上次学习</button>
                <button class="cta-btn secondary" @click="goSubjects">浏览课程</button>
                <button class="cta-btn secondary" @click="goKnowledgeMap">知识星图</button>
              </div>
            </div>
            <div class="hero-visual">
              <div class="orbit-ring ring-1"></div>
              <div class="orbit-ring ring-2"></div>
              <div class="orbit-ring ring-3"></div>
              <div class="center-orb">知</div>
            </div>
          </template>
        </div>

        <!-- ② 角色分流卡（仅未登录） -->
        <div v-if="!auth.isLoggedIn" class="role-cards">
          <div class="role-card glass-card student" @click="quickEnter('student')">
            <span class="role-label">我是学生</span>
            <span class="role-link">进入学习 →</span>
          </div>
          <div class="role-card glass-card teacher" @click="quickEnter('teacher')">
            <span class="role-label">我是教师</span>
            <span class="role-link">进入教学 →</span>
          </div>
        </div>
      </div>

      <!-- ═══ ③ 核心内容 ═══ -->
      <div class="core-section">
        <!-- 已登录：继续学习 + AI推荐 -->
        <div v-if="auth.isLoggedIn" class="dashboard-row">
          <div class="glass-card continue-card" @click="goContinue">
            <p class="card-overline">继续上次学习</p>
            <h3 class="card-main" v-if="lastLesson">{{ lastLesson.courseName }}</h3>
            <p class="card-sub" v-if="lastLesson">{{ lastLesson.lessonName }} · 第 {{ lastLesson.orderIndex }} 课时</p>
            <div class="progress-bar" v-if="lastLesson">
              <div class="progress-fill" :style="{ width: (lastLesson.progress || 0) + '%' }"></div>
            </div>
            <p class="progress-text" v-if="lastLesson">{{ lastLesson.progress || 0 }}%</p>
            <p class="card-empty" v-if="!lastLesson">暂无学习记录</p>
          </div>
          <div class="glass-card ai-rec-card">
            <p class="card-overline">AI 为你推荐</p>
            <ul class="rec-list" v-if="recommendations.length">
              <li v-for="(r, i) in recommendations" :key="i" class="rec-item">
                <span class="rec-title">{{ r.title }}</span>
                <span class="rec-source">{{ r.source }} {{ r.duration ? '丨 ' + r.duration : '' }}</span>
              </li>
            </ul>
            <p class="card-empty" v-else>完成学习后获取个性化推荐</p>
          </div>
        </div>

        <!-- 探索学科 -->
        <h2 class="section-title" ref="subjectsAnchor">探索学科</h2>
        <div class="subject-grid" v-if="subjects.length">
          <div
            v-for="(s, idx) in displaySubjects"
            :key="s.id"
            class="glass-card subject-card"
            @click="handleSubjectClick(s)"
          >
            <div class="subject-accent" :style="{ background: accentGradients[idx % accentGradients.length] }"></div>
            <div class="subject-info">
              <span class="subject-name">{{ s.name }}</span>
              <span class="subject-count">{{ s.courseCount || 0 }} 门课程</span>
            </div>
            <span class="subject-arrow">→</span>
          </div>
        </div>
        <p v-else class="empty-hint">暂无学科数据</p>
      </div>

      <!-- ═══ ④ 底部引导（仅未登录） ═══ -->
      <div v-if="!auth.isLoggedIn" class="bottom-cta">
        <h2 class="cta-heading">准备好开始了吗？</h2>
        <p class="cta-desc">加入知域，开启你的个性化学习之旅</p>
        <button class="cta-btn large" @click="handleEnter">免费注册</button>
        <p class="cta-login-link">已有账号？<span @click="auth.openLoginModal()">登录</span></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import TopNavBar from '../components/TopNavBar.vue'
import { apiSubjects, apiAbilityLatest, apiRecommendResources, apiGamificationStreak } from '../api/index.js'

const auth = useAuthStore()
const router = useRouter()

const subjects = ref([])
const lastLesson = ref(null)
const recommendations = ref([])
const streakDays = ref(0)
const masteredKps = ref(0)
const subjectsAnchor = ref(null)

const accentGradients = [
  'linear-gradient(135deg, #3b82f6, #60a5fa)',
  'linear-gradient(135deg, #a855f7, #7c3aed)',
  'linear-gradient(135deg, #06b6d4, #22d3ee)',
  'linear-gradient(135deg, #10b981, #34d399)',
  'linear-gradient(135deg, #f59e0b, #fbbf24)',
  'linear-gradient(135deg, #ef4444, #f87171)',
  'linear-gradient(135deg, #8b5cf6, #a78bfa)',
  'linear-gradient(135deg, #3b82f6, #06b6d4)',
]

const displaySubjects = computed(() => subjects.value.slice(0, 8))

async function loadSubjects() {
  try {
    const res = await apiSubjects()
    const data = res.data?.data
    subjects.value = Array.isArray(data) ? data : (data?.subjects || data?.list || [])
  } catch {
    subjects.value = []
  }
}

async function loadAuthData() {
  if (!auth.isLoggedIn) return
  try {
    const [abilityRes, streakRes] = await Promise.allSettled([
      apiAbilityLatest(),
      apiGamificationStreak(),
    ])
    if (abilityRes.status === 'fulfilled') {
      const d = abilityRes.value.data?.data || abilityRes.value.data || {}
      masteredKps.value = d.masteredKps || d.mastered_kps || d.totalMastered || 0
      if (d.lastLesson) {
        lastLesson.value = {
          courseName: d.lastLesson.courseName || d.lastLesson.course_name,
          lessonName: d.lastLesson.lessonName || d.lastLesson.lesson_name,
          orderIndex: d.lastLesson.orderIndex || d.lastLesson.order_index,
          lessonId: d.lastLesson.lessonId || d.lastLesson.lesson_id || d.lastLesson.id,
          progress: d.lastLesson.progress || 0,
        }
        loadRecommendations(lastLesson.value.lessonId)
      }
    }
    if (streakRes.status === 'fulfilled') {
      const d = streakRes.value.data?.data || streakRes.value.data || {}
      streakDays.value = d.currentStreak || d.current_streak || d.streak || 0
    }
  } catch {}
}

async function loadRecommendations(lessonId) {
  try {
    const res = await apiRecommendResources(lessonId)
    const data = res.data?.data || res.data || {}
    const list = data.resources || data.recommendations || data.list || []
    recommendations.value = list.slice(0, 4)
  } catch {
    recommendations.value = []
  }
}

function handleEnter() {
  if (auth.isLoggedIn) {
    router.push('/student/dashboard')
  } else {
    auth.openLoginModal('/student/dashboard')
  }
}

function quickEnter(role) {
  auth.openLoginModal(role === 'student' ? '/student/dashboard' : '/teacher/dashboard')
}

function scrollToSubjects() {
  subjectsAnchor.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function goContinue() { if (lastLesson.value) router.push(`/lessons/${lastLesson.value.lessonId}`) }
function goSubjects() { router.push('/subjects') }
function goKnowledgeMap() { router.push('/student/knowledge-map') }
function handleSubjectClick(s) {
  const target = `/subjects/${s.id}`
  auth.isLoggedIn ? router.push(target) : auth.openLoginModal(target)
}

onMounted(() => {
  loadSubjects()
  loadAuthData()
})
</script>

<style scoped>
.portal-root {
  min-height: 100vh;
  color: #e6edf3;
  padding: 80px 24px 40px;
}
.portal-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

/* ═══ Glass card base ═══ */
.glass-card {
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  background: rgba(255,255,255,0.04);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  transition: all var(--transition-base);
}

/* ═══ ① Hero Row ═══ */
.hero-row {
  display: flex;
  gap: 20px;
  align-items: stretch;
  min-height: 220px;
}

.hero-card {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 40px 44px;
  gap: 40px;
  transition: flex var(--transition-slow);
}
.hero-card.hero-full {
  flex: 1;
}

.hero-left {
  max-width: 480px;
}

.hero-title {
  font-size: 42px;
  font-weight: 800;
  margin: 0 0 8px;
  line-height: 1.15;
  background: linear-gradient(135deg, #e6edf3, #60a5fa, #06b6d4);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.hero-greet {
  font-size: 30px;
  font-weight: 700;
  color: #e6edf3;
  margin: 0 0 8px;
  line-height: 1.3;
}
.hero-sub {
  font-size: 15px;
  color: rgba(255,255,255,0.45);
  margin: 0 0 24px;
  letter-spacing: 2px;
}
.hero-stat {
  font-size: 14px;
  color: rgba(255,255,255,0.45);
  margin: 0 0 24px;
}
.hero-stat strong {
  color: #60a5fa;
  font-weight: 700;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.cta-btn {
  padding: 9px 24px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 16px rgba(59,130,246,0.3);
}
.cta-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(59,130,246,0.45);
}
.cta-btn.secondary {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.12);
  box-shadow: none;
  color: rgba(255,255,255,0.7);
}
.cta-btn.secondary:hover {
  background: rgba(255,255,255,0.1);
  color: #e6edf3;
  box-shadow: 0 4px 16px rgba(0,0,0,0.2);
}
.cta-btn.large {
  padding: 12px 40px;
  font-size: 16px;
}

/* ═══ Orbit animation ═══ */
.hero-visual {
  width: 180px;
  height: 170px;
  position: relative;
  flex-shrink: 0;
}
.orbit-ring {
  position: absolute;
  border-radius: 50%;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border: 1px solid rgba(255,255,255,0.06);
  pointer-events: none;
}
.ring-1 { width: 160px; height: 160px; animation: spin 20s linear infinite; }
.ring-2 { width: 115px; height: 115px; animation: spin 14s linear infinite reverse; }
.ring-3 { width: 68px; height: 68px; animation: spin 10s linear infinite; }
@keyframes spin {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to { transform: translate(-50%, -50%) rotate(360deg); }
}
.center-orb {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 800;
  color: white;
  box-shadow: 0 0 24px rgba(59,130,246,0.35);
  z-index: 2;
}

/* ═══ ② Role cards ═══ */
.role-cards {
  width: 170px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex-shrink: 0;
}
.role-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px 12px;
  cursor: pointer;
}
.role-card:hover {
  border-color: rgba(255,255,255,0.18);
  transform: translateY(-1px);
}
.role-card.student:hover {
  border-color: rgba(59,130,246,0.4);
  box-shadow: 0 4px 16px rgba(59,130,246,0.15);
}
.role-card.teacher:hover {
  border-color: rgba(168,85,247,0.4);
  box-shadow: 0 4px 16px rgba(168,85,247,0.15);
}

.role-label {
  font-size: 13px;
  color: rgba(255,255,255,0.45);
}
.role-link {
  font-size: 12px;
  font-weight: 600;
  color: rgba(255,255,255,0.55);
}

/* ═══ ③ Core section ═══ */
.core-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dashboard-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.continue-card, .ai-rec-card {
  padding: 24px 28px;
  cursor: default;
}
.continue-card {
  cursor: pointer;
}
.continue-card:hover {
  border-color: rgba(59,130,246,0.25);
  background: rgba(59,130,246,0.04);
}

.card-overline {
  font-size: 11px;
  color: rgba(255,255,255,0.35);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin: 0 0 8px;
}
.card-main {
  font-size: 16px;
  font-weight: 700;
  color: #e6edf3;
  margin: 0 0 4px;
}
.card-sub {
  font-size: 12px;
  color: rgba(255,255,255,0.4);
  margin: 0 0 12px;
}
.progress-bar {
  height: 4px;
  border-radius: 2px;
  background: rgba(255,255,255,0.08);
  overflow: hidden;
  margin-bottom: 6px;
}
.progress-fill {
  height: 100%;
  border-radius: 2px;
  background: linear-gradient(90deg, #3b82f6, #06b6d4);
  transition: width 0.4s ease;
}
.progress-text {
  font-size: 12px;
  font-weight: 600;
  color: rgba(255,255,255,0.4);
  margin: 0;
}
.card-empty {
  font-size: 13px;
  color: rgba(255,255,255,0.25);
  margin: 0;
}

.rec-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.rec-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.rec-title {
  font-size: 13px;
  color: #e6edf3;
  line-height: 1.4;
}
.rec-source {
  font-size: 11px;
  color: rgba(255,255,255,0.35);
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #e6edf3;
  margin: 8px 0 4px;
}

.subject-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.subject-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  cursor: pointer;
}
.subject-card:hover {
  border-color: rgba(255,255,255,0.15);
  transform: translateY(-1px);
  background: rgba(255,255,255,0.06);
}

.subject-accent {
  width: 6px;
  height: 36px;
  border-radius: 3px;
  flex-shrink: 0;
}

.subject-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.subject-name {
  font-size: 14px;
  font-weight: 600;
  color: #e6edf3;
}
.subject-count {
  font-size: 11px;
  color: rgba(255,255,255,0.35);
}
.subject-arrow {
  font-size: 14px;
  color: rgba(255,255,255,0.2);
  transition: color 0.2s;
  flex-shrink: 0;
}
.subject-card:hover .subject-arrow {
  color: #60a5fa;
}

.empty-hint {
  text-align: center;
  color: rgba(255,255,255,0.2);
  font-size: 13px;
  padding: 20px 0;
}

/* ═══ ④ Bottom CTA ═══ */
.bottom-cta {
  text-align: center;
  padding: 16px 0 20px;
}
.cta-heading {
  font-size: 22px;
  font-weight: 700;
  color: #e6edf3;
  margin: 0 0 10px;
}
.cta-desc {
  font-size: 14px;
  color: rgba(255,255,255,0.4);
  margin: 0 0 24px;
}
.cta-login-link {
  font-size: 13px;
  color: rgba(255,255,255,0.35);
  margin: 16px 0 0;
}
.cta-login-link span {
  color: #60a5fa;
  cursor: pointer;
  text-decoration: underline;
}
.cta-login-link span:hover {
  color: #93c5fd;
}

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .hero-card { padding: 20px 24px; }
  .hero-title { font-size: 28px; }
  .hero-greet { font-size: 22px; }
  .hero-visual { display: none; }
}
@media (max-width: 768px) {
  .portal-root { padding: 72px 14px 32px; }
  .hero-row { flex-direction: column; min-height: auto; }
  .role-cards { width: 100%; flex-direction: row; gap: 10px; }
  .role-card { padding: 14px; }
  .subject-grid { grid-template-columns: repeat(2, 1fr); }
  .dashboard-row { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  .subject-grid { grid-template-columns: 1fr; }
  .hero-actions { flex-direction: column; }
  .cta-btn { width: 100%; text-align: center; }
}
</style>
