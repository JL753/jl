<template>
  <div class="profile-page">
    <!-- User Info Card -->
    <div class="glass-card user-card">
      <div class="uc-left">
        <img v-if="userAvatar" :src="userAvatar" class="user-avatar" />
        <div v-else class="avatar-placeholder">{{ userInitial }}</div>
        <div class="uc-info">
          <h2 class="uc-name">{{ userName }}</h2>
          <div class="uc-meta">{{ userMajor }} · {{ userGrade }} · Lv.{{ userLevel }}</div>
        </div>
      </div>
      <div class="uc-right">
        <div class="uc-xp">{{ userXp }} XP</div>
        <div class="uc-streak">连续 {{ userStreak }} 天</div>
      </div>
      <button class="uc-edit-btn" @click="showEditModal = true">编辑资料</button>
    </div>

    <!-- Tabs -->
    <div class="profile-tabs">
      <button v-for="tab in tabs" :key="tab.key" class="profile-tab"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key">{{ tab.label }}</button>
    </div>

    <!-- Tab: 学习概览 -->
    <div v-show="activeTab === 'overview'" class="tab-content">
      <div class="overview-grid">
        <div class="glass-card ov-card">
          <div class="ov-label">学习统计</div>
          <div class="ov-stats">
            <span><strong>{{ stats.mastered }}</strong> 掌握知识点</span>
            <span><strong>{{ stats.lessons }}</strong> 完成课时</span>
            <span><strong>{{ stats.hours }}h</strong> 学习时长</span>
          </div>
        </div>
        <div class="glass-card ov-card" v-if="lastStudy">
          <div class="ov-label">最近学习</div>
          <div class="ov-recent">{{ lastStudy.courseName }} - {{ lastStudy.lessonName }}</div>
          <div class="ov-time">{{ lastStudy.timeAgo }} · <span class="ov-link" @click="goToLastStudy">继续学习</span></div>
        </div>
      </div>
      <div class="glass-card ov-card" v-if="profileTags.length > 0">
        <div class="ov-label">学习画像</div>
        <div class="ov-tags">
          <span v-for="tag in profileTags" :key="tag" class="ov-tag">{{ tag }}</span>
        </div>
      </div>
    </div>

    <!-- Tab: 成就 & 游戏化 -->
    <div v-show="activeTab === 'achievement'" class="tab-content">
      <AchievementGamificationPanel />
    </div>

    <!-- Tab: 学习分析 -->
    <div v-show="activeTab === 'analytics'" class="tab-content">
      <LearningAnalyticsPanel />
    </div>

    <!-- Tab: 自适应测验 -->
    <div v-show="activeTab === 'quiz'" class="tab-content">
      <AdaptiveQuizPanel />
    </div>

    <!-- Tab: 我的考试 -->
    <div v-show="activeTab === 'exam'" class="tab-content">
      <StudentExamPanel />
    </div>

    <!-- Community entry -->
    <div class="community-entry">
      <button class="ce-btn" @click="$router.push('/student/community')">问答社区</button>
    </div>

    <!-- Edit Profile Modal -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="showEditModal = false">
      <div class="glass-card edit-modal">
        <h3>编辑资料</h3>
        <div class="edit-field">
          <label>昵称</label>
          <input v-model="editForm.name" class="edit-input" />
        </div>
        <div class="edit-field">
          <label>专业</label>
          <input v-model="editForm.major" class="edit-input" />
        </div>
        <div class="edit-field">
          <label>年级</label>
          <select v-model="editForm.grade" class="edit-input">
            <option v-for="g in gradeOptions" :key="g" :value="g">{{ g }}</option>
          </select>
        </div>
        <div class="edit-field">
          <label>学习目标</label>
          <textarea v-model="editForm.goal" class="edit-input" rows="2"></textarea>
        </div>
        <div class="edit-actions">
          <button class="edit-btn cancel" @click="showEditModal = false">取消</button>
          <button class="edit-btn save" @click="saveProfile">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { apiMe, apiUpdateMe, apiGetProfile, apiGamificationStreak } from '../../api'
import AchievementGamificationPanel from '../student/AchievementCenter.vue'
import LearningAnalyticsPanel from '../student/LearningAnalyticsView.vue'
import AdaptiveQuizPanel from '../student/AdaptiveQuizView.vue'
import StudentExamPanel from '../student/StudentExam.vue'

const router = useRouter()
const activeTab = ref('overview')
const showEditModal = ref(false)

const userName = ref('用户')
const userInitial = ref('U')
const userAvatar = ref('')
const userMajor = ref('')
const userGrade = ref('')
const userLevel = ref(1)
const userXp = ref(0)
const userStreak = ref(0)

const stats = ref({ mastered: 0, lessons: 0, hours: 0 })
const lastStudy = ref(null)
const profileTags = ref([])

const editForm = ref({ name: '', major: '', grade: '', goal: '' })
const gradeOptions = ['大一', '大二', '大三', '大四', '研一', '研二', '研三', '其他']

const tabs = [
  { key: 'overview', label: '学习概览' },
  { key: 'achievement', label: '成就 & 游戏化' },
  { key: 'analytics', label: '学习分析' },
  { key: 'quiz', label: '自适应测验' },
  { key: 'exam', label: '我的考试' },
]

onMounted(async () => {
  try {
    const [meRes, profileRes, streakRes] = await Promise.all([
      apiMe().catch(() => ({ data: {} })),
      apiGetProfile().catch(() => ({ data: {} })),
      apiGamificationStreak().catch(() => ({ data: {} }))
    ])
    const me = meRes.data || {}
    userName.value = me.name || me.username || '用户'
    userInitial.value = (userName.value[0] || 'U').toUpperCase()
    userAvatar.value = me.avatar || ''
    userMajor.value = me.major || ''
    userGrade.value = me.grade || ''
    userLevel.value = me.level || 1
    userXp.value = me.xp || 0
    userStreak.value = streakRes.data?.currentStreak || streakRes.data?.current_streak || streakRes.data?.streak || 0

    const p = profileRes.data || {}
    stats.value = { mastered: p.masteredCount || 0, lessons: p.completedLessons || 0, hours: p.totalHours || 0 }
    lastStudy.value = p.lastStudy
    if (p.tags) {
      profileTags.value = p.tags.map(t => typeof t === 'string' ? t : `${t.label || t.key}: ${t.value || t}`)
    }

    editForm.value = { name: userName.value, major: userMajor.value, grade: userGrade.value, goal: p.goal || '' }
  } catch {}
})

async function saveProfile() {
  try {
    await apiUpdateMe(editForm.value)
    showEditModal.value = false
    userName.value = editForm.value.name
    userInitial.value = (editForm.value.name[0] || 'U').toUpperCase()
    userMajor.value = editForm.value.major
    userGrade.value = editForm.value.grade
  } catch {}
}

function goToLastStudy() {
  const ls = lastStudy.value
  if (ls?.courseId && ls?.lessonId) {
    router.push(`/student/courses/${ls.courseId}?sc=${ls.lessonId}`)
  } else if (ls?.courseId) {
    router.push(`/student/courses/${ls.courseId}`)
  } else {
    router.push('/student/subjects')
  }
}
</script>

<style scoped>
.profile-page { padding: 24px; max-width: 900px; margin: 0 auto; }

.user-card { display: flex; align-items: center; gap: 14px; padding: 20px; margin-bottom: 16px; position: relative; }
.uc-left { display: flex; align-items: center; gap: 12px; flex: 1; }
.user-avatar, .avatar-placeholder {
  width: 56px; height: 56px; border-radius: 50%;
  background: linear-gradient(135deg,#3b82f6,#a855f7);
  display: flex; align-items: center; justify-content: center;
  font-weight: 800; font-size: 20px; object-fit: cover;
}
.avatar-placeholder { color: #fff; flex-shrink: 0; }
.uc-name { font-weight: 700; }
.uc-meta { font-size: 11px; color: rgba(255,255,255,0.4); }
.uc-right { text-align: right; margin-right: 100px; }
.uc-xp { font-weight: 700; color: #60d9fa; }
.uc-streak { font-size: 10px; color: rgba(255,255,255,0.3); }
.uc-edit-btn {
  position: absolute; right: 20px; padding: 6px 14px; border-radius: 6px;
  border: 1px solid rgba(255,255,255,0.1); background: rgba(255,255,255,0.04);
  color: rgba(255,255,255,0.5); font-size: 11px; cursor: pointer; font-family: inherit;
}

.profile-tabs { display: flex; border-bottom: 1px solid rgba(255,255,255,0.06); margin-bottom: 16px; overflow-x: auto; }
.profile-tab {
  padding: 10px 14px; font-size: 11px; color: rgba(255,255,255,0.4);
  background: none; border: none; border-bottom: 2px solid transparent; cursor: pointer;
  font-family: inherit; white-space: nowrap;
}
.profile-tab.active { color: #60d9fa; border-bottom-color: #60d9fa; font-weight: 600; }

.tab-content { min-height: 200px; }

.overview-grid { display: flex; gap: 14px; margin-bottom: 14px; }
.ov-card { flex: 1; padding: 14px; }
.ov-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255,255,255,0.3); margin-bottom: 10px; }
.ov-stats { display: flex; justify-content: space-between; font-size: 12px; color: rgba(255,255,255,0.6); }
.ov-stats strong { color: #fff; }
.ov-recent { font-size: 12px; }
.ov-time { font-size: 10px; color: rgba(255,255,255,0.4); margin-top: 6px; }
.ov-link { color: #3b82f6; cursor: pointer; }

.ov-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.ov-tag {
  padding: 4px 10px; border-radius: 6px; font-size: 11px;
  background: rgba(59,130,246,0.1); color: rgba(255,255,255,0.6);
}

.community-entry { margin-top: 24px; padding-top: 16px; border-top: 1px solid rgba(255,255,255,0.06); text-align: center; }
.ce-btn {
  padding: 8px 24px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04); color: rgba(255,255,255,0.5); font-size: 12px; cursor: pointer; font-family: inherit;
}

/* Edit modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 200; display: flex; align-items: center; justify-content: center; }
.edit-modal { width: 400px; padding: 24px; }
.edit-modal h3 { margin-bottom: 16px; font-size: 16px; }
.edit-field { margin-bottom: 12px; }
.edit-field label { display: block; font-size: 11px; color: rgba(255,255,255,0.5); margin-bottom: 4px; }
.edit-input {
  width: 100%; padding: 8px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.04); color: #fff; font-size: 12px; font-family: inherit;
  box-sizing: border-box; outline: none;
}
.edit-input:focus { border-color: rgba(59,130,246,0.4); }
.edit-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px; }
.edit-btn { padding: 6px 16px; border-radius: 6px; border: none; font-size: 12px; cursor: pointer; font-family: inherit; }
.edit-btn.save { background: linear-gradient(135deg,#3b82f6,#2563eb); color: #fff; }
.edit-btn.cancel { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.5); }

.glass-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  backdrop-filter: blur(12px);
}
</style>
