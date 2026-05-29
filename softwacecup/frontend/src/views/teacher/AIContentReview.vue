<template>
  <div class="content-review">
    <h1>AI 内容审核</h1>
    <p class="subtitle">AI 生成的课堂内容需要你的审核才能发布</p>

    <div v-for="r in pendingReviews" :key="r.id" class="glass-card review-card">
      <div class="review-header">
        <h3>课时 #{{ r.lessonId }}</h3>
        <span class="status-badge pending">待审核</span>
      </div>
      <div class="review-content">
        <pre>{{ r.aiDraftJson }}</pre>
      </div>
      <div class="review-actions">
        <button class="glass-btn danger" @click="reject(r.id)">拒绝</button>
        <button class="glass-btn primary" @click="approve(r.id)">审核通过</button>
      </div>
    </div>
    <div v-if="pendingReviews.length === 0" class="empty-state">暂无待审核内容</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { apiContentReviewPending, apiContentReviewApprove, apiContentReviewReject } from '../../api/index.js'

const pendingReviews = ref([])

onMounted(async () => {
  try { pendingReviews.value = (await apiContentReviewPending()).data || [] }
  catch(e) { /* ignore */ }
})

async function approve(id) {
  try { await apiContentReviewApprove(id); pendingReviews.value = pendingReviews.value.filter(r => r.id !== id) }
  catch(e) { /* ignore */ }
}

async function reject(id) {
  try { await apiContentReviewReject(id); pendingReviews.value = pendingReviews.value.filter(r => r.id !== id) }
  catch(e) { /* ignore */ }
}
</script>

<style scoped>
.content-review { padding: 24px; }
h1 { color: #f1f5f9; font-size: 24px; margin-bottom: 4px; }
.subtitle { color: #64748b; margin-bottom: 20px; }
.glass-card { background: rgba(255,255,255,0.06); backdrop-filter: blur(12px); border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 16px; margin-bottom: 12px; }
.review-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.review-header h3 { color: #e2e8f0; margin: 0; }
.status-badge { padding: 2px 10px; border-radius: 10px; font-size: 12px; }
.status-badge.pending { background: rgba(234,179,8,0.2); color: #fde68a; }
.review-content { background: rgba(0,0,0,0.2); border-radius: 8px; padding: 12px; margin-bottom: 12px; max-height: 300px; overflow-y: auto; }
.review-content pre { color: #94a3b8; font-size: 12px; white-space: pre-wrap; margin: 0; }
.review-actions { display: flex; gap: 8px; justify-content: flex-end; }
.glass-btn { padding: 8px 16px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.15); background: rgba(255,255,255,0.08); backdrop-filter: blur(8px); color: #e2e8f0; cursor: pointer; }
.glass-btn.primary { background: rgba(59,130,246,0.3); border-color: rgba(59,130,246,0.4); }
.glass-btn.danger { background: rgba(239,68,68,0.3); border-color: rgba(239,68,68,0.4); }
.empty-state { color: #64748b; text-align: center; padding: 40px; }
</style>
