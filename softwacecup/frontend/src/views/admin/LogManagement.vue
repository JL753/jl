<template>
  <div class="log-mgmt-page">
    <section class="page-header">
      <div><h2>📋 操作日志</h2><p>记录所有管理操作的审计日志，支持筛选和导出。</p></div>
    </section>
    <div class="filter-bar">
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"
                     style="width: 280px;" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
      <el-select v-model="typeFilter" clearable style="width:140px;"><el-option label="全部操作" value="" /><el-option label="创建" value="create" /><el-option label="更新" value="update" /><el-option label="删除" value="delete" /></el-select>
      <span class="count-badge">共 {{ logs.length }} 条</span>
    </div>
    <div class="log-table-wrap panel">
      <el-table :data="logs" stripe v-loading="loading" max-height="560px">
        <el-table-column prop="time" label="时间" width="170" sortable />
        <el-table-column prop="adminName" label="操作人" width="110" />
        <el-table-column prop="action" label="操作类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.action === 'delete' ? 'danger' : row.action === 'create' ? 'success' : 'info'" size="small">{{ actionMap[row.action] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="target" label="目标对象" show-overflow-tooltip min-width="160" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === '成功' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { apiAdminLogs } from '../../api'

const loading = ref(false)
const dateRange = ref([])
const typeFilter = ref('')

const actionMap = { create: '创建', update: '更新', delete: '删除' }
// Logs from backend API
const rawLogs = ref([])

const logs = computed(() => {
  return (rawLogs.value || []).map(log => ({
    time: log.createdAt ? new Date(log.createdAt).toLocaleString('zh-CN') : '--',
    adminName: log.username || '-',
    action: /创建|新增/i.test(log.action) ? 'create' : /删除|移除/i.test(log.action) ? 'delete' : 'update',
    target: log.target || log.details?.substring(0, 60) || '-',
    status: '成功'
  }))
})

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await apiAdminLogs({ action: undefined })
    rawLogs.value = res.data || []
  } catch (e) {
    console.warn('Load logs failed:', e)
    rawLogs.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>

<style scoped lang="scss">
.log-mgmt-page {
  display: grid;
  gap: $spacing-lg;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: $spacing-sm;

  h2 {
    margin: 0;
    font-size: $font-size-xxl;
    color: $text-primary;
    font-weight: $font-weight-bold;
  }

  p {
    margin: 0;
    color: $text-secondary;
  }
}

.filter-bar {
  display: flex;
  gap: $spacing-sm;
  align-items: center;
  padding: $spacing-sm $spacing-lg;
  background: $bg-white;
  border-radius: $radius-medium;
  border: 1px solid $border-extra-light;
  box-shadow: $shadow-light;
}

.count-badge {
  margin-left: auto;
  font-size: $font-size-sm;
  color: $text-secondary;
  font-weight: $font-weight-medium;
  padding: 7px $spacing-sm;
  border-radius: $radius-round;
  background: $bg-lighter;
}

.log-table-wrap {
  background: $bg-white;
  border-radius: $radius-medium;
  border: 1px solid $border-extra-light;
  overflow: hidden;
  box-shadow: $shadow-card;
}
</style>
