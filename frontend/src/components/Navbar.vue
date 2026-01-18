<template>
    <nav class="navbar">
      <div class="navbar-container">
        <!-- Logo区域 -->
        <div class="navbar-brand">
          <router-link to="/" class="brand-link">
            <div class="brand-logo">
              <svg class="logo-icon" viewBox="0 0 24 24" fill="none">
                <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="brand-text">
              <h1 class="brand-name">邮觅红途</h1>
              <p class="brand-tagline">红色文化旅游系统</p>
            </div>
          </router-link>
        </div>
  
        <!-- 导航菜单 -->
        <div class="navbar-menu">
          <router-link 
            v-for="item in navItems" 
            :key="item.path"
            :to="item.path" 
            class="nav-link"
            :class="{ active: $route.path === item.path }"
          >
            <span class="nav-text">{{ item.name }}</span>
          </router-link>
        </div>
  
        <!-- 用户操作区域 -->
        <div class="navbar-actions">
          <div v-if="isAuthenticated && user" class="user-info">
            <div 
              class="user-avatar" 
              :style="avatarStyle"
              @click="showUserMenu = !showUserMenu"
              :title="user.username"
            >
            </div>
            <div v-if="showUserMenu" class="user-dropdown">
              <div class="dropdown-item" @click="goToUserCenter">
                <span class="dropdown-icon">👤</span>
                <span>用户中心</span>
              </div>
              <div class="dropdown-item" @click="handleLogout">
                <span class="dropdown-icon">🚪</span>
                <span>退出登录</span>
              </div>
            </div>
          </div>
          <div v-else class="auth-actions">
            <button @click="showLoginModal = true" class="btn btn-outline">登录</button>
          </div>
        </div>
      </div>
    </nav>
    
    <!-- 登录弹窗 -->
    <LoginModal v-model:visible="showLoginModal" @close="showLoginModal = false" />
  </template>
  
<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import LoginModal from './LoginModal.vue'

const route = useRoute()
const router = useRouter()
const { user, isAuthenticated, logout } = useAuth()

const showUserMenu = ref(false)
const showLoginModal = ref(false)

// 导航项
const navItems = ref([
  { path: '/', name: '首页' },
  { path: '/recommend', name: '旅游推荐' },
  { path: '/route', name: '路线规划' },
  { path: '/places', name: '场所查询' },
  { path: '/diary', name: '旅游日记' }
])

// 获取用户首字母
const getUserInitial = () => {
  if (!user.value || !user.value.username) return '👤'
  return user.value.username.charAt(0).toUpperCase()
}

// 默认头像路径
const DEFAULT_AVATAR = '/生成系统头像.png'

// 头像样式
const avatarStyle = computed(() => {
  let avatarUrl = DEFAULT_AVATAR
  if (user.value?.avatar) {
    const rawAvatar = user.value.avatar
    // 如果是相对路径，需要加上API基础URL
    if (rawAvatar.startsWith('/uploads/')) {
      const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
      avatarUrl = apiBase.replace(/\/api\/?$/, '') + rawAvatar
    } else if (rawAvatar.startsWith('http://') || rawAvatar.startsWith('https://')) {
      avatarUrl = rawAvatar
    } else {
      // 其他情况，尝试作为相对路径处理
      avatarUrl = rawAvatar
    }
  }
  return {
    backgroundImage: `url(${avatarUrl})`,
    backgroundSize: 'cover',
    backgroundPosition: 'center',
    backgroundColor: '#f0f0f0', // 添加背景色，避免白色背景时看不到
    color: 'transparent'
  }
})

// 跳转到用户中心
const goToUserCenter = () => {
  showUserMenu.value = false
  router.push('/user/center')
}

// 处理登出
const handleLogout = () => {
  logout()
  showUserMenu.value = false
  showLoginModal.value = true
}

// 点击外部关闭菜单
const handleClickOutside = (event) => {
  if (!event.target.closest('.user-info')) {
    showUserMenu.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>
  
  <style scoped>
  .navbar {
    background: linear-gradient(135deg, var(--color-primary-dark) 0%, var(--color-primary) 100%);
    color: white;
    box-shadow: var(--shadow-md);
    position: sticky;
    top: 0;
    z-index: 1000;
  }
  
  .navbar-container {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 var(--spacing-6);
    height: 70px;
  }
  
  /* Logo区域 */
  .navbar-brand .brand-link {
    display: flex;
    align-items: center;
    gap: var(--spacing-4);
    text-decoration: none;
    color: white;
  }
  
  .brand-logo {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .logo-icon {
    width: 100%;
    height: 100%;
    color: white;
  }
  
  .brand-text {
    display: flex;
    flex-direction: column;
  }
  
  .brand-name {
    font-size: var(--font-size-xl);
    font-weight: 700;
    line-height: 1;
    margin: 0;
  }
  
  .brand-tagline {
    font-size: var(--font-size-xs);
    opacity: 0.9;
    margin: var(--spacing-1) 0 0;
  }
  
  /* 导航菜单 */
  .navbar-menu {
    display: flex;
    gap: var(--spacing-1);
    background: rgba(255, 255, 255, 0.1);
    border-radius: var(--radius-lg);
    padding: var(--spacing-1);
  }
  
  .nav-link {
    padding: var(--spacing-3) var(--spacing-6);
    border-radius: var(--radius-md);
    text-decoration: none;
    color: rgba(255, 255, 255, 0.9);
    font-weight: 500;
    transition: all var(--transition-fast);
    display: flex;
    align-items: center;
    gap: var(--spacing-2);
  }
  
  .nav-link:hover {
    background: rgba(255, 255, 255, 0.15);
    color: white;
  }
  
  .nav-link.active {
    background: white;
    color: var(--color-primary);
  }
  
  /* 用户操作区域 */
  .navbar-actions {
    display: flex;
    align-items: center;
  }
  
  .user-info {
    position: relative;
    display: flex;
    align-items: center;
  }
  
  .user-avatar {
    width: 40px;
    height: 40px;
    background: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--color-primary);
    font-weight: bold;
    font-size: var(--font-size-lg);
    cursor: pointer;
    transition: transform 0.2s;
  }
  
  .user-avatar:hover {
    transform: scale(1.1);
  }
  
  .user-dropdown {
    position: absolute;
    top: calc(100% + 8px);
    right: 0;
    background: white;
    border-radius: var(--radius-md);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    min-width: 160px;
    z-index: 1000;
    overflow: hidden;
  }
  
  .dropdown-item {
    display: flex;
    align-items: center;
    gap: var(--spacing-2);
    padding: var(--spacing-3) var(--spacing-4);
    cursor: pointer;
    transition: background 0.2s;
    color: var(--color-text);
    font-size: var(--font-size-sm);
  }
  
  .dropdown-item:hover {
    background: var(--color-bg);
  }
  
  .dropdown-icon {
    font-size: var(--font-size-base);
  }
  
  /* 认证按钮 */
  .auth-actions {
    display: flex;
    gap: var(--spacing-3);
  }
  
  .btn {
    padding: var(--spacing-2) var(--spacing-4);
    border-radius: var(--radius-md);
    font-weight: 500;
    text-decoration: none;
    font-size: var(--font-size-sm);
  }
  
  .btn-outline {
    background: transparent;
    border: 2px solid white;
    color: white;
  }
  
  .btn-outline:hover {
    background: white;
    color: var(--color-primary);
  }
  
  .btn-primary {
    background: white;
    color: var(--color-primary);
    border: none;
  }
  
  .btn-primary:hover {
    background: #f8f9fa;
    transform: translateY(-1px);
  }
  
  /* 响应式设计 */
  @media (max-width: 768px) {
    .navbar-container {
      padding: 0 var(--spacing-4);
    }
    
    .navbar-menu {
      display: none;
    }
    
    .brand-tagline {
      display: none;
    }
  }
  </style>