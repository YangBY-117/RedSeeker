<template>
    <nav class="navbar">
      <div class="nav-container">
        <!-- Logo和品牌 -->
        <div class="nav-brand">
          <router-link to="/" class="brand-link">
            <span class="logo-icon">🚩</span>
            <span class="brand-name">RedSeeker</span>
            <span class="brand-tag">红色旅游系统</span>
          </router-link>
        </div>
  
        <!-- 导航菜单 -->
        <div class="nav-menu">
          <router-link to="/" class="nav-item" exact-active-class="active">
            <span class="nav-icon">🏠</span>
            <span class="nav-text">首页</span>
          </router-link>
          <router-link to="/recommend" class="nav-item" active-class="active">
            <span class="nav-icon">📍</span>
            <span class="nav-text">旅游推荐</span>
          </router-link>
          <router-link to="/route" class="nav-item" active-class="active">
            <span class="nav-icon">🗺️</span>
            <span class="nav-text">路线规划</span>
          </router-link>
          <router-link to="/places" class="nav-item" active-class="active">
            <span class="nav-icon">🔍</span>
            <span class="nav-text">场所查询</span>
          </router-link>
          <router-link to="/diary" class="nav-item" active-class="active">
            <span class="nav-icon">📝</span>
            <span class="nav-text">旅游日记</span>
          </router-link>
        </div>
  
        <!-- 用户相关操作 -->
        <div class="nav-user">
          <div v-if="isLoggedIn" class="user-info">
            <div class="user-avatar">{{ userInitial }}</div>
            <div class="user-details">
              <span class="user-name">{{ username }}</span>
              <button @click="logout" class="logout-btn">退出</button>
            </div>
          </div>
          <div v-else class="auth-buttons">
            <button @click="showLogin = true" class="btn-login">登录</button>
            <button @click="showRegister = true" class="btn-register">注册</button>
          </div>
        </div>
      </div>
    </nav>
  </template>
  
  <script setup>
  import { ref, computed } from 'vue'
  
  // 用户状态（暂时模拟，后期接入后端）
  const isLoggedIn = ref(true) // 改为false测试未登录状态
  const username = ref('杨博宇')
  
  // 计算用户头像首字母
  const userInitial = computed(() => {
    return username.value ? username.value.charAt(0) : 'U'
  })
  
  const logout = () => {
    isLoggedIn.value = false
    username.value = ''
    // 这里实际应该调用后端注销API
  }
  </script>
  
  <style scoped>
  .navbar {
    background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
    color: white;
    padding: 0 20px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    position: sticky;
    top: 0;
    z-index: 1000;
  }
  
  .nav-container {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 70px;
  }
  
  /* 品牌样式 */
  .nav-brand .brand-link {
    display: flex;
    align-items: center;
    gap: 10px;
    text-decoration: none;
    color: white;
  }
  
  .logo-icon {
    font-size: 28px;
  }
  
  .brand-name {
    font-size: 24px;
    font-weight: 700;
    letter-spacing: 1px;
  }
  
  .brand-tag {
    font-size: 14px;
    opacity: 0.9;
    margin-left: 8px;
  }
  
  /* 导航菜单 */
  .nav-menu {
    display: flex;
    gap: 2px;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 12px;
    padding: 4px;
  }
  
  .nav-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 20px;
    border-radius: 8px;
    text-decoration: none;
    color: rgba(255, 255, 255, 0.9);
    transition: all 0.3s ease;
  }
  
  .nav-item:hover {
    background: rgba(255, 255, 255, 0.15);
    color: white;
  }
  
  .nav-item.active {
    background: white;
    color: #2a5298;
    font-weight: 500;
  }
  
  .nav-icon {
    font-size: 18px;
  }
  
  .nav-text {
    font-size: 15px;
  }
  
  /* 用户区域 */
  .nav-user {
    display: flex;
    align-items: center;
  }
  
  .user-info {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  
  .user-avatar {
    width: 40px;
    height: 40px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    color: white;
  }
  
  .user-details {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  
  .user-name {
    font-weight: 500;
    font-size: 14px;
  }
  
  .logout-btn {
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.3);
    color: white;
    padding: 2px 10px;
    border-radius: 4px;
    font-size: 12px;
    cursor: pointer;
    transition: background 0.3s;
  }
  
  .logout-btn:hover {
    background: rgba(255, 255, 255, 0.2);
  }
  
  /* 登录注册按钮 */
  .auth-buttons {
    display: flex;
    gap: 12px;
  }
  
  .btn-login, .btn-register {
    padding: 8px 20px;
    border-radius: 6px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s ease;
  }
  
  .btn-login {
    background: transparent;
    border: 1px solid rgba(255, 255, 255, 0.3);
    color: white;
  }
  
  .btn-login:hover {
    background: rgba(255, 255, 255, 0.1);
  }
  
  .btn-register {
    background: white;
    border: none;
    color: #2a5298;
  }
  
  .btn-register:hover {
    background: #f8f9fa;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
  </style>