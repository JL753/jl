<template>
  <div class="login-page">
    <div class="login-container">
      <!-- Left Side: Space Illustration -->
      <section class="visual-side">
        <!-- 图片占位区域 - 后续放入插画图片 -->
        <div class="illustration-placeholder">
          <div class="placeholder-content">
            <div class="placeholder-icon"></div>
            <p></p>
            <span class="placeholder-hint">请将插画图片放入 /assets/images/login-bg.png</span>
          </div>
        </div>
      </section>

      <!-- Right Side: Login Form -->
      <section class="form-side">
        <div class="form-wrapper">
          <h1 class="form-title">登录</h1>

          <el-form :model="form" @submit.prevent="handleSubmit">
            <!-- Username -->
            <div class="form-item">
              <div class="input-wrapper">
                <span class="input-icon">
                  <svg viewBox="0 0 24 24" width="16" height="16">
                    <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                  </svg>
                </span>
                <input
                  v-model="form.username"
                  type="text"
                  placeholder="请输入账号/手机号"
                  class="custom-input"
                />
              </div>
            </div>

            <!-- Password -->
            <div class="form-item">
              <div class="input-wrapper">
                <span class="input-icon">
                  <svg viewBox="0 0 24 24" width="16" height="16">
                    <path fill="currentColor" d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/>
                  </svg>
                </span>
                <input
                  v-model="form.password"
                  type="password"
                  placeholder="请输入密码"
                  class="custom-input"
                />
              </div>
            </div>

            <!-- Captcha -->
            <div class="form-item captcha-row">
              <div class="input-wrapper captcha-input">
                <input
                  v-model="captchaInput"
                  type="text"
                  placeholder="验证码"
                  class="custom-input"
                />
              </div>
              <canvas
                ref="captchaCanvas"
                width="100"
                height="40"
                class="captcha-canvas"
                @click="generateCaptcha"
              ></canvas>
            </div>

            <!-- Submit Button -->
            <button
              type="submit"
              class="submit-btn"
              :disabled="loading"
              @click="handleSubmit"
            >
              <span v-if="loading" class="loading-icon">⟳</span>
              <span>{{ loading ? '登录中...' : '登录' }}</span>
            </button>
          </el-form>

          <!-- Quick Demo Accounts -->
          <div class="quick-demo">
            <p class="demo-title">快速体验测试账号</p>
            <div class="demo-btns">
              <button class="demo-btn teacher" @click="quickLogin('teacher')">
                <span class="demo-icon"></span>
                <span>教师账号</span>
              </button>
              <button class="demo-btn student" @click="quickLogin('student')">
                <span class="demo-icon"></span>
                <span>学生账号</span>
              </button>
              <button class="demo-btn admin" @click="quickLogin('admin')">
                <span class="demo-icon"></span>
                <span>管理员</span>
              </button>
            </div>
          </div>

          <!-- Register Link -->
          <div class="register-area">
            <span class="register-text">还没有账号？</span>
            <a class="register-link" @click="showRegister = true">立即注册</a>
          </div>

          <!-- Other login methods -->
          <div class="other-login">
            <div class="divider">
              <span>其他方式登录</span>
            </div>
            <div class="social-btns">
              <button class="social-btn" title="短信登录">
                <svg viewBox="0 0 24 24" width="20" height="20">
                  <path fill="currentColor" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
                </svg>
              </button>
              <button class="social-btn qq" title="QQ登录">
                <svg viewBox="0 0 1024 1024" width="20" height="20">
                  <path fill="currentColor" d="M824.8 613.2c-16-51.4-34.4-94.6-62.7-165.3C766.5 262.2 689.3 112 511.5 112 331.7 112 256.2 265.2 261 447.9c-28.4 70.8-46.7 113.7-62.7 165.3-34 109.5-23 154.8-14.6 155.8 18 2.2 70.1-82.4 70.1-82.4 0 49 25.2 112.9 79.8 159-26.4 8.1-85.7 29.9-71.6 53.8 11.4 19.3 196.2 12.3 249.5 6.3 53.3 6 238.1 13 249.5-6.3 14.1-23.8-45.3-45.7-71.6-53.8 54.6-46.2 79.8-110.1 79.8-159 0 0 52.1 84.6 70.1 82.4 8.5-1.1 19.5-46.4-14.5-155.8z"/>
                </svg>
              </button>
              <button class="social-btn wechat" title="微信登录">
                <svg viewBox="0 0 1024 1024" width="20" height="20">
                  <path fill="currentColor" d="M690.1 377.4c5.9 0 11.8.2 17.6.5-24.4-128.7-158.3-227.1-319.9-227.1C209 150.8 64 271.4 64 420.2c0 81.1 43.6 154.2 111.9 203.6 5.5 3.9 9.1 10.3 9.1 17.6 0 2.4-.5 4.6-1.1 6.9-5.5 20.3-14.2 52.8-14.6 54.3-.7 2.6-1.7 5.2-1.7 7.9 0 5.9 4.8 10.8 10.8 10.8 2.3 0 4.2-.9 6.2-2l70.9-40.9c5.3-3.1 11-5 17.2-5 3.2 0 6.4.5 9.5 1.4 33.1 9.5 68.8 14.8 105.7 14.8 6 0 11.9-.1 17.8-.4-7.1-21-10.9-43.1-10.9-66 0-135.8 132.2-245.8 295.3-245.8z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- Register Dialog -->
    <Teleport to="body">
      <div class="register-overlay" v-if="showRegister" @click.self="showRegister = false">
        <div class="register-dialog">
          <h2 class="register-title">快速注册</h2>
          
          <div class="reg-form-item">
            <label>用户名</label>
            <input v-model="regForm.username" type="text" placeholder="请输入用户名" class="custom-input" />
          </div>
          <div class="reg-form-item">
            <label>手机号</label>
            <input v-model="regForm.phone" type="text" placeholder="请输入手机号" class="custom-input" />
          </div>
          <div class="reg-form-item">
            <label>密码</label>
            <input v-model="regForm.password" type="password" placeholder="请设置密码（6位以上）" class="custom-input" />
          </div>
          <div class="reg-form-item">
            <label>确认密码</label>
            <input v-model="regForm.confirmPwd" type="password" placeholder="请再次输入密码" class="custom-input" />
          </div>
          <div class="reg-form-item">
            <label>选择角色</label>
            <div class="role-select">
              <button
                v-for="role in roles"
                :key="role.value"
                :class="['role-btn', { active: regForm.role === role.value }]"
                @click="regForm.role = role.value"
              >
                {{ role.label }}
              </button>
            </div>
          </div>

          <button class="reg-submit-btn" @click="handleRegister">注册</button>
          <p class="reg-cancel" @click="showRegister = false">取消</p>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

const form = reactive({
  username: '',
  password: ''
})

const captchaInput = ref('')
const captchaCode = ref('')
const captchaCanvas = ref(null)
const loading = ref(false)

// Register dialog
const showRegister = ref(false)
const regForm = reactive({
  username: '',
  phone: '',
  password: '',
  confirmPwd: '',
  role: 'student'
})
const roles = [
  { label: '学生', value: 'student' },
  { label: '教师', value: 'teacher' }
]

// Generate captcha
const generateCaptcha = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let code = ''
  for (let i = 0; i < 4; i++) {
    code += chars[Math.floor(Math.random() * chars.length)]
  }
  captchaCode.value = code
  drawCaptcha(code)
}

const drawCaptcha = (code) => {
  const canvas = captchaCanvas.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')

  // Background - light gray like screenshot
  ctx.fillStyle = '#f5f5f5'
  ctx.fillRect(0, 0, 100, 40)

  // Noise lines
  for (let i = 0; i < 8; i++) {
    ctx.strokeStyle = `rgba(${Math.random() * 100 + 100}, ${Math.random() * 100 + 100}, ${Math.random() * 150 + 100}, 0.4)`
    ctx.beginPath()
    ctx.moveTo(Math.random() * 100, Math.random() * 40)
    ctx.lineTo(Math.random() * 100, Math.random() * 40)
    ctx.lineWidth = 1.5
    ctx.stroke()
  }

  // Draw text - blue/purple style like screenshot
  ctx.font = 'bold 22px Arial'
  for (let i = 0; i < code.length; i++) {
    const hue = 220 + Math.random() * 60 // Blue to purple range
    ctx.fillStyle = `hsl(${hue}, 70%, 45%)`
    ctx.save()
    ctx.translate(15 + i * 22, 28)
    ctx.rotate((Math.random() - 0.5) * 0.6)
    ctx.fillText(code[i], 0, 0)
    ctx.restore()
  }

  // Noise dots
  for (let i = 0; i < 40; i++) {
    ctx.fillStyle = `rgba(${Math.random() * 100 + 150}, ${Math.random() * 100 + 150}, ${Math.random() * 100 + 150}, 0.3)`
    ctx.beginPath()
    ctx.arc(Math.random() * 100, Math.random() * 40, 1, 0, 2 * Math.PI)
    ctx.fill()
  }
}

// Star positioning
const starStyle = (n) => ({
  left: `${(n * 37 + n * 13) % 100}%`,
  top: `${(n * 29 + 7) % 100}%`,
  animationDelay: `${(n * 0.3) % 3}s`,
  width: `${1 + (n % 3)}px`,
  height: `${1 + (n % 3)}px`
})

// Quick login for demo accounts
const quickLogin = (role) => {
  form.username = role
  form.password = '123456'
  captchaInput.value = captchaCode.value
  handleSubmit()
}

// Handle register
const handleRegister = () => {
  if (!regForm.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!regForm.phone.trim() || !/^1[3-9]\d{9}$/.test(regForm.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!regForm.password || regForm.password.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  if (regForm.password !== regForm.confirmPwd) {
    ElMessage.warning('两次密码不一致')
    return
  }

  // Mock registration - auto login after register
  form.username = regForm.username
  form.password = regForm.password
  captchaInput.value = captchaCode.value
  showRegister.value = false

  ElMessage.success(`注册成功！欢迎 ${regForm.role === 'teacher' ? '教师' : '学生'} ${regForm.username}`)
  regForm.username = ''
  regForm.phone = ''
  regForm.password = ''
  regForm.confirmPwd = ''
  regForm.role = 'student'
}

// Submit
const handleSubmit = async () => {
  if (!form.username.trim()) {
    ElMessage.warning('请输入账号')
    return
  }
  if (!form.password.trim()) {
    ElMessage.warning('请输入密码')
    return
  }

  loading.value = true
  try {
    await auth.login({
      username: form.username,
      password: form.password
    })

    ElMessage.success('登录成功')

    // Redirect based on role
    const role = auth.user?.role
    if (role === 'admin') {
      router.push('/admin/dashboard')
    } else if (role === 'teacher') {
      router.push('/teacher/dashboard')
    } else {
      router.push('/student/dashboard')
    }
  } catch (error) {
    ElMessage.error(error.message || '登录失败，请检查账号密码')
    generateCaptcha()
    captchaInput.value = ''
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  generateCaptcha()
})
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  padding: 20px;
}

.login-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  width: 95vw;
  min-width: 800px;
  max-width: 1600px;
  height: calc(95vh - 40px);
  min-height: 500px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.12);
}

/* ========== Visual Side ========== */
.visual-side {
  position: relative;
  background: linear-gradient(135deg, #1a2a3a 0%, #2d3e50 50%, #1a2a3a 100%);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 插画占位区域 */
.illustration-placeholder {
  background-image: url('/assets/images/login-bg.png');
  background-size: cover;
  background-position: center;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;

  /* 装饰性背景元素 */
  &::before {
    content: '';
    position: absolute;
    top: 10%;
    left: 10%;
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: radial-gradient(circle at 30% 30%, rgba(255, 107, 107, 0.6), rgba(200, 70, 70, 0.3));
    filter: blur(2px);
  }

  &::after {
    content: '';
    position: absolute;
    bottom: 20%;
    right: 15%;
    width: 60px;
    height: 60px;
    border-radius: 50%;
    background: radial-gradient(circle at 30% 30%, rgba(78, 205, 196, 0.5), rgba(50, 160, 150, 0.2));
    filter: blur(2px);
  }
}

.placeholder-content {
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
  z-index: 1;

  .placeholder-icon {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.8;
  }

  p {
    font-size: 16px;
    margin: 0 0 8px 0;
  }

  .placeholder-hint {
    font-size: 12px;
    opacity: 0.5;
  }
}

/* ========== Form Side ========== */
.form-side {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 50px 60px;
  background: #fff;
}

.form-wrapper {
  width: 100%;
  max-width: 320px;
}

.form-title {
  font-size: 28px;
  font-weight: 500;
  color: #333;
  margin-bottom: 40px;
  text-align: center;
  letter-spacing: 2px;
}

.form-item {
  margin-bottom: 20px;
}

/* 自定义输入框样式 - 仅底边框 */
.input-wrapper {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e0e0e0;
  padding: 8px 0;
  transition: border-color 0.3s;

  &:focus-within {
    border-bottom-color: #4a90d9;
  }

  .input-icon {
    color: #999;
    margin-right: 12px;
    display: flex;
    align-items: center;
  }

  .custom-input {
    flex: 1;
    border: none;
    outline: none;
    background: transparent;
    font-size: 14px;
    color: #333;
    padding: 4px 0;

    &::placeholder {
      color: #bbb;
    }
  }
}

/* 验证码行 */
.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;

  .captcha-input {
    flex: 1;
  }
}

.captcha-canvas {
  border-radius: 4px;
  cursor: pointer;
  transition: opacity 0.3s;
  flex-shrink: 0;

  &:hover {
    opacity: 0.9;
  }
}

/* 登录按钮 - 大圆角矩形 */
.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 30px;
  background: #4a5568;
  color: #fff;
  border: none;
  border-radius: 24px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;

  &:hover:not(:disabled) {
    background: #3d4856;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(74, 85, 104, 0.3);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }

  .loading-icon {
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Quick Demo Accounts */
.quick-demo {
  margin-top: 24px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.demo-title {
  font-size: 12px;
  color: #999;
  text-align: center;
  margin: 0 0 12px 0;
}

.demo-btns {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.demo-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 10px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background: #fafafa;
  cursor: pointer;
  font-size: 12px;
  color: #666;
  transition: all 0.25s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 3px 10px rgba(0, 0, 0, 0.08);
    background: #fff;
  }

  .demo-icon {
    font-size: 14px;
  }

  &.teacher:hover {
    border-color: #667eea;
    color: #667eea;
    background: #f5f0ff;
  }

  &.student:hover {
    border-color: #10b981;
    color: #10b981;
    background: #f0fdf4;
  }

  &.admin:hover {
    border-color: #f59e0b;
    color: #f59e0b;
    background: #fffbeb;
  }
}

/* Register Area */
.register-area {
  margin-top: 20px;
  text-align: center;

  .register-text {
    font-size: 13px;
    color: #999;
  }

  .register-link {
    font-size: 13px;
    color: #4a90d9;
    cursor: pointer;
    margin-left: 4px;

    &:hover {
      text-decoration: underline;
    }
  }
}

/* Register Dialog */
.register-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.register-dialog {
  width: 400px;
  max-width: 90vw;
  background: #fff;
  border-radius: 16px;
  padding: 32px 28px 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.register-title {
  font-size: 22px;
  font-weight: 600;
  color: #333;
  text-align: center;
  margin: 0 0 24px 0;
}

.reg-form-item {
  margin-bottom: 18px;

  label {
    display: block;
    font-size: 13px;
    color: #555;
    margin-bottom: 6px;
    font-weight: 500;
  }

  .custom-input {
    width: 100%;
    height: 40px;
    padding: 0 14px;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    font-size: 14px;
    outline: none;
    transition: border-color 0.3s;
    box-sizing: border-box;

    &:focus {
      border-color: #4a90d9;
    }

    &::placeholder {
      color: #bbb;
    }
  }
}

.role-select {
  display: flex;
  gap: 10px;
}

.role-btn {
  flex: 1;
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #fafafa;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.25s;

  &:hover {
    border-color: #ccc;
  }

  &.active {
    border-color: #4a90d9;
    background: #eef4ff;
    color: #4a90d9;
    font-weight: 500;
  }
}

.reg-submit-btn {
  width: 100%;
  height: 44px;
  margin-top: 8px;
  background: linear-gradient(135deg, #4a90d9, #357abd);
  color: #fff;
  border: none;
  border-radius: 22px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(74, 144, 217, 0.35);
  }
}

.reg-cancel {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: #999;
  cursor: pointer;

  &:hover {
    color: #666;
  }
}

/* 其他登录方式 */
.other-login {
  margin-top: 40px;
}

.divider {
  position: relative;
  text-align: center;
  margin-bottom: 20px;

  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, #e0e0e0, transparent);
  }

  span {
    position: relative;
    background: #fff;
    padding: 0 16px;
    font-size: 12px;
    color: #999;
  }
}

.social-btns {
  display: flex;
  justify-content: center;
  gap: 24px;
}

.social-btn {
  width: 36px;
  height: 36px;
  border: 1px solid #e0e0e0;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  color: #999;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  &.qq {
    color: #12b7f5;
    &:hover { border-color: #12b7f5; }
  }

  &.wechat {
    color: #09bb07;
    &:hover { border-color: #09bb07; }
  }
}

/* Responsive */
@media (max-width: 900px) {
  .login-container {
    grid-template-columns: 1fr;
    width: 90%;
    max-width: 420px;
    height: auto;
    min-height: auto;
  }

  .visual-side {
    display: none;
  }

  .form-side {
    padding: 40px 30px;
  }
}
</style>
