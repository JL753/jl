<template>
  <span class="progress-dot" :class="status" :title="statusText" />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, default: 'pending' }, // pending | in_progress | completed
})

const statusText = computed(() => {
  const map = { pending: '未开始', in_progress: '进行中', completed: '已完成' }
  return map[props.status] || ''
})
</script>

<style scoped>
.progress-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  transition: all var(--transition-base);
}
.pending {
  background: rgba(255, 255, 255, 0.15);
}
.in_progress {
  background: #3b82f6;
  box-shadow: 0 0 8px rgba(59, 130, 246, 0.5);
}
.completed {
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
}
</style>
