<template>
    <footer class="footer">
      <div class="footer-content">
        <!-- 品牌信息 -->
        <div class="footer-section">
          <div class="footer-brand">
            <h2 class="brand-name">邮觅红途</h2>
            <p class="brand-description">
              传承红色基因，追寻革命足迹<br>
              个性化红色文化旅游服务平台
            </p>
          </div>
          <div class="contact-info">
            <div class="contact-item">
              <span class="contact-icon">📧</span>
              <span>contact@youmihongtu.com</span>
            </div>
            <div class="contact-item">
              <span class="contact-icon">📞</span>
              <span>400-123-4567</span>
            </div>
            <div class="contact-item">
              <span class="contact-icon">📍</span>
              <span>北京市海淀区</span>
            </div>
          </div>
        </div>
  
        <!-- 快速链接 -->
        <div class="footer-section">
          <h3 class="section-title">快速导航</h3>
          <ul class="footer-links">
            <li v-for="item in quickLinks" :key="item.path">
              <router-link :to="item.path" class="footer-link">{{ item.name }}</router-link>
            </li>
          </ul>
        </div>
  
        <!-- 系统状态 -->
        <div class="footer-section">
          <h3 class="section-title">系统状态</h3>
          <div class="system-status">
            <div class="status-item">
              <span class="status-indicator online"></span>
              <span>在线用户: 156</span>
            </div>
            <div class="status-item">
              <span class="status-indicator online"></span>
              <span>服务状态: 正常</span>
            </div>
            <div class="status-item">
              <span class="status-indicator"></span>
              <span>已规划路线: 2,347</span>
            </div>
          </div>
        </div>
  
        <!-- 二维码 -->
        <div class="footer-section">
          <h3 class="section-title">关注我们</h3>
          <div class="qrcode-placeholder">
            <div class="qrcode-box">
              <div class="qrcode-image">📱</div>
              <p>微信扫码关注</p>
            </div>
          </div>
        </div>
      </div>
  
      <!-- 底部版权 -->
      <div class="footer-bottom">
        <div class="copyright">
          <p>© 2024 邮觅红途 个性化红色旅游系统 | 数据结构课程设计项目</p>
        </div>
      </div>
    </footer>
  </template>
  
  <script setup>
  const quickLinks = [
    { path: '/', name: '首页' },
    { path: '/recommend', name: '旅游推荐' },
    { path: '/route', name: '路线规划' },
    { path: '/places', name: '场所查询' },
    { path: '/diary', name: '旅游日记' },
    { path: '/about', name: '关于我们' },
    { path: '/privacy', name: '隐私政策' },
    { path: '/terms', name: '服务条款' }
  ]
  </script>
  
  <style scoped>
  .footer {
    background: linear-gradient(135deg, var(--color-secondary) 0%, #1a1a1a 100%);
    color: #e2e8f0;
    margin-top: auto;
  }
  
  .footer-content {
    max-width: 1200px;
    margin: 0 auto;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: var(--spacing-10);
    padding: var(--spacing-10) var(--spacing-6);
  }
  
  .footer-section {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-6);
  }
  
  /* 品牌信息 */
  .brand-name {
    font-size: var(--font-size-2xl);
    font-weight: 700;
    color: white;
    margin-bottom: var(--spacing-3);
    position: relative;
    display: inline-block;
  }
  
  .brand-name::after {
    content: '';
    position: absolute;
    bottom: -8px;
    left: 0;
    width: 60px;
    height: 3px;
    background: linear-gradient(90deg, var(--color-primary), var(--color-accent));
  }
  
  .brand-description {
    line-height: 1.6;
    opacity: 0.9;
    font-size: var(--font-size-sm);
  }
  
  .contact-info {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-3);
  }
  
  .contact-item {
    display: flex;
    align-items: center;
    gap: var(--spacing-3);
    font-size: var(--font-size-sm);
  }
  
  .contact-icon {
    width: 24px;
    text-align: center;
  }
  
  /* 链接区域 */
  .section-title {
    font-size: var(--font-size-lg);
    font-weight: 600;
    color: white;
    margin-bottom: var(--spacing-4);
    position: relative;
    padding-bottom: var(--spacing-2);
  }
  
  .section-title::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 40px;
    height: 2px;
    background: var(--color-primary);
  }
  
  .footer-links {
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: var(--spacing-3);
  }
  
  .footer-link {
    color: #cbd5e1;
    text-decoration: none;
    font-size: var(--font-size-sm);
    transition: all var(--transition-fast);
    display: inline-block;
  }
  
  .footer-link:hover {
    color: var(--color-primary);
    transform: translateX(5px);
  }
  
  /* 系统状态 */
  .system-status {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-4);
  }
  
  .status-item {
    display: flex;
    align-items: center;
    gap: var(--spacing-3);
    font-size: var(--font-size-sm);
  }
  
  .status-indicator {
    width: 8px;
    height: 8px;
    background: #4ade80;
    border-radius: 50%;
  }
  
  .status-indicator.online {
    background: #4ade80;
    box-shadow: 0 0 10px rgba(74, 222, 128, 0.5);
  }
  
  /* 二维码区域 */
  .qrcode-placeholder {
    display: flex;
    justify-content: center;
  }
  
  .qrcode-box {
    text-align: center;
  }
  
  .qrcode-image {
    width: 120px;
    height: 120px;
    background: white;
    border-radius: var(--radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 48px;
    margin: 0 auto var(--spacing-3);
    color: var(--color-primary);
  }
  
  .qrcode-box p {
    font-size: var(--font-size-sm);
    opacity: 0.9;
  }
  
  /* 底部版权 */
  .footer-bottom {
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    padding: var(--spacing-6) var(--spacing-6);
  }
  
  .copyright {
    max-width: 1200px;
    margin: 0 auto;
    text-align: center;
    font-size: var(--font-size-sm);
    color: #94a3b8;
  }
  
  /* 响应式设计 */
  @media (max-width: 768px) {
    .footer-content {
      grid-template-columns: 1fr;
      gap: var(--spacing-8);
      padding: var(--spacing-8) var(--spacing-4);
    }
    
    .footer-section {
      text-align: center;
    }
    
    .brand-name::after {
      left: 50%;
      transform: translateX(-50%);
    }
    
    .section-title::after {
      left: 50%;
      transform: translateX(-50%);
    }
    
    .footer-link:hover {
      transform: translateX(0);
    }
  }
  </style>