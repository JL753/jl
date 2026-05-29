<template>
  <div class="chapter-tree">
    <div v-if="loading" class="tree-loading"><span class="spinner"></span></div>
    <div v-else-if="chapters.length === 0" class="tree-empty">暂无章节</div>
    <template v-else>
      <div v-for="chapter in chapters" :key="chapter.id" class="tree-chapter">
        <div class="tree-chapter-header" @click="toggleChapter(chapter.id)">
          <span class="tree-chapter-icon">{{ expandedChapters.has(chapter.id) ? '▾' : '▸' }}</span>
          <span class="tree-chapter-title">{{ chapter.title }}</span>
        </div>
        <div v-if="expandedChapters.has(chapter.id)" class="tree-subchapters">
          <div
            v-for="sc in chapter.subChapters" :key="sc.id"
            class="tree-subchapter"
            :class="{ active: activeId === sc.id }"
            @click="$emit('select', sc)"
          >
            <span class="tree-subchapter-dot"></span>
            <span class="tree-subchapter-title">{{ sc.title }}</span>
            <span class="tree-subchapter-type">{{ typeLabel(sc.type) }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  chapters: { type: Array, default: () => [] },
  activeId: { type: [Number, String], default: null },
  loading: { type: Boolean, default: false }
})

defineEmits(['select'])

const expandedChapters = ref(new Set())

watch(() => props.chapters, (newVal) => {
  if (newVal.length > 0) {
    newVal.forEach(ch => expandedChapters.value.add(ch.id))
  }
}, { immediate: true })

function toggleChapter(id) {
  if (expandedChapters.value.has(id)) expandedChapters.value.delete(id)
  else expandedChapters.value.add(id)
  expandedChapters.value = new Set(expandedChapters.value)
}

function typeLabel(type) {
  const map = { video: '视频', doc: '文档', quiz: '测验' }
  return map[type] || ''
}
</script>

<style scoped>
.chapter-tree { overflow-y: auto; }
.tree-chapter { margin-bottom: 4px; }
.tree-chapter-header { display: flex; align-items: center; gap: 4px; padding: 6px 8px; cursor: pointer; border-radius: 4px; font-size: 12px; font-weight: 600; color: #f1f5f9; }
.tree-chapter-header:hover { background: rgba(255,255,255,0.04); }
.tree-chapter-icon { width: 14px; color: rgba(255,255,255,0.3); flex-shrink: 0; }
.tree-subchapters { margin-left: 8px; }
.tree-subchapter { display: flex; align-items: center; gap: 6px; padding: 4px 8px 4px 22px; border-radius: 4px; cursor: pointer; font-size: 11px; color: rgba(255,255,255,0.5); }
.tree-subchapter:hover { background: rgba(255,255,255,0.04); }
.tree-subchapter.active { background: rgba(59,130,246,0.12); color: #60d9fa; font-weight: 600; }
.tree-subchapter-dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,0.2); flex-shrink: 0; }
.tree-subchapter.active .tree-subchapter-dot { background: #60d9fa; }
.tree-subchapter-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tree-subchapter-type { font-size: 9px; color: rgba(255,255,255,0.2); }
.tree-loading, .tree-empty { padding: 20px; text-align: center; font-size: 11px; color: rgba(255,255,255,0.3); }
.spinner { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.1); border-top-color: #3b82f6; border-radius: 50%; display: inline-block; animation: spin 0.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
