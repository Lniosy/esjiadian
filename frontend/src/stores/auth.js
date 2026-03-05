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
      this.setAuth(data)
      await this.fetchProfile()
    },
    async loginByCode(account, verifyCode) {
      const data = await authApi.loginCode({ account, verifyCode })
      this.setAuth(data)
      await this.fetchProfile()
    },
    async register(account, password, verifyCode, agreed) {
      await authApi.register({ account, password, verifyCode, agreed })
      // 注册后自动登录
      await this.login(account, password)
    },
    setAuth(data) {
      this.token = data.token
      this.userId = data.userId
      localStorage.setItem('token', data.token)
      localStorage.setItem('userId', String(data.userId))
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
