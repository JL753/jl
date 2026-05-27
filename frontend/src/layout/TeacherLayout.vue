<template>
  <div class="layout-root">
    <!-- 壁纸选择弹窗 -->
    <WallpaperModal
      :visible="showWallpaperModal"
      :initial-tab="'background'"
      @close="showWallpaperModal = false"
      @restore="showWallpaperModal = false"
    />

    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="logo-area" v-if="!sidebarCollapsed">
          <span class="logo-icon">👨‍🏫</span>
          <span class="logo-text">教师工作台</span>
        </div>
        <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
          <span>{{ sidebarCollapsed ? '›' : '‹' }}</span>
        </button>
      </div>
      <nav class="sidebar-nav">
        <router-link v-for="item in navItems" :key="item.path" :to="item.path"
          class="nav-item" :title="sidebarCollapsed ? item.label : ''">
          <span class="nav-icon">{{ item.icon }}</span>
          <span class="nav-label" v-if="!sidebarCollapsed">{{ item.label }}</span>
        </router-link>
      </nav>
      <div class="sidebar-footer" v-if="!sidebarCollapsed">
        <div class="user-info">
          <div class="avatar">{{ userInitial }}</div>
          <div class="user-meta">
            <div class="user-name">{{ auth.user?.username || '教师' }}</div>
            <div class="user-role">教师</div>
          </div>
        </div>
        <button class="logout-btn" @click="handleLogout">退出</button>
      </div>
      <div class="sidebar-footer-mini" v-else>
        <button class="logout-btn-mini" @click="handleLogout" title="退出">⏻</button>
      </div>
    </aside>
    <div class="main-area">
      <header class="topbar">
        <div class="topbar-left">
          <h2 class="page-title">{{ currentPageTitle }}</h2>
        </div>
        <div class="topbar-right">
          <button class="btn-wallpaper" @click="showWallpaperModal = true" title="切换壁纸">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/></svg>
          </button>
          <span class="topbar-user">{{ auth.user?.username }}</span>
          <span class="topbar-badge teacher">教师</span>
        </div>
      </header>
      <main class="content-area">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import WallpaperModal from '../components/WallpaperModal.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const sidebarCollapsed = ref(false)
const showWallpaperModal = ref(false)

const navItems = [
  { path: '/teacher/dashboard',  icon: '◇', label: '教师首页' },
  { path: '/teacher/assistant',  icon: '○', label: 'AI 助教' },
  { path: '/teacher/manage',     icon: '□', label: '学生管理' },
  { path: '/teacher/classes',    icon: '△', label: '班级管理' },
  { path: '/teacher/content',    icon: '✎', label: '内容管理' },
  { path: '/teacher/review',     icon: '◎', label: 'AI 审核' },
  { path: '/teacher/assignments',icon: '▤', label: '作业管理' },
  { path: '/teacher/exam',       icon: '▤', label: '考试管理' },
  { path: '/teacher/profile',    icon: '◈', label: '个人中心' },
]

const titleMap = Object.fromEntries(navItems.map(i => [i.path, i.label]))
const currentPageTitle = computed(() => titleMap[route.path] || '教师端')
const userInitial = computed(() => (auth.user?.username || 'T')[0].toUpperCase())

function handleLogout() {
  auth.logout()
  router.push('/')
}
</script>

<style scoped>
.layout-root { display: flex; height: 100vh; width: 100vw; overflow: hidden; background: transparent; }
.sidebar {
  width: 220px; min-width: 220px; display: flex; flex-direction: column;
  background: rgba(0,0,0,0.52); backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px);
  border-right: 1px solid rgba(255,255,255,0.08); transition: width 0.25s ease, min-width 0.25s ease; z-index: 10;
}
.sidebar.collapsed { width: 60px; min-width: 60px; }
.sidebar-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 14px 14px; border-bottom: 1px solid rgba(255,255,255,0.07); }
.logo-area { display: flex; align-items: center; gap: 8px; }
.logo-icon { font-size: 22px; }
.logo-text { font-size: 15px; font-weight: 700; color: #e6edf3; letter-spacing: 1px; }
.collapse-btn { background: rgba(255,255,255,0.08); border: none; border-radius: 6px; color: #8b949e; cursor: pointer; width: 28px; height: 28px; font-size: 16px; display: flex; align-items: center; justify-content: center; transition: background 0.2s; }
.collapse-btn:hover { background: rgba(255,255,255,0.15); color: #e6edf3; }
.sidebar-nav { flex: 1; overflow-y: auto; padding: 10px 8px; display: flex; flex-direction: column; gap: 2px; }
.sidebar-nav::-webkit-scrollbar { width: 3px; }
.sidebar-nav::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.15); border-radius: 2px; }
.nav-item { display: flex; align-items: center; gap: 10px; padding: 9px 10px; border-radius: 8px; color: #8b949e; text-decoration: none; font-size: 13.5px; transition: all 0.18s; white-space: nowrap; overflow: hidden; }
.nav-item:hover { background: rgba(255,255,255,0.08); color: #e6edf3; }
.nav-item.router-link-active { background: rgba(63,185,80,0.18); color: #3fb950; border-left: 2px solid #3fb950; }
.nav-icon { font-size: 16px; flex-shrink: 0; }
.nav-label { font-size: 13px; }
.sidebar-footer { padding: 12px 10px; border-top: 1px solid rgba(255,255,255,0.07); }
.user-info { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.avatar { width: 32px; height: 32px; border-radius: 50%; background: linear-gradient(135deg, #3fb950, #58a6ff); display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700; color: #fff; flex-shrink: 0; }
.user-meta { overflow: hidden; }
.user-name { font-size: 13px; color: #e6edf3; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.user-role { font-size: 11px; color: #3fb950; }
.logout-btn { width: 100%; padding: 7px; border-radius: 7px; background: rgba(248,81,73,0.15); border: 1px solid rgba(248,81,73,0.3); color: #f85149; font-size: 12px; cursor: pointer; transition: all 0.2s; }
.logout-btn:hover { background: rgba(248,81,73,0.28); }
.sidebar-footer-mini { padding: 12px 8px; border-top: 1px solid rgba(255,255,255,0.07); display: flex; justify-content: center; }
.logout-btn-mini { background: rgba(248,81,73,0.15); border: 1px solid rgba(248,81,73,0.3); color: #f85149; border-radius: 7px; width: 36px; height: 36px; font-size: 16px; cursor: pointer; transition: all 0.2s; }
.logout-btn-mini:hover { background: rgba(248,81,73,0.28); }
.main-area { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.topbar { display: flex; align-items: center; justify-content: space-between; padding: 0 24px; height: 56px; flex-shrink: 0; background: rgba(0,0,0,0.38); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); border-bottom: 1px solid rgba(255,255,255,0.07); }
.page-title { font-size: 16px; font-weight: 600; color: #e6edf3; margin: 0; }
.topbar-right { display: flex; align-items: center; gap: 10px; }
.topbar-user { font-size: 13px; color: #8b949e; }
.btn-wallpaper {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.06);
  color: rgba(255,255,255,0.5);
  cursor: pointer; transition: all 0.2s;
}
.btn-wallpaper:hover { background: rgba(255,255,255,0.1); color: white; }
.topbar-badge { font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: 600; }
.topbar-badge.teacher { background: rgba(63,185,80,0.18); color: #3fb950; border: 1px solid rgba(63,185,80,0.3); }
.content-area { flex: 1; overflow-y: auto; padding: 24px; background: transparent; }
.content-area::-webkit-scrollbar { width: 5px; }
.content-area::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.12); border-radius: 3px; }
</style>
