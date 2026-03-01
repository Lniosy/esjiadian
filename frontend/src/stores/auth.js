import { defineStore } from 'pinia'
import { authApi } from '../api/modules'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: Number(localStorage.getItem('userId') || 0)
  }),
  actions: {
    async login(account, password) {
      const data = await authApi.loginPassword({ account, password })
      this.token = data.token
      this.userId = data.userId
      localStorage.setItem('token', data.token)
      localStorage.setItem('userId', String(data.userId))
    },
    logout() {
      this.token = ''
      this.userId = 0
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
    }
  }
})
