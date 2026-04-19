import { defineStore } from 'pinia'
import { apiLogin, apiMe, apiRegister, apiUpdateMe } from '../api/index.js'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('sp_token') || '',
    user: null
  }),
  actions: {
    async login(form) {
      const res = await apiLogin(form)
      this.applyAuth(res.data)
    },
    async register(form) {
      const res = await apiRegister(form)
      this.applyAuth(res.data)
    },
    async fetchMe() {
      if (!this.token) return
      const res = await apiMe()
      this.user = res.data
    },
    async updateMe(form) {
      const res = await apiUpdateMe(form)
      this.user = res.data
      return res.data
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('sp_token')
    },
    applyAuth(data) {
      this.token = data.token
      this.user = {
        userId: data.userId,
        username: data.username,
        role: data.role,
        displayName: data.displayName
      }
      localStorage.setItem('sp_token', this.token)
    }
  }
})
