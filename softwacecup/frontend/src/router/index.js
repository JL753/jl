import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  // 门户首页
  { path: '/', name: 'portal', component: () => import('../views/PortalHome.vue') },

  // AI 初始问卷（新用户引导）
  { path: '/questionnaire', name: 'questionnaire', component: () => import('../views/common/AIQuestionnaire.vue'), meta: { requiresAuth: true } },

  // 学科目录（登录前可浏览）
  { path: '/subjects', name: 'subjects', component: () => import('../views/common/SubjectCatalog.vue') },
  { path: '/subjects/:id', name: 'subject-detail', component: () => import('../views/common/CourseDetail.vue') },
  // 学生端
  {
    path: '/student',
    component: () => import('../layout/StudentLayout.vue'),
    meta: { requiresAuth: true, role: 'student' },
    children: [
      { path: '', redirect: '/student/dashboard' },
      { path: 'dashboard', name: 'student-dashboard', component: () => import('../views/student/StudentDashboard.vue') },
      { path: 'subjects', name: 'student-subjects', component: () => import('../views/common/SubjectCatalog.vue') },
      { path: 'subjects/:id', name: 'student-subject-detail', component: () => import('../views/common/CourseDetail.vue') },
      { path: 'courses/:id', name: 'student-course', component: () => import('../views/student/CourseView.vue') },
      { path: 'knowledge-map', name: 'student-knowledge-map', component: () => import('../views/student/KnowledgeStarMap.vue') },
      { path: 'companion', name: 'companion', component: () => import('../views/student/AICompanionImmersive.vue') },
      { path: 'companion-immersive', redirect: '/student/companion' },
      { path: 'profile', name: 'student-profile', component: () => import('../views/common/ProfileView.vue') },
      { path: 'community', name: 'student-community', component: () => import('../views/student/CommunityView.vue') },
    ]
  },

  // 教师端（兼管理员，原 admin 合并至此）
  {
    path: '/teacher',
    component: () => import('../layout/TeacherLayout.vue'),
    meta: { requiresAuth: true, role: 'teacher' },
    children: [
      { path: '', redirect: '/teacher/dashboard' },
      { path: 'dashboard', name: 'teacher-dashboard', component: () => import('../views/teacher/TeacherDashboard.vue') },
      { path: 'assistant', name: 'teacher-assistant', component: () => import('../views/teacher/TeacherAssistant.vue') },
      { path: 'manage', name: 'teacher-manage', component: () => import('../views/teacher/TeacherManage.vue') },
      { path: 'classes', name: 'teacher-classes', component: () => import('../views/teacher/ClassManagement.vue') },
      { path: 'content', name: 'teacher-content', component: () => import('../views/teacher/ContentManagement.vue') },
      { path: 'review', name: 'teacher-review', component: () => import('../views/teacher/AIContentReview.vue') },
      { path: 'assignments', name: 'teacher-assignments', component: () => import('../views/teacher/AssignmentManagement.vue') },
      { path: 'profile', name: 'teacher-profile', component: () => import('../views/common/ProfileView.vue') },
    ]
  },

  // 兜底
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 导航守卫
router.beforeEach(async (to, from, next) => {
  if (!to.meta.requiresAuth) return next()

  const auth = useAuthStore()

  // 有 token 但没有用户信息，尝试获取
  if (auth.token && !auth.user) {
    try { await auth.fetchMe() } catch (e) { auth.logout() }
  }

  if (!auth.isLoggedIn) {
    // 弹出登录 Modal，记录目标路由
    auth.openLoginModal(to.fullPath)
    return next('/')
  }

  // 角色检查（admin 兼容旧 token，直接放行）
  const role = auth.userRole
  if (to.meta.role && to.meta.role !== role && role !== 'admin') {
    return next(`/${role}/dashboard`)
  }

  next()
})

export default router
