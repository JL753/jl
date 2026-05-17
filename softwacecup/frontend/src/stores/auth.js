import { defineStore } from 'pinia'
import { apiLogin, apiRegister, apiMe, apiUpdateMe } from '../api/index'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('sp_token') || '',
    user: null,
    showLoginModal: false,
    showRegisterModal: false,
    pendingRedirect: null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token && !!state.user,
    userRole: (state) => state.user?.role || null
  },

  actions: {
    async login(form) {
      const data = await apiLogin(form)
      this.applyAuth(data)
    },

    async register(form) {
      const data = await apiRegister(form)
      this.applyAuth(data)
    },

    async fetchMe() {
      const res = await apiMe()
      this.user = res.data || res.user || res
    },

    async updateMe(form) {
      const data = await apiUpdateMe(form)
      this.user = data.user || data
    },

    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('sp_token')
    },

    applyAuth(data) {
      const inner = data.data || data
      const token = inner.token || data.token
      let user = data.user || inner.user || null
      // LoginResponse 是扁平结构 {token, userId, username, role, displayName}
      if (!user && inner.username) {
        user = {
          id: inner.userId || inner.id,
          username: inner.username,
          role: inner.role,
          displayName: inner.displayName || inner.username
        }
      }
      if (token) {
        this.token = token
        localStorage.setItem('sp_token', token)
        this.user = user
      }
    },

    openLoginModal(redirectTo = null) {
      this.pendingRedirect = redirectTo
      this.showLoginModal = true
      this.showRegisterModal = false
    },

    openRegisterModal(redirectTo = null) {
      this.pendingRedirect = redirectTo
      this.showRegisterModal = true
      this.showLoginModal = true
    },

    closeLoginModal() {
      this.showLoginModal = false
      this.showRegisterModal = false
      this.pendingRedirect = null
    }
  }
})
