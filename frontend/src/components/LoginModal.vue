<template>
  <div v-if="isVisible" class="login-modal-overlay" @click.self="handleClose">
    <div class="login-modal">
      <button class="close-btn" @click="handleClose">×</button>
      
      <!-- 登录表单 -->
      <div v-if="mode === 'login'" class="login-form">
        <h2 class="modal-title">登录</h2>
        <p class="modal-subtitle">登录后推荐更懂你的内容</p>
        
        <form @submit.prevent="handleLogin">
          <div class="form-group">
            <label>账号</label>
            <input
              v-model="loginForm.username"
              type="text"
              placeholder="请输入账号"
              required
              class="form-input"
            />
          </div>
          
          <div class="form-group">
            <label>密码</label>
            <input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              required
              class="form-input"
            />
          </div>
          
          <div v-if="loginError" class="error-message">
            {{ loginError }}
          </div>
          
          <button type="submit" class="submit-btn" :disabled="isLoading">
            {{ isLoading ? '登录中...' : '登录' }}
          </button>
        </form>
        
        <div class="modal-footer">
          <span>还没有账号？</span>
          <a href="#" @click.prevent="switchToRegister">立即注册</a>
        </div>
      </div>
      
      <!-- 注册表单 -->
      <div v-else class="register-form">
        <h2 class="modal-title">注册</h2>
        <p class="modal-subtitle">新用户可直接注册</p>
        
        <form @submit.prevent="handleRegister">
          <div class="form-group">
            <label>账号</label>
            <input
              v-model="registerForm.username"
              type="text"
              placeholder="请输入账号（3-20个字符）"
              required
              minlength="3"
              maxlength="20"
              class="form-input"
            />
          </div>
          
          <div class="form-group">
            <label>密码</label>
            <input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码（至少6个字符）"
              required
              minlength="6"
              class="form-input"
            />
          </div>
          
          <div class="form-group">
            <label>确认密码</label>
            <input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              required
              class="form-input"
            />
          </div>
          
          <div v-if="registerError" class="error-message">
            {{ registerError }}
          </div>
          
          <button type="submit" class="submit-btn" :disabled="isLoading">
            {{ isLoading ? '注册中...' : '注册' }}
          </button>
        </form>
        
        <div class="modal-footer">
          <span>已有账号？</span>
          <a href="#" @click.prevent="switchToLogin">立即登录</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAuth } from '../composables/useAuth'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'close'])

const { login } = useAuth()

// 模式：'login' 或 'register'，默认登录
const mode = ref('login')

// 登录表单
const loginForm = ref({
  username: '',
  password: ''
})

// 注册表单
const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: ''
})

// 错误信息
const loginError = ref('')
const registerError = ref('')

// 加载状态
const isLoading = ref(false)

// 计算属性：是否显示
const isVisible = computed(() => props.visible)

// 切换到注册模式
const switchToRegister = () => {
  mode.value = 'register'
  loginError.value = ''
  registerError.value = ''
}

// 切换到登录模式
const switchToLogin = () => {
  mode.value = 'login'
  loginError.value = ''
  registerError.value = ''
}

// 关闭弹窗
const handleClose = () => {
  emit('update:visible', false)
  emit('close')
  // 重置表单
  loginForm.value = { username: '', password: '' }
  registerForm.value = {
    username: '',
    password: '',
    confirmPassword: ''
  }
  loginError.value = ''
  registerError.value = ''
  mode.value = 'login' // 重置为登录模式
}

// 处理登录
const handleLogin = async () => {
  loginError.value = ''
  isLoading.value = true
  
  try {
    await login(loginForm.value.username, loginForm.value.password)
    handleClose()
  } catch (error) {
    loginError.value = error.message || '登录失败，请检查账号密码'
  } finally {
    isLoading.value = false
  }
}

// 处理注册（暂时只显示错误提示，因为后端还没有注册接口）
const handleRegister = async () => {
  registerError.value = ''
  
  // 验证密码
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    registerError.value = '两次输入的密码不一致'
    return
  }
  
  if (registerForm.value.password.length < 6) {
    registerError.value = '密码长度至少为6个字符'
    return
  }
  
  if (registerForm.value.username.length < 3) {
    registerError.value = '账号长度至少为3个字符'
    return
  }
  
  // 注册功能需要后端支持，这里暂时提示
  registerError.value = '注册功能暂未开放，请联系管理员'
}
</script>

<style scoped>
.login-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.login-modal {
  background: white;
  border-radius: 16px;
  padding: 32px;
  width: 90%;
  max-width: 420px;
  position: relative;
  animation: slideUp 0.3s ease;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f5f5f5;
  color: #333;
}

.modal-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
  text-align: center;
}

.modal-subtitle {
  font-size: 14px;
  color: #999;
  margin: 0 0 24px 0;
  text-align: center;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  font-weight: 500;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary, #c62828);
  box-shadow: 0 0 0 3px rgba(198, 40, 40, 0.1);
}

.error-message {
  color: #ff4444;
  font-size: 14px;
  margin-bottom: 16px;
  text-align: center;
}

.submit-btn {
  width: 100%;
  padding: 14px;
  background: var(--color-primary, #c62828);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 20px;
}

.submit-btn:hover:not(:disabled) {
  background: var(--color-primary-dark, #8e0000);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(198, 40, 40, 0.3);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-footer {
  text-align: center;
  font-size: 14px;
  color: #666;
  margin-top: 16px;
}

.modal-footer a {
  color: var(--color-primary, #c62828);
  text-decoration: none;
  margin-left: 4px;
  font-weight: 500;
}

.modal-footer a:hover {
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-modal {
    padding: 24px;
    width: 95%;
  }
  
  .modal-title {
    font-size: 20px;
  }
}
</style>