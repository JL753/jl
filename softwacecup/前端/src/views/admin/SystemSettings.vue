<template>
  <div class="settings-page">
    <section class="page-header">
      <div><h2>⚙️ 系统设置</h2><p>配置平台基本参数、AI服务连接和安全策略。</p></div>
    </section>

    <div class="settings-grid">
      <!-- Basic Settings -->
      <div class="panel setting-card lift-card fade-up delay-1">
        <h3>⚙️ 基本设置</h3>
        <div class="setting-section">
          <div class="setting-item">
            <label>平台名称</label>
              <input v-model="settings.siteName" placeholder="智备优教" />
            </div>
          <div class="setting-item">
            <label>默认密码后缀</label>
              <input v-model="settings.defaultPwdPrefix" placeholder="123456" />
            </div>
          <div class="setting-item">
            <label>每页显示条数</label>
              <el-input-number v-model="settings.pageSize" :min="10" :max="100" :step="5" />
            </div>
          <div class="setting-item">
            <label>允许注册角色</label>
              <el-checkbox-group v-model="settings.allowRoles">
                <el-checkbox label="学生" value="student" />
                <el-checkbox label="教师" value="teacher" />
                <el-checkbox label="管理员" value="admin" />
              </el-checkbox-group>
            </div>
          </div>
        </div>
      </div>

      <!-- AI Service Settings -->
      <div class="panel setting-card lift-card fade-up delay-2">
        <h3>🤖 AI 服务配置</h3>
        <div class="setting-section">
          <div class="setting-item"><label>API Base URL</label><input v-model="settings.apiBaseUrl" placeholder="https://api.example.com/v2" /></div>
          <div class="setting-item"><label>API Key</label><input v-model="settings.apiKey" type="password" placeholder="sk-xxxxxxxxxxxxxxx" /></div>
          <div class="setting-item"><label>模型 ID</label><input v-model="settings.modelId" placeholder="astron-code-latest" /></div>
          <div class="setting-row">
            <div class="setting-item"><label>Temperature</label><el-slider v-model="settings.temperature" :min="0" :max="1" :step="0.1" show-input-controls /></div>
            <div class="setting-item"><label>Max Tokens</label><el-input-number v-model="settings.maxTokens" :min="512" :max="8192" :step="256" /></div>
          </div>
          <button class="btn-test-api" @click="testApiConnection" :disabled="testingApi">测试连接</button>
        </div>
      </div>

      <!-- Security Settings -->
      <div class="panel setting-card lift-card fade-up delay-3">
        <h3>🛡 安全策略</h3>
        <div class="setting-section">
          <div class="setting-item"><label>敏感词过滤（逗号分隔）</label>
            <textarea v-model="settings.blockedWords" rows="3" placeholder="作弊,违法,恐怖..." />
          </div>
          <div class="setting-item"><label>登录Token过期时间(小时)</label>
            <el-input-number v-model="settings.tokenExpireHours" :min="1" :max="168" :step="1" />
          </div>
          <div class="setting-item">
            <label>允许IP白名单（每行一个IP）</label>
            <textarea v-model="settings.ipWhitelist" rows="3" placeholder="127.0.0.1, 192.168.0.1" />
          </div>
          <button class="btn-save" @click="saveSettings">💾 保存设置</button>
        </div>
      </div>
    </div>

    <!-- Save Status -->
    <transition name="save-pop">
      <div v-if="saveStatus" :class="['save-banner', saveStatus]">
        <span>{{ saveStatus === 'saved' ? '✅' : '⚠️' }}</span>
        <span>{{ saveStatus === 'saved' ? '设置已保存' : saveStatus === 'error' ? '保存失败' : '正在保存...' }}</span>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const testingApi = ref(false)
const saveStatus = ref(null)

const settings = reactive({
  siteName: '智备优教',
  defaultPwdPrefix: '123456',
  pageSize: 20,
  allowRoles: ['student', 'teacher', 'admin'],
  apiBaseUrl: 'https://maas-coding-api.cn-huabei-1.xf-yun.com/v2',
  apiKey: '',
  modelId: 'astron-code-latest',
  temperature: 0.7,
  maxTokens: 4096,
  blockedWords: '作弊,违法,恐怖,色情,暴力,政治',
  tokenExpireHours: 24,
  ipWhitelist: '127.0.0.1, 192.168.0.2, 10.0.0.1'
})

const testApiConnection = async () => {
  testingApi.value = true
  try {
    const res = await fetch(settings.apiBaseUrl + '/health', {
      headers: { Authorization: `Bearer test-token`, 'Content-Type': 'application/json' }
    })
    const data = await res.json()
    if (data.status === 'UP') {
      ElMessage.success(`连接成功! 服务运行正常`)
    } else {
      ElMessage.warning(`服务返回: ${data.status}`)
    }
  } catch (e) {
    ElMessage.error(`连接失败: ${e.message}`)
  } finally {
    testingApi.value = false
  }
}

const saveSettings = async () => {
  saveStatus.value = 'saving'
  try {
    // TODO: Call settings update API
    localStorage.setItem('smartprep_settings', JSON.stringify(settings))
    saveStatus.value = 'saved'
    setTimeout(() => { saveStatus.value = null }, 2000)
  } catch (e) {
    saveStatus.value = 'error'
  }
}

onMounted(() => {
  try {
    const saved = JSON.parse(localStorage.getItem('smartprep_settings'))
    if (saved) Object.assign(settings, saved)
  } catch (e) { /* use defaults */ }
})
</script>

<style scoped>
.settings-page{display:grid;gap:18px}
.page-header{display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px}
.page-header h2{margin:0;font-size:22px;color:#1e293b}.page-header p{margin:0;color:#6478b}
.settings-grid{display:grid;grid-template-columns:1fr 1fr;gap:18px}
.setting-card{padding:24px;background:white;border-radius:18px;border:1px solid #edf2f8;box-shadow:0 4px 16px rgba(33,65,108,.04)}
.setting-card h3{font-size:17px;color:#1e293b;font-weight:600;margin:0 0 18px;border-bottom:1px solid #edf2f8;padding-bottom:12px}
.setting-section{display:grid;gap:14px}
.setting-item{display:grid;grid-template-columns:120px 1fr;align-items:center;gap:6px}
.setting-item label{font-size:13px;font-weight:600;color:#475569;text-align:right;padding-right:8px;}
.setting-item input,.setting-item select,.setting-item textarea{border:1.5px solid #e2e8f0;border-radius:10px;padding:8px 12px;font-size:13.5px;color:#334155;outline:none;background:#fff;font-family:inherit;transition:border-color .2s,box-shadow .2s}
.setting-item input:focus,.setting-item select:focus,.setting-item textarea:focus{border-color:#93c5fd;box-shadow:0 0 0 3px rgba(59,130,246,.08)}
.setting-row{display:grid;grid-template-columns:1fr 1fr;gap:12px}
.btn-test-api{width:100%;padding:12px;border:none;border-radius:10px;background:linear-gradient(135deg,#3b82f6,#2563eb);color:white;font-size:14px;font-weight:600;cursor:pointer;opacity:1;transition:all .2s}
.btn-test-api:hover:not(:disabled){opacity:.88;transform:translateY(-1px);box-shadow:0 8px 20px rgba(37,99,235,.25)}
.btn-test-api:disabled{opacity:.5;cursor:not-allowed}
.btn-save{width:100%;padding:12px;border:none;border-radius:10px;background:linear-gradient(135deg,#10b981,#059667);color:white;font-size:14px;font-weight:600;cursor:pointer;transition:all .2s}
.btn-save:hover{transform:translateY(-1px);box-shadow:0 8px 20px rgba(16,185,129,.22)}

.save-banner{position:sticky;top:8px;z-index:50;padding:14px 20px;border-radius:14px;display:flex;align-items:center;gap:12px;animation:slideIn .3s ease}
.save-banner.bg-saved{background:linear-gradient(135deg,#ecfdf5,#d1fae5);color:#059669;border:1px solid#a7f3d0}
.save-banner.bg-error{background:linear-gradient(135deg,#fef2f2,#fecaca);color:#ef4444;border:1px solid:#fde8e7}
.save-banner.bg-saving{background:linear-gradient(135deg,#ddd6fe,#bfdbfe);color:#64748b;border:1px solid#c4b5fd}
@keyframes slideIn{from{opacity:0;transform:translateY(-10px)}to{opacity:1;transform:translateY(0)}}
.fade-up{animation:fadeUp .55s ease both}.delay-1{animation-delay:.05s}.delay-2{animation-delay:.1s}.delay-3{animation-delay:.15s}.delay-4{animation-delay:.2s}@keyframes fadeUp{from{opacity:0;transform:translateY(16px)}to{opacity:1;transform:translateY(0)}}
</style>
