<template>
  <div class="layout-root">
    <WallpaperModal
      :visible="showWallpaperModal"
      :initial-tab="'background'"
      @close="showWallpaperModal = false"
      @restore="showWallpaperModal = false"
    />

    <TopNavBar
      @toggle-wallpaper="showWallpaperModal = true"
      @toggle-notification="handleToggleNotification"
    />

    <LeftSideNav />

    <main class="content-area">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import WallpaperModal from '../components/WallpaperModal.vue'
import TopNavBar from '../components/TopNavBar.vue'
import LeftSideNav from '../components/LeftSideNav.vue'

const showWallpaperModal = ref(false)

function handleToggleNotification() {
  // 保留通知面板入口
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
  margin-top: 56px;
  margin-left: 72px;
  overflow-y: auto;
  background: transparent;
  min-height: calc(100vh - 56px);
}

.content-area::-webkit-scrollbar {
  width: 5px;
}
.content-area::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.12);
  border-radius: 3px;
}
</style>
