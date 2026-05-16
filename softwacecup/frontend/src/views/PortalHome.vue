<template>
  <div class="portal-root">
    <TopNavBar />
    <div class="portal-inner">
      <!-- ═══ ① Hero + ② 角色分流 ═══ -->
      <div class="hero-row">
        <!-- Hero 透明区 -->
        <div class="hero-area" :class="{ full: auth.isLoggedIn }">
          <!-- 未登录 -->
          <template v-if="!auth.isLoggedIn">
            <div class="hero-left">
              <p class="hero-tagline">自适应学习路径 · 实时学情追踪</p>
              <h1 class="hero-brand">知域</h1>
              <p class="hero-subtitle">标记你的知识版图</p>
              <p class="hero-desc">
                基于人工智能与大语言模型，构建个性化学习路径，追踪学情数据，<br/>
                为每一位学习者提供精准的学习支持与知识导航。
              </p>
              <div class="hero-actions">
                <button class="cta-btn primary" @click="handleEnter">进入知域</button>
                <button class="cta-btn ghost" @click="scrollToSubjects">了解功能</button>
              </div>
            </div>
            <!-- 3D 轨道 -->
            <div class="orbit-3d" @mouseenter="orbitTilted = false" @mouseleave="orbitTilted = true">
              <div class="orbit-scene" :class="{ tilted: orbitTilted }">
                <div class="ring-full r1"></div>
                <div class="ring-full r2"></div>
                <div class="ring-segment r3"></div>
                <div class="ring-segment r4"></div>
                <div class="ring-full r5"></div>
              </div>
            </div>
          </template>
          <!-- 已登录 -->
          <template v-else>
            <div class="hero-left">
              <p class="hero-tagline">自适应学习路径 · 实时学情追踪</p>
              <h1 class="hero-greet">欢迎回来，{{ auth.user?.username }}</h1>
              <p class="hero-stat">
                已连续学习 <strong>{{ streakDays }}</strong> 天 · 掌握 <strong>{{ masteredKps }}</strong> 个知识点
              </p>
              <div class="hero-actions">
                <button class="cta-btn primary" @click="goContinue">继续上次学习</button>
                <button class="cta-btn ghost" @click="goSubjects">浏览课程</button>
                <button class="cta-btn ghost" @click="goKnowledgeMap">知识星图</button>
              </div>
            </div>
            <div class="orbit-3d" @mouseenter="orbitTilted = false" @mouseleave="orbitTilted = true">
              <div class="orbit-scene" :class="{ tilted: orbitTilted }">
                <div class="ring-full r1"></div>
                <div class="ring-full r2"></div>
                <div class="ring-segment r3"></div>
                <div class="ring-segment r4"></div>
                <div class="ring-full r5"></div>
              </div>
            </div>
          </template>
        </div>

        <!-- ② 角色分流卡（仅未登录） -->
        <div v-if="!auth.isLoggedIn" class="role-cards">
          <div class="role-card student" @click="quickEnter('student')">
            <span class="role-label">我是学生</span>
            <span class="role-arrow">进入学习 →</span>
          </div>
          <div class="role-card teacher" @click="quickEnter('teacher')">
            <span class="role-label">我是教师</span>
            <span class="role-arrow">进入教学 →</span>
          </div>
        </div>
      </div>

      <!-- ═══ ③ 核心内容 ═══ -->
      <div class="core-section">
        <!-- 已登录：继续学习 + AI推荐 -->
        <div v-if="auth.isLoggedIn" class="dashboard-row">
          <div class="continue-card" @click="goContinue">
            <p class="card-overline">继续上次学习</p>
            <h3 class="card-main" v-if="lastLesson">{{ lastLesson.courseName }}</h3>
            <p class="card-sub" v-if="lastLesson">{{ lastLesson.lessonName }} · 第 {{ lastLesson.orderIndex }} 课时</p>
            <div class="progress-bar" v-if="lastLesson">
              <div class="progress-fill" :style="{ width: (lastLesson.progress || 0) + '%' }"></div>
            </div>
            <p class="progress-text" v-if="lastLesson">{{ lastLesson.progress || 0 }}%</p>
            <p class="card-empty" v-if="!lastLesson">暂无学习记录</p>
          </div>
          <div class="ai-rec-card">
            <p class="card-overline">AI 为你推荐</p>
            <ul class="rec-list" v-if="recommendations.length">
              <li v-for="(r, i) in recommendations" :key="i" class="rec-item">
                <span class="rec-title">{{ r.title }}</span>
                <span class="rec-source">{{ r.source }}{{ r.duration ? ' 丨 ' + r.duration : '' }}</span>
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
            class="subject-card"
            @click="handleSubjectClick(s)"
          >
            <div class="subject-accent" :style="{ background: accentGradients[idx % accentGradients.length] }"></div>
            <div class="subject-info">
              <span class="subject-name">{{ s.name }}</span>
              <span class="subject-desc">{{ s.description || '' }}</span>
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
        <button class="cta-btn primary large" @click="handleEnter">免费注册</button>
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
const orbitTilted = ref(true)

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
    const data = res.data
    subjects.value = Array.isArray(data) ? data : (data?.subjects || data?.list || [])
  } catch { subjects.value = [] }
}

async function loadAuthData() {
  if (!auth.isLoggedIn) return
  try {
    const [abilityRes, streakRes] = await Promise.allSettled([
      apiAbilityLatest(), apiGamificationStreak(),
    ])
    if (abilityRes.status === 'fulfilled') {
      const d = abilityRes.value.data || {}
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
      const d = streakRes.value.data || {}
      streakDays.value = d.currentStreak || d.current_streak || d.streak || 0
    }
  } catch {}
}

async function loadRecommendations(lessonId) {
  try {
    const res = await apiRecommendResources(lessonId)
    const data = res.data || {}
    const list = data.resources || data.recommendations || data.list || []
    recommendations.value = list.slice(0, 4)
  } catch { recommendations.value = [] }
}

function handleEnter() {
  auth.isLoggedIn ? router.push('/student/dashboard') : auth.openLoginModal('/student/dashboard')
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
  auth.isLoggedIn ? router.push(`/subjects/${s.id}`) : auth.openLoginModal(`/subjects/${s.id}`)
}

onMounted(() => { loadSubjects(); loadAuthData() })
</script>

<style scoped>
.portal-root {
  min-height: 100vh;
  color: #e6edf3;
  padding: 76px 0 0;
}
.portal-inner {
  display: flex;
  flex-direction: column;
  gap: 44px;
  padding: 44px 160px 48px;
}

/* ═══ ① Hero Area ═══ */
.hero-row {
  display: flex;
  gap: 24px;
  align-items: stretch;
}
.hero-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 80px;
  min-height: 360px;
  transition: flex 0.3s ease;
}
.hero-area.full { flex: 1; }

.hero-left { max-width: 620px; }

.hero-tagline {
  font-size: 15px;
  color: #60a5fa;
  margin: 0 0 14px;
  letter-spacing: 4px;
  font-weight: 500;
}
.hero-brand {
  font-size: 72px;
  font-weight: 800;
  margin: 0 0 16px;
  line-height: 1.05;
  background: linear-gradient(135deg, #e6edf3 0%, #60a5fa 40%, #06b6d4 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 10px;
}
.hero-greet {
  font-size: 40px;
  font-weight: 700;
  color: #e6edf3;
  margin: 0 0 14px;
  line-height: 1.3;
}
.hero-subtitle {
  font-size: 15px;
  color: rgba(255,255,255,0.45);
  margin: 0 0 12px;
  letter-spacing: 4px;
}
.hero-stat {
  font-size: 15px;
  color: rgba(255,255,255,0.45);
  margin: 0 0 12px;
}
.hero-stat strong { color: #60a5fa; font-weight: 700; }
.hero-desc {
  font-size: 15px;
  color: rgba(255,255,255,0.4);
  line-height: 1.9;
  margin: 0 0 32px;
}
.hero-actions { display: flex; gap: 12px; flex-wrap: wrap; }

.cta-btn {
  padding: 10px 28px;
  border-radius: 10px;
  border: none;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.cta-btn.primary {
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  color: white;
  box-shadow: 0 4px 20px rgba(59,130,246,0.35);
}
.cta-btn.primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 28px rgba(59,130,246,0.5);
}
.cta-btn.ghost {
  background: transparent;
  border: 1px solid rgba(255,255,255,0.15);
  color: rgba(255,255,255,0.6);
}
.cta-btn.ghost:hover {
  background: rgba(255,255,255,0.06);
  color: #e6edf3;
  border-color: rgba(255,255,255,0.25);
}

/* ═══ 3D Orbit Spinner ═══ */
.orbit-3d {
  width: 220px;
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  cursor: pointer;
}
.orbit-scene {
  position: relative;
  width: 180px;
  height: 180px;
  perspective: 600px;
  transform-style: preserve-3d;
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.orbit-scene.tilted {
  transform: rotateX(55deg) rotateY(-20deg);
}

.ring-full {
  position: absolute;
  top: 50%; left: 50%;
  border-radius: 50%;
  border: 1px solid rgba(59, 130, 246, 0.15);
  pointer-events: none;
}
.ring-segment {
  position: absolute;
  top: 50%; left: 50%;
  border-radius: 50%;
  pointer-events: none;
}

.r1 {
  width: 160px; height: 160px;
  margin: -80px 0 0 -80px;
  animation: ring-spin 3s linear infinite;
  border-color: rgba(59, 130, 246, 0.2);
}
.r2 {
  width: 130px; height: 130px;
  margin: -65px 0 0 -65px;
  animation: ring-spin 2.4s linear infinite reverse;
  border-color: rgba(6, 182, 212, 0.18);
}
.r3 {
  width: 100px; height: 100px;
  margin: -50px 0 0 -50px;
  animation: ring-spin 2s linear infinite;
  border-top: 2px solid rgba(139, 92, 246, 0.35);
  border-right: 2px solid transparent;
  border-bottom: 2px solid transparent;
  border-left: 2px solid transparent;
}
.r4 {
  width: 75px; height: 75px;
  margin: -37px 0 0 -37px;
  animation: ring-spin 1.6s linear infinite reverse;
  border-top: 2px solid rgba(6, 182, 212, 0.4);
  border-right: 2px solid transparent;
  border-bottom: 2px solid transparent;
  border-left: 2px solid transparent;
}
.r5 {
  width: 50px; height: 50px;
  margin: -25px 0 0 -25px;
  animation: ring-spin 1.2s linear infinite;
  border-color: rgba(168, 85, 247, 0.25);
}

@keyframes ring-spin {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to   { transform: translate(-50%, -50%) rotate(360deg); }
}

/* ═══ ② Role cards ═══ */
.role-cards {
  width: 160px;
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
  padding: 18px 12px;
  border-radius: 12px;
  background: transparent;
  border: 1px solid rgba(255,255,255,0.08);
  cursor: pointer;
  transition: all 0.2s;
}
.role-card:hover {
  border-color: rgba(255,255,255,0.18);
  transform: translateY(-1px);
}
.role-card.student:hover { border-color: rgba(59,130,246,0.35); }
.role-card.teacher:hover { border-color: rgba(168,85,247,0.35); }
.role-label { font-size: 13px; color: rgba(255,255,255,0.4); }
.role-arrow { font-size: 12px; font-weight: 600; color: rgba(255,255,255,0.5); }

/* ═══ ③ Core section ═══ */
.core-section { display: flex; flex-direction: column; gap: 24px; }

.dashboard-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }

.continue-card, .ai-rec-card {
  padding: 24px 28px;
  border-radius: 12px;
  background: transparent;
  border: 1px solid rgba(255,255,255,0.06);
}
.continue-card { cursor: pointer; }
.continue-card:hover { border-color: rgba(59,130,246,0.2); }

.card-overline {
  font-size: 11px; color: rgba(255,255,255,0.3);
  text-transform: uppercase; letter-spacing: 1px; margin: 0 0 10px;
}
.card-main { font-size: 17px; font-weight: 700; color: #e6edf3; margin: 0 0 4px; }
.card-sub { font-size: 12px; color: rgba(255,255,255,0.35); margin: 0 0 14px; }
.progress-bar {
  height: 4px; border-radius: 2px;
  background: rgba(255,255,255,0.06);
  overflow: hidden; margin-bottom: 6px;
}
.progress-fill {
  height: 100%; border-radius: 2px;
  background: linear-gradient(90deg, #3b82f6, #06b6d4);
  transition: width 0.4s ease;
}
.progress-text { font-size: 12px; font-weight: 600; color: rgba(255,255,255,0.35); margin: 0; }
.card-empty { font-size: 13px; color: rgba(255,255,255,0.2); margin: 0; }

.rec-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 10px; }
.rec-item { display: flex; flex-direction: column; gap: 2px; }
.rec-title { font-size: 13px; color: #e6edf3; line-height: 1.4; }
.rec-source { font-size: 11px; color: rgba(255,255,255,0.3); }

.section-title { font-size: 22px; font-weight: 700; color: #e6edf3; margin: 8px 0 4px; }

.subject-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }

.subject-card {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 18px;
  border-radius: 12px;
  background: transparent;
  border: 1px solid rgba(255,255,255,0.05);
  cursor: pointer;
  transition: all 0.2s;
}
.subject-card:hover {
  border-color: rgba(255,255,255,0.12);
  transform: translateY(-1px);
}
.subject-accent {
  width: 5px; height: 32px; border-radius: 3px; flex-shrink: 0;
}
.subject-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.subject-name { font-size: 14px; font-weight: 600; color: #e6edf3; }
.subject-desc {
  font-size: 11px; color: rgba(255,255,255,0.3);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.subject-arrow {
  font-size: 14px; color: rgba(255,255,255,0.15);
  transition: color 0.2s; flex-shrink: 0;
}
.subject-card:hover .subject-arrow { color: #60a5fa; }

.empty-hint { text-align: center; color: rgba(255,255,255,0.15); font-size: 13px; padding: 24px 0; }

/* ═══ ④ Bottom CTA ═══ */
.bottom-cta { text-align: center; padding: 12px 0 20px; }
.cta-heading { font-size: 20px; font-weight: 700; color: #e6edf3; margin: 0 0 6px; }
.cta-desc { font-size: 13px; color: rgba(255,255,255,0.35); margin: 0 0 18px; }
.cta-btn.large { padding: 10px 36px; font-size: 15px; }
.cta-login-link { font-size: 12px; color: rgba(255,255,255,0.3); margin: 12px 0 0; }
.cta-login-link span { color: #60a5fa; cursor: pointer; text-decoration: underline; }
.cta-login-link span:hover { color: #93c5fd; }

/* ═══ Responsive ═══ */
@media (max-width: 1200px) {
  .portal-inner { padding: 36px 60px 40px; }
  .hero-brand { font-size: 56px; }
}
@media (max-width: 900px) {
  .portal-inner { padding: 28px 32px 40px; }
  .hero-area { min-height: auto; }
  .hero-brand { font-size: 42px; letter-spacing: 6px; }
  .orbit-3d { display: none; }
}
@media (max-width: 768px) {
  .portal-inner { padding: 20px 16px 32px; }
  .hero-row { flex-direction: column; }
  .role-cards { width: 100%; flex-direction: row; }
  .role-card { padding: 14px; }
  .subject-grid { grid-template-columns: repeat(2, 1fr); }
  .dashboard-row { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  .hero-brand { font-size: 34px; letter-spacing: 4px; }
  .subject-grid { grid-template-columns: 1fr; }
  .hero-actions { flex-direction: column; }
  .cta-btn { width: 100%; text-align: center; }
}
</style>
