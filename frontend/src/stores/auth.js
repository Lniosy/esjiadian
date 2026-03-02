import { defineStore } from 'pinia'
import { authApi, userApi } from '../api/modules'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: Number(localStorage.getItem('userId') || 0),
    profile: null
  }),
  actions: {
    async login(account, password) {
      const data = await authApi.loginPassword({ account, password })
      this.token = data.token
      this.userId = data.userId
      localStorage.setItem('token', data.token)
      localStorage.setItem('userId', String(data.userId))
      await this.fetchProfile()
    },
    async fetchProfile() {
      if (!this.token) return
      try {
        this.profile = await userApi.me()
      } catch (e) {
        // ignore or handle error
      }
    },
    logout() {
      this.token = ''
      this.userId = 0
      this.profile = null
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
    }
  }
})
