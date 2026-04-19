<template>
  <div class="layout-shell student-theme">
    <aside :class="['shell-sidebar', { collapsed }]">
      <div class="brand-block">
        <div class="brand-mark">学</div>
        <transition name="fade-slide">
          <div v-if="!collapsed" class="brand-copy">
            <strong>智备优教</strong>
            <span>Student Lab</span>
            <p>学习路径、智能问答、考试训练与数字人讲解的一站式学习空间</p>
          </div>
        </transition>
      </div>

      <nav class="shell-nav">
        <router-link v-for="item in navItems" :key="item.to" class="nav-link" :to="item.to">
          <span class="nav-icon">{{ item.icon }}</span>
          <transition name="fade-slide"><em v-if="!collapsed">{{ item.label }}</em></transition>
        </router-link>
      </nav>

      <div v-if="!collapsed" class="sidebar-foot student-card">
        <small>学习建议</small>
        <strong>建议优先完成今日课程，再进行问答复盘与考试冲刺训练。</strong>
        <p>你可以在资源推荐、图片生成、数字人讲解之间无缝切换。</p>
      </div>
    </aside>

    <div class="shell-main">
      <header class="shell-topbar panel">
        <div class="topbar-left">
          <button class="icon-btn" type="button" @click="collapsed = !collapsed">☰</button>
          <div>
            <div class="crumb-text">学生端 / 智能学习工作台</div>
            <strong>{{ auth.user?.displayName || '学生用户' }}</strong>
          </div>
        </div>

        <div class="topbar-right">
          <div class="search-box">
            <span>⌕</span>
            <input placeholder="搜索知识点、课程、考试、讲解" />
          </div>
          <button class="icon-btn ghost" type="button">◔</button>
          <button class="icon-btn ghost" type="button">⤢</button>
          <button class="profile-chip" type="button" @click="router.push('/student/profile')">
            <img v-if="auth.user?.avatarUrl" :src="auth.user.avatarUrl" alt="avatar" class="profile-avatar" />
            <span v-else class="profile-avatar text-avatar">学</span>
            <span class="profile-meta">
              <strong>{{ auth.user?.username || 'student' }}</strong>
              <small>学生账号</small>
            </span>
          </button>
          <el-button type="danger" plain @click="logout">退出</el-button>
        </div>
      </header>

      <main class="shell-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const collapsed = ref(false)

const navItems = [
  { to: '/student/dashboard', label: '首页', icon: '⌂' },
  { to: '/student/companion', label: '智能学习', icon: '✎' },
  { to: '/student/courses', label: '课程平台', icon: '📚' },
  { to: '/student/exam', label: '开始考试', icon: '➤' },
  { to: '/student/report', label: '学习报告', icon: '▣' },
  { to: '/student/profile', label: '个人信息', icon: '◪' }
]

onMounted(async () => {
  if (!auth.user && auth.token) {
    await auth.fetchMe()
  }
})

const logout = () => {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.layout-shell {
  display: grid;
  grid-template-columns: auto 1fr;
  min-height: 100vh;
  background: $bg-page;
  padding: $spacing-lg;
  gap: $spacing-lg;
}

.shell-sidebar {
  position: sticky;
  top: 18px;
  align-self: start;
  width: 276px;
  min-height: calc(100vh - 36px);
  padding: 18px 16px;
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255,255,255,.98), rgba(255,252,243,.95));
  border: 1px solid var(--panel-border);
  box-shadow: var(--panel-shadow-soft);
  display: flex;
  flex-direction: column;
  gap: 18px;
  transition: width .28s ease;
  
  &.collapsed { width: 94px; }
}

.brand-block {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 10px 8px 18px;
  border-bottom: 1px solid #edf2f8;
}

.brand-mark {
  width: 52px;
  height: 52px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  font-size: 20px;
  font-weight: 800;
  color: white;
  background: linear-gradient(135deg, #18b48f, #6fd8b9);
  box-shadow: 0 16px 30px rgba(24, 180, 143, .24);
}

.brand-copy {
  strong { display: block; font-size: 28px; color: #1f9f82; }
  span { display: block; color: #8ea0b8; font-size: 12px; letter-spacing: 1.4px; text-transform: uppercase; }
  p { margin: 8px 0 0; color: #768aa3; line-height: 1.75; font-size: 12px; }
}

.shell-nav { display: grid; gap: 8px; }

.nav-link {
  min-height: 50px;
  padding: 0 14px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #4c607d;
  text-decoration: none;
  transition: all .22s ease;
  
  &:hover, &.router-link-active {
    background: linear-gradient(90deg, #effdf8, #ffffff 86%);
    color: #16a183;
    box-shadow: inset 3px 0 0 #59d0b3;
  }
}

.nav-icon { width: 20px; text-align: center; }
.nav-link em { font-style: normal; }

.sidebar-foot {
  margin-top: auto;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid #e7f0ef;
  background: linear-gradient(180deg, #f2fffb, #fffdf6);
  
  small { color: #7d8faa; }
  strong { display: block; margin-top: 10px; color: #34506f; line-height: 1.7; }
  p { margin: 8px 0 0; color: #8c9db2; line-height: 1.8; font-size: 12px; }
}

.shell-main {
  min-width: 0;
  min-height: calc(100vh - 36px);
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.shell-topbar {
  position: sticky;
  top: 18px;
  z-index: 12;
  min-height: 82px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: rgba(255,255,255,.82);
  backdrop-filter: blur(18px);
}

.topbar-left, .topbar-right { display: flex; align-items: center; gap: 12px; }
.topbar-right { flex-wrap: wrap; justify-content: flex-end; }
.crumb-text { margin-bottom: 4px; font-size: 12px; color: #97a7bb; }

.search-box {
  width: min(34vw, 340px);
  min-width: 220px;
  height: 44px;
  border-radius: 999px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f8fffc;
  border: 1px solid #e4f3ee;
  color: #8da0b8;
  
  input {
    width: 100%;
    border: 0;
    outline: 0;
    background: transparent;
    color: #536882;
  }
}

.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: 13px;
  border: 1px solid #e6edf7;
  background: #f8fbff;
  color: #7488a4;
  cursor: pointer;
  
  &.ghost { background: rgba(248, 251, 255, .78); }
}

.profile-chip {
  border: 1px solid #e7eef8;
  background: #fbfdff;
  border-radius: 999px;
  padding: 6px 10px 6px 6px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.profile-meta { display: grid; text-align: left; }
.profile-meta strong { color: #49607d; font-size: 13px; }
.profile-meta small { color: #9aacbf; }

.profile-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  object-fit: cover;
  background: linear-gradient(135deg, #e7fff7, #dff8ff);
}

.text-avatar {
  display: grid;
  place-items: center;
  color: #1aa085;
  font-weight: 800;
}

.shell-content {
  min-width: 0;
  flex: 1;
  padding-bottom: 20px;
}

.fade-slide-enter-active, .fade-slide-leave-active { transition: all .2s ease; }
.fade-slide-enter-from, .fade-slide-leave-to { opacity: 0; transform: translateX(-6px); }

@media (max-width: 1280px) {
  .layout-shell { grid-template-columns: 1fr; }
  .shell-sidebar {
    position: relative;
    top: 0;
    width: 100%;
    min-height: auto;
    &.collapsed { width: 100%; }
  }
  .shell-topbar {
    top: 12px;
    flex-direction: column;
    align-items: stretch;
  }
  .search-box { width: 100%; }
}

@media (max-width: 760px) {
  .layout-shell { padding: 12px; gap: 12px; }
  .shell-main { min-height: auto; }
  .topbar-right { justify-content: stretch; }
  .profile-chip { width: 100%; justify-content: center; }
}
</style>
