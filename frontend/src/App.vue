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
        <div v-if="isAuthenticated" class="user-info">
          <span class="username">欢迎，{{ user?.username }}</span>
          <button @click="handleLogout" class="logout-btn">退出</button>
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
import { ref, onMounted, onUnmounted } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import LoginModal from './components/LoginModal.vue'
import { useAuth } from './composables/useAuth'

const { user, isAuthenticated, logout } = useAuth()
const showLoginModal = ref(false)

// 监听未授权事件
const handleUnauthorized = () => {
  showLoginModal.value = true
}

// 监听未授权事件
onMounted(() => {
  window.addEventListener('auth:unauthorized', handleUnauthorized)
})

onUnmounted(() => {
  window.removeEventListener('auth:unauthorized', handleUnauthorized)
})

// 处理登出
const handleLogout = () => {
  logout()
  showLoginModal.value = true
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
