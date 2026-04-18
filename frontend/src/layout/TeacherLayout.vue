<template>
  <div class="layout-shell teacher-theme">
    <aside :class="['shell-sidebar', { collapsed }]">
      <div class="brand-block">
        <div class="brand-mark">教</div>
        <transition name="fade-slide">
          <div v-if="!collapsed" class="brand-copy">
            <strong>智备优教</strong>
            <span>Teacher Studio</span>
            <p>教学设计、题库管理、考试组织与数据洞察一体化工作台</p>
          </div>
        </transition>
      </div>

      <nav class="shell-nav">
        <router-link v-for="item in navItems" :key="item.to" class="nav-link" :to="item.to">
          <span class="nav-icon">{{ item.icon }}</span>
          <transition name="fade-slide"><em v-if="!collapsed">{{ item.label }}</em></transition>
        </router-link>
      </nav>

      <div v-if="!collapsed" class="sidebar-foot teacher-card">
        <small>本周教学提醒</small>
        <strong>可继续进行教案优化、题库补充与考试发布。</strong>
        <p>建议优先检查资源库、组卷策略和班级考试进度。</p>
      </div>
    </aside>

    <div class="shell-main">
      <header class="shell-topbar panel">
        <div class="topbar-left">
          <button class="icon-btn" type="button" @click="collapsed = !collapsed">☰</button>
          <div>
            <div class="crumb-text">教师端 / 智能教学工作台</div>
            <strong>{{ auth.user?.displayName || '教师用户' }}</strong>
          </div>
        </div>

        <div class="topbar-right">
          <div class="search-box">
            <span>⌕</span>
            <input placeholder="搜索课程、资源、试题、考试" />
          </div>
          <button class="icon-btn ghost" type="button">◔</button>
          <button class="icon-btn ghost" type="button">⤢</button>
          <button class="profile-chip" type="button" @click="router.push('/teacher/profile')">
            <img v-if="auth.user?.avatarUrl" :src="auth.user.avatarUrl" alt="avatar" class="profile-avatar" />
            <span v-else class="profile-avatar text-avatar">师</span>
            <span class="profile-meta">
              <strong>{{ auth.user?.username || 'teacher' }}</strong>
              <small>教师账号</small>
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
  { to: '/teacher/dashboard', label: '首页', icon: '⌂' },
  { to: '/teacher/assistant', label: '教学助手', icon: '★' },
  { to: '/teacher/manage', label: '题库管理', icon: '▤' },
  { to: '/teacher/exam', label: '考试管理', icon: '▣' },
  { to: '/teacher/center', label: '数据中台', icon: '▥' },
  { to: '/teacher/resources', label: '课程资源平台', icon: '⎘' },
  { to: '/teacher/profile', label: '个人信息', icon: '◪' }
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
  top: $spacing-lg;
  width: 280px;
  height: fit-content;
  max-height: calc(100vh - #{$spacing-lg * 2});
  background: $bg-white;
  border-radius: $radius-xlarge;
  box-shadow: $shadow-card;
  padding: $spacing-xl 0;
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
  transition: width 0.3s ease, box-shadow $transition-base;
  overflow: hidden;

  &:hover {
    box-shadow: $shadow-card-hover;
  }

  &.collapsed {
    width: 80px;
  }
}

.brand-block {
  padding: 0 $spacing-xl;
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.brand-mark {
  width: 52px;
  height: 52px;
  border-radius: $radius-medium;
  background: $gradient-blue;
  color: $bg-white;
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.brand-copy {
  display: grid;
  gap: 2px;

  strong {
    font-size: $font-size-xl;
    color: $text-primary;
    font-weight: $font-weight-bold;
  }

  span {
    font-size: 11px;
    color: $text-secondary;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  p {
    font-size: $font-size-xs;
    color: $text-secondary;
    margin-top: 6px;
    line-height: 1.5;
  }
}

.shell-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 0 $spacing-sm;
  overflow-y: auto;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  border-radius: $radius-medium;
  color: $text-regular;
  text-decoration: none;
  transition: all $transition-base;
  font-size: $font-size-base;
  font-weight: $font-weight-medium;

  &:hover {
    background: $bg-lighter;
    color: $primary-color;
    transform: translateX(2px);
  }

  &.router-link-active {
    background: linear-gradient(135deg, rgba(64, 158, 255, 0.08), rgba(102, 126, 234, 0.08));
    color: $primary-color;
    font-weight: $font-weight-semibold;
    box-shadow: $glow-primary;
  }

  em {
    font-style: normal;
  }
}

.nav-icon {
  font-size: $font-size-lg;
  flex-shrink: 0;
}

.sidebar-foot {
  margin: 0 $spacing-sm;
  padding: $spacing-md;
  border-radius: $radius-medium;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.05), rgba(102, 126, 234, 0.05));
  border: 1px solid $border-extra-light;

  small {
    font-size: 11px;
    color: $text-secondary;
    text-transform: uppercase;
    font-weight: $font-weight-semibold;
  }

  strong {
    display: block;
    margin-top: 6px;
    font-size: $font-size-sm;
    color: $text-primary;
    line-height: 1.5;
  }

  p {
    margin-top: 6px;
    font-size: $font-size-xs;
    color: $text-secondary;
    line-height: 1.5;
  }
}

.shell-main {
  min-height: calc(100vh - #{$spacing-lg * 2});
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.shell-topbar {
  position: sticky;
  top: $spacing-lg;
  z-index: $z-index-sticky;
  background: $bg-white;
  border-radius: $radius-large;
  box-shadow: $shadow-card;
  padding: $spacing-md $spacing-xl;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $spacing-lg;
  transition: box-shadow $transition-base;

  &:hover {
    box-shadow: $shadow-card-hover;
  }
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.crumb-text {
  font-size: $font-size-xs;
  color: $text-secondary;
}

.topbar-left strong {
  font-size: $font-size-md;
  color: $text-primary;
  font-weight: $font-weight-semibold;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  background: $bg-lighter;
  border: 1px solid $border-light;
  border-radius: $radius-medium;
  padding: 10px $spacing-md;
  min-width: 280px;
  transition: all $transition-base;

  &:hover {
    border-color: $border-base;
  }

  &:focus-within {
    border-color: $primary-color;
    box-shadow: $glow-primary;
  }

  span {
    color: $text-secondary;
    font-size: $font-size-lg;
  }

  input {
    flex: 1;
    border: none;
    background: none;
    outline: none;
    font-size: $font-size-sm;
    color: $text-primary;

    &::placeholder {
      color: $text-placeholder;
    }
  }
}

.icon-btn {
  width: 42px;
  height: 42px;
  border-radius: $radius-medium;
  border: 1px solid $border-light;
  background: $bg-lighter;
  color: $text-regular;
  cursor: pointer;
  font-size: $font-size-md;
  transition: all $transition-base;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background: $bg-white;
    border-color: $primary-color;
    color: $primary-color;
    transform: translateY(-1px);
  }

  &.ghost {
    background: rgba(248, 251, 255, 0.8);
  }
}

.profile-chip {
  border: 1px solid $border-light;
  background: $bg-white;
  border-radius: $radius-round;
  padding: 6px 12px 6px 6px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all $transition-base;

  &:hover {
    border-color: $primary-color;
    box-shadow: $shadow-light;
    transform: translateY(-1px);
  }
}

.profile-meta {
  display: grid;
  text-align: left;

  strong {
    color: $text-primary;
    font-size: $font-size-sm;
    font-weight: $font-weight-semibold;
  }

  small {
    color: $text-secondary;
    font-size: $font-size-xs;
  }
}

.profile-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  object-fit: cover;
  background: $gradient-blue;
}

.text-avatar {
  display: grid;
  place-items: center;
  color: $bg-white;
  font-weight: $font-weight-bold;
  font-size: $font-size-lg;
}

.shell-content {
  min-width: 0;
  flex: 1;
  padding-bottom: $spacing-lg;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all $transition-base;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-6px);
}

@media (max-width: 1280px) {
  .layout-shell {
    grid-template-columns: 1fr;
  }

  .shell-sidebar {
    position: relative;
    top: 0;
    width: 100%;
    min-height: auto;

    &.collapsed {
      width: 100%;
    }
  }

  .shell-topbar {
    top: $spacing-sm;
    flex-direction: column;
    align-items: stretch;
  }

  .search-box {
    width: 100%;
  }
}

@media (max-width: 760px) {
  .layout-shell {
    padding: $spacing-sm;
    gap: $spacing-sm;
  }

  .shell-main {
    min-height: auto;
  }

  .topbar-right {
    justify-content: stretch;
  }

  .profile-chip {
    width: 100%;
    justify-content: center;
  }
}
</style>
.shell-sidebar {
  position: sticky;
  top: 18px;
  align-self: start;
  width: 276px;
  min-height: calc(100vh - 36px);
  padding: 18px 16px;
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255,255,255,.98), rgba(248,250,255,.94));
  border: 1px solid var(--panel-border);
  box-shadow: var(--panel-shadow-soft);
  display: flex;
  flex-direction: column;
  gap: 18px;
  transition: width .28s ease;
}
.shell-sidebar.collapsed { width: 94px; }
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
  background: linear-gradient(135deg, #4b82ff, #79b8ff);
  box-shadow: 0 16px 30px rgba(75, 130, 255, .24);
}
.brand-copy strong { display: block; font-size: 28px; color: #2e65c7; }
.brand-copy span { display: block; color: #8ea0b8; font-size: 12px; letter-spacing: 1.4px; text-transform: uppercase; }
.brand-copy p { margin: 8px 0 0; color: #768aa3; line-height: 1.75; font-size: 12px; }
.shell-nav { display: grid; gap: 8px; }
.nav-link {
  min-height: 50px;
  padding: 0 14px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #4c607d;
  transition: all .22s ease;
}
.nav-link:hover,
.nav-link.router-link-active {
  background: linear-gradient(90deg, #edf4ff, #ffffff 86%);
  color: #407cf0;
  box-shadow: inset 3px 0 0 #6ea6ff;
}
.nav-icon { width: 20px; text-align: center; }
.nav-link em { font-style: normal; }
.sidebar-foot {
  margin-top: auto;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid #e5edfa;
}
.teacher-card { background: linear-gradient(180deg, #f5f9ff, #fffdf6); }
.sidebar-foot small { color: #7d8faa; }
.sidebar-foot strong { display: block; margin-top: 10px; color: #34506f; line-height: 1.7; }
.sidebar-foot p { margin: 8px 0 0; color: #8c9db2; line-height: 1.8; font-size: 12px; }
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
.topbar-left,
.topbar-right { display: flex; align-items: center; gap: 12px; }
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
  background: #f7faff;
  border: 1px solid #e6eef8;
  color: #8da0b8;
}
.search-box input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: #536882;
}
.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: 13px;
  border: 1px solid #e6edf7;
  background: #f8fbff;
  color: #7488a4;
  cursor: pointer;
}
.icon-btn.ghost { background: rgba(248, 251, 255, .78); }
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
  background: linear-gradient(135deg, #e8f0ff, #dff8ff);
}
.text-avatar {
  display: grid;
  place-items: center;
  color: #4876e6;
  font-weight: 800;
}
.shell-content {
  min-width: 0;
  flex: 1;
  padding-bottom: 20px;
}
.fade-slide-enter-active,
.fade-slide-leave-active { transition: all .2s ease; }
.fade-slide-enter-from,
.fade-slide-leave-to { opacity: 0; transform: translateX(-6px); }
@media (max-width: 1280px) {
  .layout-shell { grid-template-columns: 1fr; }
  .shell-sidebar {
    position: relative;
    top: 0;
    width: 100%;
    min-height: auto;
  }
  .shell-sidebar.collapsed { width: 100%; }
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
