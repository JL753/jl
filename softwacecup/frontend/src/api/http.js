import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('sp_token')
  const isAuthPath = config.url?.includes('/auth/login') || config.url?.includes('/auth/register')
  if (token && !isAuthPath) {
    config.headers.Authorization = `Bearer ${token}`
  } else if (config.headers?.Authorization) {
    delete config.headers.Authorization
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    if (resp.data?.success === false) {
      return Promise.reject(new Error(resp.data.message || '请求失败'))
    }
    return resp.data
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('sp_token')
      window.location.href = '/login'
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    const message = err.response?.data?.message || err.message || '网络请求失败'
    return Promise.reject(new Error(message))
  }
)

export default http
