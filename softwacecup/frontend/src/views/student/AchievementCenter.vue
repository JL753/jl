<template>
  <div class="achievement-center">
    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <p class="loading-text">加载中...</p>
    </div>

    <template v-else>
      <!-- XP 等级进度条 -->
      <div class="glass-card xp-card">
        <div class="xp-header">
          <div class="level-circle">Lv.{{ xp.level }}</div>
          <div class="xp-info">
            <div class="xp-title">
              <span class="xp-level-label">等级 {{ xp.level }}</span>
              <span class="xp-amount">{{ xp.currentXp }} / {{ xp.xpToNext }} XP</span>
            </div>
            <div class="xp-bar">
              <div class="xp-fill" :style="{ width: xpPercent + '%' }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 徽章网格 -->
      <div class="badges-section">
        <h3 class="section-title">成就徽章</h3>
        <div v-if="badges.length > 0" class="badges-grid">
          <div
            v-for="badge in badges"
            :key="badge.id || badge.name"
            class="badge-card"
            :class="{ 'badge-locked': !badge.achieved && !badge.unlocked }"
          >
            <div class="badge-icon-wrap">
              <span class="badge-icon">{{ badge.icon || '🏅' }}</span>
            </div>
            <span class="badge-name">{{ badge.name }}</span>
          </div>
        </div>
        <div v-else class="empty-badges">
          <p>暂无成就徽章，继续学习获取吧！</p>
        </div>
      </div>

      <!-- 连续学习 -->
      <div class="glass-card streak-card">
        <h3 class="section-title">连续学习</h3>
        <div class="streak-stats">
          <div class="streak-item">
            <span class="streak-value" :class="{ active: streak.checkinToday }">{{ streak.checkinToday ? '已学' : '未学' }}</span>
            <span class="streak-label">今日</span>
          </div>
          <div class="streak-item">
            <span class="streak-value">{{ streak.currentStreak }}</span>
            <span class="streak-label">连续天数</span>
          </div>
          <div class="streak-item">
            <span class="streak-value">{{ streak.longestStreak }}</span>
            <span class="streak-label">最长连续</span>
          </div>
        </div>
        <button
          class="checkin-btn"
          :class="{ done: streak.checkinToday }"
          :disabled="streak.checkinToday || checkingIn"
          @click="handleCheckin"
        >
          {{ checkingIn ? '签到中...' : streak.checkinToday ? '已签到' : '今日签到' }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  apiGamificationProgress,
  apiGamificationBadges,
  apiGamificationStreak,
  apiGamificationCheckin,
} from '../../api/index.js'

const xp = ref({ level: 1, currentXp: 0, xpToNext: 100 })
const badges = ref([])
const streak = ref({ checkinToday: false, currentStreak: 0, longestStreak: 0 })
const checkingIn = ref(false)
const loading = ref(true)

const xpPercent = computed(() => {
  if (!xp.value.xpToNext || xp.value.xpToNext <= 0) return 0
  return Math.min(100, Math.round((xp.value.currentXp / xp.value.xpToNext) * 100))
})

onMounted(async () => {
  try {
    loading.value = true
    const [xpRes, badgesRes, streakRes] = await Promise.all([
      apiGamificationProgress(),
      apiGamificationBadges(),
      apiGamificationStreak(),
    ])
    const xpData = xpRes.data?.data || xpRes.data || {}
    if (xpData.level || xpData.currentXp !== undefined) {
      xp.value = {
        level: xpData.level || 1,
        currentXp: xpData.currentXp || 0,
        xpToNext: xpData.xpToNext || xpData.nextLevelXp || 100,
      }
    }
    const badgeData = badgesRes.data?.data || badgesRes.data || []
    badges.value = Array.isArray(badgeData) ? badgeData : []
    const streakData = streakRes.data?.data || streakRes.data || {}
    streak.value = {
      checkinToday: !!streakData.checkinToday,
      currentStreak: streakData.currentStreak || 0,
      longestStreak: streakData.longestStreak || 0,
    }
  } catch (e) {
    console.warn('Failed to load gamification data', e)
  } finally {
    loading.value = false
  }
})

async function handleCheckin() {
  if (checkingIn.value || streak.value.checkinToday) return
  checkingIn.value = true
  try {
    await apiGamificationCheckin()
    streak.value.checkinToday = true
    streak.value.currentStreak++

    // Refresh XP after check-in
    try {
      const xpRes = await apiGamificationProgress()
      const xpData = xpRes.data?.data || xpRes.data || {}
      if (xpData.level || xpData.currentXp !== undefined) {
        xp.value = {
          level: xpData.level || 1,
          currentXp: xpData.currentXp || 0,
          xpToNext: xpData.xpToNext || xpData.nextLevelXp || 100,
        }
      }
    } catch (e) { /* ignore */ }
  } catch (e) {
    console.warn('Checkin failed', e)
  } finally {
    checkingIn.value = false
  }
}
</script>

<style scoped>
.achievement-center {
  display: grid;
  gap: 16px;
}

.glass-card {
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 20px;
}

/* XP Card */
.xp-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.level-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
}

.xp-info {
  flex: 1;
  min-width: 0;
}

.xp-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 4px;
}

.xp-level-label {
  font-size: 14px;
  font-weight: 600;
  color: #e6edf3;
}

.xp-amount {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.xp-bar {
  height: 8px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 4px;
  overflow: hidden;
}

.xp-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  border-radius: 4px;
  transition: width 0.5s ease;
}

/* Badges */
.badges-section {
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 20px;
}

.section-title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
}

.badges-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.badge-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 10px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  transition: all 0.2s;
}

.badge-card:hover {
  background: rgba(255, 255, 255, 0.07);
  transform: translateY(-2px);
}

.badge-card.badge-locked {
  opacity: 0.3;
  filter: grayscale(1);
}

.badge-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(59, 130, 246, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge-icon {
  font-size: 22px;
  line-height: 1;
}

.badge-name {
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
  text-align: center;
  line-height: 1.3;
}

.empty-badges {
  text-align: center;
  padding: 24px 0 8px;
}

.empty-badges p {
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
  margin: 0;
}

/* Streak */
.streak-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.streak-item {
  text-align: center;
}

.streak-value {
  display: block;
  font-size: 26px;
  font-weight: 700;
  color: #e6edf3;
  line-height: 1.2;
  margin-bottom: 4px;
}

.streak-value.active {
  color: #10b981;
}

.streak-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}

.checkin-btn {
  width: 100%;
  padding: 10px 16px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
}

.checkin-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.3);
}

.checkin-btn.done {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.35);
  cursor: not-allowed;
}

.checkin-btn:disabled {
  cursor: not-allowed;
}

/* Loading State */
.loading-state {
  text-align: center;
  padding: 48px 0;
}

.loading-text {
  color: rgba(255, 255, 255, 0.5);
  font-size: 16px;
  margin: 0;
}

/* Responsive */
@media (max-width: 600px) {
  .badges-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .streak-value {
    font-size: 22px;
  }

  .level-circle {
    width: 52px;
    height: 52px;
    font-size: 15px;
  }
}
</style>
