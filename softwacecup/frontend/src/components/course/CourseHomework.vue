<template>
  <div class="course-homework">
    <div v-if="items.length === 0" class="empty">暂无作业</div>
    <div v-for="item in items" :key="item.id" class="hw-card glass-card">
      <span class="hw-type-icon">{{ item.type === '测试' ? '📝' : '📋' }}</span>
      <div class="hw-info"><div class="hw-title">{{ item.title }}</div><div class="hw-meta"><span>{{ item.type }}</span><span v-if="item.time">{{ item.time }}</span></div></div>
      <button class="hw-view-btn">查看</button>
    </div>
  </div>
</template>
<script setup>
defineProps({ items: { type: Array, default: () => [] } })
</script>
<style scoped>
.course-homework { display: flex; flex-direction: column; gap: 8px; }
.hw-card { display: flex; align-items: center; gap: 12px; padding: 12px; }
.hw-type-icon { font-size: 20px; flex-shrink: 0; }
.hw-info { flex: 1; }
.hw-title { font-size: 13px; color: #f1f5f9; }
.hw-meta { font-size: 10px; color: rgba(255,255,255,0.3); margin-top: 2px; display: flex; gap: 8px; }
.hw-view-btn { padding: 4px 12px; border-radius: 4px; border: 1px solid rgba(255,255,255,0.1); background: none; color: rgba(255,255,255,0.5); cursor: pointer; font-size: 11px; font-family: inherit; }
.empty { text-align: center; padding: 40px; font-size: 12px; color: rgba(255,255,255,0.3); }
</style>
