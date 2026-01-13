import { ref } from 'vue'
import { api } from '../services/api'

// 全局用户状态
const user = ref(null)
const token = ref(localStorage.getItem('token') || null)
const isAuthenticated = ref(!!token.value)

// 初始化时尝试获取用户信息
if (token.value) {
  api.get('/auth/me')
    .then(response => {
      user.value = response.data.data
      isAuthenticated.value = true
    })
    .catch((error) => {
      // 网络错误（后端未启动）或 token 无效，静默处理
      // 如果是网络错误（如 -102），不清除 token，让用户继续使用
      // 只有在明确是 401 错误时才清除 token
      if (error.response?.status === 401) {
        localStorage.removeItem('token')
        token.value = null
        isAuthenticated.value = false
      }
      // 其他错误（如网络连接失败）不处理，保持当前状态
    })
}

export function useAuth() {
  /**
   * 登录
   * @param {string} username - 用户名
   * @param {string} password - 密码
   */
  const login = async (username, password) => {
    try {
      const response = await api.post('/auth/login', {
        username,
        password
      })
      
      const { token: newToken, user: userData } = response.data.data
      
      // 保存 token 和用户信息
      token.value = newToken
      user.value = userData
      isAuthenticated.value = true
      localStorage.setItem('token', newToken)
      
      return { token: newToken, user: userData }
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '登录失败，请检查账号密码'
      )
    }
  }

  /**
   * 登出
   */
  const logout = () => {
    token.value = null
    user.value = null
    isAuthenticated.value = false
    localStorage.removeItem('token')
  }

  /**
   * 获取当前用户信息
   */
  const getCurrentUser = async () => {
    if (!token.value) {
      return null
    }
    
    try {
      const response = await api.get('/auth/me')
      user.value = response.data.data
      return response.data.data
    } catch (error) {
      logout()
      throw error
    }
  }

  return {
    user,
    token,
    isAuthenticated,
    login,
    logout,
    getCurrentUser
  }
}