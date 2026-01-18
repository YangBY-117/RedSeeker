<template>
  <div class="app-shell">
    <header class="app-header">
      <h1>邮觅红途个性化旅游系统</h1>
      <nav class="app-nav">
        <RouterLink to="/" class="nav-link">首页</RouterLink>
        <RouterLink to="/recommend" class="nav-link">推荐行程</RouterLink>
        <RouterLink to="/route" class="nav-link">路径规划</RouterLink>
        <RouterLink to="/places" class="nav-link">场所查询</RouterLink>
        <RouterLink to="/diary" class="nav-link">旅游日记</RouterLink>
        <div v-if="isAuthenticated && user" class="user-info">
          <div 
            class="user-avatar" 
            :style="avatarStyle"
            @click="showUserMenu = !showUserMenu"
            :title="user.username"
          ></div>
          <span class="username">欢迎，{{ user?.username }}</span>
          <button @click="handleLogout" class="logout-btn">退出</button>
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
      </nav>
    </header>
    <main class="app-main">
      <RouterView />
    </main>
    
    <!-- 登录弹窗 -->
    <LoginModal v-model:visible="showLoginModal" @close="showLoginModal = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import LoginModal from './components/LoginModal.vue'
import { useAuth } from './composables/useAuth'

const router = useRouter()
const { user, isAuthenticated, logout } = useAuth()
const showLoginModal = ref(false)
const showUserMenu = ref(false)

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

// 监听未授权事件
const handleUnauthorized = () => {
  showLoginModal.value = true
}

// 监听未授权事件
onMounted(() => {
  window.addEventListener('auth:unauthorized', handleUnauthorized)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  window.removeEventListener('auth:unauthorized', handleUnauthorized)
  document.removeEventListener('click', handleClickOutside)
})

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
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  padding: 20px 40px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.98) 100%);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.08);
  border-bottom: 1px solid rgba(198, 40, 40, 0.1);
}

.app-header h1 {
  margin: 0 0 16px;
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--color-primary, #c62828) 0%, var(--color-primary-dark, #8e0000) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.app-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.nav-link {
  color: #333;
  text-decoration: none;
  font-weight: 500;
  font-size: 15px;
  padding: 10px 18px;
  border-radius: 8px;
  transition: all 0.3s ease;
  position: relative;
}

.nav-link:hover {
  color: var(--color-primary, #c62828);
  background: rgba(198, 40, 40, 0.05);
}

.nav-link.router-link-active {
  color: var(--color-primary, #c62828);
  background: rgba(198, 40, 40, 0.1);
  font-weight: 600;
}

.nav-link.router-link-active::after {
  content: '';
  position: absolute;
  bottom: 6px;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 2px;
  background: var(--color-primary, #c62828);
  border-radius: 2px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-left: auto;
  padding-left: 20px;
  border-left: 1px solid rgba(0, 0, 0, 0.1);
  position: relative;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.2s;
  flex-shrink: 0;
  border: 2px solid rgba(198, 40, 40, 0.2);
}

.user-avatar:hover {
  transform: scale(1.1);
  border-color: var(--color-primary, #c62828);
}

.user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 160px;
  z-index: 1000;
  overflow: hidden;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  color: #333;
  font-size: 14px;
}

.dropdown-item:hover {
  background: #f5f5f5;
}

.dropdown-icon {
  font-size: 16px;
}

.username {
  color: #666;
  font-size: 14px;
  font-weight: 500;
}

.logout-btn {
  padding: 8px 20px;
  border: 1px solid var(--color-primary, #c62828);
  background: transparent;
  color: var(--color-primary, #c62828);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.logout-btn:hover {
  background: var(--color-primary, #c62828);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(198, 40, 40, 0.2);
}

.app-main {
  flex: 1;
  padding: 32px;
  background: #f7f8fb url('/surface.jpg') center center / cover no-repeat fixed;
}
</style>
