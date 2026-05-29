<template>
  <div class="announcement-list">
    <div v-if="items.length === 0" class="empty">暂无公告</div>
    <div v-for="item in items" :key="item.id" class="announcement-card glass-card">
      <div class="ann-card-header">
        <span class="ann-type-tag" :class="typeClass(item.type)">{{ item.type }}</span>
        <span class="ann-time">{{ formatTime(item.createdAt) }}</span>
      </div>
      <h4 class="ann-title">{{ item.title }}</h4>
      <p class="ann-content">{{ item.content }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({ items: { type: Array, default: () => [] } })

function typeClass(type) {
  if (type === '重要公告') return 'important'
  if (type === '警示公告') return 'warning'
  return 'normal'
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.announcement-list { display: flex; flex-direction: column; gap: 12px; }
.announcement-card { padding: 14px; }
.ann-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.ann-type-tag { font-size: 10px; padding: 1px 8px; border-radius: 4px; }
.ann-type-tag.normal { background: rgba(59,130,246,0.15); color: #93c5fd; }
.ann-type-tag.important { background: rgba(234,179,8,0.15); color: #fde68a; }
.ann-type-tag.warning { background: rgba(239,68,68,0.15); color: #fca5a5; }
.ann-time { font-size: 10px; color: rgba(255,255,255,0.3); }
.ann-title { font-size: 14px; font-weight: 600; color: #f1f5f9; margin: 0 0 6px; }
.ann-content { font-size: 12px; color: rgba(255,255,255,0.5); line-height: 1.6; margin: 0; }
.empty { text-align: center; padding: 40px; font-size: 12px; color: rgba(255,255,255,0.3); }
.glass-card { background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; backdrop-filter: blur(12px); }
</style>
