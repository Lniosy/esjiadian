import axios from 'axios'

const http = axios.create({
  baseURL: '/',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use((resp) => {
  const body = resp.data
  if (body.code !== 200) {
    return Promise.reject(new Error(body.message || '请求失败'))
  }
  return body.data
})

export default http
