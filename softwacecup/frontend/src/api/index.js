import http from './http'
import axios from 'axios'

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

export const apiPortal = () => http.get('/common/portal')

// ==================== 知域 v2 API ====================
export const apiSubjects = () => http.get('/subjects')
export const apiSubjectUnits = (subjectId) => http.get(`/subjects/${subjectId}/units`)
export const apiUnitLessons = (unitId) => http.get(`/units/${unitId}/lessons`)
export const apiLessonDetail = (lessonId) => http.get(`/lessons/${lessonId}`)
export const apiLessonExercises = (lessonId) => http.get(`/lessons/${lessonId}/exercises`)
export const apiCompleteLesson = (lessonId) => http.post(`/progress/lesson/${lessonId}/complete`)
export const apiLessonProgress = (lessonId) => http.get(`/progress/lesson/${lessonId}`)
export const apiSubjectTree = (courseId) => http.get('/course/tree', { params: { courseId } })
export const apiLessonKnowledgePoints = (lessonId) => http.get(`/lessons/${lessonId}/knowledge-points`)
export const apiAbilityEvaluate = () => http.post('/ability/evaluate')
export const apiAbilityLatest = () => http.get('/ability/latest')
export const apiAbilityHistory = () => http.get('/ability/history')
export const apiRecommendResources = (lessonId) => http.post(`/lessons/${lessonId}/recommend-resources`)
export const apiMyClasses = () => http.get('/classes/my')
export const apiJoinClass = (inviteCode) => http.post('/classes/join', { inviteCode })
export const apiCreateClass = (data) => http.post('/classes', data)
export const apiClassMembers = (classId) => http.get(`/classes/${classId}/members`)
export const apiMyClassList = () => http.get('/classes/my-classes')
export const apiMyAssignments = () => http.get('/assignments/my')
export const apiSubmitAssignment = (id) => http.post(`/assignments/${id}/submit`)
export const apiContentReviewPending = () => http.get('/content-review/pending')
export const apiContentReviewApprove = (id) => http.post(`/content-review/${id}/approve`)
export const apiContentReviewReject = (id) => http.post(`/content-review/${id}/reject`)
export const apiAiGenerateContent = (lessonId) => http.post(`/content-review/lessons/${lessonId}/ai-generate-content`)
export const apiGamificationProgress = () => http.get('/gamification/progress')
export const apiGamificationBadges = () => http.get('/gamification/badges')
export const apiGamificationStreak = () => http.get('/gamification/streak')
export const apiGamificationCheckin = () => http.post('/gamification/checkin')
export const apiHealth = () => http.get('/common/health')
export const apiDatacenter = () => http.get('/common/datacenter')
export const apiSubmitAnswer = (payload) => http.post('/knowledge-graph/submit-answer', payload)
export const apiKnowledgeGraphFull = () => http.get('/knowledge-graph/full')
export const apiKnowledgeGraphProgress = () => http.get('/knowledge-graph/my-progress')
export const apiKnowledgeGraphNextRecommended = () => http.get('/knowledge-graph/next-recommended')
export const apiKpExercises = (kpId) => http.get(`/knowledge-graph/exercises/${kpId}`)

export const apiGamificationAddXp = (amount, reason) => http.post('/gamification/add-xp', { amount, reason })
export const apiGamificationLeaderboard = (limit = 10) => http.get('/gamification/leaderboard', { params: { limit } })
export const apiGamificationDailyChallenges = () => http.get('/gamification/daily-challenges')
export const apiAnalyticsDashboard = () => http.get('/analytics/dashboard')
export const apiGenerateAdaptiveQuiz = (payload) => http.post('/adaptive-quiz/generate', payload)
export const apiSubmitAdaptiveQuiz = (payload) => http.post('/adaptive-quiz/submit', payload)
export const apiAnalyzeMistake = (payload) => http.post('/adaptive-quiz/analyze-mistake', payload)
export const apiPrerequisiteChain = (kpId) => http.get(`/knowledge-graph/prerequisite-chain/${kpId}`)

// ==================== B站视频导入 ====================
export const apiBilibiliParse = (url) => http.post('/bilibili/parse', { url })
export const apiBilibiliSearch = (keyword, page = 1, pageSize = 10) => http.post('/bilibili/search', { keyword, page, pageSize })
export const apiBilibiliPlaylist = (url) => http.post('/bilibili/playlist', { url })
export const apiBilibiliImport = (bvids, autoGenerate = true) => http.post('/bilibili/import', { bvids, autoGenerate })
export const apiMyImports = () => http.get('/lessons/my-imports')

// ==================== Neo4j 知识图谱 ====================
export const apiGraphNeo4j = () => http.get('/graph/neo4j')
export const apiGraphSearchResource = (nodeName, subjectName = '') => http.post('/graph/search-resource', { nodeName, subjectName })

// ==================== 学习数据采集 ====================
export const apiExerciseSubmit = (data) => http.post('/exercise/submit', data)
export const apiStudyHeartbeat = (subChapterId, seconds) => http.post('/study/heartbeat', { subChapterId, seconds })

// ==================== AI 虚拟人伴学 ====================
/** 流式 AI 对话（Unity 虚拟人专用，SSE） */
export const apiAgentChatStream = ({ question, history }) => {
  const token = localStorage.getItem('sp_token') || ''
  return fetch('/api/agent/chat-stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ question, history: history || [] })
  })
}

/** 语音识别 */
export const apiSTT = (audioBlob) => {
  const formData = new FormData()
  formData.append('file', audioBlob, 'recording.wav')
  return axios.post('/api/agent/stt', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 语音合成 */
export const apiTTS = (text) => {
  const token = localStorage.getItem('sp_token') || ''
  return fetch('/api/agent/tts', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ text })
  })
}

// ==================== v3 课程章节体系 ====================
export const apiCourseChapters = (courseId) => http.get(`/courses/${courseId}/chapters`)
export const apiSubChapterDetail = (subChapterId) => http.get(`/sub-chapters/${subChapterId}`)
export const apiCreateChapter = (data) => http.post('/chapters', data)
export const apiUpdateChapter = (id, data) => http.put(`/chapters/${id}`, data)
export const apiDeleteChapter = (id) => http.delete(`/chapters/${id}`)
export const apiCreateSubChapter = (data) => http.post('/sub-chapters', data)
export const apiUpdateSubChapter = (id, data) => http.put(`/sub-chapters/${id}`, data)
export const apiDeleteSubChapter = (id) => http.delete(`/sub-chapters/${id}`)

export const apiChapterResources = (chapterId) => http.get(`/chapters/${chapterId}/resources`)
export const apiAddChapterResource = (chapterId, data) => http.post(`/chapters/${chapterId}/resources`, data)
export const apiDeleteResource = (id) => http.delete(`/resources/${id}`)
export const apiCourseResources = (courseId) => http.get(`/courses/${courseId}/resources`)

export const apiCourseAnnouncements = (courseId) => http.get(`/courses/${courseId}/announcements`)
export const apiCreateAnnouncement = (courseId, data) => http.post(`/courses/${courseId}/announcements`, data)

// ==================== v3 课程Q&A ====================
export const apiCourseQuestions = (courseId, page = 1, pageSize = 20) =>
  http.get(`/courses/${courseId}/questions`, { params: { page, pageSize } })
export const apiAskQuestion = (courseId, data) => http.post(`/courses/${courseId}/questions`, data)
export const apiQuestionAnswers = (questionId) => http.get(`/questions/${questionId}/answers`)
export const apiPostAnswer = (questionId, data) => http.post(`/questions/${questionId}/answers`, data)
export const apiAiAnswer = (questionId) => http.post(`/questions/${questionId}/ai-answer`)

// ==================== v3 课程CRUD ====================
export const apiCoursesBySubject = (subjectId) => http.get(`/subjects/${subjectId}/courses`)
export const apiCourseDetail = (courseId) => http.get(`/courses/${courseId}`)
export const apiCreateCourse = (data) => http.post('/courses', data)
export const apiUpdateCourse = (id, data) => http.put(`/courses/${id}`, data)

// ==================== v3 进度（适配SubChapter） ====================
export const apiSubChapterProgress = (subChapterId) => http.get(`/progress/sub-chapter/${subChapterId}`)
export const apiCompleteSubChapter = (subChapterId) => http.post(`/progress/sub-chapter/${subChapterId}/complete`)
export const apiCourseProgress = (courseId) => http.get(`/progress/course/${courseId}`)
