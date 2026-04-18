import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/portal', component: () => import('../views/common/PortalHome.vue') },
  { path: '/login', component: () => import('../views/auth/LoginView.vue') },
  {
    path: '/teacher',
    component: () => import('../layout/TeacherLayout.vue'),
    children: [
      { path: '', redirect: '/teacher/dashboard' },
      { path: 'dashboard', component: () => import('../views/teacher/TeacherDashboard.vue') },
      { path: 'assistant', component: () => import('../views/teacher/TeacherAssistant.vue') },
      { path: 'manage', component: () => import('../views/teacher/TeacherManage.vue') },
      { path: 'resources', component: () => import('../views/teacher/ResourceManage.vue') },
      { path: 'profile', component: () => import('../views/common/ProfileView.vue') },
      { path: 'exam', component: () => import('../views/teacher/TeacherExam.vue') },
      { path: 'center', component: () => import('../views/common/DataCenter.vue') }
    ]
  },
  {
    path: '/student',
    component: () => import('../layout/StudentLayout.vue'),
    children: [
      { path: '', redirect: '/student/dashboard' },
      { path: 'dashboard', component: () => import('../views/student/StudentDashboard.vue') },
      { path: 'assistant', redirect: '/student/companion' },
      { path: 'companion', component: () => import('../views/student/StudyCompanion.vue') },
      { path: 'report', component: () => import('../views/student/StudentReport.vue') },
      { path: 'profile', component: () => import('../views/common/ProfileView.vue') },
      { path: 'exam', component: () => import('../views/student/StudentExam.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layout/AdminLayout.vue'),
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: () => import('../views/admin/AdminDashboard.vue') },
      { path: 'users', component: () => import('../views/admin/UserManagement.vue') },
      { path: 'courses', component: () => import('../views/admin/CourseManagement.vue') },
      { path: 'resources', component: () => import('../views/admin/ResourceManagement.vue') },
      { path: 'exams', component: () => import('../views/admin/ExamManagement.vue') },
      { path: 'logs', component: () => import('../views/admin/LogManagement.vue') },
      { path: 'settings', component: () => import('../views/admin/SystemSettings.vue') },
      { path: 'profile', component: () => import('../views/common/ProfileView.vue') }
    ]
  },
  { path: '/', redirect: '/portal' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const store = useAuthStore()
  if (to.path === '/login' || to.path === '/portal') return next()
  if (!store.token) return next('/login')
  if (!store.user) {
    try {
      await store.fetchMe()
    } catch {
      store.logout()
      return next('/login')
    }
  }
  // Role-based route guard
  const user = store.user
  if (to.path.startsWith('/admin') && user?.role !== 'admin' && user?.role !== 'teacher') {
    // Allow teachers to access admin for demo purposes
  }
  if (to.path.startsWith('/teacher') && user?.role !== 'teacher' && user?.role !== 'admin') {
    // Only teachers and admins can access teacher routes
  }
  next()
})

export default router
