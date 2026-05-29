<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="visible" class="wallpaper-overlay" @click.self="handleCancel" @keydown.esc="handleCancel">
        <div class="wallpaper-container">
          <h3 class="modal-title">切换壁纸</h3>

          <!-- 壁纸缩略图网格 -->
          <div class="thumbnail-grid">
            <button
              v-for="wp in wallpapers" :key="wp.id"
              :class="['thumb-card', { selected: selectedId === wp.id }]"
              @click="selectedId = wp.id"
              @mouseenter="previewWallpaper(wp)"
              @mouseleave="clearPreview"
            >
              <img :src="wp.preview || wp.url" :alt="wp.title" class="thumb-img" loading="lazy" />
              <div class="thumb-title">{{ wp.title }}</div>
            </button>
          </div>

          <!-- 亮度与模糊调节 -->
          <div class="slider-group">
            <div class="slider-item">
              <label>壁纸亮度 <span>{{ brightness }}%</span></label>
              <input type="range" min="40" max="150" :value="brightness" @input="onBrightness" />
            </div>
            <div class="slider-item">
              <label>模糊 <span>{{ blur }}px</span></label>
              <input type="range" min="0" max="20" :value="blur" @input="onBlur" />
            </div>
          </div>
          <div class="slider-group">
            <div class="slider-item">
              <label>字体亮度 <span>{{ textContrast }}</span></label>
              <input type="range" min="0" max="100" :value="textContrast" @input="onTextContrast" />
            </div>
          </div>

          <!-- 底部按钮 -->
          <div class="modal-actions">
            <button class="action-btn ghost" @click="handleRestore">恢复默认</button>
            <button class="action-btn ghost" @click="handleCancel">取消</button>
            <button class="action-btn primary" @click="handleConfirm">确认</button>
          </div>

          <button class="close-btn" @click="handleCancel">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { useBackgroundStore } from '../stores/background'

const bgStore = useBackgroundStore()

const props = defineProps({
  visible: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'confirm', 'restore'])

const wallpapers = bgStore.wallpapers
const selectedId = ref(null)
const brightness = ref(bgStore.brightness)
const blur = ref(bgStore.blur)
const textContrast = ref(bgStore.textContrast)
const previousBg = ref(null)

// 打开时快照
watch(() => props.visible, (val) => {
  if (val) {
    selectedId.value = bgStore.current?.id || null
    brightness.value = bgStore.brightness
    blur.value = bgStore.blur
    textContrast.value = bgStore.textContrast
    previousBg.value = { ...bgStore.current, brightness: bgStore.brightness, blur: bgStore.blur, textContrast: bgStore.textContrast }
  }
})

const previewWallpaper = (wp) => {
  bgStore.current = { id: wp.id, title: wp.title, url: wp.url }
  bgStore.applyToDOM()
}

const clearPreview = () => {
  if (!previousBg.value) return
  if (selectedId.value) {
    const wp = wallpapers.find(w => w.id === selectedId.value)
    if (wp) {
      bgStore.current = { id: wp.id, title: wp.title, url: wp.url }
      bgStore.applyToDOM()
      return
    }
  }
  bgStore.current = { ...previousBg.value }
  bgStore.brightness = previousBg.value.brightness
  bgStore.blur = previousBg.value.blur
  bgStore.applyToDOM()
}

const onBrightness = (e) => {
  brightness.value = parseInt(e.target.value)
  bgStore.setBrightness(brightness.value)
}

const onBlur = (e) => {
  blur.value = parseInt(e.target.value)
  bgStore.setBlur(blur.value)
}

const onTextContrast = (e) => {
  textContrast.value = parseInt(e.target.value)
  bgStore.setTextContrast(textContrast.value)
}

const handleConfirm = () => {
  const wp = wallpapers.find(w => w.id === selectedId.value)
  if (wp) {
    bgStore.setBackground(wp)
    bgStore.setBrightness(brightness.value)
    bgStore.setBlur(blur.value)
    bgStore.setTextContrast(textContrast.value)
  }
  emit('close')
}

const handleCancel = () => {
  if (previousBg.value) {
    bgStore.current = { ...previousBg.value }
    bgStore.brightness = previousBg.value.brightness
    bgStore.blur = previousBg.value.blur
    bgStore.textContrast = previousBg.value.textContrast
    bgStore.applyToDOM()
  }
  emit('close')
}

const handleRestore = () => {
  const def = wallpapers[0]
  bgStore.setBackground(def)
  bgStore.setBrightness(85)
  bgStore.setBlur(5)
  bgStore.setTextContrast(30)
  selectedId.value = def.id
  brightness.value = 85
  blur.value = 5
  textContrast.value = 30
  emit('restore')
}

const onKeydown = (e) => {
  if (e.key === 'Escape') handleCancel()
}

watch(() => props.visible, (val) => {
  if (val) document.addEventListener('keydown', onKeydown)
  else document.removeEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* Overlay */
.wallpaper-overlay {
  position: fixed; inset: 0; z-index: 3000;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

/* Container */
.wallpaper-container {
  position: relative;
  width: min(780px, 94vw);
  background: rgba(10, 15, 30, 0.92);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  padding: 28px 28px 22px;
  box-shadow: 0 32px 80px rgba(0, 0, 0, 0.6), inset 0 1px 0 rgba(255,255,255,0.06);
  display: flex; flex-direction: column; gap: 18px;
}

.modal-title {
  margin: 0;
  font-size: 17px; font-weight: 600;
  color: rgba(255,255,255,0.9);
  text-align: center;
}

/* Thumbnail Grid */
.thumbnail-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.thumb-card {
  border: 2px solid transparent;
  border-radius: 12px;
  overflow: hidden;
  background: rgba(255,255,255,0.03);
  cursor: pointer;
  transition: all 0.25s ease;
  padding: 0;
  display: flex; flex-direction: column;
}
.thumb-card:hover {
  border-color: rgba(255,255,255,0.3);
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.4);
}
.thumb-card.selected {
  border-color: #3b82f6;
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.3);
}

.thumb-img {
  width: 100%; height: 90px;
  object-fit: cover; display: block;
}

.thumb-title {
  padding: 8px;
  font-size: 12px;
  color: rgba(255,255,255,0.6);
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Sliders */
.slider-group {
  display: flex; gap: 24px;
}
.slider-item {
  flex: 1; display: flex; flex-direction: column; gap: 6px;
}
.slider-item label {
  display: flex; justify-content: space-between;
  font-size: 13px; color: rgba(255,255,255,0.6);
}
.slider-item label span {
  color: rgba(255,255,255,0.8); font-variant-numeric: tabular-nums;
}
.slider-item input[type="range"] {
  -webkit-appearance: none; appearance: none;
  width: 100%; height: 6px;
  border-radius: 3px;
  background: rgba(255,255,255,0.12);
  outline: none; cursor: pointer;
}
.slider-item input[type="range"]::-webkit-slider-thumb {
  -webkit-appearance: none; appearance: none;
  width: 18px; height: 18px; border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(59,130,246,0.4);
}

/* Actions */
.modal-actions {
  display: flex; justify-content: center; gap: 16px;
}
.action-btn {
  width: 100px; height: 40px; border-radius: 10px;
  border: none; cursor: pointer; font-size: 14px; font-weight: 500;
  transition: all 0.2s;
}
.action-btn.ghost {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.12);
  color: rgba(255,255,255,0.7);
}
.action-btn.ghost:hover { background: rgba(255,255,255,0.1); color: white; }
.action-btn.primary {
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  color: white; box-shadow: 0 4px 16px rgba(59,130,246,0.3);
}
.action-btn.primary:hover { transform: translateY(-1px); box-shadow: 0 6px 24px rgba(59,130,246,0.45); }

/* Close */
.close-btn {
  position: absolute; top: 16px; right: 16px;
  width: 32px; height: 32px; border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.15);
  background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.5);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.close-btn:hover { background: rgba(255,255,255,0.12); color: white; }

/* Transition */
.modal-fade-enter-active { transition: all 0.25s ease; }
.modal-fade-leave-active { transition: all 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
</style>
