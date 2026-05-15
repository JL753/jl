<template>
  <div class="glass-card" :class="[variant, { hover: hoverable, active: active }]" :style="cardStyle">
    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: { type: String, default: 'default' }, // 'default', 'accent', 'gradient'
  hoverable: Boolean,
  active: Boolean,
  padding: { type: String, default: 'var(--space-md)' },
})

const cardStyle = computed(() => ({
  padding: props.padding,
}))
</script>

<style scoped>
.glass-card {
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.04);
  transition: all var(--transition-base);
}
.glass-card.hover:hover {
  background: rgba(255, 255, 255, 0.07);
  border-color: rgba(255, 255, 255, 0.12);
  transform: translateY(-1px);
}
.glass-card.accent {
  background: rgba(59, 130, 246, 0.08);
  border-color: rgba(59, 130, 246, 0.15);
}
.glass-card.gradient {
  background: linear-gradient(135deg, rgba(59,130,246,0.12), rgba(168,85,247,0.08));
  border-color: rgba(168,85,247,0.15);
}
</style>
