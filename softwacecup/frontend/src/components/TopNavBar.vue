<template>
  <header class="top-nav-bar">
    <div class="top-left">
      <div class="logo-icon">知</div>
      <span class="logo-text">知域</span>
    </div>

    <div class="top-center">
      <div class="search-box" @click="goAICompanion">
        <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
        <span class="search-placeholder">AI 爬虫搜索 · 知识探索</span>
      </div>
    </div>

    <div class="top-right">
      <button class="btn-icon" @click="showWallpaper = true" title="切换壁纸">
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <path d="m21 15-5-5L5 21" />
        </svg>
      </button>
      <button class="btn-icon notification-btn" title="通知">
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
      </button>
      <div class="user-avatar" :title="auth.user?.username || '用户'">
        {{ userInitial }}
      </div>
    </div>

    <WallpaperModal :visible="showWallpaper" @close="showWallpaper = false" />
  </header>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import WallpaperModal from './WallpaperModal.vue'

const auth = useAuthStore()
const router = useRouter()
const showWallpaper = ref(false)

const userInitial = computed(() => {
  return (auth.user?.username || 'U')[0].toUpperCase()
})

function goAICompanion() {
  if (auth.isLoggedIn) {
    router.push('/student/companion')
  } else {
    auth.openLoginModal('/student/companion')
  }
}
</script>

<style scoped>
.top-nav-bar {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: var(--z-topbar);
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: rgba(8, 13, 31, 0.72);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

/* Left */
.top-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.logo-icon {
  width: 30px; height: 30px;
  border-radius: 7px;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  display: flex; align-items: center; justify-content: center;
  font-size: 15px; font-weight: 700; color: #fff;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}
.logo-text {
  font-size: 16px; font-weight: 700;
  color: #e6edf3;
  letter-spacing: 2px;
}

/* Center — search */
.top-center {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 0 24px;
  max-width: 480px;
}
.search-box {
  width: 100%;
  height: 36px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.search-box:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.18);
}
.search-icon {
  color: rgba(255, 255, 255, 0.35);
  flex-shrink: 0;
}
.search-placeholder {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.3);
}

/* Right */
.top-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.btn-icon {
  width: 34px; height: 34px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.45);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.btn-icon:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #e6edf3;
  border-color: rgba(255, 255, 255, 0.15);
}
.user-avatar {
  width: 32px; height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #a855f7);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700; color: #fff;
  cursor: pointer; flex-shrink: 0;
  transition: box-shadow 0.2s;
  margin-left: 2px;
}
.user-avatar:hover {
  box-shadow: 0 0 12px rgba(59, 130, 246, 0.4);
}

/* Responsive */
@media (max-width: 640px) {
  .top-center { display: none; }
  .logo-text { display: none; }
}
</style>
