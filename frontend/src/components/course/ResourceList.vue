<template>
  <div class="resource-list">
    <div v-if="items.length === 0" class="empty">暂无资源</div>
    <div v-for="item in items" :key="item.id" class="resource-card glass-card">
      <span class="res-icon">{{ typeIcon(item.type) }}</span>
      <div class="res-info">
        <div class="res-title">{{ item.title }}</div>
        <div class="res-meta">
          <span>{{ item.size || '未知大小' }}</span>
          <span v-if="item.createdAt">{{ formatTime(item.createdAt) }}</span>
        </div>
      </div>
      <a v-if="item.url" :href="item.url" target="_blank" class="res-download">下载</a>
    </div>
  </div>
</template>

<script setup>
defineProps({ items: { type: Array, default: () => [] } })

function typeIcon(type) {
  if (type === '视频') return '▶'; if (type === '课件') return '▤'; if (type === '文档') return '≡'
  return '⊟'
}
function formatTime(t) { return t ? new Date(t).toLocaleDateString('zh-CN') : '' }
</script>

<style scoped>
.resource-list { display: flex; flex-direction: column; gap: 8px; }
.resource-card { display: flex; align-items: center; gap: 12px; padding: 12px; }
.res-icon { font-size: 20px; flex-shrink: 0; }
.res-info { flex: 1; min-width: 0; }
.res-title { font-size: 13px; color: #f1f5f9; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.res-meta { font-size: 10px; color: rgba(255,255,255,0.3); margin-top: 2px; display: flex; gap: 8px; }
.res-download { color: #60d9fa; font-size: 12px; text-decoration: none; flex-shrink: 0; }
.res-download:hover { text-decoration: underline; }
.empty { text-align: center; padding: 40px; font-size: 12px; color: rgba(255,255,255,0.3); }
</style>
