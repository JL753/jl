<template>
  <div class="res-mgmt-page">
    <section class="page-header">
      <div><h2>📦 资源管理</h2><p>管理平台生成的各类学习资源，包括文档、思维导图、练习题库等。</p></div>
    </section>

    <div class="filter-bar">
      <el-select v-model="typeFilter" clearable placeholder="资源类型" style="width:160px;">
        <el-option label="全部类型" value="" />
        <el-option label="文档(document)" value="document" />
        <option label="思维导图(mindmap)" value="mindmap" />
        <option label="练习题(question)" value="question" />
        <option label="代码实操(coding)" value="coding" />
        <option label="视频脚本(media)" value="media" />
      </el-select>
      <span class="count-badge">共 {{ resources.length }} 条</span>
    </div>

    <div class="resource-list">
      <div v-for="(res, idx) in filteredResources" :key="res.id || idx" class="res-item panel lift-card">
        <div :class="['res-type', res.resourceType]">{{ typeMap[res.resourceType] || res.resourceType }}</div>
        <div class="res-info">
          <h4>{{ res.title }}</h4>
          <p class="res-desc">{{ (res.content || '').substring(0, 80) }}...</p>
          <div class="res-meta">
            <span class="conf-badge">置信度 {{ res.confidence || 85 }}%</span>
            <span class="res-time">{{ res.createdAt ? new Date(res.createdAt).toLocaleDateString() : '-' }}</span>
          </div>
        </div>
        <div class="res-actions">
          <el-button size="small" link @click="viewDetail(res)">查看详情</el-button>
          <el-popconfirm title="确定删除此资源？" @confirm="handleDelete(res)">
            <el-button size="small" type="danger" link>删除</el-button>
          </el-popconfirm>
        </div>
      </div>
      <div v-if="filteredResources.length === 0 && !loading" class="empty-state">暂无资源数据</div>
    </div>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="资源详情" width="680px" destroy-on-close>
      <div v-if="selectedRes" class="detail-content">
        <h3>{{ selectedRes.title }}</h3>
        <p class="detail-type"><span :class="'type-pill', selectedRes.type">{{ typeMap[selectedRes.type] }}</span> 置信度 {{ selectedRes.confidence }}% | {{ selectedRes.agentName }}</p>
        <div class="markdown-body" v-html="renderedMarkdown"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { ElMessage } from 'element-plus'
import { apiAdminListResources, apiAdminDeleteResource } from '../../api'

const loading = ref(false)
const typeFilter = ref('')
const detailVisible = ref(false)
const selectedRes = ref(null)
const renderedMarkdown = ref('')

const typeMap = { document: '📄 文档', mindmap: '🧠 思维导图', question: '📝 练习题', coding: '💻 代码', media: '🎬 视频' }

// Resources from backend
const resources = ref([])

const filteredResources = computed(() => {
  if (!typeFilter.value) return resources.value
  return resources.value.filter(r => r.resourceType === typeFilter.value)
})

// Load resources from API
const loadResources = async () => {
  loading.value = true
  try {
    const res = await apiAdminListResources({ type: undefined })
    resources.value = res.data || []
  } catch (e) {
    console.warn('Load resources failed:', e)
  } finally {
    loading.value = false
  }
}

const viewDetail = (res) => {
  selectedRes.value = res
  renderedMarkdown.value = DOMPurify.sanitize(marked.parse(res.content || ''))
  detailVisible.value = true
}

const handleDelete = async (res) => {
  try {
    await apiAdminDeleteResource(res.id)
    resources.value = resources.value.filter(r => r.id !== res.id)
    ElMessage.success('资源已删除')
  } catch (e) { ElMessage.error('删除失败: ' + (e.message || '')) }
}

onMounted(loadResources)
</script>

<style scoped lang="scss">
.res-mgmt-page {
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

.resource-list {
  display: grid;
  gap: $spacing-sm;
}

.res-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: $spacing-sm;
  padding: $spacing-lg $spacing-lg;
  background: $bg-white;
  border-radius: $radius-medium;
  border: 1px solid $border-extra-light;
  transition: all $transition-base;

  &:hover {
    transform: translateX(4px);
    box-shadow: $shadow-medium;
    border-color: $primary-lighter;
  }
}

.res-type {
  padding: 5px $spacing-sm;
  border-radius: $radius-base;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: white;
  width: fit-content;
  text-align: center;

  &.document {
    background: $gradient-blue;
  }

  &.mindmap {
    background: linear-gradient(135deg, #a78bfa, #7c3aed);
  }

  &.question {
    background: $gradient-warning;
  }

  &.coding {
    background: linear-gradient(135deg, #8b5cf6, #6366f1);
  }

  &.media {
    background: $gradient-danger;
  }
}

.res-info {
  h3 {
    margin: 0 0 4px;
    font-size: $font-size-lg;
    color: $text-primary;
  }
}

.res-desc {
  color: $text-regular;
  font-size: $font-size-sm;
  line-height: 1.6;
  margin: 0 0 $spacing-xs;
}

.res-meta {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
  align-items: center;
  font-size: $font-size-xs;
}

.conf-badge {
  padding: 3px 10px;
  border-radius: $radius-round;
  background: #eff6ff;
  color: $primary-color;
  font-weight: $font-weight-semibold;
}

.agent-name {
  color: $text-regular;
}

.res-time {
  color: $text-secondary;
  margin-left: auto;
}

.res-actions {
  display: flex;
  gap: $spacing-xs;
  align-items: center;
  border-left: 1px solid $border-extra-light;
  padding-left: $spacing-sm;
  margin-left: auto;
}

.lift-card {
  transition: transform $transition-slow, box-shadow $transition-slow;

  &:hover {
    transform: translateY(-4px);
    box-shadow: $shadow-heavy;
  }
}

.empty-state {
  text-align: center;
  padding: 60px $spacing-lg;
  color: $text-secondary;
}

.detail-content {
  h3 {
    font-size: $font-size-xl;
    color: $text-primary;
    margin: 0 0 $spacing-sm;
  }
}

.detail-type {
  display: inline-flex;
  padding: 5px $spacing-sm;
  border-radius: $radius-round;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  margin-bottom: $spacing-sm;
}

.type-pill {
  background: #dbeafe;
  color: $primary-color;

  &.mindmap {
    background: #ede9fe;
    color: #7c3aed;
  }
}

.markdown-body {
  font-size: $font-size-base;
  line-height: 1.85;
  color: $text-regular;
  padding: $spacing-md 0;
  background: $bg-lighter;
  border-radius: $radius-medium;
  max-height: 400px;
  overflow-y: auto;
}
</style>
