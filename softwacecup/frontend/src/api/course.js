import http from './http'

// ==================== 学科 & 课程 ====================
export const apiSubjects = () => http.get('/subjects')
export const apiCoursesBySubject = (subjectId) => http.get(`/subjects/${subjectId}/courses`)
export const apiCourseDetail = (courseId) => http.get(`/courses/${courseId}`)
export const apiCreateCourse = (data) => http.post('/courses', data)
export const apiUpdateCourse = (id, data) => http.put(`/courses/${id}`, data)

export const apiSubjectTree = (courseId) => http.get('/course/tree', { params: { courseId } })
export const apiPortal = () => http.get('/common/portal')
export const apiHealth = () => http.get('/common/health')
export const apiDatacenter = () => http.get('/common/datacenter')

// ==================== 章节体系 ====================
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

// ==================== Q&A ====================
export const apiCourseQuestions = (courseId, page = 1, pageSize = 20) =>
  http.get(`/courses/${courseId}/questions`, { params: { page, pageSize } })
export const apiAskQuestion = (courseId, data) => http.post(`/courses/${courseId}/questions`, data)
export const apiQuestionAnswers = (questionId) => http.get(`/questions/${questionId}/answers`)
export const apiPostAnswer = (questionId, data) => http.post(`/questions/${questionId}/answers`, data)
export const apiAiAnswer = (questionId) => http.post(`/questions/${questionId}/ai-answer`)

// ==================== 进度 ====================
export const apiSubChapterProgress = (subChapterId) => http.get(`/progress/sub-chapter/${subChapterId}`)
export const apiCompleteSubChapter = (subChapterId) => http.post(`/progress/sub-chapter/${subChapterId}/complete`)
export const apiCourseProgress = (courseId) => http.get(`/progress/course/${courseId}`)
