<template>
  <div class="settings-page">
    <section class="page-header">
      <div><h2>⚙️ 系统设置</h2><p>管理平台全局配置、AI服务参数和安全策略。</p></div>
      <el-button type="primary" :loading="saving" @click="handleSave">保存设置</el-button>
    </section>

    <div class="settings-grid">
      <!-- 基本设置 -->
      <div class="settings-card panel lift-card">
        <h3><span class="icon">🌐</span> 基本信息</h3>
        <el-form label-width="120px" label-position="top" :model="basicForm">
          <el-form-item label="站点名称"><el-input v-model="basicForm.siteName" placeholder="平台名称"/></el-form-item>
          <el-form-item logoUrl="站点Logo URL"><el-input v-model="basicForm.logoUrl" placeholder="Logo图片URL"/></el-form-item>
          <el-form-item footerText="页脚文字"><el-input v-model="basicForm.footerText" placeholder="版权信息等"/></el-form-item>
        </el-form>
      </div>

      <!-- AI 服务配置 -->
      <div class="settings-card panel lift-card">
        <h3><span class="icon">🤖</span> AI 服务连接</h3>
        <el-form label-width="140px" label-position="top" :model="aiConfig">
          <el-form-item label="Base URL"><el-input v-model="aiConfig.llmBaseUrl" placeholder="LLM API地址"/></el-form-item>
          <el-form-item label="API Key"><el-input v-model="aiConfig.apiKey" type="password" show-password placeholder="API密钥"/></el-form-item>
          <el-form-item label="模型名称"><el-input v-model="aiConfig.modelName" placeholder="如: gpt-4, qwen-plus"/></el-form-item>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
            <el-form-item label="Temperature"><el-slider v-model="aiConfig.temperature" :min="0" :max="2" :step="0.1" show-stops /></el-form-item>
            <el-form-item label="Max Tokens"><el-input-number v-model="aiConfig.maxTokens" :min="256" :max="16384" :step="256" /></el-form-item>
          </div>
        </el-form>
      </div>

      <!-- 安全策略 -->
      <div class="settings-card panel lift-card">
        <h3><span class="icon">🔒</span> 安全策略</h3>
        <el-form label-width="140px" label-position="top" :model="securityForm">
          <el-form-item label="敏感词过滤"><el-input v-model="securityForm.blockedWords" type="textarea" :rows="2" placeholder="用逗号分隔"/></el-form-item>
          <el-form-item label="IP 白名单（可选）"><el-input v-model="securityForm.ipWhitelist" placeholder="留空表示不限制"/></el-form-item>
          <el-form-item label="Token 过期时间（小时）"><el-input-number v-model="securityForm.tokenExpireHours" :min="1" :max="720" /></el-form-item>
        </el-form>
      </div>

      <!-- 功能开关 -->
      <div class="settings-card panel lift-card">
        <h3><span class="icon">🎛️</span> 功能开关</h3>
        <div class="switch-list">
          <div class="switch-row">
            <div><strong>注册功能</strong><p>允许新用户自主注册账号</p></div>
            <el-switch v-model="toggles.allowRegister" />
          </div>
          <div class="switch-row">
            <div><strong>数字人模块</strong><p>启用VTuber虚拟教师讲解功能</p></div>
            <el-switch v-model="toggles.digitalHumanEnabled" />
          </div>
          <div class="switch-row">
            <div><strong>SSE 流式输出</strong><p>智能辅导使用流式返回</p></div>
            <el-switch v-model="toggles.sseStreamEnabled" />
          </div>
          <div class="switch-row">
            <div><strong>自动种子数据</strong><p>启动时自动创建演示数据</p></div>
            <el-switch v-model="toggles.autoSeedData" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { apiAdminGetSettings, apiAdminUpdateSettings } from '../../api'

const saving = ref(false)

const basicForm = reactive({ siteName: '', logoUrl: '', footerText: '' })
const aiConfig = reactive({ llmBaseUrl: '', apiKey: '', modelName: '', temperature: 0.7, maxTokens: 4096 })
const securityForm = reactive({ blockedWords: '', ipWhitelist: '', tokenExpireHours: 24 })
const toggles = reactive({ allowRegister: true, digitalHumanEnabled: true, sseStreamEnabled: true, autoSeedData: true })

onMounted(async () => {
  try {
    const res = await apiAdminGetSettings()
    if (res.data) {
      Object.assign(basicForm, {
        siteName: res.data.siteName || '智备优教',
        logoUrl: res.data.logoUrl || '',
        footerText: res.data.footerText || ''
      })
      Object.assign(aiConfig, {
        llmBaseUrl: res.data.llmBaseUrl || '',
        apiKey: res.data.apiKey || '',
        modelName: res.data.model || '',
        temperature: res.data.temperature ?? 0.7,
        maxTokens: res.data.maxTokens ?? 4096
      })
      Object.assign(securityForm, {
        blockedWords: res.data.blockedWords || '',
        ipWhitelist: res.data.ipWhitelist || '',
        tokenExpireHours: res.data.tokenExpireHours || 24
      })
    }
  } catch (e) {
    console.warn('Load settings failed:', e)
  }
})

const handleSave = async () => {
  saving.value = true
  try {
    await apiAdminUpdateSettings({
      ...basicForm,
      llmBaseURL: aiConfig.llmBaseUrl,
      apiKey: aiConfig.apiKey,
      model: aiConfig.modelName,
      temperature: aiConfig.temperature,
      maxTokens: aiConfig.maxTokens,
      ...securityForm,
      toggles
    })
    ElMessage.success('设置已保存')
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || ''))
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.settings-page {
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

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-lg;
}

.settings-card {
  padding: $spacing-xl;
  background: $bg-white;
  border-radius: $radius-medium;
  border: 1px solid $border-extra-light;
  box-shadow: $shadow-card;

  h3 {
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 0 0 $spacing-lg;
    font-size: $font-size-lg;
    color: $text-primary;
    font-weight: $font-weight-bold;
  }

  .icon {
    font-size: $font-size-xxl;
  }

  .el-form {
    margin-top: 4px;
  }
}

.switch-list {
  display: grid;
  gap: $spacing-md;
}

.switch-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-sm $spacing-lg;
  border-radius: $radius-medium;
  background: $bg-lighter;
  border: 1px solid $border-extra-light;

  strong {
    font-size: $font-size-base;
    color: $text-primary;
    display: block;
    margin-bottom: 2px;
  }

  p {
    margin: 0;
    font-size: $font-size-sm;
    color: $text-secondary;
  }
}

.lift-card {
  transition: transform $transition-slow, box-shadow $transition-slow;

  &:hover {
    transform: translateY(-3px);
    box-shadow: $shadow-card-hover;
  }
}

@media (max-width: 900px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>
