<template>
  <transition name="spinner-fade" @after-leave="$emit('done')">
    <div v-if="visible" class="spinner-overlay">
      <div class="spinner-3d">
        <div class="cube">
          <div class="face front">知</div>
          <div class="face back">域</div>
          <div class="face right">学</div>
          <div class="face left">习</div>
          <div class="face top">探</div>
          <div class="face bottom">索</div>
        </div>
        <div class="orbit-ring o1"></div>
        <div class="orbit-ring o2"></div>
        <div class="orbit-ring o3"></div>
      </div>
      <p class="spinner-text">加载中...</p>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const props = defineProps({
  duration: { type: Number, default: 1500 }
})
defineEmits(['done'])

const visible = ref(true)

onMounted(() => {
  setTimeout(() => { visible.value = false }, props.duration)
})
</script>

<style scoped>
.spinner-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #080d1f;
}

.spinner-3d {
  position: relative;
  width: 120px;
  height: 120px;
  perspective: 600px;
}

.cube {
  position: absolute;
  width: 60px;
  height: 60px;
  top: 30px;
  left: 30px;
  transform-style: preserve-3d;
  animation: cube-spin 1.8s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
}

.face {
  position: absolute;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 800;
  color: #e6edf3;
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: 6px;
}

.front  { background: rgba(59,130,246,0.25); transform: translateZ(30px); }
.back   { background: rgba(6,182,212,0.25); transform: rotateY(180deg) translateZ(30px); }
.right  { background: rgba(139,92,246,0.25); transform: rotateY(90deg) translateZ(30px); }
.left   { background: rgba(236,72,153,0.25); transform: rotateY(-90deg) translateZ(30px); }
.top    { background: rgba(16,185,129,0.25); transform: rotateX(90deg) translateZ(30px); }
.bottom { background: rgba(245,158,11,0.25); transform: rotateX(-90deg) translateZ(30px); }

@keyframes cube-spin {
  0%   { transform: rotateX(-20deg) rotateY(0deg); }
  50%  { transform: rotateX(20deg) rotateY(180deg); }
  100% { transform: rotateX(-20deg) rotateY(360deg); }
}

.orbit-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  border: 1px solid rgba(59, 130, 246, 0.15);
  pointer-events: none;
}

.o1 {
  width: 100px; height: 100px;
  animation: ring-spin 3s linear infinite;
  border-color: rgba(59, 130, 246, 0.2);
}

.o2 {
  width: 120px; height: 120px;
  animation: ring-spin 2.4s linear infinite reverse;
  border-color: rgba(6, 182, 212, 0.15);
}

.o3 {
  width: 80px; height: 80px;
  animation: ring-spin 2s linear infinite;
  border-top: 2px solid rgba(139, 92, 246, 0.3);
  border-right: 2px solid transparent;
  border-bottom: 2px solid transparent;
  border-left: 2px solid transparent;
  border-radius: 50%;
}

@keyframes ring-spin {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to   { transform: translate(-50%, -50%) rotate(360deg); }
}

.spinner-text {
  margin-top: 24px;
  color: rgba(255,255,255,0.35);
  font-size: 13px;
  letter-spacing: 3px;
  animation: pulse-text 1.5s ease-in-out infinite;
}

@keyframes pulse-text {
  0%, 100% { opacity: 0.35; }
  50%      { opacity: 0.8; }
}

.spinner-fade-leave-active {
  transition: opacity 0.6s ease, transform 0.6s ease;
}

.spinner-fade-leave-to {
  opacity: 0;
  transform: scale(1.05);
}
</style>
