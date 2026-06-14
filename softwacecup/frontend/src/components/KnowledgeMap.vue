<template>
  <div class="knowledge-map">
    <div v-if="data" class="km-container">
      <div class="km-header">
        <h3>{{ data.name || '知识图谱' }}</h3>
      </div>
      <div class="km-placeholder">
        <p>📊 知识图谱可视化区域</p>
        <p class="km-hint">节点数: {{ nodeCount }} | 关系数: {{ edgeCount }}</p>
      </div>
    </div>
    <div v-else class="km-empty">
      <p>暂无知识图谱数据</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Object, default: null },
  title: { type: String, default: '' }
})

const nodeCount = computed(() => props.data?.nodes?.length || 0)
const edgeCount = computed(() => props.data?.edges?.length || 0)
</script>

<style scoped>
.knowledge-map {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.km-container { width: 100%; text-align: center; }
.km-header h3 { font-size: 16px; color: rgba(255,255,255,0.8); margin: 0 0 12px; }
.km-placeholder p { color: rgba(255,255,255,0.5); font-size: 14px; margin: 4px 0; }
.km-hint { font-size: 12px !important; color: rgba(255,255,255,0.3) !important; }
.km-empty p { color: rgba(255,255,255,0.3); font-size: 14px; }
</style>
