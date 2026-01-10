<template>
  <div class="app-shell">
    <header class="app-header">
      <h1>RedSeeker 个性化旅游系统</h1>
      <nav class="app-nav">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/recommend">推荐行程</RouterLink>
        <RouterLink to="/route">路径规划</RouterLink>
        <RouterLink to="/places">场所查询</RouterLink>
        <RouterLink to="/diary">旅游日记</RouterLink>
      </nav>
    </header>
    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router';
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  padding: 24px 32px;
  border-bottom: 1px solid #e6e6e6;
  background: #ffffff;
}

.app-header h1 {
  margin: 0 0 12px;
  font-size: 24px;
  color: #1f2a44;
}

.app-nav {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.app-nav a {
  color: #1f2a44;
  text-decoration: none;
  font-weight: 500;
}

.app-nav a.router-link-active {
  color: #3b82f6;
}

.app-main {
  flex: 1;
  padding: 32px;
  background: #f7f8fb;
}
</style>
