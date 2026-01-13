import axios from 'axios'

// 创建 axios 实例
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：添加 token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：处理错误
api.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    // 网络连接错误（如后端未启动），不处理，直接返回错误
    // 这样调用方可以自行决定如何处理
    if (!error.response) {
      // 网络错误，可能是后端未启动或网络问题
      console.warn('API 请求失败，可能是后端服务未启动:', error.message)
      return Promise.reject(error)
    }
    
    // 401 未授权，清除 token 并跳转到登录
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      // 可以在这里触发登录弹窗
      window.dispatchEvent(new CustomEvent('auth:unauthorized'))
    }
    return Promise.reject(error)
  }
)

export { api }