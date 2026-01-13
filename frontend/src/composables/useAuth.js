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
      
      // 检查响应格式
      if (!response.data || !response.data.data) {
        throw new Error('登录响应格式错误，请联系管理员')
      }
      
      // 检查 success 字段
      if (response.data.success === false) {
        const errorMsg = response.data.message || '登录失败'
        throw new Error(errorMsg)
      }
      
      const { token: newToken, user: userData } = response.data.data
      
      if (!newToken) {
        throw new Error('登录成功但未返回登录凭证，请联系管理员')
      }
      
      // 保存 token 和用户信息
      token.value = newToken
      user.value = userData
      isAuthenticated.value = true
      localStorage.setItem('token', newToken)
      
      return { token: newToken, user: userData }
    } catch (error) {
      // 处理错误响应
      if (error.response?.data) {
        const responseData = error.response.data
        let errorMessage = '登录失败，请检查账号密码'
        
        if (responseData.message) {
          errorMessage = responseData.message
        } else if (responseData.error) {
          errorMessage = responseData.error
        }
        
        // 提供更友好的错误提示
        if (errorMessage.includes('invalid username or password') ||
            errorMessage.includes('invalid credentials')) {
          errorMessage = '账号或密码错误，请重新输入'
        }
        
        throw new Error(errorMessage)
      }
      
      throw new Error(error.message || '登录失败，请检查账号密码')
    }
  }

  /**
   * 注册
   * @param {string} username - 用户名
   * @param {string} password - 密码
   */
  const register = async (username, password) => {
    try {
      console.log('📝 开始注册请求:', { username })
      const response = await api.post('/auth/register', {
        username,
        password
      })
      
      console.log('✅ 注册响应:', response.data)
      
      // 检查响应格式
      // 后端返回格式: { success: true, message: "OK", data: { token: "...", user: {...} } }
      if (!response.data) {
        console.error('❌ 注册响应为空')
        throw new Error('注册响应格式错误，请联系管理员')
      }
      
      // 检查 success 字段
      if (response.data.success === false) {
        const errorMsg = response.data.message || '注册失败'
        console.error('❌ 注册失败:', errorMsg)
        throw new Error(errorMsg)
      }
      
      // 检查 data 字段
      if (!response.data.data) {
        console.error('❌ 注册响应中缺少 data 字段:', response.data)
        throw new Error('注册响应格式错误，请联系管理员')
      }
      
      const { token: newToken, user: userData } = response.data.data
      
      if (!newToken) {
        console.error('❌ 注册响应中缺少 token:', response.data)
        throw new Error('注册成功但未返回登录凭证，请联系管理员')
      }
      
      // 保存 token 和用户信息
      token.value = newToken
      user.value = userData
      isAuthenticated.value = true
      localStorage.setItem('token', newToken)
      
      return { token: newToken, user: userData }
    } catch (error) {
      // 详细错误日志
      console.error('❌ 注册失败:', error)
      console.error('   错误类型:', error.name)
      console.error('   错误消息:', error.message)
      
      if (error.response) {
        // 后端返回了响应
        console.error('   响应状态:', error.response.status)
        console.error('   响应数据:', JSON.stringify(error.response.data, null, 2))
        console.error('   完整错误对象:', error.response)
        
        // 后端返回的格式是 ApiResponse，可能是：
        // { success: false, message: "...", code: "...", data: null }
        const responseData = error.response.data
        let errorMessage = '注册失败，请稍后重试'
        
        if (responseData) {
          // 优先使用后端返回的 message
          if (responseData.message) {
            errorMessage = responseData.message
          } else if (responseData.error) {
            errorMessage = responseData.error
          } else if (typeof responseData === 'string') {
            errorMessage = responseData
          }
          
          // 如果是用户名已存在，提供更友好的提示
          if (errorMessage.includes('username already exists') || 
              errorMessage.includes('用户名已存在') ||
              errorMessage.includes('already exists')) {
            errorMessage = '用户名已存在，请更换其他用户名'
          }
          
          // 如果是验证错误，提供更友好的提示
          if (errorMessage.includes('username is required') || 
              errorMessage.includes('password is required')) {
            errorMessage = '账号和密码不能为空'
          }
          
          // 如果是内部服务器错误，提供更友好的提示
          if (errorMessage.includes('Internal server error') ||
              errorMessage.includes('Failed to register user') ||
              error.response.status === 500) {
            // 检查是否是数据库相关错误
            const fullError = JSON.stringify(responseData)
            if (fullError.includes('table') || fullError.includes('SQL') || fullError.includes('database')) {
              errorMessage = '数据库错误：可能是users表不存在，请联系后端开发人员检查数据库'
            } else {
              // 500错误可能是多种原因，提供更具体的提示
              errorMessage = '服务器内部错误，可能的原因：\n' +
                           '1. 数据库连接失败\n' +
                           '2. users表不存在\n' +
                           '3. 数据库权限问题\n\n' +
                           '请检查后端日志或联系管理员'
            }
            
            // 在开发环境下显示更详细的错误信息
            if (import.meta.env.DEV) {
              console.error('🔍 详细错误信息:', {
                status: error.response.status,
                statusText: error.response.statusText,
                data: responseData,
                headers: error.response.headers,
                requestUrl: error.config?.url,
                requestMethod: error.config?.method,
                requestData: error.config?.data
              })
              console.error('💡 提示: 请检查后端控制台的错误日志，查看具体的异常堆栈信息')
            }
          }
        } else {
          // 如果没有响应数据，根据状态码提供默认消息
          if (error.response.status === 500) {
            errorMessage = '服务器内部错误，请稍后重试或联系管理员'
          } else if (error.response.status === 400) {
            errorMessage = '请求参数错误，请检查输入'
          } else if (error.response.status === 409) {
            errorMessage = '用户名已存在，请更换其他用户名'
          }
        }
        
        throw new Error(errorMessage)
      } else if (error.request) {
        // 请求已发出但没有收到响应（网络错误）
        console.error('   网络错误: 请求已发出但未收到响应')
        console.error('   请检查后端服务是否运行')
        throw new Error('无法连接到服务器，请检查网络连接或联系管理员')
      } else {
        // 其他错误
        console.error('   其他错误:', error.message)
        throw new Error(error.message || '注册失败，请稍后重试')
      }
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
    register,
    logout,
    getCurrentUser
  }
}