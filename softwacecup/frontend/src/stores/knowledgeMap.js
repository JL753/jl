import { defineStore } from 'pinia'

// 知识域定义（与课程平台对应）
export const KNOWLEDGE_DOMAINS = [
  { id: 'ds', name: '数据结构与算法', icon: '🌲', domain: '算法域', color: '#10b981', guideName: '结构向导' },
  { id: 'ml', name: '机器学习基础', icon: '🤖', domain: '智能域', color: '#8b5cf6', guideName: 'ML向导' },
  { id: 'web', name: 'Web全栈开发', icon: '🌐', domain: '应用域', color: '#3b82f6', guideName: 'Web向导' },
  { id: 'os', name: '操作系统', icon: '💻', domain: '系统域', color: '#f59e0b', guideName: '系统向导' },
  { id: 'cn', name: '计算机网络', icon: '🔗', domain: '系统域', color: '#ec4899', guideName: '网络向导' },
  { id: 'db', name: '数据库原理', icon: '🗄️', domain: '数据域', color: '#06b6d4', guideName: '数据向导' },
]

export const useKnowledgeMapStore = defineStore('knowledgeMap', {
  state: () => ({
    // 研习档案列表 { id, courseId, courseName, guideName, icon, color, domain, createdAt, status }
    archives: JSON.parse(localStorage.getItem('km_archives') || '[]'),
    // 当前激活的档案 id
    currentArchiveId: null,
    // 聊天消息 { [archiveId]: [{role, content}] }
    messages: JSON.parse(localStorage.getItem('km_messages') || '{}'),
    // 版图节点状态 { [courseId]: 'lit' | 'active' | 'fog' }
    mapStatus: JSON.parse(localStorage.getItem('km_mapStatus') || '{}'),
    // 收入版图的知识点列表
    mapTopics: JSON.parse(localStorage.getItem('km_topics') || '[]'),
  }),

  getters: {
    litCount: (state) => Object.values(state.mapStatus).filter(s => s === 'lit').length,
    activeCount: (state) => Object.values(state.mapStatus).filter(s => s === 'active').length,
    currentArchive: (state) => state.archives.find(a => a.id === state.currentArchiveId),
    getArchiveByCourse: (state) => (courseId) => state.archives.find(a => a.courseId === courseId),
    getDomainStatus: (state) => (courseId) => state.mapStatus[courseId] || 'fog',
  },

  actions: {
    // 创建或激活研习档案
    createOrActivateArchive(course) {
      let archive = this.archives.find(a => a.courseId === course.id)
      const isNew = !archive
      if (isNew) {
        archive = {
          id: 'arc_' + Date.now(),
          courseId: course.id,
          courseName: course.name,
          guideName: course.guideName || (course.name + '向导'),
          icon: course.icon,
          color: course.color,
          domain: course.domain,
          createdAt: new Date().toISOString(),
          status: '研习中',
        }
        this.archives.push(archive)
        this.messages[archive.id] = [{
          role: 'guide',
          content: `你好！我是 **${archive.guideName}**，你的《${course.name}》专属学习向导。\n\n我可以帮你：\n1. 梳理课程知识框架\n2. 生成针对性练习题\n3. 解释复杂概念与代码\n4. 关联其他课程的知识点\n\n今天我们从哪里开始？`,
        }]
        // 更新版图状态
        if (!this.mapStatus[course.id]) {
          this.mapStatus[course.id] = 'active'
        }
      }
      this.currentArchiveId = archive.id
      this._persist()
      return { archive, isNew }
    },

    // 标记领域为已点亮
    lightUpDomain(courseId) {
      this.mapStatus[courseId] = 'lit'
      this._persist()
    },

    // 添加消息
    addMessage(archiveId, role, content) {
      if (!this.messages[archiveId]) this.messages[archiveId] = []
      this.messages[archiveId].push({ role, content })
      this._persist()
    },

    // 收入版图
    addToMapTopics(topic) {
      if (!this.mapTopics.includes(topic)) {
        this.mapTopics.push(topic)
        this._persist()
      }
    },

    _persist() {
      localStorage.setItem('km_archives', JSON.stringify(this.archives))
      localStorage.setItem('km_messages', JSON.stringify(this.messages))
      localStorage.setItem('km_mapStatus', JSON.stringify(this.mapStatus))
      localStorage.setItem('km_topics', JSON.stringify(this.mapTopics))
    },
  },
})
