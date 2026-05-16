<template>
  <div class="course-card glass-card" :class="{ compact }" @click="$emit('click')">
    <div class="card-thumb" v-if="compact">
      <span class="card-icon">{{ icon }}</span>
    </div>
    <div class="card-body">
      <h3 class="card-title">{{ title }}</h3>
      <p class="card-desc" v-if="!compact">{{ description }}</p>
      <div class="card-tags">
        <span class="tag" v-for="tag in tags" :key="tag">{{ tag }}</span>
      </div>
    </div>
    <div class="card-progress" v-if="progress >= 0 && !compact">
      <div class="progress-track"><div class="progress-fill" :style="{ width: progress + '%' }"></div></div>
      <span>{{ progress }}%</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: String,
  description: String,
  icon: { type: String, default: '📘' },
  tags: { type: Array, default: () => [] },
  progress: { type: Number, default: -1 },
  compact: Boolean,
})

defineEmits(['click'])
</script>

<style scoped>
.course-card {
  display: flex;
  flex-direction: column;
  padding: 16px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-base);
  position: relative;
  overflow: hidden;
}
.course-card:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 255, 255, 0.18);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35);
}

/* ── Compact variant ── */
.course-card.compact {
  flex-direction: row;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  min-height: unset;
}
.course-card.compact .card-thumb {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.course-card.compact .card-icon {
  font-size: 20px;
}
.course-card.compact .card-body {
  flex: 1;
  min-width: 0;
  padding: 0;
}
.course-card.compact .card-title {
  font-size: 14px;
  margin: 0 0 4px;
}
.course-card.compact .card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.course-card.compact .tag {
  font-size: 10px;
  padding: 1px 7px;
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.12);
  color: #60a5fa;
  border: 1px solid rgba(59, 130, 246, 0.2);
}

/* ── Full variant ── */
.card-body {
  flex: 1;
  min-width: 0;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #e6edf3;
  margin: 0 0 6px;
  line-height: 1.3;
}
.card-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  margin: 0 0 10px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: auto;
}
.tag {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

/* ── Progress bar (full variant) ── */
.card-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}
.progress-track {
  flex: 1;
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: 2px;
  background: linear-gradient(90deg, #3b82f6, #06b6d4);
  transition: width 0.4s ease;
}
.card-progress span {
  font-size: 11px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.5);
  min-width: 32px;
  text-align: right;
}
</style>
