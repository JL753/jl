import http from './http'

/** 获取最近问答历史 */
export const apiGetQaHistory = (limit = 20) => http.get('/qa/recent', { params: { limit } })

/** 保存问答历史 */
export const apiSaveQaHistory = (payload) => http.post('/qa/save', payload)

/** 按会话ID获取问答历史 */
export const apiGetQaBySession = (sessionId) => http.get(`/qa/session/${sessionId}`)
