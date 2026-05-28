import http from './http'

// ==================== 学习路径 ====================
export const apiGeneratePath = (payload) => http.post('/path/generate', payload)
export const apiMyPath = () => http.get('/path/mine')

// ==================== 评估 ====================
export const apiEvaluate = (payload) => http.post('/assessment/evaluate', payload)
export const apiMyAssessment = () => http.get('/assessment/mine')

// ==================== AI辅导 ====================
export const apiAskTutor = (payload) => http.post('/tutor/ask', payload)

// ==================== 自适应测验 ====================
export const apiGenerateAdaptiveQuiz = (payload) => http.post('/adaptive-quiz/generate', payload)
export const apiSubmitAdaptiveQuiz = (payload) => http.post('/adaptive-quiz/submit', payload)
export const apiAnalyzeMistake = (payload) => http.post('/adaptive-quiz/analyze-mistake', payload)

// ==================== AI 虚拟人伴学 ====================
/** 流式 AI 对话（SSE） */
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
  return fetch('/api/agent/stt', {
    method: 'POST',
    headers: { Authorization: `Bearer ${localStorage.getItem('sp_token') || ''}` },
    body: formData
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
