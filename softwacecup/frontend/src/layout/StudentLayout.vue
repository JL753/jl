<template>
  <div class="layout-root">
    <!-- 壁纸选择弹窗 -->
    <WallpaperModal
      :visible="showWallpaperModal"
      :initial-tab="'background'"
      @close="showWallpaperModal = false"
      @restore="showWallpaperModal = false"
    />

    <!-- 顶部导航栏 -->
    <TopNavBar
      @toggle-wallpaper="showWallpaperModal = true"
      @toggle-notification="handleToggleNotification"
    />

    <!-- 主内容区（留出顶部导航和底部标签栏的空间） -->
    <main class="content-area">
      <router-view />
    </main>

    <!-- 底部标签栏（仅学生路由显示） -->
    <BottomTabBar v-if="isStudentRoute" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import WallpaperModal from '../components/WallpaperModal.vue'
import TopNavBar from '../components/TopNavBar.vue'
import BottomTabBar from '../components/BottomTabBar.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const showWallpaperModal = ref(false)

const isStudentRoute = computed(() => route.path.startsWith('/student'))

function handleLogout() {
  auth.logout()
  router.push('/')
}

function handleToggleNotification() {
  // TODO: 实现通知面板
}
</script>

<style scoped>
.layout-root {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  background: transparent;
}

.content-area {
  flex: 1;
  margin-top: 56px;   /* TopNavBar 高度 */
  margin-bottom: 60px; /* BottomTabBar 高度 */
  overflow-y: auto;
  background: transparent;
}

.content-area::-webkit-scrollbar {
  width: 5px;
}
.content-area::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.12);
  border-radius: 3px;
}
</style>
