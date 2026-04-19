<template>
  <div class="layout-shell admin-theme">
    <aside :class="['shell-sidebar', { collapsed }]">
      <div class="brand-block">
        <div class="brand-mark">管</div>
        <transition name="fade-slide">
          <div v-if="!collapsed" class="brand-copy">
            <strong>智备优教</strong>
            <span>Control Center</span>
            <p>师生账号管理、课程资源配置、数据概览的一体化后台管理中心</p>
          </div>
        </transition>
      </div>

      <nav class="shell-nav">
        <router-link v-for="item in navItems" :key="item.to" class="nav-link" :to="item.to">
          <span class="nav-icon" v-html="item.icon"></span>
          <transition name="fade-slide"><em v-if="!collapsed">{{ item.label }}</em></transition>
        </router-link>
      </nav>

      <div v-if="!collapsed" class="sidebar-foot admin-card">
        <small>控制中心提示</small>
        <strong>支持对师生账号、课程资源进行增删改查操作</strong>
        <p>所有数据变更实时同步到各端展示页面</p>
      </div>
    </aside>

    <div class="shell-main">
      <header class="shell-topbar panel">
        <div class="topbar-left">
          <button class="icon-btn" type="button" @click="collapsed = !collapsed">☰</button>
          <div>
            <div class="crumb-text">管理端 / 控制面板</div>
            <strong>{{ auth.user?.displayName || '管理员' }}</strong>
          </div>
        </div>

        <div class="topbar-right">
          <div class="search-box">
            <span>⌕</span>
            <input placeholder="搜索账号、资源、配置" />
          </div>
          <button class="icon-btn ghost" type="button">◔</button>
          <button class="profile-chip" type="button" @click="router.push('/admin/profile')">
            <img v-if="auth.user?.avatarUrl" :src="auth.user.avatarUrl" alt="avatar" class="profile-avatar" />
            <span v-else class="profile-avatar text-avatar">管</span>
            <span class="profile-meta">
              <strong>{{ auth.user?.username || 'admin' }}</strong>
              <small>管理员账号</small>
            </span>
          </button>
          <el-button type="danger" plain @click="logout">退出</el-button>
        </div>
      </header>

      <main class="shell-content admin-content">
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
  { to: '/admin/dashboard', label: '仪表盘', icon: '📊' },
  { to: '/admin/users', label: '用户管理', icon: '👥' },
  { to: '/admin/courses', label: '课程管理', icon: '📚' },
  { to: '/admin/resources', label: '资源管理', icon: '📦' },
  { to: '/admin/exams', label: '考试管理', icon: '📝' },
  { to: '/admin/logs', label: '操作日志', icon: '📋' },
  { to: '/admin/settings', label: '系统设置', icon: '⚙️' },
  { to: '/admin/profile', label: '个人信息', icon: '👤' },
  { to: '/portal', label: '门户首页', icon: '🏠' }
]

onMounted(async () => {
  if (!auth.user && auth.token) {
    try { await auth.fetchMe() } catch(e) { /* ignore */ }
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
  background: $gradient-primary;
  color: $bg-white;
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
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
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.08), rgba(118, 75, 162, 0.08));
    color: $primary-color;
    font-weight: $font-weight-semibold;
    box-shadow: $glow-primary;
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
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05), rgba(118, 75, 162, 0.05));
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

.admin-content {
  padding: 0;
  display: grid;
  gap: $spacing-lg;
  align-content: start;
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
    top: 12px;
    flex-direction: column;
    align-items: stretch;
    padding: 12px;
  }

  .search-box {
    width: 100%;
  }
}

@media (max-width: 760px) {
  .layout-shell {
    padding: 12px;
    gap: 12px;
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
