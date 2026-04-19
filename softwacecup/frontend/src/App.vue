<template>
  <router-view v-slot="{ Component, route }">
    <transition :name="route.meta.transition || 'page-fade'" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </router-view>
</template>

<style>
/* Page transition animations */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* Smooth scroll for the whole app */
html {
  scroll-behavior: smooth;
}

/* Element Plus global overrides for consistency */
.el-button--primary {
  --el-button-bg-color: #5f78ff;
  --el-button-border-color: #5f78ff;
  --el-button-hover-bg-color: #4d66ef;
  --el-button-hover-border-color: #4d66ef;
}

.el-dialog {
  border-radius: 18px !important;
  overflow: hidden;
}

.el-dialog__header {
  border-bottom: 1px solid #edf2f8;
  padding: 18px 22px 14px;
  margin-right: 0;
}

.el-dialog__title {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
}

.el-table th.el-table__cell {
  background-color: #f8fafc !important;
  color: #334155;
  font-weight: 600;
  font-size: 13px;
}

.el-tag--primary {
  background-color: #eff6ff;
  color: #2563eb;
  border-color: #bfdbfe;
}

/* Scrollbar styling */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.35);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover {
  background: rgba(148, 163, 184, 0.55);
}

/* Print styles */
@media print {
  .shell-sidebar,
  .shell-topbar,
  .nav-links,
  .filter-bar { display: none !important; }
  .layout-shell { display: block; }
  body { background: white; color: black; }
}
</style>
