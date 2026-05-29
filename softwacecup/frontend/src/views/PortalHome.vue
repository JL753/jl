<template>
  <div class="portal-root">
    <TopNavBar />
    <div class="portal-inner">
      <!-- ═══ ① Hero + ② 角色分流 ═══ -->
      <div class="hero-row">
        <!-- Hero 左侧文字 -->
        <div class="hero-area" :class="{ full: auth.isLoggedIn }">
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
          </template>
          <template v-else>
            <div class="hero-left">
              <p class="hero-tagline">自适应学习路径 · 实时学情追踪</p>
              <h1 class="hero-greet">欢迎回来，{{ auth.user?.displayName || auth.user?.username }}</h1>
              <p class="hero-stat">
                已连续学习 <strong>{{ streakDays }}</strong> 天 · 掌握 <strong>{{ masteredKps }}</strong> 个知识点
              </p>
              <div class="hero-actions">
                <button class="cta-btn primary" @click="goContinue">继续上次学习</button>
                <button class="cta-btn ghost" @click="goSubjects">浏览课程</button>
                <button class="cta-btn ghost" @click="goKnowledgeMap">知识星图</button>
              </div>
            </div>
          </template>
        </div>

        <!-- 右侧: 3D轨道 + 角色分流卡 -->
        <div class="hero-right-group">
          <div class="orbit-3d" @mouseenter="orbitTilted = false" @mouseleave="orbitTilted = true">
            <div class="orbit-scene" :class="{ tilted: orbitTilted }">
              <div class="plane p1"><div class="ring r1"><span class="dot d1"></span></div></div>
              <div class="plane p2"><div class="ring r2"><span class="dot d2"></span></div></div>
              <div class="plane p3"><div class="ring r3"><span class="dot d3"></span></div></div>
              <div class="plane p4"><div class="ring r4"></div></div>
              <div class="plane p5"><div class="ring r5"><span class="dot d4"></span></div></div>
              <div class="core-glow"></div>
              <div class="core-orb">知</div>
            </div>
          </div>
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
import { apiSubjects, apiAbilityLatest, apiGamificationStreak } from '../api/index.js'

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
  // 有token但无user时先fetchMe恢复登录态
  if (auth.token && !auth.user) {
    try { await auth.fetchMe() } catch {}
  }
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
      }
    }
    if (streakRes.status === 'fulfilled') {
      const d = streakRes.value.data || {}
      streakDays.value = d.currentStreak || d.current_streak || d.streak || 0
    }
  } catch {}
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
function goContinue() {
  if (lastLesson.value?.lessonId) {
    router.push(`/student/courses/0?sc=${lastLesson.value.lessonId}`)
  } else {
    router.push('/student/subjects')
  }
}
function goSubjects() {
  router.push(auth.isLoggedIn ? '/student/subjects' : '/subjects')
}
function goKnowledgeMap() { router.push('/student/knowledge-map') }
function handleSubjectClick(s) {
  auth.isLoggedIn ? router.push(`/student/subjects/${s.id}`) : auth.openLoginModal(`/student/subjects/${s.id}`)
}

onMounted(() => { loadSubjects(); loadAuthData() })
</script>

<style scoped>
.portal-root {
  min-height: 100vh;
  color: #f0f4fa;
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
  gap: 16px;
  align-items: center;
  justify-content: center;
  min-height: 360px;
}
.hero-area {
  display: flex;
  align-items: center;
  transition: flex 0.3s ease;
}
.hero-area.full { }

.hero-left { max-width: 620px; }

.hero-right-group {
  display: flex;
  align-items: center;
  gap: 0;
  flex-shrink: 0;
}

.hero-tagline {
  font-size: 15px;
  color: #93c5fd;
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
  color: #f0f4fa;
  margin: 0 0 14px;
  line-height: 1.3;
}
.hero-subtitle {
  font-size: 15px;
  color: rgba(255,255,255,0.55);
  margin: 0 0 12px;
  letter-spacing: 4px;
}
.hero-stat {
  font-size: 15px;
  color: rgba(255,255,255,0.55);
  margin: 0 0 12px;
}
.hero-stat strong { color: #60a5fa; font-weight: 700; }
.hero-desc {
  font-size: 15px;
  color: rgba(255,255,255,0.6);
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
  color: #f0f4fa;
  border-color: rgba(255,255,255,0.25);
}

/* ═══ 3D Orbit Spinner ═══ */
.orbit-3d {
  width: 240px; height: 240px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; cursor: pointer;
}
.orbit-scene {
  position: relative;
  width: 200px; height: 200px;
  perspective: 500px;
  transform-style: preserve-3d;
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.orbit-scene.tilted {
  transform: rotateX(-60deg) rotateY(-15deg);
}

/* 轨道平面 — 每个平面有独立倾角 */
.plane {
  position: absolute;
  top: 50%; left: 50%;
  transform-style: preserve-3d;
  pointer-events: none;
}
.p1 { transform: translate(-50%, -50%) rotateX(0deg) rotateY(0deg); }
.p2 { transform: translate(-50%, -50%) rotateX(70deg) rotateY(0deg); }
.p3 { transform: translate(-50%, -50%) rotateX(0deg) rotateY(65deg); }
.p4 { transform: translate(-50%, -50%) rotateX(-70deg) rotateY(0deg); }
.p5 { transform: translate(-50%, -50%) rotateX(0deg) rotateY(-60deg); }

.ring {
  position: absolute;
  top: 50%; left: 50%;
  border-radius: 50%;
  pointer-events: none;
  transform: translate(-50%, -50%);
}

/* r1: 最大外环 — 蓝光 */
.r1 {
  width: 190px; height: 190px;
  animation: spin 5s linear infinite;
  border: 1.5px solid rgba(59, 130, 246, 0.3);
  box-shadow: 0 0 14px rgba(59, 130, 246, 0.2), inset 0 0 8px rgba(59, 130, 246, 0.06);
}
/* r2: 青环 */
.r2 {
  width: 155px; height: 155px;
  animation: spin 3.6s linear infinite reverse;
  border: 1.5px solid rgba(6, 182, 212, 0.3);
  box-shadow: 0 0 10px rgba(6, 182, 212, 0.18);
}
/* r3: 紫环 */
.r3 {
  width: 120px; height: 120px;
  animation: spin 2.8s linear infinite;
  border: 1.5px solid rgba(168, 85, 247, 0.3);
  box-shadow: 0 0 10px rgba(168, 85, 247, 0.18);
}
/* r4: 小青环 */
.r4 {
  width: 88px; height: 88px;
  animation: spin 2.2s linear infinite reverse;
  border: 1.2px solid rgba(6, 182, 212, 0.35);
  box-shadow: 0 0 8px rgba(6, 182, 212, 0.2);
}
/* r5: 内金环 */
.r5 {
  width: 56px; height: 56px;
  animation: spin 1.6s linear infinite;
  border: 1.2px solid rgba(245, 158, 11, 0.35);
  box-shadow: 0 0 6px rgba(245, 158, 11, 0.2);
}

/* 发光点 */
.dot {
  position: absolute;
  width: 6px; height: 6px;
  border-radius: 50%;
  pointer-events: none;
  top: -3px; left: 50%; margin-left: -3px;
}
.d1 { background: #60a5fa; box-shadow: 0 0 10px #3b82f6, 0 0 20px rgba(59,130,246,0.7); }
.d2 { background: #22d3ee; box-shadow: 0 0 10px #06b6d4, 0 0 20px rgba(6,182,212,0.7); }
.d3 { background: #c084fc; box-shadow: 0 0 10px #a855f7, 0 0 20px rgba(168,85,247,0.7); }
.d4 { background: #fbbf24; box-shadow: 0 0 10px #f59e0b, 0 0 20px rgba(245,158,11,0.7); }

/* 中心光晕 */
.core-glow {
  position: absolute;
  top: 50%; left: 50%;
  width: 70px; height: 70px;
  margin: -35px 0 0 -35px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(59,130,246,0.2) 0%, rgba(6,182,212,0.08) 40%, transparent 70%);
  animation: pulse 2.2s ease-in-out infinite;
  z-index: 1;
}
@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.4); opacity: 1; }
}
/* 中心球 */
.core-orb {
  position: absolute;
  top: 50%; left: 50%;
  width: 30px; height: 30px;
  margin: -15px 0 0 -15px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 800; color: white;
  box-shadow: 0 0 24px rgba(59,130,246,0.7), 0 0 48px rgba(59,130,246,0.3), 0 0 72px rgba(6,182,212,0.15);
  z-index: 2;
}

@keyframes spin {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to   { transform: translate(-50%, -50%) rotate(360deg); }
}

/* ═══ ② Role cards ═══ */
.role-cards {
  width: 120px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 0;
}
.role-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 14px 6px;
  border-radius: 10px;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}
.role-card:hover {
  transform: translateY(-1px);
}
.role-card.student:hover { box-shadow: 0 0 20px rgba(59,130,246,0.08); }
.role-card.teacher:hover { box-shadow: 0 0 20px rgba(168,85,247,0.08); }
.role-label { font-size: 13px; color: var(--text-main); opacity: 0.6; }
.role-arrow { font-size: 12px; font-weight: 600; color: var(--text-main); opacity: 0.7; }

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
  font-size: 11px; color: rgba(255,255,255,0.5);
  text-transform: uppercase; letter-spacing: 1px; margin: 0 0 10px;
}
.card-main { font-size: 17px; font-weight: 700; color: #f0f4fa; margin: 0 0 4px; }
.card-sub { font-size: 12px; color: rgba(255,255,255,0.5); margin: 0 0 14px; }
.progress-bar {
  height: 4px; border-radius: 2px;
  background: rgba(255,255,255,0.1);
  overflow: hidden; margin-bottom: 6px;
}
.progress-fill {
  height: 100%; border-radius: 2px;
  background: linear-gradient(90deg, #3b82f6, #06b6d4);
  transition: width 0.4s ease;
}
.progress-text { font-size: 12px; font-weight: 600; color: rgba(255,255,255,0.5); margin: 0; }
.card-empty { font-size: 13px; color: rgba(255,255,255,0.5); margin: 0; }

.rec-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 10px; }
.rec-item { display: flex; flex-direction: column; gap: 2px; }
.rec-title { font-size: 13px; color: #f0f4fa; line-height: 1.4; }
.rec-source { font-size: 11px; color: rgba(255,255,255,0.45); }

.section-title { font-size: 22px; font-weight: 700; color: #f0f4fa; margin: 8px 0 4px; }

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
.subject-name { font-size: 14px; font-weight: 600; color: #f0f4fa; }
.subject-desc {
  font-size: 11px; color: rgba(255,255,255,0.5);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.subject-arrow {
  font-size: 14px; color: rgba(255,255,255,0.25);
  transition: color 0.2s; flex-shrink: 0;
}
.subject-card:hover .subject-arrow { color: #60a5fa; }

.empty-hint { text-align: center; color: rgba(255,255,255,0.25); font-size: 13px; padding: 24px 0; }

/* ═══ ④ Bottom CTA ═══ */
.bottom-cta { text-align: center; padding: 12px 0 20px; }
.cta-heading { font-size: 20px; font-weight: 700; color: #f0f4fa; margin: 0 0 6px; }
.cta-desc { font-size: 13px; color: rgba(255,255,255,0.5); margin: 0 0 18px; }
.cta-btn.large { padding: 10px 36px; font-size: 15px; }
.cta-login-link { font-size: 12px; color: rgba(255,255,255,0.5); margin: 12px 0 0; }
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
