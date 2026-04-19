import http from './http'

export const apiTeacherQuestionBank = () => http.get('/teacher-tools/questions')
export const apiAddTeacherQuestion = (payload) => http.post('/teacher-tools/questions', payload)
export const apiCreateTeacherPaper = (payload) => http.post('/teacher-tools/papers', payload)
export const apiTeacherPptTemplates = () => http.get('/teacher-tools/ppt-templates')
export const apiGenerateTeachingPpt = (payload) => http.post('/teacher-tools/teaching-ppt', payload)

export const apiLogin = (payload) => http.post('/auth/login', payload)
export const apiRegister = (payload) => http.post('/auth/register', payload)
export const apiMe = () => http.get('/auth/me')
export const apiUpdateMe = (payload) => http.put('/auth/me', payload)

export const apiBuildProfile = (payload) => http.post('/profile/dialogue', payload)
export const apiGetProfile = () => http.get('/profile/mine')

export const apiGenerateResources = (payload) => http.post('/resources/generate', payload)
export const apiMyResources = () => http.get('/resources/mine')
export const apiTeacherLibrary = () => http.get('/resources/teacher-library')
export const apiResourceProgress = () => http.get('/resources/progress')
export const apiResourceRecommendation = (prompt) => http.get('/resources/recommendation', { params: { prompt } })

export const apiGeneratePath = (payload) => http.post('/path/generate', payload)
export const apiMyPath = () => http.get('/path/mine')

export const apiEvaluate = (payload) => http.post('/assessment/evaluate', payload)
export const apiMyAssessment = () => http.get('/assessment/mine')

export const apiAskTutor = (payload) => http.post('/tutor/ask', payload)

export const apiTeacherDashboard = () => http.get('/dashboard/teacher')
export const apiStudentDashboard = () => http.get('/dashboard/student')
export const apiTeacherWorkspace = () => http.get('/dashboard/teacher/workspace')
export const apiStudentWorkspace = () => http.get('/dashboard/student/workspace')
export const apiDataScreen = () => http.get('/dashboard/screen')
export const apiExamList = (role = 'student') => http.get('/dashboard/exams', { params: { role } })
export const apiExamDetail = (examId, role = 'student') => http.get('/dashboard/exam-detail', { params: { examId, role } })
export const apiPublishExam = (payload) => http.post('/dashboard/publish-exam', payload)
export const apiExamRecords = () => http.get('/dashboard/exam-records')
export const apiWrongQuestions = () => http.get('/dashboard/wrong-questions')
export const apiExamScoreboard = (examId) => http.get('/dashboard/exam-scoreboard', { params: { examId } })
export const apiExamScoreboardSummary = (examId) => http.get('/dashboard/exam-scoreboard-summary', { params: { examId } })
export const apiExamScoreDetail = (recordId) => http.get('/dashboard/exam-score-detail', { params: { recordId } })
export const apiWrongQuestionDetail = (wrongQuestionId) => http.get('/dashboard/wrong-question-detail', { params: { wrongQuestionId } })
export const apiExamDimensionStats = () => http.get('/dashboard/exam-dimension-stats')
export const apiProfileCard = () => http.get('/dashboard/profile-card')
export const apiGradeExam = (payload) => http.post('/dashboard/grade-exam', payload)
export const apiSubmitExam = (payload) => http.post('/dashboard/submit-exam', payload)

// ==================== Admin API ====================
export const apiAdminDashboard = () => http.get('/admin/dashboard')

// 用户管理
export const apiAdminListUsers = (params) => http.get('/admin/users', { params })
export const apiAdminCreateUser = (payload) => http.post('/admin/users', payload)
export const apiAdminUpdateUser = (id, payload) => http.put(`/admin/users/${id}`, payload)
export const apiAdminDeleteUser = (id) => http.delete(`/admin/users/${id}`)

// 课程管理
export const apiAdminListCourses = (params) => http.get('/admin/courses', { params })
export const apiAdminCreateCourse = (payload) => http.post('/admin/courses', payload)
export const apiAdminUpdateCourse = (id, payload) => http.put(`/admin/courses/${id}`, payload)
export const apiAdminDeleteCourse = (id) => http.delete(`/admin/courses/${id}`)

// 资源管理
export const apiAdminListResources = (params) => http.get('/admin/resources', { params })
export const apiAdminDeleteResource = (id) => http.delete(`/admin/resources/${id}`)

// 考试管理
export const apiAdminListExams = (params) => http.get('/admin/exams', { params })
export const apiAdminCreateExam = (payload) => http.post('/admin/exams', payload)
export const apiAdminUpdateExam = (id, payload) => http.put(`/admin/exams/${id}`, payload)
export const apiAdminDeleteExam = (id) => http.delete(`/admin/exams/${id}`)

// 日志与设置
export const apiAdminLogs = (params) => http.get('/admin/logs', { params })
export const apiAdminGetSettings = () => http.get('/admin/settings')
export const apiAdminUpdateSettings = (payload) => http.put('/admin/settings', payload)

export const apiPortal = () => http.get('/common/portal')
export const apiHealth = () => http.get('/common/health')
export const apiDatacenter = () => http.get('/common/datacenter')
