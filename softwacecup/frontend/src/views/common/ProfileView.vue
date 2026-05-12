<template>
  <div class="profile-page modern-profile">
    <!-- Save Success Banner -->
    <transition name="save-pop">
      <div v-if="savePulse" class="success-banner">
        <span class="banner-icon">✦</span>
        <div><strong>保存成功</strong><p>个人信息与画像已同步更新</p></div>
      </div>
    </transition>

    <!-- Hero Section -->
    <section class="hero-section panel">
      <div class="hero-left">
        <p class="eyebrow">{{ roleTitleMap[role]?.toUpperCase() || 'PERSONAL CENTER' }}</p>
        <h2>{{ titleMap[role] || '个人中心' }}</h2>
        <p class="hero-desc">以更完整的分组表单维护你的账号信息、教学/学习画像与个性化目标，保存后会实时同步到相关页面。</p>
        <div class="hero-tags">
          <span class="soft-tag role-tag">{{ roleLabelMap[role] || '平台账号' }}</span>
          <span class="soft-tag date-tag">{{ todayText }}</span>
          <span :class="['soft-tag', form.examGoal ? 'status-ok' : 'status-warn']">{{ form.examGoal ? '✓ 画像已建立' : '○ 画像待完善' }}</span>
        </div>
      </div>
      <div class="hero-right">
        <div class="metric-card blue-metric">
          <span>{{ roleMetricLabel }}数</span>
          <strong>{{ profile.examCount || 0 }}</strong>
          <small>{{ roleMetricDesc }}</small>
        </div>
        <div class="metric-card green-metric">
          <span>资源沉淀数</span>
          <strong>{{ profile.resourceCount || 0 }}</strong>
          <small>能力指数 {{ profile.accuracy || '0' }}</small>
        </div>
      </div>
    </section>

    <!-- Main Layout: Sidebar + Content -->
    <section class="profile-layout">
      <!-- Left Sidebar -->
      <aside class="sidebar">
        <!-- Avatar Panel -->
        <div class="avatar-panel">
          <div class="avatar-ring-wrap">
            <img v-if="form.avatarUrl" :src="form.avatarUrl" alt="avatar" class="avatar-img" />
            <div v-else class="avatar-placeholder">{{ (form.displayName || auth.user?.username || '?').slice(0,1) }}</div>
            <div class="ring-decor ring-outer"></div>
            <div class="ring-decor ring-inner"></div>
          </div>
          <h3 class="display-name">{{ form.displayName || auth.user?.displayName || '未设置昵称' }}</h3>
          <p class="username-text">@{{ profile.username || auth.user?.username || '-' }}</p>
          <div class="role-badges">
            <span class="badge primary-badge">{{ roleLabelMap[role] }}</span>
            <span v-if="form.course" class="badge course-badge">{{ form.course }}</span>
          </div>
        </div>

        <!-- Upload Panel -->
        <div class="upload-panel">
          <div class="upload-header"><label>头像设置</label><span>本地上传 / 图片链接</span></div>
          <label class="upload-btn-label" for="avatarFileInput">📷 上传图片</label>
          <input id="avatarFileInput" type="file" accept="image/*" @change="onAvatarUpload" style="display:none;" />
          <button class="clear-avatar-btn" @click="form.avatarUrl = ''" v-if="form.avatarUrl">清除</button>
          <input v-model="form.avatarUrl" placeholder="或粘贴图片URL..." class="url-input" />
          <div class="preset-grid">
            <button v-for="(preset, idx) in avatarPresets" :key="idx" class="preset-btn" @click="form.avatarUrl = preset" type="button">
              <img :src="preset" alt="" />
            </button>
          </div>
        </div>

        <!-- Stats Cards -->
        <div class="stats-row">
          <div class="stat-mini"><b>{{ profile.examCount || 0 }}</b><span>考试次数</span></div>
          <div class="stat-mini"><b>{{ profile.resourceCount || 0 }}</b><span>资源数量</span></div>
          <div class="stat-mini"><b>{{ profile.accuracy || 0 }}</b><span>能力指数</span></div>
        </div>
      </aside>

      <!-- Right Main Content -->
      <main class="main-content">
        <!-- Group 01: Basic Info -->
        <article class="panel form-panel">
          <div class="panel-head"><div><span class="group-tag">GROUP 01</span><h3>基础信息</h3></div><span class="soft-tag info-tag">账号资料</span></div>
          <el-form label-position="top" :model="form" class="form-grid">
            <el-form-item label="显示名称"><el-input v-model="form.displayName" placeholder="请输入显示名称" /></el-form-item>
            <el-form-item label="账号密码"><el-input v-model="form.password" type="password" show-password placeholder="留空则不修改" /></el-form-item>
            <el-form-item :label="majorLabel">
              <el-input v-model="form.major" :placeholder="majorPlaceholder" />
            </el-form-item>
            <el-form-item :label="courseLabel">
              <el-input v-model="form.course" :placeholder="coursePlaceholder" />
            </el-form-item>
          </el-form>
        </article>

        <!-- Group 02: Role-specific Profile (画像) -->
        <article class="panel form-panel">
          <div class="panel-head"><div><span class="group-tag group-2">GROUP 02</span><h3>{{ profileTitle }}</h3></div><span class="soft-tag accent-tag">角色差异化</span></div>
          <el-form label-position="top" :model="form" class="form-grid">
            <el-form-item :label="baseLabel"><el-input v-model="form.knowledgeBase" :placeholder="basePlaceholder" /></el-form-item>
            <el-form-item :label="styleLabel"><el-input v-model="form.cognitiveStyle" :placeholder="stylePlaceholder" /></el-form-item>
            <el-form-item :label="interestLabel"><el-input v-model="form.interestPreference" :placeholder="interestPlaceholder" /></el-form-item>
            <el-form-item :label="paceLabel"><el-input v-model="form.pacePreference" :placeholder="pacePlaceholder" /></el-form-item>
            <el-form-item :class="'span-full'" :label="weakLabel">
              <el-input v-model="form.weakPoints" type="textarea" :rows="3" :placeholder="weakPlaceholder" />
            </el-form-item>
            <el-form-item :class="'span-full'" :label="goalLabel">
              <el-input v-model="form.examGoal" type="textarea" :rows="2" :placeholder="goalPlaceholder" />
            </el-form-item>
          </el-form>
        </article>

        <!-- Group 03: Summary Preview -->
        <article class="panel summary-panel">
          <div class="panel-head"><div><span class="group-tag group-3">GROUP 03</span><h3>画像标签与摘要</h3></div><span class="soft-tag preview-tag">实时预览</span></div>
          <div class="summary-cards">
            <div v-for="(item, idx) in summaryItems" :key="idx" class="sum-card">
              <span>{{ item.label }}</span><strong>{{ item.value }}</strong>
            </div>
          </div>
          <div class="tag-cloud">
            <span v-for="(tag, idx) in dynamicTags" :key="idx" class="cloud-tag">{{ tag }}</span>
            <span v-if="dynamicTags.length === 0" class="empty-hint">填写表单后，标签将自动生成...</span>
          </div>
        </article>

        <!-- Group 04: Course Schedule (仅学生端显示) -->
        <article v-if="role === 'student'" class="panel form-panel">
          <div class="panel-head">
            <div><span class="group-tag group-4">GROUP 04</span><h3>我的课表</h3></div>
            <span class="soft-tag schedule-tag">课程安排</span>
          </div>
          <div class="schedule-actions">
            <el-button type="primary" size="small" @click="showAddScheduleDialog = true">➕ 添加课程</el-button>
            <el-button size="small" @click="loadSchedule">🔄 刷新</el-button>
            <el-button size="small" type="danger" plain @click="clearSchedule">🗑️ 清空课表</el-button>
          </div>
          <div class="schedule-grid">
            <div v-for="day in 7" :key="day" class="day-column">
              <div class="day-header">{{ ['周一', '周二', '周三', '周四', '周五', '周六', '周日'][day - 1] }}</div>
              <div class="course-list">
                <div v-for="course in getCoursesForDay(day)" :key="course.id" class="course-item" @click="editCourse(course)">
                  <div class="course-time">{{ course.startTime }}-{{ course.endTime }}</div>
                  <div class="course-name">{{ course.courseName }}</div>
                  <div class="course-location">{{ course.location }}</div>
                  <div class="course-teacher">{{ course.teacher }}</div>
                </div>
                <div v-if="getCoursesForDay(day).length === 0" class="empty-day">无课</div>
              </div>
            </div>
          </div>
        </article>

        <!-- Sticky Action Bar -->
        <div class="action-bar">
          <div class="action-info">
            <strong>已启用高级表单模式</strong>
            <p>支持角色差异化字段、头像上传、保存动效反馈。</p>
          </div>
          <div class="action-buttons">
            <el-button @click="resetForm">重置</el-button>
            <el-button type="primary" @click="save">保存修改</el-button>
          </div>
        </div>
      </main>
    </section>

    <!-- 添加/编辑课程对话框 -->
    <el-dialog v-model="showAddScheduleDialog" :title="editingCourse ? '编辑课程' : '添加课程'" width="500px">
      <el-form :model="scheduleForm" label-width="80px">
        <el-form-item label="星期">
          <el-select v-model="scheduleForm.dayOfWeek" placeholder="请选择">
            <el-option v-for="(day, idx) in ['周一', '周二', '周三', '周四', '周五', '周六', '周日']" :key="idx + 1" :label="day" :value="idx + 1" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-picker v-model="scheduleForm.startTime" format="HH:mm" value-format="HH:mm" placeholder="选择时间" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-picker v-model="scheduleForm.endTime" format="HH:mm" value-format="HH:mm" placeholder="选择时间" />
        </el-form-item>
        <el-form-item label="课程名称">
          <el-input v-model="scheduleForm.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="上课地点">
          <el-input v-model="scheduleForm.location" placeholder="请输入上课地点" />
        </el-form-item>
        <el-form-item label="授课教师">
          <el-input v-model="scheduleForm.teacher" placeholder="请输入教师姓名" />
        </el-form-item>
        <el-form-item label="上课周次">
          <el-input v-model="scheduleForm.weeks" placeholder="如: 1-16" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddScheduleDialog = false">取消</el-button>
        <el-button v-if="editingCourse" type="danger" @click="deleteCourse">删除</el-button>
        <el-button type="primary" @click="saveCourse">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiProfileCard } from '../../api'
import { useAuthStore } from '../../stores/auth'
import axios from 'axios'

const auth = useAuthStore()
const profile = ref({ summary: [] })
const savePulse = ref(false)
const form = reactive({
  displayName: '', password: '', avatarUrl: '',
  major: '', course: '', knowledgeBase: '',
  cognitiveStyle: '', weakPoints: '', interestPreference: '',
  pacePreference: '', examGoal: ''
})

// 课表相关状态
const scheduleList = ref([])
const showAddScheduleDialog = ref(false)
const editingCourse = ref(null)
const scheduleForm = reactive({
  dayOfWeek: 1,
  startTime: '08:00',
  endTime: '09:40',
  courseName: '',
  location: '',
  teacher: '',
  weeks: '1-16'
})

const role = computed(() => auth.user?.role || 'student')
const titleMap = { teacher: '教师端个人中心', student: '学生端个人中心', admin: '管理员个人中心' }
const roleTitleMap = { teacher: 'TEACHER CENTER', student: 'STUDENT CENTER', admin: 'ADMIN CENTER' }
const roleLabelMap = { teacher: '教师账号', student: '学生账号', admin: '管理员账号' }
const todayText = computed(() => new Date().toLocaleDateString('zh-CN'))

// Role-dependent labels
const roleMetricLabel = computed(() => role.value === 'teacher' ? '教学活动' : role.value === 'admin' ? '管理动作' : '学习记录')
const roleMetricDesc = computed(() => {
  if (role.value === 'teacher') return '近阶段教学与考试任务'
  if (role.value === 'admin') return '近阶段治理与巡检操作'
  return '近阶段学习与考试行为'
})
const profileTitle = computed(() => role.value === 'teacher' ? '教学画像' : role.value === 'admin' ? '管理画像' : '学习画像')
const majorLabel = computed(() => role.value === 'teacher' ? '任教学科 / 方向' : role.value === 'admin' ? '管理方向' : '所属专业 / 方向')
const majorPlaceholder = computed(() => {
  if (role.value === 'teacher') return '如：人工智能教育 / 软件工程'
  if (role.value === 'admin') return '如：平台运营 / 数据治理'
  return '如：软件工程 / 人工智能'
})
const courseLabel = computed(() => role.value === 'teacher' ? '当前授课课程' : role.value === 'admin' ? '当前负责模块' : '当前课程')
const coursePlaceholder = computed(() => {
  if (role.value === 'teacher') return '如：人工智能导论'
  if (role.value === 'admin') return '如：数据中台 / 平台运营'
  return '如：人工智能导论'
})
const baseLabel = computed(() => role.value === 'teacher' ? '教学基础' : role.value === 'admin' ? '管理基础' : '知识基础')
const basePlaceholder = computed(() => {
  if (role.value === 'teacher') return '扎实 / 丰富'
  if (role.value === 'admin') return '熟悉系统 / 熟悉流程'
  return '中等 / 扎实 / 偏弱'
})
const styleLabel = computed(() => role.value === 'teacher' ? '授课风格' : role.value === 'admin' ? '工作风格' : '认知风格')
const stylePlaceholder = computed(() => {
  if (role.value === 'teacher') return '案例驱动 / 图解优先'
  if (role.value === 'admin') return '数据驱动 / 过程导向'
  return '图文结合 / 视频优先'
})
const interestLabel = computed(() => role.value === 'teacher' ? '内容偏好' : role.value === 'admin' ? '管理偏好' : '兴趣偏好')
const interestPlaceholder = computed(() => {
  if (role.value === 'teacher') return '项目案例 / 实验演示'
  if (role.value === 'admin') return '数据看板 / 巡检复盘'
  return '项目实战 / 理论精讲'
})
const paceLabel = computed(() => role.value === 'teacher' ? '工作节奏' : role.value === 'admin' ? '运营节奏' : '学习节奏')
const pacePlaceholder = computed(() => {
  if (role.value === 'teacher') return '每周4次 / 课前集中备课'
  if (role.value === 'admin') return '每日巡检 / 每周复盘'
  return '每周3次 / 高强度冲刺'
})
const weakLabel = computed(() => role.value === 'teacher' ? '当前难点' : role.value === 'admin' ? '当前痛点' : '薄弱环节')
const weakPlaceholder = computed(() => {
  if (role.value === 'teacher') return '课堂节奏、案例组织、试卷讲评等'
  if (role.value === 'admin') return '数据联动、系统巡检、用户活跃度等'
  return '知识点、能力项或考试短板'
})
const goalLabel = computed(() => role.value === 'teacher' ? '阶段目标' : role.value === 'admin' ? '管理目标' : '考试/学习目标')
const goalPlaceholder = computed(() => {
  if (role.value === 'teacher') return '课堂完成度95+ / 组卷效率提升'
  if (role.value === 'admin') return '平台稳定运行 / 数据闭环清晰'
  return '期末85分以上 / 强化模型评估能力'
})

const avatarPresets = [
  'https://api.dicebear.com/7.x/initials/svg?seed=Teacher&backgroundColor=f0f9ff',
  'https://api.dicebear.com/7.x/initials/svg?seed=Student&backgroundColor=ecfdf5',
  'https://api.dicebear.com/7.x/initials/svg?seed=Admin&backgroundColor=eff6ff',
  'https://api.dicebear.com/7.x/initials/svg?seed=SmartPrep&backgroundColor=fef2ff'
]

const onAvatarUpload = (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => { form.avatarUrl = String(reader.result || '') }
  reader.readAsDataURL(file)
  e.target.value = ''
}

const summaryItems = computed(() => [
  { label: '当前角色', value: roleLabelMap[role.value] || '平台账号' },
  { label: role.value === 'teacher' ? '授课课程' : role.value === 'admin' ? '负责模块' : '当前课程', value: form.course || '-' },
  { label: role.value === 'teacher' ? '教学基础' : role.value === 'admin' ? '管理基础' : '知识基础', value: form.knowledgeBase || '-' },
  { label: role.value === 'teacher' ? '授课风格' : role.value === 'admin' ? '工作风格' : '认知风格', value: form.cognitiveStyle || '-' },
  { label: '资源总数', value: `${profile.value.resourceCount || 0} 份` },
  { label: '考试记录', value: `${profile.value.examCount || 0} 次` }
])

const dynamicTags = computed(() => {
  const tags = []
  if (form.course) tags.push(form.course)
  if (form.major) tags.push(form.major)
  if (form.knowledgeBase) tags.push(form.knowledgeBase)
  if (form.cognitiveStyle) tags.push(form.cognitiveStyle)
  if (form.interestPreference) tags.push(form.interestPreference)
  if (form.pacePreference) tags.push(form.pacePreference)
  if (form.weakPoints) tags.push(form.weakPoints)
  if (form.examGoal) tags.push(form.examGoal)
  return tags
})

const fillForm = () => {
  form.displayName = profile.value.displayName || auth.user?.displayName || ''
  form.password = ''
  form.avatarUrl = profile.value.avatarUrl || auth.user?.avatarUrl || ''
  form.major = profile.value.major || ''
  form.course = profile.value.course || ''
  form.knowledgeBase = profile.value.knowledgeBase || ''
  form.cognitiveStyle = profile.value.cognitiveStyle || ''
  form.weakPoints = profile.value.weakPoints || ''
  form.interestPreference = profile.value.interestPreference || ''
  form.pacePreference = profile.value.pacePreference || ''
  form.examGoal = profile.value.examGoal || ''
}

const load = async () => {
  try {
    const res = await apiProfileCard()
    profile.value = res.data || { summary: [] }
    fillForm()
  } catch (e) { console.warn('Profile API unavailable') }
}

const resetForm = () => fillForm()

// 课表管理方法
const loadSchedule = async () => {
  try {
    const token = localStorage.getItem('sp_token')
    const response = await axios.get('/api/schedule/my', {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (response.data.success) {
      scheduleList.value = response.data.data || []
    }
  } catch (e) {
    console.warn('Failed to load schedule:', e)
  }
}

const getCoursesForDay = (day) => {
  return scheduleList.value.filter(c => c.dayOfWeek === day)
}

const editCourse = (course) => {
  editingCourse.value = course
  Object.assign(scheduleForm, {
    dayOfWeek: course.dayOfWeek,
    startTime: course.startTime,
    endTime: course.endTime,
    courseName: course.courseName,
    location: course.location,
    teacher: course.teacher,
    weeks: course.weeks
  })
  showAddScheduleDialog.value = true
}

const saveCourse = async () => {
  if (!scheduleForm.courseName) {
    ElMessage.warning('请输入课程名称')
    return
  }
  try {
    const token = localStorage.getItem('sp_token')
    if (editingCourse.value) {
      // 更新
      await axios.put('/api/schedule/update', {
        id: editingCourse.value.id,
        ...scheduleForm
      }, {
        headers: { Authorization: `Bearer ${token}` }
      })
      ElMessage.success('课程更新成功')
    } else {
      // 新增
      await axios.post('/api/schedule/add', scheduleForm, {
        headers: { Authorization: `Bearer ${token}` }
      })
      ElMessage.success('课程添加成功')
    }
    showAddScheduleDialog.value = false
    editingCourse.value = null
    resetScheduleForm()
    await loadSchedule()
  } catch (e) {
    ElMessage.error('保存失败：' + (e.response?.data?.message || e.message))
  }
}

const deleteCourse = async () => {
  try {
    await ElMessageBox.confirm('确定要删除这门课程吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const token = localStorage.getItem('sp_token')
    await axios.delete(`/api/schedule/${editingCourse.value.id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    ElMessage.success('删除成功')
    showAddScheduleDialog.value = false
    editingCourse.value = null
    resetScheduleForm()
    await loadSchedule()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败：' + (e.response?.data?.message || e.message))
    }
  }
}

const clearSchedule = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有课程吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const token = localStorage.getItem('sp_token')
    await axios.delete('/api/schedule/clear', {
      headers: { Authorization: `Bearer ${token}` }
    })
    ElMessage.success('课表已清空')
    await loadSchedule()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('清空失败：' + (e.response?.data?.message || e.message))
    }
  }
}

const resetScheduleForm = () => {
  Object.assign(scheduleForm, {
    dayOfWeek: 1,
    startTime: '08:00',
    endTime: '09:40',
    courseName: '',
    location: '',
    teacher: '',
    weeks: '1-16'
  })
}

const save = async () => {
  try {
    await auth.updateMe({ ...form })
    ElMessage.success('个人信息已更新')
    savePulse.value = true
    setTimeout(() => { savePulse.value = false }, 2000)
    await load()
  } catch (e) { ElMessage.error('保存失败：' + e.message) }
}

onMounted(async () => {
  await load()
  if (role.value === 'student') {
    await loadSchedule()
  }
})
</script>

<style scoped>
.modern-profile { display: grid; gap: 18px; }

.success-banner {
  position: sticky; top: 8px; z-index: 50; padding: 14px 20px; border-radius: 16px;
  background: linear-gradient(135deg,#ecfdf5,#d1fae5); color: #059669;
  border: 1px solid #a7f3d0; box-shadow: 0 12px 28px rgba(16,185,129,.15);
  display: flex; align-items: center; gap: 12px; animation: slideIn .35s ease;
}
.banner-icon {
  width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center;
  color: white; background: linear-gradient(135deg,#10b981,#34d399); animation: iconGlow .9s ease infinite alternate;
}
@keyframes iconGlow{from{transform:scale(1)}to{transform:scale(1.08)}}
@keyframes slideIn{from{opacity:0;transform:translateY(-8px)}to{opacity:1;transform:translateY(0)}}

.hero-section {
  padding: 26px 28px; display: grid; grid-template-columns: 1.25fr .75fr; gap: 22px;
  background: radial-gradient(circle at right top, rgba(117,194,255,.12), transparent 25%),
              radial-gradient(circle at left bottom, rgba(79,129,255,.08), transparent 22%),
              linear-gradient(180deg, rgba(255,255,255,.98), rgba(248,250,255,.94));
  border-radius: 20px; border: 1px solid #eaf0fa;
}
.hero-left h2 { margin: 8px 0 8px; font-size: 28px; color: #1e293b; font-weight: 700; }
.hero-desc { margin: 0; color: #64748b; line-height: 1.75; max-width: 720px; font-size: 14.5px; }
.hero-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 14px; }
.hero-right { display: grid; gap: 12px; align-content: start; }
.metric-card {
  padding: 18px; border-radius: 18px; border: 1px solid #e2e8f0;
  display: grid; gap: 6px; transition: transform .2s;
}
.metric-card:hover { transform: translateY(-3px); }
.blue-metric { background: linear-gradient(135deg, rgba(88,123,255,.1), rgba(147,208,255,.15)); }
.green-metric { background: linear-gradient(135deg, rgba(34,197,94,.1), rgba(110,231,183,.13)); }
.metric-card span { font-size: 12.5px; color: #64748b; }
.metric-card strong { font-size: 34px; color: #1e293b; font-weight: 700; line-height: 1.1; }
.metric-card small { font-size: 11.5px; color: #94a3b8; }

.profile-layout { display: grid; grid-template-columns: 340px 1fr; gap: 18px; align-items: start; }
.sidebar { display: grid; gap: 16px; position: sticky; top: 92px; }
.avatar-panel {
  text-align: center; padding: 22px 18px; border-radius: 20px;
  background: linear-gradient(180deg, #f8fbff, #fffef8); border: 1px solid #edf3fb;
}
.avatar-ring-wrap {
  position: relative; width: 140px; height: 140px; margin: 0 auto 16px;
  display: grid; place-items: center;
  background: radial-gradient(circle at center, rgba(96,141,255,.2), rgba(99,214,255,.06));
  border-radius: 50%;
}
.ring-decor {
  position: absolute; border: 1.5px dashed rgba(95,141,255,.25); border-radius: 50%; pointer-events: none;
}
.ring-outer { inset: -6px; animation: spinRing 10s linear infinite; }
.ring-inner { inset: -14px; animation: spinRing 7s linear infinite reverse; }
@keyframes spinRing { to { transform: rotate(360deg); } }
.avatar-img, .avatar-placeholder {
  position: relative; z-index: 1; width: 100%; height: 100%; border-radius: 50%;
  object-fit: cover; background: linear-gradient(135deg, #5e8dff, #6ed8ff);
  border: 4px solid rgba(255,255,255,.95); box-shadow: 0 12px 28px rgba(78,117,186,.15);
}
.avatar-placeholder {
  display: grid; place-items: center; color: white; font-size: 42px; font-weight: 800;
}
.display-name { margin: 4px 0 2px; font-size: 22px; color: #1e293b; font-weight: 600; }
.username-text { margin: 0 0 12px; font-size: 13px; color: #94a3b8; }
.role-badges { justify-content: center; display: flex; gap: 8px; flex-wrap: wrap; }
.badge {
  padding: 4px 12px; border-radius: 999px; font-size: 11.5px; font-weight: 500;
}
.primary-badge { background: #dbeafe; color: #2563eb; }
.course-badge { background: #fef3c7; color: #d97706; }

.upload-panel {
  display: grid; gap: 10px; padding: 18px; border-radius: 18px;
  background: rgba(248,251,255,.92); border: 1px solid #eaf0f8;
}
.upload-header { display: flex; justify-content: space-between; align-items: center; }
.upload-header label { font-weight: 700; color: #475569; font-size: 13px; }
.upload-header span { font-size: 11.5px; color: #a1aab8; }
.upload-btn-label {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 90px; height: 36px; padding: 0 14px;
  border-radius: 10px; background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white; cursor: pointer; font-size: 13px; font-weight: 600;
  transition: all .2s;
}
.upload-btn-label:hover { background: linear-gradient(135deg, #2563eb, #1d4ed8); transform: translateY(-1px); }
.clear-avatar-btn {
  border: 1px solid #e2e8f0; border-radius: 8px; padding: 4px 12px;
  background: white; color: #64748b; cursor: pointer; font-size: 12px; transition: all .2s;
}
.clear-avatar-btn:hover { background: #fef2f2; color: #ef4444; border-color: #fecaca; }
.url-input {
  width: 100%; height: 36px; padding: 0 12px; border: 1px solid #e2e8f0; border-radius: 10px;
  font-size: 13px; outline: none; transition: border-color .2s; color: #334155;
}
.url-input:focus { border-color: #93c5fd; box-shadow: 0 0 0 3px rgba(59,130,246,.08); }
.preset-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.preset-btn {
  padding: 0; border: 1.5px solid #e5ecf5; border-radius: 12px; overflow: hidden;
  cursor: pointer; transition: all .2s; background: white;
}
.preset-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 18px rgba(39,76,129,.1); border-color: #93c5fd; }
.preset-btn img { width: 100%; height: 56px; object-fit: cover; }
.stats-row { display: grid; gap: 8px; }
.stat-mini {
  padding: 14px; border-radius: 14px; background: rgba(247,250,252,.92);
  border: 1px solid #edf3fb; display: flex; justify-content: space-between; align-items: center;
  transition: transform .2s;
}
.stat-mini:hover { transform: translateY(-2px); }
.stat-mini b { font-size: 20px; color: #1e293b; font-weight: 700; }
.stat-mini span { font-size: 11.5px; color: #94a3b8; }

.main-content { display: grid; gap: 16px; }
.form-panel, .summary-panel {
  padding: 24px; background: white; border-radius: 18px;
  border: 1px solid #edf2f8; box-shadow: 0 4px 16px rgba(33,65,108,.04);
}
.panel-head {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; gap: 12px;
}
.panel-head h3 { margin: 0; font-size: 19px; color: #1e293b; font-weight: 700; }
.group-tag {
  padding: 4px 12px; border-radius: 8px; font-size: 11px; font-weight: 700;
  letter-spacing: .5px; text-transform: uppercase;
}
.group-tag { background: #eff6ff; color: #2563eb; }
.group-2 { background: #fdf4ff; color: #7c3aed; }
.group-3 { background: #f0fdf4; color: #059669; }
.info-tag { background: #eff6ff; color: #2563eb; }
.accent-tag { background: #fef3c7; color: #d97706; }
.preview-tag { background: #f0fdf4; color: #059669; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.span-full { grid-column: span 2; }

.summary-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 18px; }
.sum-card {
  padding: 16px; border-radius: 14px; background: rgba(247,250,252,.92);
  border: 1px solid #edf3fb; display: grid; gap: 6px; transition: transform .2s;
}
.sum-card:hover { transform: translateY(-2px); }
.sum-card span { font-size: 12.5px; color: #8ea1b7; }
.sum-card strong { color: #1e293b; font-weight: 600; }

.tag-cloud { display: flex; flex-wrap: wrap; gap: 8px; }
.cloud-tag {
  padding: 9px 16px; border-radius: 999px;
  background: linear-gradient(135deg, #f4f8ff, #eef7ff);
  border: 1px solid #e0eeff; color: #5578ad; font-size: 12.5px; font-weight: 500;
  transition: all .2s;
}
.cloud-tag:hover { background: linear-gradient(135deg, #dbeafe, #eff6ff); border-color: #93c5fd; color: #2563eb; }
.empty-hint { color: #c4cbd9; font-style: italic; }

.action-bar {
  position: sticky; bottom: 0; z-index: 5; padding: 16px 20px;
  border-radius: 16px; background: rgba(255,255,255,.9);
  backdrop-filter: blur(12px); border: 1px solid #e6edf6;
  display: flex; justify-content: space-between; align-items: center; gap: 16px;
}
.action-info strong { display: block; color: #334155; font-size: 14px; }
.action-info p { margin: 4px 0 0; color: #94a3b8; font-size: 12px; }
.action-buttons { display: flex; gap: 10px; }

.save-pop-enter-active, .save-pop-leave-active { transition: all .35s ease; }
.save-pop-enter-from, .save-pop-leave-to { opacity: 0; transform: translateY(-8px); }

/* 课表样式 */
.schedule-actions { display: flex; gap: 10px; margin-bottom: 16px; }
.schedule-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 10px; }
.day-column { border: 1px solid #e6edf6; border-radius: 12px; overflow: hidden; background: white; }
.day-header { padding: 10px; background: linear-gradient(135deg, #eff6ff, #dbeafe); color: #2563eb; font-weight: 600; text-align: center; font-size: 13px; }
.course-list { padding: 8px; display: grid; gap: 8px; min-height: 100px; }
.course-item { padding: 8px 10px; border-radius: 8px; background: linear-gradient(135deg, #fef3c7, #fef9c3); border: 1px solid #fde047; cursor: pointer; transition: all .2s; }
.course-item:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(250, 204, 21, 0.3); }
.course-time { font-size: 11px; color: #92400e; font-weight: 600; margin-bottom: 4px; }
.course-name { font-size: 13px; color: #78350f; font-weight: 600; margin-bottom: 3px; }
.course-location { font-size: 11px; color: #a16207; }
.course-teacher { font-size: 11px; color: #a16207; margin-top: 2px; }
.empty-day { padding: 20px; text-align: center; color: #cbd5e1; font-size: 12px; }
.group-4 { background: linear-gradient(135deg, #fef3c7, #fde047); color: #78350f; }
.schedule-tag { background: #fef9c3; color: #a16207; }

@media(max-width:1200px){.profile-layout,.form-grid,.summary-cards{grid-template-columns:1fr}.sidebar{position:static}.span-full{grid-column-span:1}.action-bar{position:static;flex-direction:column;align-items:stretch;}.action-buttons{justify-content:flex-end}.schedule-grid{grid-template-columns:1fr}}
@media(max-width:760px){.preset-grid{grid-template-columns:repeat(2,1fr)}.action-buttons{flex-direction:column}}
</style>
