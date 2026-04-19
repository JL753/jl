<template>
  <div class="study-companion-page">
    <SplitPaneStudy />
  </div>
</template>

<script setup>
import SplitPaneStudy from '@/components/SplitPaneStudy.vue'
</script>

<style scoped>
.study-companion-page {
  height: 100%;
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}
</style>
