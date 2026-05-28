import http from './http'

// ==================== 教师工具 ====================
export const apiTeacherQuestionBank = () => http.get('/teacher-tools/questions')
export const apiAddTeacherQuestion = (payload) => http.post('/teacher-tools/questions', payload)
export const apiCreateTeacherPaper = (payload) => http.post('/teacher-tools/papers', payload)
export const apiTeacherPptTemplates = () => http.get('/teacher-tools/ppt-templates')
export const apiGenerateTeachingPpt = (payload) => http.post('/teacher-tools/teaching-ppt', payload)

// ==================== 资源生成 ====================
export const apiGenerateResources = (payload) => http.post('/resources/generate', payload)
export const apiMyResources = () => http.get('/resources/mine')
export const apiTeacherLibrary = () => http.get('/resources/teacher-library')
export const apiResourceProgress = () => http.get('/resources/progress')
export const apiResourceRecommendation = (prompt) => http.get('/resources/recommendation', { params: { prompt } })

// ==================== 内容审核 ====================
export const apiContentReviewPending = () => http.get('/content-review/pending')
export const apiContentReviewApprove = (id) => http.post(`/content-review/${id}/approve`)
export const apiContentReviewReject = (id) => http.post(`/content-review/${id}/reject`)
export const apiAiGenerateContent = (lessonId) => http.post(`/content-review/lessons/${lessonId}/ai-generate-content`)

// ==================== 班级 & 作业 ====================
export const apiMyClasses = () => http.get('/classes/my')
export const apiJoinClass = (inviteCode) => http.post('/classes/join', { inviteCode })
export const apiCreateClass = (data) => http.post('/classes', data)
export const apiClassMembers = (classId) => http.get(`/classes/${classId}/members`)
export const apiMyClassList = () => http.get('/classes/my-classes')
export const apiMyAssignments = () => http.get('/assignments/my')
export const apiSubmitAssignment = (id) => http.post(`/assignments/${id}/submit`)

// ==================== 游戏化 ====================
export const apiGamificationProgress = () => http.get('/gamification/progress')
export const apiGamificationBadges = () => http.get('/gamification/badges')
export const apiGamificationStreak = () => http.get('/gamification/streak')
export const apiGamificationCheckin = () => http.post('/gamification/checkin')
export const apiGamificationAddXp = (amount, reason) => http.post('/gamification/add-xp', { amount, reason })
export const apiGamificationLeaderboard = (limit = 10) => http.get('/gamification/leaderboard', { params: { limit } })
export const apiGamificationDailyChallenges = () => http.get('/gamification/daily-challenges')
