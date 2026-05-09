<template>
  <div class="course-platform-page">
    <!-- Top Navigation Bar -->
    <section class="platform-nav panel">
      <div class="nav-left">
        <div class="logo-area">
          <span class="logo-icon">📚</span>
          <div class="logo-text">
            <strong>课程资源平台</strong>
            <small>Course Resource Center</small>
          </div>
        </div>
      </div>
      <div class="nav-center">
        <div class="search-bar">
          <span class="search-icon">⌕</span>
          <input v-model="searchKeyword" placeholder="搜索课程名称、关键词、讲师..." />
          <button class="search-btn">搜索</button>
        </div>
      </div>
      <div class="nav-right">
        <button class="nav-chip" :class="{ active: filterCategory === 'all' }" @click="filterCategory = 'all'">
          全部
        </button>
        <button class="nav-chip" :class="{ active: filterCategory === 'cs' }" @click="filterCategory = 'cs'">
          计算机
        </button>
        <button class="nav-chip" :class="{ active: filterCategory === 'ai' }" @click="filterCategory = 'ai'">
          人工智能
        </button>
        <button class="nav-chip" :class="{ active: filterCategory === 'math' }" @click="filterCategory = 'math'">
          数学
        </button>
        <button class="nav-chip" :class="{ active: filterCategory === 'language' }" @click="filterCategory = 'language'">
          语言
        </button>
      </div>
    </section>

    <!-- Main Content Area -->
    <div class="main-content">
      <!-- Left: Course List -->
      <aside class="course-list-sidebar panel">
        <div class="sidebar-header">
          <h3>课程目录</h3>
          <span class="course-count">{{ filteredCourses.length }} 个课程</span>
        </div>
        
        <div class="course-filter-bar">
          <el-select v-model="filterDifficulty" placeholder="难度筛选" size="small" clearable style="width: 100%">
            <el-option label="全部难度" value="" />
            <el-option label="入门" value="beginner" />
            <el-option label="初级" value="easy" />
            <el-option label="中级" value="medium" />
            <el-option label="高级" value="hard" />
          </el-select>
          <el-select v-model="filterProgress" placeholder="进度筛选" size="small" clearable style="width: 100%">
            <el-option label="全部" value="" />
            <el-option label="未开始" value="not_started" />
            <el-option label="进行中" value="in_progress" />
            <el-option label="已完成" value="completed" />
          </el-select>
        </div>

        <div class="course-list">
          <div 
            v-for="course in filteredCourses" 
            :key="course.id" 
            :class="['course-card', { active: selectedCourse?.id === course.id }]"
            @click="selectCourse(course)"
          >
            <div class="course-cover">
              <img :src="course.cover" :alt="course.name" />
              <div class="progress-overlay">
                <div class="progress-ring">
                  <svg viewBox="0 0 36 36">
                    <path class="ring-bg" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                    <path class="ring-progress" :stroke-dasharray="`${course.progress}, 100`" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                  </svg>
                  <span class="progress-text">{{ course.progress }}%</span>
                </div>
              </div>
              <span :class="['course-tag', course.category]">{{ course.categoryLabel }}</span>
            </div>
            <div class="course-info">
              <h4>{{ course.name }}</h4>
              <p class="course-teacher">{{ course.teacher }}</p>
              <div class="course-meta">
                <span class="meta-item">
                  <span class="meta-icon">⏱</span>
                  {{ course.duration }}
                </span>
                <span class="meta-item">
                  <span class="meta-icon">👥</span>
                  {{ course.students }}人在学
                </span>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- Right: Course Detail / Learning Area -->
      <main class="course-detail-area">
        <!-- Course Overview (when no course selected) -->
        <div v-if="!selectedCourse" class="no-selection-placeholder panel">
          <div class="placeholder-content">
            <span class="placeholder-icon">📖</span>
            <h3>选择一门课程开始学习</h3>
            <p>从左侧课程列表中选择，或使用搜索功能查找感兴趣的课程</p>
            <div class="quick-stats">
              <div class="stat-card">
                <strong>{{ totalCourses }}</strong>
                <span>课程总数</span>
              </div>
              <div class="stat-card">
                <strong>{{ inProgressCount }}</strong>
                <span>进行中</span>
              </div>
              <div class="stat-card">
                <strong>{{ completedCount }}</strong>
                <span>已完成</span>
              </div>
              <div class="stat-card">
                <strong>{{ totalHours }}</strong>
                <span>累计学时</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Course Detail View -->
        <div v-else class="course-detail panel">
          <!-- Course Header -->
          <div class="detail-header">
            <div class="header-info">
              <div class="breadcrumb">
                <span>课程资源</span>
                <span class="sep">/</span>
                <span>{{ selectedCourse.categoryLabel }}</span>
                <span class="sep">/</span>
                <strong>{{ selectedCourse.name }}</strong>
              </div>
              <h2>{{ selectedCourse.name }}</h2>
              <p class="course-description">{{ selectedCourse.description }}</p>
              <div class="teacher-info">
                <img :src="selectedCourse.teacherAvatar" alt="" class="teacher-avatar" />
                <div>
                  <strong>{{ selectedCourse.teacher }}</strong>
                  <span>{{ selectedCourse.teacherTitle }}</span>
                </div>
              </div>
              <div class="detail-meta">
                <span class="meta-tag">
                  <span class="meta-icon">⏱</span>
                  {{ selectedCourse.duration }}
                </span>
                <span class="meta-tag">
                  <span class="meta-icon">📅</span>
                  {{ selectedCourse.chapters.length }} 章节
                </span>
                <span class="meta-tag">
                  <span class="meta-icon">👥</span>
                  {{ selectedCourse.students }} 人已学习
                </span>
                <span class="meta-tag">
                  <span class="meta-icon">⭐</span>
                  {{ selectedCourse.rating }} 分
                </span>
              </div>
            </div>
            <div class="header-actions">
              <button class="action-btn primary" @click="startLearning">
                <span class="btn-icon">▶</span>
                {{ selectedCourse.progress > 0 ? '继续学习' : '开始学习' }}
              </button>
              <button class="action-btn secondary" @click="toggleFavorite(selectedCourse)">
                <span class="btn-icon">{{ selectedCourse.isFavorite ? '❤️' : '🤍' }}</span>
                {{ selectedCourse.isFavorite ? '已收藏' : '收藏' }}
              </button>
              <button class="action-btn secondary">
                <span class="btn-icon">↗</span>
                分享
              </button>
            </div>
          </div>

          <!-- Learning Tabs -->
          <div class="learning-tabs">
            <button 
              v-for="tab in learningTabs" 
              :key="tab.key" 
              :class="['tab-btn', { active: activeTab === tab.key }]"
              @click="activeTab = tab.key"
            >
              <span class="tab-icon">{{ tab.icon }}</span>
              {{ tab.label }}
            </button>
          </div>

          <!-- Tab Content: Chapters -->
          <div v-if="activeTab === 'chapters'" class="tab-content">
            <div class="chapters-list">
              <div 
                v-for="(chapter, index) in selectedCourse.chapters" 
                :key="index" 
                :class="['chapter-item', { completed: chapter.completed, locked: chapter.locked }]"
              >
                <div class="chapter-header" @click="toggleChapter(index)">
                  <div class="chapter-status">
                    <span v-if="chapter.completed" class="status-icon completed">✓</span>
                    <span v-else-if="chapter.locked" class="status-icon locked">🔒</span>
                    <span v-else class="status-icon pending">{{ index + 1 }}</span>
                  </div>
                  <div class="chapter-info">
                    <h4>{{ chapter.name }}</h4>
                    <p>{{ chapter.duration }} · {{ chapter.resources.length }}个资源</p>
                  </div>
                  <span class="expand-icon">{{ expandedChapter === index ? '▲' : '▼' }}</span>
                </div>
                <div v-show="expandedChapter === index" class="chapter-content">
                  <div class="chapter-goal">
                    <strong>学习目标：</strong>{{ chapter.goal }}
                  </div>
                  <div class="resources-list">
                    <div 
                      v-for="(resource, rIdx) in chapter.resources" 
                      :key="rIdx"
                      :class="['resource-item', resource.type]"
                      @click="openResource(resource)"
                    >
                      <span class="resource-icon">{{ getResourceIcon(resource.type) }}</span>
                      <div class="resource-info">
                        <strong>{{ resource.name }}</strong>
                        <span>{{ resource.duration || resource.size }}</span>
                      </div>
                      <span v-if="resource.completed" class="resource-completed">已完成</span>
                      <button v-else class="resource-play">播放</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Tab Content: Resources -->
          <div v-if="activeTab === 'resources'" class="tab-content">
            <div class="resources-grid">
              <div 
                v-for="(resource, index) in allResources" 
                :key="index"
                class="resource-card"
                @click="openResource(resource)"
              >
                <div class="resource-preview" :class="resource.type">
                  <span class="preview-icon">{{ getResourceIcon(resource.type) }}</span>
                  <div class="preview-overlay">
                    <button class="play-overlay">▶</button>
                  </div>
                </div>
                <div class="resource-card-info">
                  <h5>{{ resource.name }}</h5>
                  <p>{{ resource.description }}</p>
                  <div class="resource-card-meta">
                    <span>{{ resource.type === 'video' ? resource.duration : resource.size }}</span>
                    <span v-if="resource.completed" class="completed-badge">已完成</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Tab Content: Notes -->
          <div v-if="activeTab === 'notes'" class="tab-content">
            <div class="notes-header">
              <h4>我的笔记</h4>
              <el-button type="primary" size="small" @click="showNoteEditor = true">新建笔记</el-button>
            </div>
            <div class="notes-list">
              <div v-for="note in myNotes" :key="note.id" class="note-item">
                <div class="note-header">
                  <strong>{{ note.title }}</strong>
                  <span class="note-date">{{ note.date }}</span>
                </div>
                <p class="note-content">{{ note.content }}</p>
                <div class="note-tags">
                  <span v-for="tag in note.tags" :key="tag" class="note-tag">{{ tag }}</span>
                </div>
              </div>
              <div v-if="myNotes.length === 0" class="empty-notes">
                <span>📝</span>
                <p>暂无笔记，开始学习后可添加</p>
              </div>
            </div>
          </div>

          <!-- Tab Content: Discussion -->
          <div v-if="activeTab === 'discussion'" class="tab-content">
            <div class="discussion-header">
              <h4>课程讨论</h4>
              <span class="discussion-count">{{ discussions.length }} 条讨论</span>
            </div>
            <div class="discussion-input">
              <textarea v-model="newDiscussion" placeholder="发表你的看法..." rows="3"></textarea>
              <button @click="postDiscussion">发布</button>
            </div>
            <div class="discussions-list">
              <div v-for="d in discussions" :key="d.id" class="discussion-item">
                <img :src="d.avatar" alt="" class="discussion-avatar" />
                <div class="discussion-body">
                  <div class="discussion-meta">
                    <strong>{{ d.user }}</strong>
                    <span>{{ d.date }}</span>
                  </div>
                  <p>{{ d.content }}</p>
                  <div class="discussion-actions">
                    <button @click="d.likes++">👍 {{ d.likes }}</button>
                    <button>回复</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      <!-- Right Sidebar: Learning Progress & Stats -->
      <aside v-if="selectedCourse" class="progress-sidebar panel">
        <div class="progress-card">
          <h4>学习进度</h4>
          <div class="progress-ring-large">
            <svg viewBox="0 0 120 120">
              <circle cx="60" cy="60" r="54" class="ring-bg" />
              <circle 
                cx="60" cy="60" r="54" 
                class="ring-progress"
                :stroke-dasharray="`${selectedCourse.progress * 3.39}, 339`"
              />
            </svg>
            <div class="progress-center">
              <strong>{{ selectedCourse.progress }}%</strong>
              <span>完成度</span>
            </div>
          </div>
          <div class="progress-stats">
            <div class="stat-row">
              <span>已学章节</span>
              <strong>{{ completedChapters }} / {{ selectedCourse.chapters.length }}</strong>
            </div>
            <div class="stat-row">
              <span>学习时长</span>
              <strong>{{ selectedCourse.learnedTime }} / {{ selectedCourse.totalTime }}</strong>
            </div>
            <div class="stat-row">
              <span>获得学分</span>
              <strong>{{ selectedCourse.credits }}</strong>
            </div>
          </div>
        </div>

        <div class="recent-learning">
          <h4>最近学习</h4>
          <div v-for="item in recentLearning" :key="item.id" class="recent-item">
            <img :src="item.cover" alt="" />
            <div class="recent-info">
              <strong>{{ item.name }}</strong>
              <span>{{ item.chapter }}</span>
            </div>
            <span class="recent-progress">{{ item.progress }}%</span>
          </div>
        </div>

        <div class="recommended-courses">
          <h4>推荐课程</h4>
          <div v-for="course in recommendedCourses" :key="course.id" class="recommended-item">
            <img :src="course.cover" alt="" />
            <div class="recommended-info">
              <strong>{{ course.name }}</strong>
              <span>{{ course.teacher }}</span>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <!-- Resource Player Modal -->
    <Teleport to="body">
      <transition name="modal-fade">
        <div v-if="showPlayer" class="player-modal-overlay" @click.self="closePlayer">
          <div class="player-modal">
            <div class="player-header">
              <h3>{{ currentResource?.name }}</h3>
              <button class="close-btn" @click="closePlayer">×</button>
            </div>
            <div class="player-content">
              <div class="video-player" v-if="currentResource?.type === 'video'">
                <div class="video-placeholder">
                  <span>▶</span>
                  <p>视频播放器区域</p>
                  <small>实际环境中将嵌入真实的视频内容</small>
                </div>
                <div class="video-controls">
                  <button class="control-btn">⏮</button>
                  <button class="control-btn play">▶</button>
                  <button class="control-btn">⏭</button>
                  <div class="progress-bar">
                    <div class="progress-fill" :style="{ width: '35%' }"></div>
                  </div>
                  <span class="time">05:30 / 15:00</span>
                </div>
              </div>
              <div class="document-viewer" v-else-if="currentResource?.type === 'document'">
                <div class="doc-preview">
                  <span class="doc-icon">📄</span>
                  <h4>{{ currentResource?.name }}</h4>
                  <p>文档预览区域</p>
                </div>
              </div>
              <div class="ppt-viewer" v-else-if="currentResource?.type === 'ppt'">
                <div class="ppt-preview-area">
                  <span class="ppt-icon">📊</span>
                  <h4>{{ currentResource?.name }}</h4>
                  <p>PPT演示区域</p>
                </div>
              </div>
            </div>
            <div class="player-sidebar">
              <div class="resource-list-panel">
                <h4>章节资源</h4>
                <div v-for="(ch, idx) in selectedCourse?.chapters" :key="idx" class="ch-resource">
                  <strong>{{ ch.name }}</strong>
                  <div v-for="res in ch.resources" :key="res.name" 
                       :class="['res-item', { active: currentResource?.name === res.name }]"
                       @click="openResource(res)">
                    <span>{{ getResourceIcon(res.type) }}</span>
                    {{ res.name }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>

    <!-- Note Editor Dialog -->
    <el-dialog v-model="showNoteEditor" title="新建笔记" width="500px" destroy-on-close>
      <el-form :model="noteForm" label-width="80px">
        <el-form-item label="笔记标题">
          <el-input v-model="noteForm.title" placeholder="输入笔记标题" />
        </el-form-item>
        <el-form-item label="笔记内容">
          <el-input v-model="noteForm.content" type="textarea" :rows="5" placeholder="输入笔记内容..." />
        </el-form-item>
        <el-form-item label="关联章节">
          <el-select v-model="noteForm.chapterId" placeholder="选择关联章节">
            <el-option v-for="(ch, idx) in selectedCourse?.chapters" :key="idx" :label="ch.name" :value="idx" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="noteForm.tags" multiple placeholder="选择或添加标签">
            <el-option label="重点" value="重点" />
            <el-option label="难点" value="难点" />
            <el-option label="易错" value="易错" />
            <el-option label="复习" value="复习" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNoteEditor = false">取消</el-button>
        <el-button type="primary" @click="saveNote">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'

// Filters
const searchKeyword = ref('')
const filterCategory = ref('all')
const filterDifficulty = ref('')
const filterProgress = ref('')
const selectedCourse = ref(null)
const activeTab = ref('chapters')
const expandedChapter = ref(0)

// Player
const showPlayer = ref(false)
const currentResource = ref(null)

// Notes
const showNoteEditor = ref(false)
const noteForm = reactive({
  title: '',
  content: '',
  chapterId: '',
  tags: []
})
const myNotes = ref([])

// Discussion
const newDiscussion = ref('')
const discussions = ref([
  { id: 1, user: '李同学', avatar: 'https://dummyimage.com/40x40/4c8dff/fff&text=李', date: '2024-01-15', content: '这门课讲得很清楚，推荐！', likes: 12 },
  { id: 2, user: '王同学', avatar: 'https://dummyimage.com/40x40/34d399/fff&text=王', date: '2024-01-14', content: '第三章的机器学习基础讲得不错', likes: 8 }
])

// Learning tabs
const learningTabs = [
  { key: 'chapters', label: '章节', icon: '📑' },
  { key: 'resources', label: '资源', icon: '📁' },
  { key: 'notes', label: '笔记', icon: '📝' },
  { key: 'discussion', label: '讨论', icon: '💬' }
]

// Mock courses data
const courses = ref([
  {
    id: 1,
    name: 'Python机器学习基础',
    teacher: '张教授',
    teacherTitle: '计算机学院 · 博士生导师',
    teacherAvatar: 'https://dummyimage.com/48x48/4c8dff/fff&text=张',
    category: 'ai',
    categoryLabel: '人工智能',
    difficulty: 'medium',
    progress: 65,
    learnedTime: '6.5小时',
    totalTime: '10小时',
    credits: 3.2,
    duration: '10小时',
    students: 2847,
    rating: 4.8,
    description: '本课程系统讲解Python机器学习的基础知识，包括监督学习、无监督学习、深度学习等核心概念，配合丰富的实战项目，帮助学生快速掌握机器学习技能。',
    cover: 'https://dummyimage.com/280x160/4c8dff/fff&text=ML',
    isFavorite: true,
    chapters: [
      {
        name: '第一章：机器学习概述',
        duration: '45分钟',
        goal: '理解机器学习的基本概念和应用场景',
        completed: true,
        locked: false,
        resources: [
          { name: '1.1 机器学习简介', type: 'video', duration: '15分钟', completed: true },
          { name: '1.2 机器学习分类', type: 'video', duration: '20分钟', completed: true },
          { name: '章节课件', type: 'ppt', size: '2.5MB', completed: true }
        ]
      },
      {
        name: '第二章：监督学习算法',
        duration: '2小时',
        goal: '掌握线性回归、逻辑回归等监督学习算法',
        completed: true,
        locked: false,
        resources: [
          { name: '2.1 线性回归基础', type: 'video', duration: '25分钟', completed: true },
          { name: '2.2 逻辑回归实战', type: 'video', duration: '30分钟', completed: true },
          { name: '2.3 决策树与随机森林', type: 'video', duration: '35分钟', completed: false },
          { name: '代码练习：sklearn入门', type: 'document', size: '156KB', completed: false }
        ]
      },
      {
        name: '第三章：深度学习入门',
        duration: '3小时',
        goal: '了解神经网络基本结构和反向传播算法',
        completed: false,
        locked: false,
        resources: [
          { name: '3.1 神经网络基础', type: 'video', duration: '30分钟', completed: false },
          { name: '3.2 BP算法详解', type: 'video', duration: '40分钟', completed: false },
          { name: '3.3 CNN卷积神经网络', type: 'video', duration: '45分钟', completed: false },
          { name: '深度学习参考资料', type: 'document', size: '3.2MB', completed: false }
        ]
      }
    ]
  },
  {
    id: 2,
    name: '数据结构与算法',
    teacher: '刘教授',
    teacherTitle: '软件学院 · 硕士生导师',
    teacherAvatar: 'https://dummyimage.com/48x48/34d399/fff&text=刘',
    category: 'cs',
    categoryLabel: '计算机',
    difficulty: 'hard',
    progress: 30,
    learnedTime: '4.5小时',
    totalTime: '15小时',
    credits: 4.0,
    duration: '15小时',
    students: 4521,
    rating: 4.9,
    description: '系统学习常见数据结构（链表、树、图）和算法（排序、查找、动态规划），培养算法思维和编程能力。',
    cover: 'https://dummyimage.com/280x160/34d399/fff&text=DSA',
    isFavorite: false,
    chapters: [
      { name: '第一章：基础数据结构', duration: '2小时', goal: '掌握数组、链表、栈、队列', completed: true, locked: false, resources: [] },
      { name: '第二章：树与图', duration: '3小时', goal: '理解二叉树、图的概念和应用', completed: false, locked: false, resources: [] },
      { name: '第三章：算法基础', duration: '2.5小时', goal: '掌握排序和查找算法', completed: false, locked: false, resources: [] }
    ]
  },
  {
    id: 3,
    name: '人工智能导论',
    teacher: '陈教授',
    teacherTitle: '人工智能学院 · 院士',
    teacherAvatar: 'https://dummyimage.com/48x48/f59e0b/fff&text=陈',
    category: 'ai',
    categoryLabel: '人工智能',
    difficulty: 'beginner',
    progress: 0,
    learnedTime: '0小时',
    totalTime: '8小时',
    credits: 2.5,
    duration: '8小时',
    students: 6234,
    rating: 4.7,
    description: '人工智能入门课程，介绍AI发展历史、核心技术、应用领域和发展趋势。',
    cover: 'https://dummyimage.com/280x160/f59e0b/fff&text=AI',
    isFavorite: false,
    chapters: [
      { name: '第一章：AI概述', duration: '1小时', goal: '了解AI基本概念', completed: false, locked: false, resources: [] },
      { name: '第二章：机器学习基础', duration: '2小时', goal: '掌握ML基本原理', completed: false, locked: true, resources: [] }
    ]
  },
  {
    id: 4,
    name: '线性代数',
    teacher: '赵老师',
    teacherTitle: '数学学院 · 讲师',
    teacherAvatar: 'https://dummyimage.com/48x48/f472b6/fff&text=赵',
    category: 'math',
    categoryLabel: '数学',
    difficulty: 'medium',
    progress: 85,
    learnedTime: '7小时',
    totalTime: '8小时',
    credits: 3.0,
    duration: '8小时',
    students: 1892,
    rating: 4.5,
    description: '线性代数基础课程，包括矩阵运算、向量空间、特征值与特征向量等核心内容。',
    cover: 'https://dummyimage.com/280x160/f472b6/fff&text=Math',
    isFavorite: true,
    chapters: [
      { name: '第一章：矩阵基础', duration: '2小时', goal: '掌握矩阵运算', completed: true, locked: false, resources: [] },
      { name: '第二章：向量空间', duration: '2小时', goal: '理解向量空间概念', completed: true, locked: false, resources: [] }
    ]
  },
  {
    id: 5,
    name: '大学英语四级备考',
    teacher: 'Emily老师',
    teacherTitle: '外国语学院 · 外教',
    teacherAvatar: 'https://dummyimage.com/48x48/60a5fa/fff&text=Em',
    category: 'language',
    categoryLabel: '语言',
    difficulty: 'easy',
    progress: 50,
    learnedTime: '5小时',
    totalTime: '10小时',
    credits: 2.0,
    duration: '10小时',
    students: 3456,
    rating: 4.6,
    description: '针对大学英语四级考试的专项辅导课程，包含听力、阅读、写作、翻译四大模块。',
    cover: 'https://dummyimage.com/280x160/60a5fa/fff&text=English',
    isFavorite: false,
    chapters: [
      { name: '听力技巧', duration: '2.5小时', goal: '掌握听力技巧', completed: true, locked: false, resources: [] },
      { name: '阅读理解', duration: '3小时', goal: '提高阅读速度', completed: false, locked: false, resources: [] }
    ]
  }
])

// Computed properties
const filteredCourses = computed(() => {
  return courses.value.filter(c => {
    const matchSearch = !searchKeyword.value || c.name.includes(searchKeyword.value) || c.teacher.includes(searchKeyword.value)
    const matchCat = filterCategory.value === 'all' || c.category === filterCategory.value
    const matchDiff = !filterDifficulty.value || c.difficulty === filterDifficulty.value
    const matchProgress = !filterProgress.value || (
      filterProgress.value === 'not_started' && c.progress === 0 ||
      filterProgress.value === 'in_progress' && c.progress > 0 && c.progress < 100 ||
      filterProgress.value === 'completed' && c.progress === 100
    )
    return matchSearch && matchCat && matchDiff && matchProgress
  })
})

const totalCourses = computed(() => courses.value.length)
const inProgressCount = computed(() => courses.value.filter(c => c.progress > 0 && c.progress < 100).length)
const completedCount = computed(() => courses.value.filter(c => c.progress === 100).length)
const totalHours = computed(() => courses.value.reduce((sum, c) => sum + parseFloat(c.learnedTime), 0).toFixed(1) + '小时')

const completedChapters = computed(() => {
  if (!selectedCourse.value) return 0
  return selectedCourse.value.chapters.filter(ch => ch.completed).length
})

const allResources = computed(() => {
  if (!selectedCourse.value) return []
  return selectedCourse.value.chapters.flatMap(ch => ch.resources)
})

const recentLearning = computed(() => {
  return courses.value
    .filter(c => c.progress > 0 && c.progress < 100)
    .slice(0, 3)
    .map(c => ({
      id: c.id,
      name: c.name,
      chapter: c.chapters.find(ch => !ch.completed)?.name || '已完成',
      progress: c.progress,
      cover: c.cover
    }))
})

const recommendedCourses = computed(() => {
  return courses.value
    .filter(c => c.id !== selectedCourse.value?.id && !c.isFavorite)
    .slice(0, 3)
})

// Methods
function selectCourse(course) {
  selectedCourse.value = course
  activeTab.value = 'chapters'
  expandedChapter.value = 0
}

function toggleChapter(index) {
  expandedChapter.value = expandedChapter.value === index ? null : index
}

function startLearning() {
  if (selectedCourse.value) {
    // Find first uncompleted chapter
    const uncompletedIdx = selectedCourse.value.chapters.findIndex(ch => !ch.completed)
    if (uncompletedIdx >= 0) {
      expandedChapter.value = uncompletedIdx
      activeTab.value = 'chapters'
    }
    ElMessage.success('开始学习之旅！')
  }
}

function toggleFavorite(course) {
  course.isFavorite = !course.isFavorite
  ElMessage.success(course.isFavorite ? '已收藏课程' : '已取消收藏')
}

function getResourceIcon(type) {
  const icons = { video: '🎬', document: '📄', ppt: '📊', audio: '🎧', code: '💻' }
  return icons[type] || '📁'
}

function openResource(resource) {
  currentResource.value = resource
  showPlayer.value = true
}

function closePlayer() {
  showPlayer.value = false
  currentResource.value = null
}

function saveNote() {
  if (!noteForm.title || !noteForm.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  myNotes.value.push({
    id: Date.now(),
    title: noteForm.title,
    content: noteForm.content,
    date: new Date().toLocaleDateString(),
    tags: [...noteForm.tags]
  })
  showNoteEditor.value = false
  Object.assign(noteForm, { title: '', content: '', chapterId: '', tags: [] })
  ElMessage.success('笔记已保存')
}

function postDiscussion() {
  if (!newDiscussion.value.trim()) return
  discussions.value.unshift({
    id: Date.now(),
    user: '我',
    avatar: 'https://dummyimage.com/40x40/4c8dff/fff&text=我',
    date: new Date().toLocaleDateString(),
    content: newDiscussion.value,
    likes: 0
  })
  newDiscussion.value = ''
  ElMessage.success('发布成功')
}
</script>

<style scoped>
.course-platform-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  height: calc(100vh - 160px);
}

/* Navigation Bar */
.platform-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  gap: 20px;
  flex-wrap: wrap;
}

.nav-left {
  display: flex;
  align-items: center;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 32px;
}

.logo-text strong {
  display: block;
  font-size: 18px;
  color: #1e293b;
}

.logo-text small {
  font-size: 11px;
  color: #94a3b8;
}

.nav-center {
  flex: 1;
  max-width: 500px;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f1f5f9;
  border-radius: 999px;
  padding: 8px 16px;
}

.search-icon {
  color: #94a3b8;
  font-size: 18px;
}

.search-bar input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 14px;
  color: #334155;
}

.search-btn {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 6px 16px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
}

.nav-right {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.nav-chip {
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid #e2e8f0;
  background: white;
  color: #64748b;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.nav-chip:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.nav-chip.active {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

/* Main Content */
.main-content {
  display: grid;
  grid-template-columns: 300px 1fr 280px;
  gap: 14px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* Course List Sidebar */
.course-list-sidebar {
  overflow-y: auto;
  padding: 14px;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  color: #1e293b;
}

.course-count {
  font-size: 12px;
  color: #94a3b8;
}

.course-filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.course-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.course-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.course-card:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.1);
}

.course-card.active {
  border-color: #3b82f6;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.2);
}

.course-cover {
  position: relative;
  height: 100px;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.progress-overlay {
  position: absolute;
  bottom: 6px;
  right: 6px;
}

.progress-ring {
  width: 36px;
  height: 36px;
  position: relative;
}

.progress-ring svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: rgba(255, 255, 255, 0.3);
  stroke-width: 3;
}

.ring-progress {
  fill: none;
  stroke: #22c55e;
  stroke-width: 3;
  stroke-linecap: round;
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 9px;
  color: white;
  font-weight: 600;
}

.course-tag {
  position: absolute;
  top: 6px;
  left: 6px;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 10px;
  color: white;
}

.course-tag.cs { background: #34d399; }
.course-tag.ai { background: #a78bfa; }
.course-tag.math { background: #f472b6; }
.course-tag.language { background: #60a5fa; }

.course-info {
  padding: 10px;
}

.course-info h4 {
  margin: 0 0 4px;
  font-size: 13px;
  color: #1e293b;
}

.course-teacher {
  margin: 0 0 8px;
  font-size: 11px;
  color: #94a3b8;
}

.course-meta {
  display: flex;
  gap: 10px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #64748b;
}

.meta-icon {
  font-size: 12px;
}

/* Course Detail Area */
.course-detail-area {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow-y: auto;
}

.no-selection-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-content {
  text-align: center;
  padding: 40px;
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 16px;
  display: block;
}

.placeholder-content h3 {
  margin: 0 0 8px;
  color: #1e293b;
}

.placeholder-content p {
  color: #64748b;
  margin: 0 0 24px;
}

.quick-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-card {
  background: linear-gradient(135deg, #f1f5f9, #fff);
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
}

.stat-card strong {
  display: block;
  font-size: 24px;
  color: #2563eb;
  margin-bottom: 4px;
}

.stat-card span {
  font-size: 12px;
  color: #64748b;
}

/* Course Detail */
.course-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.header-info {
  flex: 1;
}

.breadcrumb {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 8px;
}

.breadcrumb .sep {
  margin: 0 4px;
}

.breadcrumb strong {
  color: #334155;
}

.detail-header h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #1e293b;
}

.course-description {
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
  margin: 0 0 16px;
}

.teacher-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.teacher-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
}

.teacher-info strong {
  display: block;
  color: #1e293b;
}

.teacher-info span {
  font-size: 12px;
  color: #64748b;
}

.detail-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.meta-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: #f1f5f9;
  border-radius: 999px;
  font-size: 12px;
  color: #64748b;
}

.header-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 10px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.action-btn.primary {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
}

.action-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
}

.action-btn.secondary {
  background: #f1f5f9;
  color: #334155;
}

.action-btn.secondary:hover {
  background: #e2e8f0;
}

/* Learning Tabs */
.learning-tabs {
  display: flex;
  gap: 4px;
  background: #f1f5f9;
  border-radius: 10px;
  padding: 4px;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.tab-btn.active {
  background: white;
  color: #2563eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.tab-icon {
  font-size: 16px;
}

/* Chapters */
.chapters-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chapter-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}

.chapter-item.completed {
  border-color: #bbf7d0;
}

.chapter-item.locked {
  opacity: 0.6;
}

.chapter-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  cursor: pointer;
}

.status-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.status-icon.completed {
  background: #dcfce7;
  color: #16a34a;
}

.status-icon.locked {
  background: #f1f5f9;
  color: #94a3b8;
}

.status-icon.pending {
  background: #dbeafe;
  color: #2563eb;
  font-weight: 600;
}

.chapter-info {
  flex: 1;
}

.chapter-info h4 {
  margin: 0 0 4px;
  font-size: 14px;
  color: #1e293b;
}

.chapter-info p {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
}

.expand-icon {
  color: #94a3b8;
  font-size: 12px;
}

.chapter-content {
  padding: 0 14px 14px;
  border-top: 1px solid #e2e8f0;
}

.chapter-goal {
  padding: 12px;
  background: #f0f9ff;
  border-radius: 8px;
  font-size: 13px;
  color: #334155;
  margin: 12px 0;
}

.resources-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.resource-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.resource-item:hover {
  border-color: #3b82f6;
}

.resource-icon {
  font-size: 20px;
}

.resource-info {
  flex: 1;
}

.resource-info strong {
  display: block;
  font-size: 13px;
  color: #1e293b;
}

.resource-info span {
  font-size: 11px;
  color: #94a3b8;
}

.resource-play {
  padding: 4px 12px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.resource-completed {
  padding: 4px 8px;
  background: #dcfce7;
  color: #16a34a;
  border-radius: 4px;
  font-size: 11px;
}

/* Resources Grid */
.resources-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.resource-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.resource-card:hover {
  border-color: #3b82f6;
  transform: translateY(-2px);
}

.resource-preview {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.resource-preview.video { background: linear-gradient(135deg, #1e3a5f, #0f172a); }
.resource-preview.document { background: linear-gradient(135deg, #fef3c7, #fde68a); }
.resource-preview.ppt { background: linear-gradient(135deg, #dbeafe, #bfdbfe); }

.preview-icon {
  font-size: 40px;
}

.preview-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.resource-card:hover .preview-overlay {
  opacity: 1;
}

.play-overlay {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: white;
  border: none;
  cursor: pointer;
  font-size: 18px;
}

.resource-card-info {
  padding: 12px;
}

.resource-card-info h5 {
  margin: 0 0 4px;
  font-size: 14px;
  color: #1e293b;
}

.resource-card-info p {
  margin: 0 0 8px;
  font-size: 12px;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.resource-card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #94a3b8;
}

.completed-badge {
  color: #16a34a;
}

/* Notes */
.notes-header, .discussion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.notes-header h4, .discussion-header h4 {
  margin: 0;
  font-size: 16px;
  color: #1e293b;
}

.notes-list, .discussions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.note-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 14px;
}

.note-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.note-header strong {
  color: #1e293b;
}

.note-date {
  font-size: 12px;
  color: #94a3b8;
}

.note-content {
  margin: 0 0 10px;
  font-size: 13px;
  color: #475569;
  line-height: 1.7;
}

.note-tags {
  display: flex;
  gap: 6px;
}

.note-tag {
  padding: 2px 8px;
  background: #e0f2fe;
  color: #0369a1;
  border-radius: 4px;
  font-size: 11px;
}

.empty-notes {
  text-align: center;
  padding: 40px;
  color: #94a3b8;
}

.empty-notes span {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}

/* Discussion */
.discussion-count {
  font-size: 12px;
  color: #94a3b8;
}

.discussion-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.discussion-input textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  resize: none;
  font-size: 14px;
  font-family: inherit;
}

.discussion-input button {
  align-self: flex-end;
  padding: 8px 20px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.discussion-item {
  display: flex;
  gap: 12px;
  padding: 14px;
  background: #f8fafc;
  border-radius: 10px;
}

.discussion-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

.discussion-body {
  flex: 1;
}

.discussion-meta {
  display: flex;
  gap: 8px;
  align-items: baseline;
  margin-bottom: 6px;
}

.discussion-meta strong {
  color: #1e293b;
  font-size: 14px;
}

.discussion-meta span {
  font-size: 12px;
  color: #94a3b8;
}

.discussion-body p {
  margin: 0 0 10px;
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
}

.discussion-actions {
  display: flex;
  gap: 12px;
}

.discussion-actions button {
  background: transparent;
  border: none;
  color: #64748b;
  cursor: pointer;
  font-size: 12px;
}

.discussion-actions button:hover {
  color: #2563eb;
}

/* Progress Sidebar */
.progress-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.progress-card, .recent-learning, .recommended-courses {
  background: #f8fafc;
  border-radius: 12px;
  padding: 14px;
}

.progress-card h4, .recent-learning h4, .recommended-courses h4 {
  margin: 0 0 14px;
  font-size: 14px;
  color: #1e293b;
}

.progress-ring-large {
  width: 120px;
  height: 120px;
  margin: 0 auto 14px;
  position: relative;
}

.progress-ring-large svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.progress-ring-large circle {
  fill: none;
  stroke-width: 8;
}

.progress-ring-large .ring-bg {
  stroke: #e2e8f0;
}

.progress-ring-large .ring-progress {
  stroke: #3b82f6;
  stroke-linecap: round;
}

.progress-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.progress-center strong {
  display: block;
  font-size: 22px;
  color: #1e293b;
}

.progress-center span {
  font-size: 11px;
  color: #94a3b8;
}

.progress-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.stat-row span {
  color: #64748b;
}

.stat-row strong {
  color: #1e293b;
}

.recent-item, .recommended-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  background: white;
  border-radius: 8px;
  margin-bottom: 8px;
}

.recent-item img, .recommended-item img {
  width: 48px;
  height: 36px;
  border-radius: 6px;
  object-fit: cover;
}

.recent-info, .recommended-info {
  flex: 1;
  min-width: 0;
}

.recent-info strong, .recommended-info strong {
  display: block;
  font-size: 12px;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recent-info span, .recommended-info span {
  font-size: 11px;
  color: #94a3b8;
}

.recent-progress {
  font-size: 12px;
  color: #3b82f6;
  font-weight: 500;
}

/* Player Modal */
.player-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 999;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
}

.player-modal {
  width: 90vw;
  max-width: 1200px;
  height: 80vh;
  background: #1e293b;
  border-radius: 16px;
  display: grid;
  grid-template-columns: 1fr 300px;
  overflow: hidden;
}

.player-header {
  grid-column: 1 / -1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: #0f172a;
}

.player-header h3 {
  margin: 0;
  color: white;
  font-size: 16px;
}

.close-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #334155;
  border: none;
  color: white;
  font-size: 20px;
  cursor: pointer;
}

.player-content {
  display: flex;
  flex-direction: column;
}

.video-player {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.video-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3a5f, #0f172a);
  color: white;
}

.video-placeholder span {
  font-size: 64px;
  margin-bottom: 16px;
}

.video-placeholder p {
  margin: 0 0 8px;
  font-size: 18px;
}

.video-placeholder small {
  opacity: 0.6;
}

.video-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: #0f172a;
}

.control-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #334155;
  border: none;
  color: white;
  cursor: pointer;
}

.control-btn.play {
  width: 44px;
  height: 44px;
  background: #3b82f6;
}

.progress-bar {
  flex: 1;
  height: 4px;
  background: #334155;
  border-radius: 2px;
}

.progress-fill {
  height: 100%;
  background: #3b82f6;
  border-radius: 2px;
}

.time {
  color: #94a3b8;
  font-size: 12px;
}

.document-viewer, .ppt-viewer {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.doc-preview, .ppt-preview-area {
  text-align: center;
  color: white;
}

.doc-icon, .ppt-icon {
  font-size: 64px;
  display: block;
  margin-bottom: 16px;
}

.player-sidebar {
  background: #0f172a;
  padding: 14px;
  overflow-y: auto;
}

.resource-list-panel h4 {
  margin: 0 0 12px;
  color: #e2e8f0;
  font-size: 14px;
}

.ch-resource {
  margin-bottom: 12px;
}

.ch-resource > strong {
  display: block;
  color: #94a3b8;
  font-size: 12px;
  margin-bottom: 6px;
}

.res-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 6px;
  color: #e2e8f0;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.res-item:hover, .res-item.active {
  background: #334155;
}

.modal-fade-enter-active, .modal-fade-leave-active {
  transition: opacity 0.2s;
}

.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
}

@media (max-width: 1400px) {
  .main-content {
    grid-template-columns: 280px 1fr;
  }
  .progress-sidebar {
    display: none;
  }
}

@media (max-width: 1000px) {
  .main-content {
    grid-template-columns: 1fr;
  }
  .course-list-sidebar {
    display: none;
  }
  .resources-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .quick-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .resources-grid {
    grid-template-columns: 1fr;
  }
  .detail-header {
    flex-direction: column;
  }
  .header-actions {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
