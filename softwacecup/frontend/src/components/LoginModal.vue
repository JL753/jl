<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="auth.showLoginModal" class="overlay" @click.self="handleClose">
        <div class="login-card">
          <button class="close" @click="handleClose">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>

          <div class="head">
            <div class="brand">知</div>
            <h2>知域</h2>
            <p>{{ isRegister ? '创建你的知识账户' : '标记你的知识版图' }}</p>
          </div>

          <!-- 登录表单 -->
          <div v-if="!isRegister" class="form">
            <div class="field" :class="{ focus: focusUsername }">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
              <input v-model="form.username" type="text" placeholder="账号" @focus="focusUsername = true" @blur="focusUsername = false" @keyup.enter="handleLogin" />
            </div>
            <div class="field" :class="{ focus: focusPassword }">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
              <input v-model="form.password" type="password" placeholder="密码" @focus="focusPassword = true" @blur="focusPassword = false" @keyup.enter="handleLogin" />
            </div>
            <div class="field captcha-field">
              <input v-model="captchaInput" type="text" placeholder="验证码" class="captcha-input" @keyup.enter="handleLogin" />
              <canvas ref="captchaCanvas" width="90" height="36" class="captcha" @click="genCaptcha" title="点击刷新"></canvas>
            </div>
          </div>

          <!-- 注册表单 -->
          <div v-else class="form">
            <div class="field">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
              <input v-model="form.username" type="text" placeholder="设置账号" @keyup.enter="handleRegister" />
            </div>
            <div class="field">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
              <input v-model="form.password" type="password" placeholder="设置密码" @keyup.enter="handleRegister" />
            </div>
            <div class="field">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
              <input v-model="confirmPassword" type="password" placeholder="确认密码" @keyup.enter="handleRegister" />
            </div>
            <div class="field captcha-field">
              <input v-model="captchaInput" type="text" placeholder="验证码" class="captcha-input" @keyup.enter="handleRegister" />
              <canvas ref="captchaCanvas" width="90" height="36" class="captcha" @click="genCaptcha" title="点击刷新"></canvas>
            </div>
          </div>

          <!-- 登录按钮 -->
          <button v-if="!isRegister" class="login-btn" :class="{ loading }" @click="handleLogin" :disabled="loading">
            {{ loading ? '登录中...' : '登 录' }}
          </button>
          <button v-else class="login-btn" :class="{ loading }" @click="handleRegister" :disabled="loading">
            {{ loading ? '注册中...' : '注 册' }}
          </button>

          <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

          <!-- 切换 登录/注册 -->
          <div class="switch-row" v-if="!isRegister">
            还没有账号？<span class="switch-link" @click="switchToRegister">注册</span>
          </div>
          <div class="switch-row" v-else>
            已有账号？<span class="switch-link" @click="switchToLogin">登录</span>
          </div>

          <div class="quick" v-if="!isRegister">
            <span>快速体验</span>
            <div class="quick-row">
              <button @click="quickLogin('student')">学生</button>
              <button @click="quickLogin('teacher')">教师</button>
              <button @click="quickLogin('admin')">管理员</button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { apiAbilityEvaluate } from '../api'

const auth = useAuthStore()
const router = useRouter()

const isRegister = computed(() => auth.showRegisterModal)

const form = reactive({ username: '', password: '' })
const confirmPassword = ref('')
const captchaInput = ref('')
const captchaCode = ref('')
const captchaCanvas = ref(null)
const loading = ref(false)
const errorMsg = ref('')
const focusUsername = ref(false)
const focusPassword = ref(false)

function genCaptcha() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let code = ''
  for (let i = 0; i < 4; i++) code += chars[Math.floor(Math.random() * chars.length)]
  captchaCode.value = code
  const canvas = captchaCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, 90, 36)
  ctx.fillStyle = 'rgba(255,255,255,0.04)'
  ctx.fillRect(0, 0, 90, 36)
  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `rgba(${100 + Math.random()*100}, ${100 + Math.random()*100}, ${150 + Math.random()*100}, 0.35)`
    ctx.beginPath(); ctx.moveTo(Math.random() * 90, Math.random() * 36)
    ctx.lineTo(Math.random() * 90, Math.random() * 36); ctx.lineWidth = 1.2; ctx.stroke()
  }
  ctx.font = 'bold 17px Inter, sans-serif'
  for (let i = 0; i < code.length; i++) {
    ctx.fillStyle = `hsl(${200 + Math.random() * 60}, 70%, 75%)`
    ctx.save(); ctx.translate(10 + i * 20, 26)
    ctx.rotate((Math.random() - 0.5) * 0.4); ctx.fillText(code[i], 0, 0); ctx.restore()
  }
}

function quickLogin(role) {
  form.username = role; form.password = '123456'
  captchaInput.value = captchaCode.value
  handleLogin()
}

async function handleLogin() {
  if (!form.username.trim()) { errorMsg.value = '请输入账号'; return }
  if (!form.password.trim()) { errorMsg.value = '请输入密码'; return }
  if (captchaInput.value.toUpperCase() !== captchaCode.value) {
    errorMsg.value = '验证码错误'; genCaptcha(); captchaInput.value = ''; return
  }
  loading.value = true; errorMsg.value = ''
  try {
    await auth.login({ username: form.username, password: form.password })
    auth.closeLoginModal()
    const redirect = auth.pendingRedirect
    const role = auth.userRole
    if (redirect) router.push(redirect)
    else if (role === 'teacher') router.push('/teacher/dashboard')
    else router.push('/student/dashboard')
  } catch (e) {
    errorMsg.value = e.message || '登录失败'
    genCaptcha(); captchaInput.value = ''
  } finally { loading.value = false }
}

async function handleRegister() {
  if (!form.username.trim()) { errorMsg.value = '请输入账号'; return }
  if (!form.password.trim()) { errorMsg.value = '请输入密码'; return }
  if (form.password !== confirmPassword.value) { errorMsg.value = '两次密码不一致'; return }
  if (form.password.length < 4) { errorMsg.value = '密码至少4位'; return }
  if (captchaInput.value.toUpperCase() !== captchaCode.value) {
    errorMsg.value = '验证码错误'; genCaptcha(); captchaInput.value = ''; return
  }
  loading.value = true; errorMsg.value = ''
  try {
    await auth.register({ username: form.username, password: form.password })
    // 注册成功后初始化六维能力空数据
    try { await apiAbilityEvaluate() } catch {}
    auth.closeLoginModal()
    const redirect = auth.pendingRedirect
    if (redirect) router.push(redirect)
    else router.push('/student/dashboard')
  } catch (e) {
    errorMsg.value = e.message || '注册失败'
    genCaptcha(); captchaInput.value = ''
  } finally { loading.value = false }
}

function switchToRegister() {
  form.username = ''; form.password = ''
  confirmPassword.value = ''
  captchaInput.value = ''; errorMsg.value = ''
  genCaptcha()
  auth.showRegisterModal = true
}

function switchToLogin() {
  form.username = ''; form.password = ''
  confirmPassword.value = ''
  captchaInput.value = ''; errorMsg.value = ''
  genCaptcha()
  auth.showRegisterModal = false
}

function handleClose() { auth.closeLoginModal() }

onMounted(() => genCaptcha())
</script>

<style scoped>
.overlay {
  position: fixed; inset: 0; z-index: 2000;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}

.login-card {
  position: relative;
  width: min(380px, 92vw);
  background: rgba(12, 17, 32, 0.85);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 36px 32px 28px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.5);
}

.close {
  position: absolute; top: 14px; right: 14px;
  width: 30px; height: 30px; border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.05); color: rgba(255,255,255,0.5);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.close:hover { background: rgba(255,255,255,0.12); color: white; }

.head { text-align: center; margin-bottom: 28px; }
.brand {
  width: 44px; height: 44px; border-radius: 14px;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 800; color: white;
  box-shadow: 0 6px 20px rgba(59,130,246,0.35);
  margin-bottom: 12px;
}
.head h2 { font-size: 20px; font-weight: 700; color: #e6edf3; margin: 0 0 4px; }
.head p { font-size: 12px; color: rgba(255,255,255,0.35); margin: 0; letter-spacing: 1px; }

.form { display: flex; flex-direction: column; gap: 12px; margin-bottom: 20px; }
.field {
  display: flex; align-items: center; gap: 10px;
  height: 44px; border-radius: 12px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  padding: 0 14px;
  transition: border-color 0.2s, background 0.2s;
}
.field.focus { border-color: rgba(59,130,246,0.4); background: rgba(59,130,246,0.04); }
.field svg { color: rgba(255,255,255,0.3); flex-shrink: 0; }
.field input {
  flex: 1; border: none; outline: none; background: transparent;
  color: rgba(255,255,255,0.85); font-size: 14px;
}
.field input::placeholder { color: rgba(255,255,255,0.25); }

.captcha-field { padding-right: 6px; }
.captcha-input { width: 100%; }
.captcha { border-radius: 6px; cursor: pointer; opacity: 0.85; flex-shrink: 0; }
.captcha:hover { opacity: 1; }

.login-btn {
  width: 100%; height: 44px; border: none; border-radius: 22px;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  color: white; font-size: 15px; font-weight: 600; cursor: pointer;
  box-shadow: 0 6px 20px rgba(59,130,246,0.35);
  transition: all 0.2s;
}
.login-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 8px 28px rgba(59,130,246,0.45); }
.login-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.login-btn.loading { opacity: 0.75; }

.error { color: #ef6b6b; font-size: 12px; text-align: center; margin: 10px 0 0; }

/* 切换登录/注册 */
.switch-row { text-align: center; font-size: 12px; color: rgba(255,255,255,0.3); margin-top: 14px; }
.switch-link { color: #60d9fa; cursor: pointer; font-weight: 500; margin-left: 4px; }
.switch-link:hover { color: #3b82f6; }

/* Quick login */
.quick { text-align: center; margin-top: 20px; }
.quick span { font-size: 11px; color: rgba(255,255,255,0.3); display: block; margin-bottom: 10px; }
.quick-row { display: flex; gap: 8px; justify-content: center; }
.quick-row button {
  flex: 1; max-width: 80px; padding: 7px 0; border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.04); color: rgba(255,255,255,0.55);
  font-size: 12px; cursor: pointer; transition: all 0.2s;
}
.quick-row button:hover { background: rgba(255,255,255,0.1); color: white; border-color: rgba(255,255,255,0.2); }

/* Transition */
.modal-enter-active { transition: all 0.25s ease; }
.modal-leave-active { transition: all 0.18s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .login-card { transform: translateY(12px) scale(0.97); }
.modal-leave-to .login-card { transform: translateY(-6px) scale(0.98); }
</style>
