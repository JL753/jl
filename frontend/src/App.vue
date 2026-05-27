<template>
  <!-- 启动加载动画 -->
  <LoadingSpinner :duration="2000" @done="spinnerDone = true" />

  <!-- 全局壁纸背景层（静态） -->
  <div class="global-bg-layer" :class="{ hidden: isDynamic }"></div>

  <!-- 全局壁纸背景层（动态） -->
  <video v-if="isDynamic && videoUrl" class="global-bg-video" :class="{ visible: isDynamic }"
    :key="videoUrl" autoplay muted loop playsinline>
    <source :src="videoUrl" type="video/webm" />
  </video>

  <!-- 路由视图 -->
  <router-view v-slot="{ Component, route }">
    <transition :name="route.meta.transition || 'page-fade'" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </router-view>

  <!-- 全局登录弹窗 -->
  <LoginModal v-if="auth.showLoginModal" />
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { useBackgroundStore } from './stores/background'
import LoginModal from './components/LoginModal.vue'
import LoadingSpinner from './components/LoadingSpinner.vue'

const auth = useAuthStore()
const bgStore = useBackgroundStore()
const route = useRoute()
const spinnerDone = ref(false)
const videoUrl = ref('')
const isDynamic = ref(false)

function updateBackground() {
  const id = bgStore.current?.id
  const wp = bgStore.wallpapers.find(w => w.id === id) || bgStore.wallpapers[0]
  isDynamic.value = wp.type === 'dynamic'
  videoUrl.value = wp.type === 'dynamic' ? wp.url : ''

  const root = document.documentElement
  if (!id || id === 'default' || !wp.url) {
    root.style.setProperty('--leleo-bg-image', 'none')
  } else if (wp.type === 'static') {
    root.style.setProperty('--leleo-bg-image', `url("${wp.url}")`)
  }
  root.style.setProperty('--leleo-brightness', `${bgStore.brightness}%`)
  root.style.setProperty('--leleo-blur', `${bgStore.blur}px`)
}

onMounted(() => {
  bgStore.init()
  updateBackground()
})

watch(() => bgStore.current?.id, updateBackground)
watch(() => bgStore.brightness, (v) => {
  document.documentElement.style.setProperty('--leleo-brightness', `${v}%`)
})
watch(() => bgStore.blur, (v) => {
  document.documentElement.style.setProperty('--leleo-blur', `${v}px`)
})
</script>

<style>
/* 全局壁纸背景（静态） */
.global-bg-layer {
  position: fixed;
  inset: 0;
  z-index: -100;
  background-color: #080d1f;
  background-image: var(--leleo-bg-image, none);
  background-size: cover;
  background-position: center;
  filter: brightness(var(--leleo-brightness, 85%)) blur(var(--leleo-blur, 5px));
  transition: background-image 0.8s ease, opacity 0.5s ease;
  transform: scale(1.1);
}
.global-bg-layer.hidden {
  opacity: 0;
}
.global-bg-layer::after {
  content: "";
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
}

/* 全局壁纸背景（动态） */
.global-bg-video {
  position: fixed;
  inset: 0;
  z-index: -100;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  filter: brightness(var(--leleo-brightness, 85%)) blur(var(--leleo-blur, 5px));
  transition: opacity 0.8s ease;
  transform: scale(1.1);
}
.global-bg-video.visible {
  opacity: 1;
}
.global-bg-video::after {
  content: "";
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  pointer-events: none;
}

/* 页面过渡 */
.page-fade-enter-active, .page-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.page-fade-enter-from { opacity: 0; transform: translateY(8px); }
.page-fade-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
