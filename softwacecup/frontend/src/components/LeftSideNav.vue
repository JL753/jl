<template>
  <nav class="left-side-nav">
    <div class="nav-logo" @click="$router.push('/student/dashboard')">
      <span class="logo-text">知</span>
    </div>

    <div class="nav-items">
      <button
        v-for="item in navItems"
        :key="item.path"
        class="nav-item"
        :class="{ active: isActive(item) }"
        @click="$router.push(item.path)"
        :title="item.label"
      >
        <svg class="nav-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" v-html="item.iconSvg"></svg>
        <span class="nav-label">{{ item.label }}</span>
      </button>
    </div>

    <div class="nav-bottom">
      <button
        class="nav-item"
        :class="{ active: isActive({ path: '/student/profile' }) }"
        @click="$router.push('/student/profile')"
        title="我的"
      >
        <svg class="nav-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
          <circle cx="12" cy="7" r="4"/>
        </svg>
        <span class="nav-label">我的</span>
      </button>
    </div>
  </nav>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  {
    label: '首页',
    path: '/student/dashboard',
    iconSvg: '<rect x="3" y="3" width="18" height="18" rx="2"/><polyline points="9 3 9 21"/>'
  },
  {
    label: '学科',
    path: '/student/subjects',
    iconSvg: '<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>'
  },
  {
    label: '星图',
    path: '/student/knowledge-map',
    iconSvg: '<circle cx="12" cy="12" r="3"/><circle cx="19" cy="5" r="2"/><circle cx="5" cy="19" r="2"/><circle cx="5" cy="5" r="2"/><circle cx="19" cy="19" r="2"/><line x1="13.4" y1="10.6" x2="17.2" y2="6.8"/><line x1="10.6" y1="13.4" x2="6.8" y2="17.2"/><line x1="13.4" y1="13.4" x2="17.2" y2="17.2"/><line x1="10.6" y1="10.6" x2="6.8" y2="6.8"/>'
  },
  {
    label: 'AI',
    path: '/student/companion',
    iconSvg: '<path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2z"/><path d="M9 12a3 3 0 1 0 6 0 3 3 0 1 0-6 0z"/><path d="M7 17c.94.94 2.59 1.53 5 1.53s4.06-.59 5-1.53"/>'
  },
  {
    label: '沉浸',
    path: '/student/companion-immersive',
    iconSvg: '<circle cx="12" cy="12" r="10"/><circle cx="12" cy="10" r="4"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="M4.93 4.93l1.41 1.41"/><path d="M17.66 17.66l1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/>'
  }
]

function isActive(item) {
  if (item.path === '/student/dashboard') return route.path === '/student/dashboard'
  return route.path.startsWith(item.path)
}
</script>

<style scoped>
.left-side-nav {
  position: fixed;
  left: 0;
  top: 56px;
  bottom: 0;
  width: 72px;
  background: rgba(8, 13, 31, 0.95);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0;
  z-index: 100;
  backdrop-filter: blur(12px);
}

.nav-logo {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #a855f7);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  margin-bottom: 16px;
}

.logo-text {
  font-weight: 800;
  font-size: 18px;
  color: #fff;
}

.nav-items {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.nav-item {
  width: 56px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  background: transparent;
  border: none;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.4);
  transition: all 0.15s;
  font-family: inherit;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.7);
}

.nav-item.active {
  background: rgba(59, 130, 246, 0.15);
  color: #60d9fa;
}

.nav-label {
  font-size: 10px;
  font-weight: 500;
  line-height: 1;
}

.nav-bottom {
  margin-top: auto;
}
</style>
