<template>
  <nav class="bottom-tab-bar">
    <router-link
      v-for="tab in tabs"
      :key="tab.path"
      :to="tab.path"
      class="tab-item"
      :class="{ active: isActive(tab.path) }"
    >
      <span class="tab-icon">{{ tab.icon }}</span>
      <span class="tab-label">{{ tab.label }}</span>
    </router-link>
  </nav>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const tabs = [
  { path: '/student/dashboard', icon: '🏠', label: '首页' },
  { path: '/student/knowledge-map', icon: '🗺️', label: '星图' },
  { path: '/student/subjects', icon: '📚', label: '学科' },
  { path: '/student/companion', icon: '🤖', label: 'AI' },
  { path: '/student/profile', icon: '👤', label: '我的' },
]

const isActive = (path) => route.path.startsWith(path)
</script>

<style scoped>
.bottom-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: var(--z-fixed);
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: rgba(8, 13, 31, 0.92);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}
.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 12px;
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.4);
  text-decoration: none;
  font-size: 10px;
  transition: all var(--transition-fast);
}
.tab-item.active {
  color: #60a5fa;
}
.tab-item.active .tab-icon {
  transform: scale(1.1);
}
.tab-icon {
  font-size: 20px;
  transition: transform var(--transition-fast);
}
.tab-label {
  font-weight: 500;
}
</style>
