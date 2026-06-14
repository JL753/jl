import { defineStore } from 'pinia'
import { apiGetQaHistory } from '../api/qa'

function genSessionId() {
  // crypto.randomUUID 在现代浏览器均可用
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  // fallback
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 10)
}

function defaultWelcome() {
  return {
    role: 'assistant',
    content: '你好！我是知域 AI 智能助手\n\n我可以回答学习问题，也可以帮你导航到对应页面。\n\n试试说：「跳转到课程平台」、「沉浸伴学」或直接提问！',
    html: '你好！我是知域 AI 智能助手<br><br>我可以回答学习问题，也可以帮你导航到对应页面。<br><br>试试说：「跳转到课程平台」、「沉浸伴学」或直接提问！',
    timestamp: ''
  }
}

export const useChatStore = defineStore('chat', {
  state: () => ({
    sessionId: genSessionId(),
    messages: [defaultWelcome()],
    historyLoaded: false,
    loading: false
  }),

  getters: {
    /** 最近 N 条消息作为 LLM 上下文 */
    recentContext: (state) => {
      return state.messages.slice(-6).map(m => ({
        role: m.role === 'assistant' ? 'assistant' : 'user',
        content: m.content
      }))
    }
  },

  actions: {
    /** 从后端加载最近历史并合并到消息列表 */
    async loadHistory() {
      if (this.historyLoaded) return
      try {
        const res = await apiGetQaHistory(20)
        const records = res?.data || res || []
        if (Array.isArray(records) && records.length > 0) {
          const historyMsgs = []
          // 后端返回按时间倒序，反转成时间正序
          const sorted = [...records].reverse()
          for (const r of sorted) {
            const simpleHtml = (text) => text ? text.replace(/\n/g, '<br>') : ''
            historyMsgs.push({
              role: 'user',
              content: r.question,
              html: simpleHtml(r.question),
              timestamp: r.createdAt ? r.createdAt.substring(11, 16) : ''
            })
            historyMsgs.push({
              role: 'assistant',
              content: r.answer,
              html: simpleHtml(r.answer),
              timestamp: r.createdAt ? r.createdAt.substring(11, 16) : ''
            })
          }
          // 使用最近一次会话的 sessionId
          if (records[0]?.sessionId) {
            this.sessionId = records[0].sessionId
          }
          // 替换初始欢迎消息为历史记录（保留欢迎消息作为最后一条）
          this.messages = [...historyMsgs, defaultWelcome()]
        }
      } catch (e) {
        console.warn('加载聊天历史失败:', e)
      } finally {
        this.historyLoaded = true
      }
    },

    /** 追加用户消息 */
    addUserMessage(content) {
      this.messages.push({ role: 'user', content, timestamp: '' })
    },

    /** 追加 AI 消息 */
    addAssistantMessage(content, html) {
      this.messages.push({
        role: 'assistant',
        content,
        html: html || content.replace(/\n/g, '<br>'),
        timestamp: ''
      })
    },

    /** 刷新会话 ID（新对话时调用） */
    newSession() {
      this.sessionId = genSessionId()
      this.messages = [defaultWelcome()]
      this.historyLoaded = true
    },

    /** 清空消息（保留会话ID） */
    clearMessages() {
      this.messages = [defaultWelcome()]
    },

    /** 替换欢迎消息（仅在无历史记录时生效） */
    replaceWelcome(content, html) {
      if (this.messages.length === 1 && this.messages[0].role === 'assistant') {
        this.messages[0] = { role: 'assistant', content, html, timestamp: '' }
      }
    }
  }
})
