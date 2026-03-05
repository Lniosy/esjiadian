import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/',
  timeout: 10000
})

// 简易防抖，避免同一时间弹出多个相同的限流提示
let lastErrorTime = 0
let lastErrorMessage = ''
const showError = (msg) => {
  const now = Date.now()
  if (now - lastErrorTime < 2000 && msg === lastErrorMessage) return
  lastErrorTime = now
  lastErrorMessage = msg
  ElMessage.error(msg)
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body.code === 429) {
      showError('请求过于频繁，请稍后再试')
      return Promise.reject(new Error('Rate limited'))
    }
    if (body.code !== 200) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body.data
  },
  (error) => {
    if (error.response?.status === 429) {
      showError('请求过于频繁，请稍后再试')
    } else {
      // 其他错误不需要防抖，但也建议统一提示逻辑
      const msg = error.response?.data?.message || error.message || '网络异常'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default http
