<template>
  <div class="community-page">
    <header class="page-header">
      <div class="header-info">
        <h2>💡 学习社区与问答广场</h2>
        <p>悬赏问答、优质内容推荐、教师认证回答</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="showAskDialog = true">❓ 我要提问</el-button>
      </div>
    </header>

    <div class="community-content">
      <!-- 标签筛选 -->
      <div class="filter-bar">
        <el-radio-group v-model="filterType" size="small">
          <el-radio-button value="hot">🔥 热门</el-radio-button>
          <el-radio-button value="new">🆕 最新</el-radio-button>
          <el-radio-button value="unanswered">❓ 待解答</el-radio-button>
          <el-radio-button value="bounty">💰 悬赏</el-radio-button>
          <el-radio-button value="teacher">👨‍🏫 教师回答</el-radio-button>
        </el-radio-group>
        <div class="tag-filters">
          <el-tag v-for="tag in hotTags" :key="tag" size="small" :type="selectedTags.includes(tag) ? '' : 'info'"
            @click="toggleTag(tag)" style="cursor:pointer;margin:2px">
            {{ tag }}
          </el-tag>
        </div>
      </div>

      <div class="community-main">
        <!-- 左侧：问答列表 -->
        <div class="qa-list">
          <div v-for="qa in filteredQA" :key="qa.id" class="qa-card" @click="openDetail(qa)">
            <div class="qa-votes">
              <button class="vote-btn up" @click.stop="vote(qa.id, 'up')">▲</button>
              <span class="vote-count">{{ qa.votes }}</span>
              <button class="vote-btn down" @click.stop="vote(qa.id, 'down')">▼</button>
            </div>
            <div class="qa-body">
              <div class="qa-header">
                <h3 class="qa-title">{{ qa.title }}</h3>
                <el-tag v-if="qa.bounty" type="warning" size="small">💰 {{ qa.bounty }} 积分</el-tag>
                <el-tag v-if="qa.hasTeacherAnswer" type="success" size="small">👨‍🏫 已有教师回答</el-tag>
                <el-tag v-if="qa.solved" type="success" size="small">✓ 已解决</el-tag>
              </div>
              <p class="qa-preview">{{ qa.preview }}</p>
              <div class="qa-tags">
                <el-tag v-for="tag in qa.tags" :key="tag" size="small" type="info">{{ tag }}</el-tag>
              </div>
              <div class="qa-meta">
                <span class="qa-author">
                  <span class="author-avatar" :style="{ background: qa.authorColor }">{{ qa.author.charAt(0) }}</span>
                  {{ qa.author }}
                  <el-tag v-if="qa.authorRole === 'teacher'" size="small" type="warning">教师</el-tag>
                </span>
                <span class="qa-time">{{ qa.time }}</span>
                <span class="qa-answers">💬 {{ qa.answerCount }} 回答</span>
                <span class="qa-views">👁 {{ qa.views }} 浏览</span>
                <button class="map-btn" @click.stop="addToMap(qa)" :class="{ added: qa.inMap }">
                  {{ qa.inMap ? '✓ 已入版图' : '🗺️ 收入版图' }}
                </button>
              </div>
            </div>
          </div>

          <div class="load-more">
            <el-button @click="loadMore">加载更多</el-button>
          </div>
        </div>

        <!-- 右侧：侧边栏 -->
        <div class="community-sidebar">
          <!-- 热门笔记 -->
          <div class="sidebar-card">
            <h4>📖 热门笔记</h4>
            <div v-for="note in hotNotes" :key="note.id" class="hot-note-item">
              <span class="note-rank">{{ note.rank }}</span>
              <div>
                <p class="note-title">{{ note.title }}</p>
                <span class="note-likes">❤️ {{ note.likes }}</span>
              </div>
            </div>
          </div>

          <!-- 活跃用户 -->
          <div class="sidebar-card">
            <h4>🌟 活跃用户</h4>
            <div v-for="user in activeUsers" :key="user.name" class="user-item">
              <span class="user-avatar" :style="{ background: user.color }">{{ user.name.charAt(0) }}</span>
              <div>
                <p class="user-name">{{ user.name }}</p>
                <p class="user-stats">回答 {{ user.answers }} · 积分 {{ user.points }}</p>
              </div>
              <el-tag v-if="user.role === 'teacher'" size="small" type="warning">教师</el-tag>
            </div>
          </div>

          <!-- 我的悬赏 -->
          <div class="sidebar-card">
            <h4>💰 我的悬赏</h4>
            <div class="my-bounty">
              <p>可用积分：<b>{{ myPoints }}</b></p>
              <p>已悬赏：<b>{{ spentPoints }}</b></p>
              <p>已获积分：<b>{{ earnedPoints }}</b></p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 提问弹窗 -->
    <el-dialog v-model="showAskDialog" title="发布问题" width="600px">
      <el-form :model="askForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="askForm.title" placeholder="简洁描述你的问题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="askForm.content" type="textarea" :rows="6" placeholder="详细描述问题，包括你尝试过的方法..." />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="askForm.tags" multiple filterable allow-create placeholder="添加标签" style="width:100%">
            <el-option v-for="tag in hotTags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="悬赏积分">
          <el-input-number v-model="askForm.bounty" :min="0" :max="myPoints" :step="10" />
          <span style="margin-left:8px;color:#999">悬赏越高，回答越快</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAskDialog = false">取消</el-button>
        <el-button type="primary" @click="submitQuestion">发布</el-button>
      </template>
    </el-dialog>

    <!-- 问题详情弹窗 -->
    <el-dialog v-model="showDetailDialog" :title="detailQA?.title" width="700px" top="5vh">
      <div v-if="detailQA" class="detail-content">
        <div class="detail-question">
          <div class="detail-meta">
            <span class="author-avatar" :style="{ background: detailQA.authorColor }">{{ detailQA.author.charAt(0) }}</span>
            <span>{{ detailQA.author }}</span>
            <span>{{ detailQA.time }}</span>
          </div>
          <p class="detail-text">{{ detailQA.preview }}</p>
          <div class="detail-tags">
            <el-tag v-for="tag in detailQA.tags" :key="tag" size="small" type="info">{{ tag }}</el-tag>
          </div>
        </div>

        <div class="detail-answers">
          <h4>💬 回答 ({{ detailQA.answerCount }})</h4>
          <div v-for="ans in detailQA.answers" :key="ans.id" :class="['answer-item', { teacher: ans.role === 'teacher', accepted: ans.accepted }]">
            <div class="answer-header">
              <span class="author-avatar small" :style="{ background: ans.color }">{{ ans.author.charAt(0) }}</span>
              <span class="ans-author">{{ ans.author }}</span>
              <el-tag v-if="ans.role === 'teacher'" size="small" type="warning">教师认证</el-tag>
              <el-tag v-if="ans.accepted" size="small" type="success">✓ 已采纳</el-tag>
            </div>
            <p class="ans-text">{{ ans.text }}</p>
            <div class="ans-actions">
              <span>👍 {{ ans.likes }}</span>
              <span>👎 {{ ans.dislikes }}</span>
              <el-button v-if="!ans.accepted && detailQA.author === '我'" size="small" type="primary" @click="acceptAnswer(ans)">采纳</el-button>
            </div>
          </div>
        </div>

        <div class="detail-reply">
          <el-input v-model="replyText" type="textarea" :rows="3" placeholder="写下你的回答..." />
          <el-button type="primary" @click="submitReply" style="margin-top:8px">提交回答</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useKnowledgeMapStore } from '../../stores/knowledgeMap'

const filterType = ref('hot')
const selectedTags = ref([])
const kmStore = useKnowledgeMapStore()
const showAskDialog = ref(false)
const showDetailDialog = ref(false)
const detailQA = ref(null)
const replyText = ref('')
const myPoints = ref(860)
const spentPoints = ref(100)
const earnedPoints = ref(350)

const hotTags = ['数据结构', '算法', '高数', '物理', 'Python', '操作系统', '网络', '英语']

const askForm = ref({ title: '', content: '', tags: [], bounty: 0 })

const qaList = ref([
  { id: 1, inMap: false, title: 'AVL树和红黑树的区别是什么？什么场景该用哪个？', preview: '在学习平衡二叉树时，AVL树和红黑树都要求平衡，但实现方式不同。请问在实际工程中如何选择？', votes: 45, answerCount: 6, views: 320, tags: ['数据结构', '算法'], author: '算法小王', authorColor: '#3b82f6', authorRole: 'student', time: '2小时前', bounty: 50, hasTeacherAnswer: true, solved: true, answers: [{ id: 1, author: '张教授', color: '#f59e0b', role: 'teacher', text: 'AVL树是严格平衡的，左右子树高度差不超过1，查找效率高但插入删除需要更多旋转。红黑树是弱平衡的，只要求最长路径不超过最短路径的2倍，插入删除效率更高。一般需要频繁查找用AVL，频繁修改用红黑树。Java的TreeMap、C++的map都用红黑树。', likes: 28, dislikes: 0, accepted: true }] },
  { id: 2, inMap: false, title: '动态规划的状态转移方程怎么推导？有什么技巧？', preview: '每次遇到DP题都不知道怎么定义状态，更不知道怎么推导转移方程，有没有系统的方法？', votes: 38, answerCount: 4, views: 256, tags: ['算法', '动态规划'], author: 'DP苦手', authorColor: '#ef4444', authorRole: 'student', time: '5小时前', bounty: 30, hasTeacherAnswer: false, solved: false, answers: [{ id: 1, author: '学霸小李', color: '#10b981', role: 'student', text: '推荐按照"定义状态→推导方程→确定边界→优化空间"四步走。定义状态是最关键的，通常问自己：到第i步时需要什么信息？', likes: 12, dislikes: 1, accepted: false }] },
  { id: 3, inMap: false, title: 'Python中深拷贝和浅拷贝的区别？', preview: 'a = [1,2,[3,4]]，b = a.copy()后修改b[2][0]为什么a也变了？', votes: 22, answerCount: 3, views: 180, tags: ['Python'], author: 'Python新手', authorColor: '#8b5cf6', authorRole: 'student', time: '1天前', bounty: 0, hasTeacherAnswer: true, solved: true, answers: [] },
  { id: 4, inMap: false, title: '如何理解操作系统中的死锁？', preview: '死锁的四个必要条件是什么？如何预防和避免死锁？', votes: 18, answerCount: 2, views: 145, tags: ['操作系统'], author: 'OS学渣', authorColor: '#ec4899', authorRole: 'student', time: '2天前', bounty: 20, hasTeacherAnswer: false, solved: false, answers: [] }
])

const hotNotes = [
  { id: 1, rank: 1, title: '数据结构期末复习笔记（全）', likes: 128 },
  { id: 2, rank: 2, title: '高数公式速查表', likes: 96 },
  { id: 3, rank: 3, title: 'Python 面试100题解析', likes: 85 }
]

const activeUsers = [
  { name: '张教授', color: '#f59e0b', role: 'teacher', answers: 156, points: 12500 },
  { name: '学霸小李', color: '#10b981', role: 'student', answers: 89, points: 5600 },
  { name: '算法小王', color: '#3b82f6', role: 'student', answers: 67, points: 4200 }
]

const filteredQA = computed(() => {
  let result = qaList.value
  if (selectedTags.value.length) result = result.filter(qa => qa.tags.some(t => selectedTags.value.includes(t)))
  if (filterType.value === 'unanswered') result = result.filter(qa => qa.answerCount === 0)
  if (filterType.value === 'bounty') result = result.filter(qa => qa.bounty > 0)
  if (filterType.value === 'teacher') result = result.filter(qa => qa.hasTeacherAnswer)
  if (filterType.value === 'hot') result = [...result].sort((a, b) => b.votes - a.votes)
  if (filterType.value === 'new') result = [...result].sort((a, b) => b.id - a.id)
  return result
})

const toggleTag = (tag) => {
  const idx = selectedTags.value.indexOf(tag)
  if (idx > -1) selectedTags.value.splice(idx, 1)
  else selectedTags.value.push(tag)
}

const vote = (id, type) => {
  const qa = qaList.value.find(q => q.id === id)
  if (qa) qa.votes += type === 'up' ? 1 : -1
}

const openDetail = (qa) => { detailQA.value = qa; showDetailDialog.value = true }

const submitQuestion = () => {
  if (!askForm.value.title.trim()) { ElMessage.warning('请输入标题'); return }
  qaList.value.unshift({
    id: Date.now(),
    title: askForm.value.title,
    preview: askForm.value.content.slice(0, 100),
    votes: 0,
    answerCount: 0,
    views: 0,
    tags: askForm.value.tags,
    author: '我',
    authorColor: '#10b981',
    authorRole: 'student',
    time: '刚刚',
    bounty: askForm.value.bounty,
    hasTeacherAnswer: false,
    solved: false,
    answers: []
  })
  if (askForm.value.bounty) myPoints.value -= askForm.value.bounty
  showAskDialog.value = false
  ElMessage.success('问题发布成功！')
}

const submitReply = () => {
  if (!replyText.value.trim()) return
  if (!detailQA.value.answers) detailQA.value.answers = []
  detailQA.value.answers.push({
    id: Date.now(),
    author: '我',
    color: '#10b981',
    role: 'student',
    text: replyText.value,
    likes: 0,
    dislikes: 0,
    accepted: false
  })
  detailQA.value.answerCount++
  replyText.value = ''
  ElMessage.success('回答已提交！')
}

const acceptAnswer = (ans) => { ans.accepted = true; detailQA.value.solved = true; ElMessage.success('已采纳！') }
const loadMore = () => ElMessage.info('加载更多问题...')

const addToMap = (qa) => {
  if (qa.inMap) return
  qa.inMap = true
  kmStore.addToMapTopics(qa.title)
  ElMessage.success(`「${qa.title.slice(0, 15)}...」已收入版图`)
}
</script>

<style scoped lang="scss">
.community-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  overflow-y: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: 16px;
  color: #fff;
  h2 { margin: 0 0 4px; font-size: 20px; }
  p { margin: 0; opacity: 0.85; font-size: 13px; }
}

.filter-bar {
  background: #fff;
  border-radius: 12px;
  padding: 14px 18px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.community-main {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 16px;
}

.qa-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.qa-card {
  display: flex;
  gap: 16px;
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: pointer;
  transition: all 0.2s;

  &:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); transform: translateY(-1px); }
}

.qa-votes {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 40px;
}

.vote-btn {
  border: none;
  background: none;
  font-size: 14px;
  cursor: pointer;
  color: #999;
  padding: 2px;
  &:hover { color: #3b82f6; }
  &.up:hover { color: #10b981; }
  &.down:hover { color: #ef4444; }
}

.vote-count { font-size: 16px; font-weight: 700; color: #333; }

.qa-body { flex: 1; min-width: 0; }
.qa-header { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.qa-title { font-size: 15px; font-weight: 600; margin: 0; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.qa-preview { font-size: 13px; color: #666; margin: 4px 0 8px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.qa-tags { margin-bottom: 8px; }
.qa-meta { display: flex; align-items: center; gap: 14px; font-size: 12px; color: #999; flex-wrap: wrap; }
.map-btn {
  margin-left: auto;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid rgba(6,182,212,0.4);
  background: rgba(6,182,212,0.08);
  color: #06b6d4;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { background: rgba(6,182,212,0.18); }
  &.added { border-color: rgba(63,185,80,0.4); background: rgba(63,185,80,0.08); color: #3fb950; cursor: default; }
}
.qa-author { display: flex; align-items: center; gap: 4px; }
.author-avatar { width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 10px; font-weight: 700; }
.author-avatar.small { width: 18px; height: 18px; font-size: 9px; }

.load-more { text-align: center; padding: 12px; }

/* 侧边栏 */
.community-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-card {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  h4 { margin: 0 0 12px; font-size: 14px; }
}

.hot-note-item {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  &:last-child { border-bottom: none; }

  .note-rank { font-size: 18px; font-weight: 800; color: #f59e0b; width: 24px; text-align: center; }
  .note-title { font-size: 13px; margin: 0; font-weight: 500; }
  .note-likes { font-size: 11px; color: #999; }
}

.user-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  &:last-child { border-bottom: none; }

  .user-avatar { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 12px; font-weight: 700; }
  .user-name { font-size: 13px; font-weight: 600; margin: 0; }
  .user-stats { font-size: 11px; color: #999; margin: 2px 0 0; }
}

.my-bounty {
  p { font-size: 13px; margin: 4px 0; color: #555; }
  b { color: #f59e0b; }
}

/* 详情弹窗 */
.detail-content {
  max-height: 70vh;
  overflow-y: auto;
}

.detail-question {
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  .detail-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; font-size: 13px; color: #999; }
  .detail-text { font-size: 14px; line-height: 1.7; color: #333; }
}

.detail-answers {
  margin-top: 16px;
  h4 { margin: 0 0 12px; font-size: 14px; }
}

.answer-item {
  padding: 14px;
  border-radius: 10px;
  border: 1px solid #f0f0f0;
  margin-bottom: 10px;
  transition: all 0.2s;

  &.teacher { border-left: 3px solid #f59e0b; background: #fffbeb; }
  &.accepted { border-left: 3px solid #10b981; background: #ecfdf5; }

  .answer-header { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
  .ans-author { font-size: 13px; font-weight: 600; }
  .ans-text { font-size: 13px; line-height: 1.7; color: #555; }
  .ans-actions { display: flex; align-items: center; gap: 12px; margin-top: 8px; font-size: 12px; color: #999; }
}

.detail-reply {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

@media (max-width: 1024px) {
  .community-main { grid-template-columns: 1fr; }
  .community-sidebar { display: none; }
}
</style>
