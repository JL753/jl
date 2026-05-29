import http from './http'

// ==================== 仪表盘 ====================
export const apiTeacherDashboard = () => http.get('/dashboard/teacher')
export const apiStudentDashboard = () => http.get('/dashboard/student')
export const apiTeacherWorkspace = () => http.get('/dashboard/teacher/workspace')
export const apiStudentWorkspace = () => http.get('/dashboard/student/workspace')
export const apiDataScreen = () => http.get('/dashboard/screen')

// ==================== 考试 ====================
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

// ==================== 分析 ====================
export const apiAnalyticsDashboard = () => http.get('/analytics/dashboard')

// ==================== 能力评估 ====================
export const apiAbilityEvaluate = () => http.post('/ability/evaluate')
export const apiAbilityLatest = () => http.get('/ability/latest')
export const apiAbilityHistory = () => http.get('/ability/history')

// ==================== 学习数据 ====================
export const apiExerciseSubmit = (data) => http.post('/exercise/submit', data)
export const apiStudyHeartbeat = (subChapterId, seconds) => http.post('/study/heartbeat', { subChapterId, seconds })
