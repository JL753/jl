<template>
  <div class="course-design">
    <div class="design-section"><h4 class="design-label">课程背景</h4><p class="design-text">{{ background || '暂无' }}</p></div>
    <div class="design-section"><h4 class="design-label">教学目标</h4><p class="design-text">{{ target || '暂无' }}</p></div>
    <div class="design-section"><h4 class="design-label">设计原则</h4><p class="design-text">{{ principle || '暂无' }}</p></div>
  </div>
</template>
<script setup>
defineProps({ background: { type: String, default: '' }, target: { type: String, default: '' }, principle: { type: String, default: '' } })
</script>
<style scoped>
.course-design { display: flex; flex-direction: column; gap: 20px; }
.design-label { font-size: 13px; font-weight: 600; color: #93c5fd; margin: 0 0 6px; }
.design-text { font-size: 13px; color: rgba(255,255,255,0.6); line-height: 1.7; margin: 0; }
</style>
