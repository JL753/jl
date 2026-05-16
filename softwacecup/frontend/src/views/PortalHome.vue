<template>
  <div class="portal-root">
    <!-- ═══ Hero Section ═══ -->
    <section class="hero-section glass-card">
      <div class="hero-content">
        <h1 class="hero-title">探索知识宇宙</h1>
        <p class="hero-subtitle">AI驱动的个性化学习平台</p>
        <p class="hero-desc">
          基于人工智能与大语言模型，构建自适应学习路径，追踪学情数据，<br/>
          为每一位学习者提供精准的学习支持与知识导航。
        </p>
        <button class="cta-btn" @click="handleEnter">进入知域</button>
      </div>

      <!-- 轨道动画 -->
      <div class="hero-visual">
        <div class="orbit-ring ring-1"></div>
        <div class="orbit-ring ring-2"></div>
        <div class="orbit-ring ring-3"></div>
        <div class="orbit-dot dot-1"><span>AI</span></div>
        <div class="orbit-dot dot-2"><span>ML</span></div>
        <div class="orbit-dot dot-3"><span>DS</span></div>
        <div class="orbit-dot dot-4"><span>DB</span></div>
        <div class="center-orb">知</div>
      </div>
    </section>

    <!-- ═══ AI Dialog Bar ═══ -->
    <section class="ai-bar-section">
      <AIDialogBar />
    </section>

    <!-- ═══ 推荐课程 (紧凑卡片) ═══ -->
    <section class="courses-section">
      <h2 class="section-title">推荐课程</h2>
      <div class="course-grid" v-if="subjects.length > 0">
        <CourseCard
          v-for="(course, index) in subjects"
          :key="course.id"
          :title="course.name || course.title || '未知学科'"
          :description="course.description || ''"
          :icon="course.icon || courseIcons[index % courseIcons.length]"
          :tags="course.tags || []"
          :compact="true"
          @click="handleCourseClick(course)"
        />
      </div>
      <p v-else class="empty-hint">暂无推荐课程</p>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { apiSubjects } from '../api/index.js'
import CourseCard from '../components/CourseCard.vue'
import AIDialogBar from '../components/AIDialogBar.vue'

const auth = useAuthStore()
const router = useRouter()
const subjects = ref([])

const courseIcons = ['💻', '📐', '🤖', '📊', '🎨', '🌐', '🔬', '📖']

onMounted(async () => {
  try {
    const res = await apiSubjects()
    subjects.value = res.data?.data || res.data || []
  } catch (e) {
    console.warn('Failed to fetch subjects', e)
  }
})

function handleEnter() {
  if (auth.isLoggedIn) {
    router.push('/student/dashboard')
  } else {
    auth.openLoginModal('/student/dashboard')
  }
}

function handleCourseClick(course) {
  const target = `/subjects/${course.id}`
  if (auth.isLoggedIn) {
    router.push(target)
  } else {
    auth.openLoginModal(target)
  }
}
</script>

<style scoped>
.portal-root {
  min-height: 100vh;
  color: #e6edf3;
  display: flex;
  flex-direction: column;
  gap: 32px;
  padding: 40px 48px 80px;
  max-width: 1200px;
  margin: 0 auto;
}

/* ═══ Hero ═══ */
.hero-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 60px;
  min-height: 340px;
  padding: 40px 48px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  position: relative;
  overflow: hidden;
}
.hero-content {
  max-width: 480px;
  flex-shrink: 0;
}
.hero-title {
  font-size: 52px;
  font-weight: 800;
  margin: 0 0 12px;
  line-height: 1.1;
  background: linear-gradient(135deg, #e6edf3 0%, #60a5fa 50%, #06b6d4 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.hero-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0 0 20px;
  letter-spacing: 2px;
}
.hero-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.45);
  line-height: 1.8;
  margin: 0 0 32px;
}
.cta-btn {
  padding: 14px 40px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 4px 24px rgba(59, 130, 246, 0.35);
}
.cta-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 36px rgba(59, 130, 246, 0.5);
}

/* ═══ Orbit Animation ═══ */
.hero-visual {
  width: 340px;
  height: 320px;
  position: relative;
  flex-shrink: 0;
}
.orbit-ring {
  position: absolute;
  border-radius: 50%;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  pointer-events: none;
}
.ring-1 { width: 260px; height: 260px; animation: spin 20s linear infinite; }
.ring-2 { width: 190px; height: 190px; animation: spin 14s linear infinite reverse; }
.ring-3 { width: 120px; height: 120px; animation: spin 10s linear infinite; }
@keyframes spin {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

.orbit-dot {
  position: absolute;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: white;
  pointer-events: none;
}
.dot-1 { background: #3b82f6; border: 1px solid #60a5fa; top: 42px; left: 150px; animation: orbit1 12s linear infinite; }
.dot-2 { background: #a855f7; border: 1px solid #c084fc; top: 90px; left: 34px; animation: orbit2 15s linear infinite reverse; }
.dot-3 { background: #60d9fa; border: 1px solid #99e9fc; top: 195px; left: 42px; animation: orbit3 18s linear infinite; }
.dot-4 { background: #3b82f6; border: 1px solid #60a5fa; top: 230px; left: 160px; animation: orbit4 14s linear infinite reverse; }

@keyframes orbit1 {
  0% { transform: translate(0, 0); }
  25% { transform: translate(24px, -12px); }
  50% { transform: translate(0, -24px); }
  75% { transform: translate(-24px, -12px); }
  100% { transform: translate(0, 0); }
}
@keyframes orbit2 {
  0% { transform: translate(0, 0); }
  25% { transform: translate(18px, -8px); }
  50% { transform: translate(0, -18px); }
  75% { transform: translate(-18px, -8px); }
  100% { transform: translate(0, 0); }
}
@keyframes orbit3 {
  0% { transform: translate(0, 0); }
  25% { transform: translate(20px, -10px); }
  50% { transform: translate(0, -20px); }
  75% { transform: translate(-20px, -10px); }
  100% { transform: translate(0, 0); }
}
@keyframes orbit4 {
  0% { transform: translate(0, 0); }
  25% { transform: translate(16px, -6px); }
  50% { transform: translate(0, -16px); }
  75% { transform: translate(-16px, -6px); }
  100% { transform: translate(0, 0); }
}

.center-orb {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 800;
  color: white;
  box-shadow: 0 0 40px rgba(59, 130, 246, 0.4);
  z-index: 2;
}

/* ═══ AI Dialog Bar ═══ */
.ai-bar-section {
  width: 100%;
}

/* ═══ Courses Section ═══ */
.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #e6edf3;
  margin: 0 0 20px;
}
.course-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.empty-hint {
  text-align: center;
  color: rgba(255, 255, 255, 0.3);
  font-size: 14px;
  padding: 40px 0;
}

/* ═══ Responsive ═══ */
@media (max-width: 1024px) {
  .hero-section {
    flex-direction: column;
    gap: 32px;
    min-height: auto;
  }
  .hero-visual { width: 280px; height: 280px; }
  .course-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 900px) {
  .portal-root { padding: 24px 16px 60px; }
}
@media (max-width: 640px) {
  .portal-root { padding: 16px 12px 60px; gap: 24px; }
  .hero-section { padding: 24px 20px; }
  .hero-title { font-size: 36px; }
  .hero-visual { display: none; }
  .course-grid { grid-template-columns: 1fr; }
}
</style>
