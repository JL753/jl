<template>
  <div class="resource-page">
    <section class="page-hero panel">
      <div>
        <p class="eyebrow">PPT STUDIO</p>
        <h2>选择模板生成完整教学 PPT</h2>
        <p>内置多套教学模板，支持选择封面风格并生成完整课堂演示大纲，保留可用生成流程。</p>
      </div>
      <div class="head-actions">
        <el-button type="primary" @click="generate">生成完整PPT</el-button>
        <el-button @click="load">刷新模板</el-button>
      </div>
    </section>

    <section class="resource-layout">
      <div class="panel config-card">
        <div class="card-title"><h3>生成参数</h3><span class="soft-tag">教师端</span></div>
        <el-form label-position="top" :model="form">
          <el-form-item label="课程"><el-input v-model="form.course" /></el-form-item>
          <el-form-item label="章节"><el-input v-model="form.chapter" /></el-form-item>
          <el-form-item label="教学目标"><el-input v-model="form.teachingGoal" type="textarea" :rows="4" /></el-form-item>
          <el-form-item label="学生层次"><el-input v-model="form.studentLevel" /></el-form-item>
        </el-form>
      </div>

      <div class="panel preview-card">
        <div class="card-title"><h3>PPT 模板库</h3><span class="soft-tag">可选模板</span></div>
        <div class="library-grid">
          <article class="library-item" v-for="item in templates" :key="item.id" :class="{ active: form.templateCode === item.templateCode }" @click="form.templateCode = item.templateCode">
            <img class="ppt-cover" :src="item.coverUrl" :alt="item.templateName" />
            <div class="library-top"><strong>{{ item.templateName }}</strong><el-tag size="small">{{ item.sceneTag }}</el-tag></div>
            <p class="library-content">{{ item.description }}</p>
            <div class="media-chip-row"><span class="media-chip" v-for="name in item.previews" :key="name">{{ name }}</span></div>
          </article>
        </div>
      </div>
    </section>

    <section class="panel result-panel" v-if="pptResult.previewPages?.length">
      <div class="card-title"><h3>生成结果预览</h3><span class="soft-tag">{{ pptResult.template?.name }}</span></div>
      <div class="ppt-preview-grid">
        <article class="ppt-page" v-for="(page, index) in pptResult.previewPages" :key="page.title + index">
          <div class="ppt-index">0{{ index + 1 }}</div>
          <strong>{{ page.title }}</strong>
          <p>{{ page.subtitle }}</p>
        </article>
      </div>
      <div class="download-row"><a :href="pptResult.downloadUrl" target="_blank">打开预览封面 / 下载入口</a></div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiGenerateTeachingPpt, apiTeacherPptTemplates } from '../../api'

const templates = ref([])
const pptResult = ref({})
const form = reactive({ course: '人工智能导论', chapter: '机器学习基础', teachingGoal: '帮助学生理解机器学习的基本概念、分类方式与典型应用。', studentLevel: '学生基础薄弱', templateCode: 'campus_fresh' })

const load = async () => {
  const res = await apiTeacherPptTemplates()
  templates.value = res.data || []
  if (!form.templateCode && templates.value.length) form.templateCode = templates.value[0].templateCode
}

const generate = async () => {
  const res = await apiGenerateTeachingPpt({ ...form })
  pptResult.value = res.data || {}
  ElMessage.success(pptResult.value.message || 'PPT 已生成')
}

onMounted(load)
</script>

<style scoped>
.resource-page { display: grid; gap: 16px; }
.head-actions { display: flex; gap: 12px; }
.resource-layout { display: grid; grid-template-columns: 320px 1fr; gap: 16px; }
.config-card, .preview-card, .result-panel { padding: 18px; }
.card-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.card-title h3 { margin: 0; }
.library-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.library-item { padding: 14px; border-radius: 16px; background: #f8fbff; border: 1px solid #eef3f8; cursor: pointer; transition: .2s; }
.library-item.active { border-color: #5d90ff; box-shadow: 0 10px 24px rgba(93,144,255,.18); }
.ppt-cover { width: 100%; height: 170px; object-fit: cover; border-radius: 12px; margin-bottom: 12px; }
.library-top { display: flex; justify-content: space-between; gap: 10px; align-items: flex-start; margin-bottom: 8px; }
.library-content { margin: 0 0 10px; color: #4c5f78; line-height: 1.8; }
.media-chip-row { display: flex; flex-wrap: wrap; gap: 8px; }
.media-chip { padding: 4px 10px; border-radius: 999px; background: #eef3ff; color: #5478b1; font-size: 12px; }
.ppt-preview-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.ppt-page { padding: 18px; border-radius: 16px; background: linear-gradient(180deg, #f9fbff, #ffffff); border: 1px solid #eef3f8; min-height: 140px; display: grid; align-content: start; gap: 10px; }
.ppt-index { width: 42px; height: 42px; border-radius: 50%; background: #eef4ff; color: #4c84ea; display: grid; place-items: center; font-weight: 700; }
.download-row { margin-top: 16px; }
.download-row a { color: #4f7fb5; text-decoration: none; }
@media (max-width: 1200px) { .resource-layout, .library-grid, .ppt-preview-grid { grid-template-columns: 1fr; } }
</style>
