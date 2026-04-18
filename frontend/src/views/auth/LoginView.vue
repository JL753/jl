<template>
  <div class="login-page">
    <div class="login-container">
      <!-- Left Side: Space Visual Area -->
      <section class="visual-side">
        <!-- Decorative planets -->
        <div class="planet planet-red"></div>
        <div class="planet planet-cyan"></div>
        <div class="planet-ring"></div>
        <!-- Orbit lines -->
        <div class="orbit orbit-1"></div>
        <div class="orbit orbit-2"></div>
        <div class="orbit orbit-3"></div>
        <!-- Stars -->
        <div class="stars">
          <span v-for="n in 20" :key="n" :class="'star star-' + n" :style="starStyle(n)"></span>
        </div>

        <!-- Hand/Astronaut Illustration Area -->
        <div class="visual-illustration">
          <div class="hand-figure">
            <div class="hand-body"></div>
            <div class="hand-fingers">
              <span class="finger f1"></span>
              <span class="finger f2"></span>
              <span class="finger f3"></span>
              <span class="finger f4"></span>
            </div>
          </div>
        </div>
      </section>

      <!-- Right Side: Form Area -->
      <section class="form-side">
        <div class="form-wrapper">
          <h2 class="form-title">登录</h2>

          <el-form :model="form" label-position="top" @submit.prevent="submit">
            <div class="form-field">
              <el-input v-model="form.username" size="large" placeholder="请输入账号/手机号" prefix-icon="User" />
            </div>

            <div class="form-field">
              <el-input v-model="form.password" :type="showPassword ? 'text' : 'password'" size="large" placeholder="请输入密码" prefix-icon="Lock" show-password @change="(val) => showPassword = val">
                <template #suffix>
                  <button type="button" class="toggle-pwd" @click="showPassword = !showPassword">
                    {{ showPassword ? '🙈' : '👁' }}
                  </button>
                </template>
              </el-input>
            </div>

            <div class="form-field captcha-field">
              <div class="captcha-row">
                <el-input v-model="captchaInput" size="large" placeholder="验证码" />
                <canvas ref="captchaCanvas" width="90" height="36" class="captcha-img" @click="refreshCaptcha"></canvas>
                <el-button size="large" class="refresh-captcha-btn" @click="refreshCaptcha">刷新</el-button>
              </div>
            </div>

            <!-- Submit button -->
            <el-button type="primary" size="large" class="btn-submit" :loading="loading" @click="submit" :disabled="loading">
              {{ loading ? '登录 中...' : '登 录' }}
            </el-button>
          </el-form>

          <!-- Other login methods -->
          <div class="other-login">
            <p>其他方式登录</p>
            <div class="social-icons">
              <button class="social-btn qq" title="QQ登录">
                <svg viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12c0 4.84 3.44 8.87 8 9.8V15H8v-2h2V9.5C10 7.57 11.57 6 13.5 6H16v2h-2.5C12.67 8 12 8.67 12 9.5V13h2v2h-2v5.95c5.05-.5 9-4.76 9-9.95 0-5.52-4.48-10-10-10z"/></svg>
              </button>
              <button class="social-btn weibo" title="微博登录">
                <svg viewBox="0 0 24 24"><path d="M20 8h-3V5h-2v3h-3v2h3v3h2v-3h3V8zM10 14H6v-2h4v2zm0-4H6V8h4v2zm0-4H6V4h4v2z"/></svg>
              </button>
              <button class="social-btn wechat" title="微信登录">
                <svg viewBox="0 0 24 24"><path d="M8.5 11a1 1 0 110-2 1 1 0 010 2zm5 0a1 1 0 110-2 1 1 0 010 2zM17 7c-.55 0-1 .45-1 1s.45 1 1 1 1-.45 1-1-.45-1-1-1zM12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z"/></svg>
              </button>
            </div>
          </div>

          <div class="quick-fill-hint">
            <span>演示账号：</span>
            <a href="#" @click.prevent="fill('teacher')">教师</a> / 
            <a href="#" @click.prevent="fill('student')">学生</a>
            <span>密码：123456</span>
          </div>
        </div>
      </section>
    </div>

    <!-- Bottom tip banner -->
    <div class="bottom-tip-bar">
      <span class="tip-icon">💡</span>
      <span>PC端有两种登录方式</span>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()
const showPassword = ref(false)
const loading = ref(false)
const captchaCanvas = ref(null)
const captchaInput = ref('12')
let captchaCode = ''

const form = reactive({
  username: '16602365140',
  password: '123456'
})

// Generate random captcha code
const refreshCaptcha = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  captchaCode = ''
  for (let i = 0; i < 4; i++) {
    captchaCode += chars[Math.floor(Math.random() * chars.length)]
  }
  drawCaptcha()
}

const drawCaptcha = () => {
  const canvas = captchaCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = '#eef2f7'
  ctx.fillRect(0, 0, 90, 36)

  // Draw noise lines
  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `rgba(${Math.random()*100},${Math.random()*150 + 100},${Math.random()*200+50},0.3)`
    ctx.beginPath()
    ctx.moveTo(Math.random() * 90, Math.random() * 36)
    ctx.lineTo(Math.random() * 90, Math.random() * 36)
    ctx.stroke()
  }

  // Draw text
  ctx.font = 'bold 20px Arial'
  ctx.fillStyle = '#334155'
  for (let i = 0; i < captchaCode.length; i++) {
    ctx.save()
    ctx.translate(16 + i * 18, 25)
    ctx.rotate((Math.random() - 0.5) * 0.4)
    ctx.fillText(captchaCode[i], 0, 0)
    ctx.restore()
  }
}

const starStyle = (n) => ({
  left: `${(n * 37 + n * 13) % 100}%`,
  top: `${(n * 29 + 7) % 100}%`,
  animationDelay: `${(n * 0.3) % 3}s`,
  width: `${1 + (n % 3)}px`,
  height: `${1 + (n % 3)}px`
})

const fill = (role) => {
  form.username = role === 'teacher' ? 'teacher' : 'student'
  form.password = '123456'
}

const jumpByRole = () => {
  if (auth.user?.role === 'teacher' || auth.user?.role === 'admin') {
    router.push('/teacher/dashboard')
  } else {
    router.push('/student/dashboard')
  }
}

const submit = async () => {
  // Simple captcha validation - lenient for demo
  if (!captchaInput.value.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }

  loading.value = true
  try {
    await auth.login({ username: form.username, password: form.password })
    jumpByRole()
  } catch (e) {
    ElMessage.error(e.message || '登录失败，请检查账号密码')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">

.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-lg;
  background: linear-gradient(180deg, #173e64 0%, #224d70 40%, #194f77 100%);
  overflow: hidden;
  position: relative;
}

.login-container {
  display: grid;
  grid-template-columns: 480px 420px;
  width: min(1080px, 100%);
  min-height: 600px;
  border-radius: $radius-xlarge;
  overflow: hidden;
  box-shadow: $shadow-heavy;
}

/* ========== Visual Side (Left) ========== */
.visual-side {
  position: relative;
  overflow: hidden;
  padding: 40px 32px;
  background: linear-gradient(180deg, #10253d 0%, #173456 60%, #194f77 100%);
  color: #eaf5ff;
  display: flex;
  align-items: flex-end;
  justify-content: center;

  &::after {
    content: '';
    position: absolute;
    right: -54px;
    top: -20px;
    width: 190px;
    height: 115%;
    background: $bg-white;
    border-radius: 46% 0 0 46%;
  }
}

/* Planets */
.planet {
  position: absolute;
  border-radius: 50%;
  filter: blur(.3px);
  animation: floatSlow 12s ease-in-out infinite;
}

.planet-red {
  top: -35px;
  left: -45px;
  width: 220px;
  height: 220px;
  background: radial-gradient(circle at 38% 35%, #ff9278, #ea5757 52%, #a83e5a 100%);
  box-shadow:
    0 0 0 18px rgba(242,120,120,.07),
    0 0 0 42px rgba(242,120,120,.03),
    inset 0 0 30px rgba(255,140,120,.15);
}

.planet-cyan {
  right: 80px;
  top: 150px;
  width: 140px;
  height: 140px;
  background: radial-gradient(circle at 38% 35%, #a8ffff, #52d2d9 55%, #2f6f89 100%);
  box-shadow:
    0 0 0 14px rgba(82,210,217,.06),
    inset 0 0 25px rgba(82,210,217,.12);
  animation-delay: -3s;
}

.planet-ring {
  position: absolute;
  right: 55px;
  top: 125px;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  border: 1.5px solid rgba(82,210,217,.18);
  transform: rotateX(65deg);
  animation: ringSpin 18s linear infinite;
}

/* Orbits */
.orbit {
  position: absolute;
  border-radius: 999px;
  opacity: .25;
}

.orbit-1 {
  left: 30px;
  bottom: 100px;
  width: 300px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(149,232,255,.4), transparent);
}

.orbit-2 {
  left: 70px;
  bottom: 70px;
  width: 240px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(149,232,255,.25), transparent);
}

.orbit-3 {
  left: 50px;
  bottom: 130px;
  width: 190px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(149,232,255,.18), transparent);
}

/* Stars */
.stars {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.star {
  position: absolute;
  background: $bg-white;
  border-radius: 50%;
  opacity: 0;
  animation: twinkle 3s ease-in-out infinite;
}

/* Hand/Astronaut illustration */
.visual-illustration {
  position: relative;
  z-index: 2;
  margin-bottom: $spacing-lg;
}

.hand-figure {
  position: relative;
  width: 160px;
  height: 200px;
}

.hand-body {
  position: absolute;
  bottom: 0;
  left: 30%;
  width: 70px;
  height: 140px;
  background: linear-gradient(135deg, #ffe4c4, #f5d4b8 50%, #ebc8a0);
  border-radius: 35px 35px 40px 40px;
  transform: rotate(-10deg);
  box-shadow: 0 10px 30px rgba(0,0,0,.15);
}

.hand-fingers {
  position: absolute;
  bottom: 110px;
  left: 42%;
  display: flex;
  gap: 6px;
  transform: rotate(-15deg);
}

.finger {
  width: 22px;
  height: 55px;
  background: linear-gradient(135deg, #ffe4c4, #f0d0a8);
  border-radius: 14px 14px 10px 10px;
  box-shadow: 0 4px 12px rgba(0,0,0,.1);

  &.f1 {
    height: 62px;
    transform: translateY(-8px);
  }

  &.f2 {
    height: 58px;
    transform: translateY(-4px);
  }

  &.f3 {
    height: 52px;
  }

  &.f4 {
    height: 46px;
    transform: translateY(4px);
  }
}

/* ========== Form Side (Right) ========== */
.form-side {
  background: linear-gradient(180deg, $bg-white, #fbfcff);
  display: grid;
  place-items: center;
  padding: 36px 32px;
  position: relative;
}

.form-wrapper {
  width: 100%;
  max-width: 380px;
}

.form-title {
  font-size: 34px;
  font-weight: $font-weight-bold;
  color: $text-primary;
  text-align: center;
  margin: 0 0 $spacing-xl;
}

.form-field {
  margin-bottom: $spacing-md;
}

.captcha-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: $spacing-sm;
  align-items: center;
}

.captcha-img {
  border-radius: $radius-base;
  cursor: pointer;
  border: 1.5px solid $border-light;
  transition: border-color $transition-base;
  height: 38px !important;

  &:hover {
    border-color: $primary-light;
  }
}

.refresh-captcha-btn {
  padding: 0 $spacing-sm !important;
  font-size: $font-size-sm !important;
  color: $primary-color !important;
  border-color: $primary-light !important;
  background: rgba(64, 158, 255, 0.05) !important;
}

.toggle-pwd {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: none;
  cursor: pointer;
  font-size: $font-size-md;
  padding: 4px;
}

.btn-submit {
  width: 100% !important;
  height: 48px !important;
  border-radius: $radius-medium !important;
  font-size: $font-size-md !important;
  font-weight: $font-weight-semibold !important;
  margin-top: $spacing-xs;
  letter-spacing: 4px;
  background: $gradient-primary !important;
  border: none !important;
  box-shadow: $shadow-card !important;
  transition: all $transition-base !important;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-card-hover, $glow-primary !important;
  }

  &:active {
    transform: translateY(0);
  }
}

.other-login {
  text-align: center;
  padding-top: $spacing-lg;
  border-top: 1px solid $border-extra-light;
  margin-top: $spacing-xs;

  p {
    font-size: $font-size-xs;
    color: $text-secondary;
    margin: 0 0 $spacing-sm;
  }
}

.social-icons {
  display: flex;
  justify-content: center;
  gap: $spacing-sm;
}

.social-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 1.5px solid $border-light;
  background: $bg-white;
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: all $transition-base;

  svg {
    width: 20px;
    height: 20px;
    fill: currentColor;
  }

  &.qq {
    color: #12b7f5;

    &:hover {
      border-color: #12b7f5;
      background: #eff6ff;
      transform: translateY(-2px);
    }
  }

  &.weibo {
    color: #e6162d;

    &:hover {
      border-color: #e6162d;
      background: #fef2f2;
      transform: translateY(-2px);
    }
  }

  &.wechat {
    color: #07c160;

    &:hover {
      border-color: #07c160;
      background: #ecfdf5;
      transform: translateY(-2px);
    }
  }
}

.quick-fill-hint {
  margin-top: $spacing-md;
  text-align: center;
  font-size: $font-size-xs;
  color: $text-secondary;

  span {
    margin-right: 4px;
  }

  a {
    color: $primary-color;
    cursor: pointer;
    font-weight: $font-weight-medium;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

/* Bottom tip bar */
.bottom-tip-bar {
  position: fixed;
  bottom: $spacing-lg;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255,255,255,.12);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,.15);
  border-radius: $radius-round;
  padding: $spacing-xs $spacing-xl;
  color: rgba(255,255,255,.85);
  font-size: $font-size-sm;
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  z-index: $z-index-fixed;

  .tip-icon {
    font-size: $font-size-md;
  }
}

/* Animations */
@keyframes floatSlow {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(14px);
  }
}

@keyframes ringSpin {
  from {
    transform: rotateX(65deg) rotateZ(0deg);
  }
  to {
    transform: rotateX(65deg) rotateZ(360deg);
  }
}

@keyframes twinkle {
  0%, 100% {
    opacity: 0;
  }
  50% {
    opacity: .7;
  }
}

/* Responsive */
@media (max-width: 1024px) {
  .login-container {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .visual-side {
    min-height: 280px;
    align-items: flex-end;

    &::after {
      display: none;
    }
  }

  .form-side {
    padding: 32px $spacing-xl;
  }

  .bottom-tip-bar {
    position: static;
    margin-top: $spacing-md;
    transform: none;
  }
}

@media (max-width: 520px) {
  .login-container {
    border-radius: $radius-large;
  }

  .captcha-row {
    grid-template-columns: 1fr;
  }

  .form-wrapper {
    max-width: 100%;
  }
}
</style>
