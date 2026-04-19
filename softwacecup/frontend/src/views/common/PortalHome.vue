<template>
  <div class="portal-page">
    <header class="portal-header">
      <div class="brand-row">
        <div class="brand-icon">
          <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="40" height="40" rx="12" fill="url(#brandGrad)"/>
            <path d="M10 20L17 27L30 13" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            <defs><linearGradient id="brandGrad" x1="0" y1="0" x2="40" y2="40"><stop stop-color="#4C8DFF"/><stop offset="1" stop-color="#2563EB"/></linearGradient></defs>
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-name">智备优教</span>
          <span class="brand-sub">SMART PREPARATION</span>
        </div>
      </div>
      <nav class="nav-bar">
        <a href="#hero" :class="{ active: currentSection === 'hero' }">首页</a>
        <a href="#courses" :class="{ active: currentSection === 'courses' }">课程</a>
        <a href="#features" :class="{ active: currentSection === 'features' }">团队</a>
        <a href="#about" :class="{ active: currentSection === 'about' }">关于</a>
      </nav>
      <div class="header-actions">
        <div class="search-bar">
          <svg class="search-icon" viewBox="0 0 20 20"><path d="M9 2a7 7 0 015.6 11L19 17.5l-1.3 1.3L13.2 14A7 7 0 119 2z" fill="none" stroke="currentColor" stroke-width="1.6"/></svg>
          <input type="text" placeholder="搜索课程、AI 场景、教学云资源" />
        </div>
        <button class="btn-primary" @click="goLogin">进入平台</button>
      </div>
    </header>

    <!-- Hero Section - 参照截图image.png的蓝色渐变风格 -->
    <section id="hero" class="hero-section">
      <div class="hero-bg"></div>
      <div class="hero-container">
        <div class="hero-left">
          <div class="hero-tag">AI + 教学 + 学习 + 考评</div>
          <h1 class="hero-title">让教学、学习与考试协同在一个漂亮的工作台里发生</h1>
          <p class="hero-desc">围绕高校课程教学、学习辅导、资源推荐、在线考试与数据中台，打造教师端、学生端与管理端统一联动的智慧教育平台。</p>
          <div class="hero-actions">
            <button class="btn-primary btn-lg" @click="goLogin">立即加入</button>
            <button class="btn-outline btn-lg" @click="scrollTo('courses')">浏览课程</button>
        </div>
        <div class="hero-stats">
            <div v-for="stat in heroStats" :key="stat.label" class="stat-item">
              <strong>{{ stat.value }}</strong>
              <span>{{ stat.label }}</span>
            </div>
          </div>
        </div>
        <div class="hero-right">
          <div class="feature-card main-feature">
            <div class="fc-label">智能教学闭环</div>
            <h3>备课 → 资源 → 考试 → 批改 → 学情分析</h3>
            <p>从课堂设计到学生反馈，所有能力都在一套真实可用的系统里联动运行。</p>
      </div>
          <div class="feature-stack">
            <div v-for="(feat, idx) in featureCards" :key="idx" :class="['mini-card', feat.color]">
              <span>{{ feat.tag }}</span>
              <strong>{{ feat.title }}</strong>
      </div>
      </div>
        </div>
      </div>
    </section>

    <!-- Course Section - 课程可交互跳转 -->
    <section id="courses" class="course-section">
      <div class="section-header center">
        <p>课程与能力场景</p>
        <h2>热门教学能力模块</h2>
      </div>
      <div class="course-grid">
        <article
          v-for="(course, idx) in allCourses"
          :key="course.id || idx"
          class="course-card"
          @click="openCourseDetail(course)"
        >
          <div class="card-cover" :style="{ background: course.gradient }">
            <span class="cover-tag">{{ course.tag }}</span>
            <div class="cover-pattern"></div>
          </div>
          <div class="card-body">
            <small>{{ course.category }}</small>
            <h3>{{ course.title }}</h3>
            <p>{{ course.desc }}</p>
            <div class="card-footer">
              <span class="status-tag">{{ course.status }}</span>
              <strong class="price">{{ course.price }}</strong>
            </div>
          </div>
        </article>
      </div>
    </section>

    <!-- Teacher Team Section -->
    <section id="features" class="team-section">
      <div class="section-header center">
        <p>教学团队</p>
        <h2>教师经验与 AI 智能体协同</h2>
      </div>
      <div class="team-grid">
        <article v-for="t in teamMembers" :key="t.name" class="team-card">
          <div class="team-avatar" :style="{ background: t.avatarBg }">{{ t.name[0] }}</div>
          <strong>{{ t.name }}</strong>
          <span>{{ t.role }}</span>
          <p>{{ t.desc }}</p>
        </article>
      </div>
    </section>

    <!-- Process Section -->
    <section id="about" class="process-section">
      <div class="section-header left-align">
        <p>核心流程</p>
        <h2>一体化教学运行路径</h2>
      </div>
      <div class="process-grid">
        <div v-for="(step, idx) in processSteps" :key="idx" class="process-card">
          <span class="step-num">0{{ idx + 1 }}</span>
          <h4>{{ step.title }}</h4>
          <p>{{ step.desc }}</p>
        </div>
      </div>
    </section>

    <!-- Subscribe Section -->
    <section class="subscribe-section">
      <div class="subscribe-card">
        <div class="sub-left">
          <small>订阅我们的时事通讯</small>
          <h2>关注平台版本更新与课程资讯</h2>
        </div>
        <div class="sub-right">
          <input type="email" placeholder="输入一个有效的电子邮箱地址" />
          <button class="btn-success">开始订阅</button>
      </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="portal-footer">
      <div class="footer-branding">
        <span class="f-brand">智备优教</span>
        <p>让每一个学习者都能拥有智能而精准的课程陪伴。</p>
      </div>
      <div class="footer-links">
        <a>课程资源</a>
        <a>数据服务</a>
        <a>联系我们</a>
        <a>隐私政策</a>
      </div>
    </footer>

    <!-- Course Detail Modal -->
    <Teleport to="body">
      <div v-if="showCourseModal" class="modal-overlay" @click.self="showCourseModal = false">
        <div class="modal-content">
          <button class="close-btn" @click="showCourseModal = false">&times;</button>
          <div class="modal-cover" :style="{ background: selectedCourse?.gradient }"></div>
          <div class="modal-body">
            <small>{{ selectedCourse?.category }}</small>
            <h2>{{ selectedCourse?.title }}</h2>
            <p>{{ selectedCourse?.fullDesc || selectedCourse?.desc }}</p>
            <div class="modal-meta">
              <span><b>课程类型：</b>{{ selectedCourse?.type || '理论+实践' }}</span>
              <span><b>课时安排：</b>{{ selectedCourse?.hours || '32课时' }}</span>
              <span><b>适合对象：</b>{{ selectedCourse?.audience || '高校师生' }}</span>
            </div>
            <div class="modal-chapters" v-if="selectedCourse?.chapters">
              <h4>课程章节</h4>
              <ul>
                <li v-for="(ch, i) in selectedCourse.chapters" :key="i">{{ i + 1 }}. {{ ch }}</li>
              </ul>
            </div>
            <div class="modal-actions">
              <button class="btn-primary" @click="goLogin">立即学习</button>
              <button class="btn-outline" @click="showCourseModal = false">关闭</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiPortal } from '../../api'

const router = useRouter()
const currentSection = ref('hero')
const showCourseModal = ref(false)
const selectedCourse = ref(null)
const heroStats = ref([])
const courses = ref([])

// 默认课程数据（可由管理员通过后台导入）
const defaultCourses = [
  {
    id: 1,
    tag: 'AI', category: '人工智能', title: '人工智能导论',
    desc: '覆盖概念、案例、题库与代码实操。',
    fullDesc: '本课程系统讲解人工智能的基础概念、核心算法与应用实践。涵盖机器学习基础、深度学习入门、神经网络原理等内容，配合丰富的案例分析和编程实操训练。',
    status: '平台能力已接入', price: '免费',
    gradient: 'linear-gradient(135deg, #ff9f43, #ffc75f)',
    type: '理论+实践', hours: '48课时', audience: '计算机相关专业本科生',
    chapters: ['AI发展历史与趋势', '机器学习基础概念', '监督学习算法详解', '无监督学习与聚类', '深度学习与神经网络', '自然语言处理入门', '计算机视觉应用', '项目实战与综合考核']
  },
  {
    id: 2, tag: 'ML', category: '机器学习', title: '机器学习基础',
    desc: '围绕分类、回归、评估指标组织多模态资源。',
    fullDesc: '从数学基础到算法实现，系统讲解机器学习的核心方法。包括线性回归、逻辑回归、决策树、支持向量机、集成学习等经典算法，并配有大量实战练习。',
    status: '平台能力已接入', price: '\u00a5299',
    gradient: 'linear-gradient(135deg, #59a9ff, #5fd8ff)',
    type: '理论为主', hours: '36课时', audience: '理工科研究生及高年级本科生',
    chapters: ['数学基础回顾', '线性回归与梯度下降', '分类问题与逻辑回归', '决策树与随机森林', 'SVM与核方法', '模型评估与选择', '聚类与降维', '期末项目']
  },
  {
    id: 3, tag: 'DL', category: '深度学习', title: '深度学习实践',
    desc: '适合竞赛冲刺和项目实训的高级课程。',
    fullDesc: '面向有一定基础的学员，深入讲解CNN、RNN、Transformer等前沿架构。结合PyTorch/TensorFlow框架进行实战，涵盖图像识别、NLP、强化学习等领域。',
    status: '平台能力已接入', price: '\u00a5399',
    gradient: 'linear-gradient(135deg, #6f7fff, #9ca8ff)',
    type: '实战为主', hours: '56课时', audience: '有ML基础的研究生和开发者',
    chapters: ['深度学习概述', '卷积神经网络(CNN)', '循环神经网络(RNN/LSTM)', '注意力机制与Transformer', '目标检测与图像分割', '预训练大模型', '强化学习入门', '综合实战项目']
  },
  {
    id: 4, tag: 'SE', category: '软件工程', title: '软件工程导论',
    desc: '从需求分析到软件测试的全流程覆盖。',
    fullDesc: '全面介绍软件工程的各个环节，包括需求分析、系统设计、编码规范、项目管理、质量保证等核心内容，培养现代软件开发能力。',
    status: '即将上线', price: '免费',
    gradient: 'linear-gradient(135deg, #18b48f, #6fd8b9)',
    type: '理论+实践', hours: '40课时', audience: '所有专业本科生',
    chapters: ['软件工程概述', '需求工程', '软件设计模式', '敏捷开发方法', '代码质量与重构', '软件测试策略', 'DevOps实践', '课程项目']
  },
  {
    id: 5, tag: 'DS', category: '数据科学', title: 'Python数据分析',
    desc: '基于NumPy/Pandas/Matplotlib的数据科学入门。',
    fullDesc: '从Python基础出发，系统学习数据处理、可视化和统计分析。使用真实数据集进行实操，培养数据思维和分析能力。',
    status: '即将上线', price: '\u00a5199',
    gradient: 'linear-gradient(135deg, #f093fb, #f5576c)',
    type: '实战为主', hours: '32课时', audience: '对数据分析感兴趣的所有学生',
    chapters: ['Python快速入门', 'NumPy数值计算', 'Pandas数据处理', 'Matplotlib可视化', '数据清洗技巧', '统计分析基础', '实战案例：电商数据分析', '期末项目']
  }
]

const teamMembers = [
  { name: '陈老师', role: '主讲教师', desc: '人工智能方向，10年教学经验', avatarBg: 'linear-gradient(135deg,#4b91ff,#59d4ff)' },
  { name: '赵老师', role: '首席讲师', desc: '教育评价方向，发表多篇核心论文', avatarBg: 'linear-gradient(135deg,#ff7eb3,#ff758c)' },
  { name: '马老师', role: '金牌导师', desc: '软件工程方向，企业级项目经验丰富', avatarBg: 'linear-gradient(135deg,#43e97b,#38f9d7)' },
  { name: '张老师', role: '课程顾问', desc: '课程设计方向，专注教学法研究', avatarBg: 'linear-gradient(135deg,#fa709a,#fee140)' }
]

const featureCards = [
  { tag: '教师端', title: '题库管理 / PPT 生成', color: 'blue' },
  { tag: '学生端', title: '问答 / 资源推荐 / 在线考试', color: 'green' },
  { tag: '管理端', title: '学校 / 学院 / 个人数据看板', color: 'violet' }
]

const processSteps = [
  { title: '画像构建', desc: '通过自然语言对话自动抽取学生特征。' },
  { title: '多智能体生成', desc: '不同智能体协同生成多模态学习资源。' },
  { title: '路径规划', desc: '结合学习进度和目标动态调整学习步骤。' },
  { title: '评估优化', desc: '根据行为与测评数据持续优化资源推送。' }
]

const allCourses = computed(() => {
  return courses.value.length > 0 ? courses.value : defaultCourses
})

const goLogin = () => router.push('/login')
const scrollTo = (id) => document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })

const openCourseDetail = (course) => {
  selectedCourse.value = course
  showCourseModal.value = true
}

onMounted(async () => {
  try {
    const data = await apiPortal()
    if (data?.heroStats) heroStats.value = data.heroStats
    if (data?.courses && data.courses.length > 0) courses.value = data.courses
  } catch (e) {
    console.warn('Portal API unavailable, using defaults')
  }

  // 滚动监听当前区域
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) currentSection.value = entry.target.id
    })
  }, { threshold: 0.3 })
  document.querySelectorAll('.hero-section, .course-section, .team-section, .process-section').forEach(el => observer.observe(el))
})
</script>

<style scoped>
/* ========== 全局变量 ========== */
.portal-page { min-height: 100vh; background: linear-gradient(180deg, #fbfdff, #f5f8fc); color: #26364d; overflow-x: hidden; }

/* ========== Header ========== */
.portal-header {
  position: sticky; top: 0; z-index: 100;
  height: 72px; display: flex; align-items: center; justify-content: space-between;
  padding: 0 40px; background: rgba(255,255,255,.92); backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(232,240,252,.8);
}
.brand-row { display: flex; align-items: center; gap: 14px; }
.brand-icon svg { width: 40px; height: 40px; }
.brand-text { display: flex; flex-direction: column; }
.brand-name { font-size: 24px; font-weight: 800; color: #1e40af; letter-spacing: -.5px; }
.brand-sub { font-size: 10px; color: #94a3b8; letter-spacing: 2.5px; text-transform: uppercase; font-weight: 600; }
.nav-bar { display: flex; gap: 32px; }
.nav-bar a { font-size: 15px; color: #64748b; text-decoration: none; transition: color .2s; position: relative; padding: 22px 0; font-weight: 500; }
.nav-bar a::after { content:''; position:absolute; bottom:0; left:0; right:0; height:2.5px; background:#3b82f6; transform:scaleX(0); transition:transform .2s; }
.nav-bar a:hover { color: #1e40af; }
.nav-bar a.active { color: #1e40af; font-weight: 600; }
.nav-bar a.active::after { transform: scaleX(1); }
.header-actions { display: flex; align-items: center; gap: 14px; }
.search-bar {
  display: flex; align-items: center; gap: 8px; width: 260px; height: 40px;
  padding: 0 14px; border-radius: 999px; background: #f8fafc; border: 1px solid #e2e8f0; transition: box-shadow .2s;
}
.search-bar:focus-within { box-shadow: 0 0 0 3px rgba(59,130,246,.1); border-color: #93c5fd; }
.search-icon { width: 16px; height: 16px; color: #94a3b8; flex-shrink: 0; }
.search-bar input { border: none; outline: none; background: transparent; font-size: 13.5px; color: #334155; width: 100%; }
.btn-primary {
  padding: 9px 22px; border-radius: 10px; border: none; background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white; font-size: 14px; font-weight: 600; cursor: pointer; transition: transform .2s, box-shadow .2s;
}
.btn-primary:hover { transform: translateY(-1px); box-shadow: 0 8px 20px rgba(37,99,235,.28); }

/* ========== Hero Section ========== */
.hero-section {
  position: relative; padding: 0 36px 60px; margin-top: 0; min-height: 580px; display: flex; align-items: center;
}
.hero-bg {
  position: absolute; inset: 0; z-index: 0; margin: 0 36px;
  background: linear-gradient(135deg, #1e3a8a 0%, #1e40af 35%, #3b82f6 68%, #60a5fa 100%);
  border-radius: 28px; overflow: hidden;
}
.hero-bg::before {
  content: ''; position: absolute; top: -15%; right: -8%; width: 400px; height: 400px;
  background: radial-gradient(circle, rgba(255,255,255,.14), transparent 70%); pointer-events: none;
}
.hero-bg::after {
  content: ''; position: absolute; bottom: -10%; left: -5%; width: 300px; height: 300px;
  background: radial-gradient(circle, rgba(96,165,250,.12), transparent 65%); pointer-events: none;
}
.hero-container {
  position: relative; z-index: 1; max-width: 1280px; width: 100%; margin: 0 auto;
  display: grid; grid-template-columns: 1.1fr .9fr; gap: 36px; padding: 44px 0;
}
.hero-left { color: white; display: flex; flex-direction: column; justify-content: center; }
.hero-tag {
  display: inline-flex; align-self: flex-start; padding: 6px 16px; border-radius: 999px;
  background: rgba(255,255,255,.16); border: 1px solid rgba(255,255,255,.22);
  font-size: 13px; font-weight: 600; letter-spacing: .5px; backdrop-filter: blur(4px); margin-bottom: 18px;
}
.hero-title { font-size: 50px; line-height: 1.15; font-weight: 800; margin: 0 0 18px; max-width: 720px; }
.hero-desc { font-size: 16px; line-height: 1.85; opacity: .92; margin: 0 0 28px; max-width: 640px; color: rgba(255,255,255,.88); }
.hero-actions { display: flex; gap: 14px; margin-bottom: 34px; }
.btn-lg { padding: 14px 30px; font-size: 15px; border-radius: 14px; }
.btn-outline {
  padding: 14px 30px; border-radius: 14px; border: 2px solid rgba(255,255,255,.45); background: transparent;
  color: white; font-size: 15px; font-weight: 600; cursor: pointer; transition: background .2s;
}
.btn-outline:hover { background: rgba(255,255,255,.12); }
.hero-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.stat-item {
  padding: 18px 16px; border-radius: 18px; background: rgba(255,255,255,.13);
  border: 1px solid rgba(255,255,255,.18); backdrop-filter: blur(6px); transition: transform .2s;
}
.stat-item:hover { transform: translateY(-3px); }
.stat-item strong { display: block; font-size: 32px; font-weight: 700; line-height: 1.2; }
.stat-item span { display: block; font-size: 13px; opacity: .86; margin-top: 4px; }

.hero-right { display: flex; flex-direction: column; gap: 16px; justify-content: center; }
.main-feature {
  padding: 26px 28px; border-radius: 24px; background: rgba(255,255,255,.13);
  border: 1px solid rgba(255,255,255,.2); backdrop-filter: blur(12px); transition: transform .2s;
}
.main-feature:hover { transform: translateY(-4px); }
.fc-label { font-size: 13px; opacity: .82; font-weight: 500; }
.main-feature h3 { color: white; font-size: 26px; margin: 10px 0 10px; line-height: 1.35; font-weight: 700; }
.main-feature p { color: rgba(255,255,255,.88); font-size: 14.5px; line-height: 1.8; margin: 0; }
.feature-stack { display: grid; gap: 12px; }
.mini-card {
  padding: 18px 22px; border-radius: 18px; background: rgba(255,255,255,.12);
  border: 1px solid rgba(255,255,255,.16); backdrop-filter: blur(8px); transition: transform .2s; cursor: default;
}
.mini-card:hover { transform: translateY(-3px); }
.mini-card span { display: block; font-size: 12.5px; opacity: .82; margin-bottom: 6px; font-weight: 500; }
.mini-card strong { color: white; font-size: 19px; font-weight: 700; }
.mini-card.blue span { color: #bfdbfe; }
.mini-card.green span { color: #bbf7d0; }
.mini-card.violet span { color: #ddd6fe; }

/* ========== Course Section ========== */
.course-section { padding: 72px 40px; }
.section-header { margin-bottom: 38px; }
.section-header.center { text-align: center; }
.section-header.left-align { text-align: left; padding-left: 10px; }
.section-header p { font-size: 14px; color: #8999ae; margin: 0 0 8px; letter-spacing: 1px; text-transform: uppercase; font-weight: 500; }
.section-header h2 { font-size: 40px; margin: 0; color: #1e293b; font-weight: 800; }
.course-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 24px; max-width: 1240px; margin: 0 auto; }
.course-card {
  background: white; border-radius: 24px; overflow: hidden; box-shadow: 0 4px 20px rgba(33,65,108,.06);
  cursor: pointer; transition: transform .28s ease, box-shadow .28s ease;
}
.course-card:hover { transform: translateY(-8px); box-shadow: 0 20px 48px rgba(33,68,120,.14); }
.card-cover {
  height: 170px; position: relative; display: flex; align-items: center; justify-content: center;
  overflow: hidden;
}
.cover-tag {
  font-size: 32px; font-weight: 800; color: rgba(255,255,255,.95); text-shadow: 0 2px 12px rgba(0,0,0,.12); letter-spacing: 1px;
  position: relative; z-index: 1;
}
.cover-pattern {
  position: absolute; inset: 0; background:
    radial-gradient(circle at 20% 80%, rgba(255,255,255,.15), transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255,255,255,.1), transparent 50%);
  pointer-events: none;
}
.card-body { padding: 22px 24px 24px; }
.card-body small { font-size: 12.5px; color: #8d9caf; text-transform: uppercase; letter-spacing: .5px; font-weight: 600; }
.card-body h3 { font-size: 24px; margin: 10px 0 10px; color: #1e293b; font-weight: 700; }
.card-body p { color: #64748b; font-size: 14.5px; line-height: 1.7; margin: 0 0 18px; min-height: 52px; }
.card-footer { display: flex; justify-content: space-between; align-items: center; }
.status-tag { font-size: 13px; color: #059669; font-weight: 500; }
.price { font-size: 20px; color: #059669; font-weight: 700; }

/* ========== Team Section ========== */
.team-section { padding: 56px 40px 72px; background: #f8fafc; }
.team-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 22px; max-width: 1100px; margin: 0 auto; }
.team-card {
  background: white; border-radius: 22px; padding: 28px 22px; text-align: center;
  box-shadow: 0 4px 16px rgba(33,65,108,.05); transition: transform .28s ease, box-shadow .28s ease;
}
.team-card:hover { transform: translateY(-6px); box-shadow: 0 18px 40px rgba(33,68,120,.12); }
.team-avatar {
  width: 76px; height: 76px; border-radius: 50%; margin: 0 auto 16px;
  display: grid; place-items: center; color: white; font-size: 30px; font-weight: 800; box-shadow: 0 8px 20px rgba(0,0,0,.08);
}
.team-card strong { display: block; font-size: 18px; color: #1e293b; margin-bottom: 4px; }
.team-card span { font-size: 13px; color: #64748b; }
.team-card p { font-size: 13.5px; color: #94a3b8; line-height: 1.7; margin: 10px 0 0; }

/* ========== Process Section ========== */
.process-section { padding: 72px 40px; }
.process-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; max-width: 1160px; }
.process-card {
  background: white; border-radius: 20px; padding: 26px 22px; box-shadow: 0 4px 16px rgba(33,65,108,.06);
  transition: transform .28s ease, box-shadow .28s ease; border: 1px solid #f1f5f9;
}
.process-card:hover { transform: translateY(-5px); box-shadow: 0 16px 36px rgba(33,68,120,.1); }
.step-num {
  display: inline-block; padding: 4px 12px; border-radius: 999px; background: #eff6ff; color: #3b82f6;
  font-size: 13px; font-weight: 700; margin-bottom: 12px;
}
.process-card h4 { font-size: 19px; margin: 0 0 8px; color: #1e293b; font-weight: 700; }
.process-card p { font-size: 14px; color: #64748b; line-height: 1.75; margin: 0; }

/* ========== Subscribe Section ========== */
.subscribe-section { padding: 0 40px 72px; }
.subscribe-card {
  display: flex; align-items: center; justify-content: space-between; gap: 32px;
  background: linear-gradient(135deg, #1e3a8a, #1e40af); border-radius: 24px; padding: 36px 42px; box-shadow: 0 12px 40px rgba(30,58,138,.2);
}
.sub-left small { font-size: 13px; color: #93c5fd; font-weight: 500; letter-spacing: .5px; }
.sub-left h2 { color: white; font-size: 28px; margin: 6px 0 0; font-weight: 700; }
.sub-right { display: grid; grid-template-columns: 320px 120px; gap: 12px; }
.sub-right input {
  padding: 0 18px; border-radius: 12px; border: none; font-size: 14px; outline: none;
  height: 46px; background: rgba(255,255,255,.14); color: white; border: 1px solid rgba(255,255,255,.2);
}
.sub-right input::placeholder { color: rgba(255,255,255,.55); }
.btn-success {
  padding: 0 24px; border-radius: 12px; border: none; background: #10b981; color: white;
  font-size: 14px; font-weight: 600; cursor: pointer; transition: background .2s; height: 46px;
}
.btn-success:hover { background: #059669; }

/* ========== Footer ========== */
.portal-footer {
  display: flex; justify-content: space-between; align-items: center; padding: 36px 40px; background: #1e293b; color: #e2e8f0;
}
.f-brand { font-size: 36px; font-weight: 800; color: #3b82f6; display: block; margin-bottom: 8px; }
.footer-branding p { font-size: 14px; color: #94a3b8; margin: 0; max-width: 360px; }
.footer-links { display: flex; gap: 24px; }
.footer-links a { color: #cbd5e1; font-size: 14px; text-decoration: none; transition: color .2s; }
.footer-links a:hover { color: white; }

/* ========== Modal ========== */
.modal-overlay {
  position: fixed; inset: 0; z-index: 200; background: rgba(0,0,0,.5);
  display: flex; align-items: center; justify-content: center; padding: 20px; backdrop-filter: blur(4px);
}
.modal-content {
  width: min(680px, 100%); max-height: 90vh; overflow-y: auto; background: white; border-radius: 28px;
  box-shadow: 0 24px 64px rgba(0,0,0,.2); position: relative;
}
.close-btn {
  position: absolute; top: 16px; right: 18px; z-index: 10; width: 36px; height: 36px; border-radius: 50%;
  border: none; background: rgba(0,0,0,.08); font-size: 20px; cursor: pointer; color: #64748b; transition: background .2s;
}
.close-btn:hover { background: rgba(0,0,0,.15); }
.modal-cover { height: 180px; border-radius: 28px 28px 0 0; }
.modal-body { padding: 28px 32px 32px; }
.modal-body small { font-size: 12.5px; color: #64748b; text-transform: uppercase; font-weight: 600; }
.modal-body h2 { font-size: 28px; margin: 8px 0 12px; color: #0f172a; font-weight: 800; }
.modal-body > p { color: #475569; line-height: 1.8; margin: 0 0 18px; font-size: 15px; }
.modal-meta { display: grid; gap: 10px; margin-bottom: 20px; padding: 16px; background: #f8fafc; border-radius: 16px; }
.modal-meta span { font-size: 14px; color: #334155; }
.modal-meta b { color: #1e293b; }
.modal-chapters h4 { font-size: 17px; margin: 0 0 12px; color: #1e293b; font-weight: 700; }
.modal-chapters ul { list-style: none; padding: 0; margin: 0 0 24px; display: grid; gap: 8px; }
.modal-chapters li { padding: 12px 16px; background: #f1f5f9; border-radius: 12px; font-size: 14px; color: #334155; }
.modal-actions { display: flex; gap: 12px; justify-content: flex-end; }

/* ========== Responsive ========== */
@media (max-width: 1120px) {
  .hero-container { grid-template-columns: 1fr; gap: 24px; }
  .hero-title { font-size: 38px; }
  .hero-stats { grid-template-columns: repeat(2, 1fr); }
  .portal-header { padding: 0 20px; flex-wrap: wrap; height: auto; gap: 12px; padding: 12px 20px; }
  .nav-bar { display: none; }
  .search-bar { width: 160px; }
  .course-grid { grid-template-columns: repeat(2, 1fr); }
  .team-grid { grid-template-columns: repeat(2, 1fr); }
  .process-grid { grid-template-columns: repeat(2, 1fr); }
  .subscribe-card { flex-direction: column; text-align: center; }
  .sub-right { grid-template-columns: 1fr 100px; }
  .footer-branding { text-align: center; }
  .portal-footer { flex-direction: column; gap: 24px; text-align: center; }
  .hero-bg { margin: 0 16px; }
  .hero-section { padding: 0 16px 40px; }
}
@media (max-width: 640px) {
  .course-grid, .team-grid, .process-grid { grid-template-columns: 1fr; }
  .hero-stats { grid-template-columns: 1fr 1fr; }
  .hero-actions { flex-direction: column; }
  .search-bar { display: none; }
}
</style>
